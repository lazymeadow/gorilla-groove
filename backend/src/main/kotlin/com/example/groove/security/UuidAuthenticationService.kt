package com.example.groove.security

import com.example.groove.controllers.UserAuthenticationDTO
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.dao.UserTokenRepository
import com.example.groove.db.model.User
import com.example.groove.db.model.UserToken
import com.example.groove.services.DeviceService
import com.example.groove.util.logger
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Suppress("unused")
@Service
class UuidAuthenticationService(
		private val userRepository: UserRepository,
		private val userTokenRepository: UserTokenRepository,
		private val deviceService: DeviceService
) : UserAuthenticationService {

	@Transactional
	override fun login(authDTO: UserAuthenticationDTO, ipAddress: String?): String {
		val email = authDTO.email
		val user = userRepository.findByEmail(email)
		if (user == null) {
			logger.info("A user attempted to log in with the email address $email but it was not found")
			throw BadCredentialsException("No user found with the email $email")
		}

		if (!BCrypt.checkpw(authDTO.password, user.encryptedPassword)) {
			logger.info("A user attempted to log in to the email address $email but provided the wrong password")
			throw BadCredentialsException("The password provided was invalid")
		}

		val loggedInDevice = authDTO.deviceId?.let {
			deviceService.createOrUpdateDevice(
					user = user,
					deviceId = it,
					deviceType = authDTO.deviceType!!,
					preferredDeviceName = authDTO.preferredDeviceName,
					version = authDTO.version!!,
					ipAddress = ipAddress,
					additionalData = null
			)
		}

		val uuid = UUID.randomUUID().toString()

		// Now that the user has created a token, save it for later so that we can authenticate against it
		val userToken = UserToken(user = user, token = uuid, device = loggedInDevice)
		userTokenRepository.save(userToken)

		return uuid
	}

	@Transactional
	override fun logout(user: User) {
		val token = user.currentAuthToken!!.token
		userTokenRepository.deleteByToken(token)
	}

	@Transactional(readOnly = true)
	override fun findByToken(token: String): User {
		val dbToken = userTokenRepository.findByToken(token)
				?: throw BadCredentialsException("User not found with the provided token $token")
		dbToken.user.currentAuthToken = dbToken
		return dbToken.user
	}

	companion object {
		val logger = logger()
	}
}