package com.example.groove.services.socket

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.Device
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.security.SecurityConfiguration
import com.example.groove.services.DeviceService
import com.example.groove.util.*
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

@Configuration
@EnableWebSocket
class WebSocket(
		val userRepository: UserRepository,
		val trackRepository: TrackRepository,
		val deviceService: DeviceService,
		val nowListeningSocketHandler: NowListeningSocketHandler,
		val remotePlaySocketHandler: RemotePlaySocketHandler
) : WebSocketConfigurer {

	private val objectMapper = createMapper()
	val sessions = ConcurrentHashMap<String, WebSocketSession>()

	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry
				.addHandler(SocketTextHandler(), "/api/socket")
				.setAllowedOrigins(*SecurityConfiguration.allowedOrigins)
				.setHandshakeHandler(object : DefaultHandshakeHandler() {
					override fun determineUser(request: ServerHttpRequest, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Principal? {
						// I know it's confusing that "name" is "email", that's because the email is unique, so that's what
						// is assigned to the name in the principal
						val email = request.principal!!.name

						// We can now look up this user based off the unique email in order to get the ID associated with them
						val user = userRepository.findByEmail(email)
								?: throw IllegalStateException("No user found with the email $email!")

						// Now throw it in the extra attributes so we can find it more readily as this user uses this session
						attributes["userId"] = user.id

						return request.principal
					}
				})
				.addInterceptors(object : HandshakeInterceptor {
					override fun beforeHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Boolean {
						// New user logins will have the device ID associated to the principle and don't need to be passed in.
						// Everything in the "run" is temporary and can be deleted in the future when all clients have adapted to the new model
						val deviceIdentifier = loadLoggedInUser().currentAuthToken!!.device?.deviceId ?: run {
							// I think this is temporary. I'd like to save the deviceId with the logged in user's principal going forward, actually
							val paramParts = request.uri.query?.split("=")
							if (paramParts.isNullOrEmpty() || paramParts.size != 2) {
								response.setStatusCode(HttpStatus.BAD_REQUEST)
								return false
							}

							val (paramKey, paramValue) = paramParts
							if (paramKey != "deviceIdentifier") {
								response.setStatusCode(HttpStatus.BAD_REQUEST)
								return false
							}

							paramValue
						}

						val activeDevice = deviceService.getDeviceById(deviceIdentifier)

						attributes["deviceIdentifier"] = deviceIdentifier
						attributes["deviceType"] = activeDevice.deviceType
						return true
					}

					override fun afterHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, exception: Exception?) {}
				})
	}

	@Transactional(readOnly = true)
	fun getActiveDevices(excludingDeviceId: String?): List<Device> {
		// Load the user in this transaction so we can load its partyDevices.
		// If we try to access the devices otherwise we'll get a LazyInitializationException
		val user = userRepository.get(loadLoggedInUser().id)!!

		// Grab all of our own active devices (including our current one) and then add in all
		// devices we have access to via Party Mode
		val currentDevice = excludingDeviceId?.let { deviceService.getDeviceById(it) }

		val ownDevices = sessions.values
				.filter { it.userId == user.id && it.deviceIdentifier != currentDevice?.deviceId }
				.map { deviceService.getDeviceById(it.deviceIdentifier) }

		val now = DateUtils.now()

		val partyDevices = user.partyDevices.filter {
			it.partyEnabledUntil != null && it.partyEnabledUntil!! > now
		}

		// Finally, remove devices from our list if they have not been seen polling
		return ownDevices + partyDevices
	}

	inner class SocketTextHandler : TextWebSocketHandler() {

		override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
			logger.debug("Received message from user ${session.userId} session ${session.id}")
			val clientMessage = try {
				objectMapper.readValue(message.payload, WebSocketMessage::class.java)
			} catch (e: Exception) {
				logger.error("Could not deserialize WebSocket message! Message: $message", e)
				return
			}

			when (clientMessage) {
				is NowListeningRequest -> nowListeningSocketHandler.handleMessage(session, clientMessage)
				is RemotePlayRequest -> remotePlaySocketHandler.handleMessage(session, clientMessage)
				else -> throw IllegalArgumentException("Incorrect message type!")
			}
		}

		override fun afterConnectionEstablished(session: WebSocketSession) {
			logger.debug("New user with ID: ${session.userId} connected to socket with ID: ${session.id}")
			sessions[session.id] = session

			// This is mostly here for debugging. But it's a nice easter egg too if anyone finds it
			session.sendIfOpen(ConnectionEstablishedResponse(message = connectionMessages.random()))

			// Tell this new user about all the things being listened to
			nowListeningSocketHandler.sendAllListensToSession(session)
		}

		override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
			logger.debug("User with ID: ${session.userId} disconnected from socket with ID: ${session.id}")

			sessions.remove(session.id)
			nowListeningSocketHandler.removeSession(session)
		}
	}

	fun sessionsFor(userId: Long?, deviceType: DeviceType?): List<WebSocketSession> {
		return sessions.values
				.filter { userId == null || it.userId == userId }
				.filter { deviceType == null || it.deviceType == deviceType }
	}

	companion object {
		val logger = logger()
	}
}

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "messageType",
		visible = true
)
@JsonSubTypes(
		JsonSubTypes.Type(value = NowListeningRequest::class, name = "NOW_PLAYING"),
		JsonSubTypes.Type(value = RemotePlayRequest::class, name = "REMOTE_PLAY")
)
interface WebSocketMessage {
	val messageType: EventType
}

interface SocketHandler<T> {
	fun handleMessage(session: WebSocketSession, data: T)
}

enum class EventType {
	NOW_PLAYING, REMOTE_PLAY, REVIEW_QUEUE, CONNECTION_ESTABLISHED
}

inline fun <reified T : Any> T.merge(other: T?): T {
	if (other == null) {
		return this
	}

	val propertiesByName = T::class.declaredMemberProperties.associateBy { it.name }
	val primaryConstructor = T::class.primaryConstructor
			?: throw IllegalArgumentException("merge type must have a primary constructor")
	val args = primaryConstructor.parameters.associateWith { parameter ->
		val property = propertiesByName[parameter.name]
				?: throw IllegalStateException("no declared member property found with name '${parameter.name}'")
		(property.get(this) ?: property.get(other))
	}
	return primaryConstructor.callBy(args)
}

data class ConnectionEstablishedResponse(
		override val messageType: EventType = EventType.CONNECTION_ESTABLISHED,
		val message: String
) : WebSocketMessage

private val connectionMessages = setOf(
		"Hey. Glad you're here",
		"It's a good time to Groove",
		"Whoa, you found me",
		"Your sound card works perfectly"
)
