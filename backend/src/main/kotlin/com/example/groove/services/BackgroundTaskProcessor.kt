package com.example.groove.services

import com.example.groove.db.dao.BackgroundTaskItemRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.BackgroundTaskItem
import com.example.groove.db.model.User
import com.example.groove.db.model.enums.BackgroundProcessStatus.*
import com.example.groove.db.model.enums.BackgroundProcessType
import com.example.groove.db.model.enums.BackgroundProcessType.*
import com.example.groove.dto.MetadataImportRequestDTO
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.services.review.ReviewSourceArtistService
import com.example.groove.services.socket.BackgroundTaskSocketHandler
import com.example.groove.util.DateUtils.now
import com.example.groove.util.createMapper
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import kotlin.concurrent.thread
import kotlin.reflect.KClass
import kotlin.reflect.cast


@Service
class BackgroundTaskProcessor(
		private val backgroundTaskItemRepository: BackgroundTaskItemRepository,
		private val trackService: TrackService,
		private val youtubeDownloadService: YoutubeDownloadService,
		private val backgroundTaskSocketHandler: BackgroundTaskSocketHandler,
		private val reviewSourceArtistService: ReviewSourceArtistService,
		private val trackRepository: TrackRepository
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

	fun startImport(ids: List<Long>): List<BackgroundTaskItem> {
		val user = loadLoggedInUser()

		// We do these checks again later when stuff runs, but better to do them early before we individually process
		val tracks = trackService.getTracksByIds(ids.toSet(), loadLoggedInUser())
		tracks.forEach {
			require(it.user.id != user.id) { "You cannot import your own tracks" }
		}

		return tracks.map { track ->
			val idHolder = IdHolder(track.id)

			addBackgroundTask(USER_LIBRARY_IMPORT, idHolder, track.name)
		}
	}

	fun addBackgroundTask(type: BackgroundProcessType, payload: Any, descriptionOverride: String? = null): BackgroundTaskItem {
		val currentUser = loadLoggedInUser()

		logger.info("Adding background task of type $type for user ${currentUser.name}")

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
				payload as MetadataImportRequestDTO
				"${payload.name} - ${payload.artist}"
			}
			else -> ""
		}

		val backgroundTaskItem = BackgroundTaskItem(
				originatingDevice = currentUser.currentAuthToken!!.device!!,
				status = PENDING,
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

				task.status = FAILED
				backgroundTaskItemRepository.save(task)

				backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task)
			}
		}

		synchronized(this) {
			activeUserProcesses.remove(user.id)
			logger.info("Done processing background tasks for user ${user.name}")
		}
	}

	private fun processTaskInternal(task: BackgroundTaskItem, user: User) {
		task.status = RUNNING
		backgroundTaskItemRepository.save(task)

		backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task)

		when (task.type) {
			YT_DOWNLOAD -> {
				val payload = om.readKClass(task.payload, YoutubeDownloadDTO::class)
				val track = trackService.saveFromYoutube(payload, user)

				task.status = COMPLETE
				// Now that the track is downloaded, we can update the description to be the most optimal it can be.
				// Straight YT downloads set the "description" as just the URL which isn't great. At least now if
				// a client gets the most up to date version, they'll have a good description to show the user on complete
				task.description = "${track.name} - ${track.artist}"
				backgroundTaskItemRepository.save(task)

				backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task, track.id)
			}
			NAMED_IMPORT -> {
				val metadata = om.readKClass(task.payload, MetadataImportRequestDTO::class)

				val video = youtubeDownloadService.searchYouTube(
						artist = metadata.artist,
						trackName = metadata.name,
						targetLength = metadata.length
				).firstOrNull()

				if (video == null) {
					task.status = FAILED
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
				if (metadata.addToReview) {
					val association = try {
						reviewSourceArtistService.addInactiveArtist(metadata.artistQueueName!!, user)
					} catch (e: Throwable) {
						logger.error("Could not find spotify artist for name '${metadata.artistQueueName!!}'. Deleting track and failing task")
						trackService.deleteTracks(user, listOf(track.id))

						task.status = FAILED
						backgroundTaskItemRepository.save(task)

						backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task, track.id)
						return
					}
					track.inReview = true
					track.addedToLibrary = null
					track.reviewSource = association.reviewSource
					track.updatedAt = now()
					trackRepository.save(track)
				}

				task.status = COMPLETE
				backgroundTaskItemRepository.save(task)

				backgroundTaskSocketHandler.broadcastBackgroundTaskStatus(task, track.id)
			}
			USER_LIBRARY_IMPORT -> {
				val payload = om.readKClass(task.payload, IdHolder::class)
				val track = trackService.importTrackForUser(user, payload.id)

				task.status = COMPLETE
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
				statuses = listOf(PENDING, RUNNING),
				userId = user.id
		)
	}

	@PostConstruct
	fun loadSavedPendingTasks() {
		val unfinishedTasks = backgroundTaskItemRepository.findWhereStatusIsIn(listOf(PENDING, RUNNING), userId = null)

		// If the server was interrupted while something was running, then it probably needs to be restarted
		val runningTasks = unfinishedTasks.filter { it.status == RUNNING }
		runningTasks.forEach { it.status = PENDING }
		backgroundTaskItemRepository.saveAll(runningTasks)

		// Now we have only pending tasks. Assign the tasks to the state of this service and kick off processing
		synchronized(this) {
			val idToTasks = unfinishedTasks.groupBy { it.user.id }
			idToTasks.forEach { (userId, tasksForUser) ->
				val user = tasksForUser.first().user

				userIdToTasks[userId] = tasksForUser.toMutableList()
				activeUserProcesses.add(userId)

				logger.info("User ${user.name} had ${tasksForUser.size} previously unfinished task(s). Starting to process them...")
				thread { processBackgroundTasksForUser(user) }
			}
		}
	}

	companion object {
		private val logger = logger()
	}
}

fun<T: Any> ObjectMapper.readKClass(payload: String, kClass: KClass<T>): T {
	return kClass.cast(this.readValue(payload, kClass.java))
}

data class IdHolder(val id: Long)
