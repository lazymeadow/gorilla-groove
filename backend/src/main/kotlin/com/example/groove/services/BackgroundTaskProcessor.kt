package com.example.groove.services

import com.example.groove.db.dao.BackgroundTaskItemRepository
import com.example.groove.db.model.BackgroundTaskItem
import com.example.groove.db.model.User
import com.example.groove.db.model.enums.BackgroundProcessStatus
import com.example.groove.db.model.enums.BackgroundProcessType
import com.example.groove.db.model.enums.BackgroundProcessType.*
import com.example.groove.dto.MetadataDTO
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.services.socket.BackgroundTaskSocketHandler
import com.example.groove.util.createMapper
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.reflect.KClass
import kotlin.reflect.cast


@Service
class BackgroundTaskProcessor(
		private val backgroundTaskItemRepository: BackgroundTaskItemRepository,
		private val trackService: TrackService,
		private val youtubeDownloadService: YoutubeDownloadService,
		private val backgroundTaskSocketHandler: BackgroundTaskSocketHandler
) {

	private val userIdToTasks = ConcurrentHashMap<Long, MutableList<BackgroundTaskItem>>()
	private val activeUserProcesses = ConcurrentHashMap.newKeySet<Long>()

	private val om = createMapper()

	fun addPlaylist(url: String): List<BackgroundTaskItem> {
		val videoProperties = youtubeDownloadService.getVideoPropertiesOnPlaylist(url)

		return videoProperties.map { properties ->
			val youtubeDownloadDTO = YoutubeDownloadDTO(url = YoutubeApiClient.VIDEO_URL + properties.id)

			addBackgroundTask(YT_DOWNLOAD, youtubeDownloadDTO, properties.title)
		}
	}

	fun addBackgroundTask(type: BackgroundProcessType, payload: Any, descriptionOverride: String? = null): BackgroundTaskItem {
		val currentUser = loadLoggedInUser()

		val description = descriptionOverride ?: when (type) {
			YT_DOWNLOAD -> {
				payload as YoutubeDownloadDTO
				if (payload.name != null && payload.artist != null) {
					"${payload.name} - ${payload.artist}"
				} else {
					payload.name ?: payload.url
				}
			}
			NAMED_IMPORT -> {
				payload as MetadataDTO
				"${payload.name} - ${payload.artist}"
			}
		}

		val backgroundTaskItem = BackgroundTaskItem(
				originatingDevice = currentUser.currentAuthToken!!.device!!,
				status = BackgroundProcessStatus.PENDING,
				user = currentUser,
				type = type,
				payload = om.writeValueAsString(payload),
				description = description
		)

		backgroundTaskItemRepository.save(backgroundTaskItem)

		backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(backgroundTaskItem)

		synchronized(this) {
			if (userIdToTasks[currentUser.id] == null) {
				// Adding to the end, removing from the front. Ideal LinkedList candidate
				userIdToTasks[currentUser.id] = LinkedList()
			}
		}

		val itemsForUser = userIdToTasks[currentUser.id]!!

		itemsForUser.add(backgroundTaskItem)

		// If the size == 1, it means it was empty before.
		// If the size was greater than 1, then it means that background tasks are already being processed for this user.
		// We don't need to kick off another thread
		synchronized(this) {
			if (!activeUserProcesses.contains(currentUser.id)) {
				activeUserProcesses.add(currentUser.id)

				thread {
					logger.info("Starting new background process thread for user ${currentUser.name}")
					processBackgroundTasksForUser(currentUser)
				}
			}
		}

		return backgroundTaskItem
	}

	fun processBackgroundTasksForUser(user: User) {
		val itemsForUser = userIdToTasks[user.id]!!

		while (itemsForUser.isNotEmpty()) {
			val task = itemsForUser.removeFirst()

			logger.info("Processing background task item with type ${task.type} for user ${user.name}")

			try {
				processTaskInternal(task, user)
			} catch (e: Throwable) {
				logger.error("Could not process background task with ID: ${task.id}", e)

				task.status = BackgroundProcessStatus.FAILED
				backgroundTaskItemRepository.save(task)
			}
		}

		synchronized(this) {
			activeUserProcesses.remove(user.id)
			logger.info("Done processing background tasks for user ${user.name}")
		}
	}

	private fun processTaskInternal(task: BackgroundTaskItem, user: User) {
		task.status = BackgroundProcessStatus.RUNNING
		backgroundTaskItemRepository.save(task)

		backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task)

		when (task.type) {
			YT_DOWNLOAD -> {
				val payload = om.readKClass(task.payload, YoutubeDownloadDTO::class)
				val track = trackService.saveFromYoutube(payload, user)

				task.status = BackgroundProcessStatus.COMPLETE
				backgroundTaskItemRepository.save(task)

				backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task, track.id)
			}
			NAMED_IMPORT -> {
				val metadata = om.readKClass(task.payload, MetadataDTO::class)

				val video = youtubeDownloadService.searchYouTube(
						artist = metadata.artist,
						trackName = metadata.name,
						targetLength = metadata.length
				).firstOrNull()

				if (video == null) {
					task.status = BackgroundProcessStatus.FAILED
					backgroundTaskItemRepository.save(task)

					backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task)
					return
				}

				val downloadDto = YoutubeDownloadDTO(
						url = video.videoUrl,
						name = metadata.name,
						artist = metadata.artist,
						album = metadata.album,
						trackNumber = metadata.trackNumber,
						releaseYear = metadata.releaseYear,
						artUrl = metadata.albumArtLink
				)

				val track = trackService.saveFromYoutube(downloadDto, user)

				task.status = BackgroundProcessStatus.COMPLETE
				backgroundTaskItemRepository.save(task)

				backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task, track.id)
			}
		}
	}

	fun getTasksForUserWithIds(ids: Set<Long>, user: User): List<BackgroundTaskItem> {
		val foundTasks = backgroundTaskItemRepository.findAllByIdIn(ids.toList())

		val foundIds = foundTasks.filter{ it.user.id == user.id }.map { it.id }.toSet()
		val missingTaskIds = ids - foundIds

		if (missingTaskIds.isNotEmpty()) {
			throw ResourceNotFoundException("Could not find IDs $missingTaskIds")
		}

		return foundTasks
	}

	fun getUnfinishedTasksForUser(user: User): List<BackgroundTaskItem> {
		return backgroundTaskItemRepository.findWhereStatusIsIn(
				statuses = listOf(BackgroundProcessStatus.PENDING, BackgroundProcessStatus.RUNNING),
				userId = user.id
		)
	}

	companion object {
		private val logger = logger()
	}
}

fun<T: Any> ObjectMapper.readKClass(payload: String, kClass: KClass<T>): T {
	return kClass.cast(this.readValue(payload, kClass.java))
}
