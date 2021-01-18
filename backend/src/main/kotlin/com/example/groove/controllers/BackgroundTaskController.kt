package com.example.groove.controllers

import com.example.groove.db.model.BackgroundTaskItem
import com.example.groove.db.model.enums.BackgroundProcessType
import com.example.groove.dto.MetadataDTO
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.services.BackgroundTaskProcessor
import com.example.groove.util.loadLoggedInUser

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/background-task")
class BackgroundTaskController(
		private val backgroundTaskProcessor: BackgroundTaskProcessor
) {

	@GetMapping("/unfinished")
    fun getUnfinishedTasks(): BackgroundTaskResponse {
		return BackgroundTaskResponse(
				items = backgroundTaskProcessor.getUnfinishedTasksForUser(loadLoggedInUser())
		)
    }

	@GetMapping
	fun getTasksWithIds(@RequestParam("ids") ids: Set<Long>): BackgroundTaskResponse {
		require(ids.isNotEmpty()) {
			"At least one ID is required to get task status"
		}
		return BackgroundTaskResponse(
				items = backgroundTaskProcessor.getTasksForUserWithIds(ids, loadLoggedInUser())
		)
	}

	@PostMapping("/youtube-dl")
    fun enqueueYoutubeDl(@RequestBody body: YoutubeDownloadDTO): BackgroundTaskResponse {
		val tasks = if (body.url.contains("&list")) {
			backgroundTaskProcessor.addPlaylist(body.url)
		} else {
			listOf(backgroundTaskProcessor.addBackgroundTask(type = BackgroundProcessType.YT_DOWNLOAD, payload = body))
		}

		return BackgroundTaskResponse(items = tasks)
    }

	// This is a download based off a prior request to get Metadata from probably the SearchController
	@PostMapping("/metadata-dl")
	fun enqueueNamedDl(@RequestBody body: MetadataDTO): BackgroundTaskResponse {
		val task = backgroundTaskProcessor.addBackgroundTask(
				type = BackgroundProcessType.NAMED_IMPORT,
				payload = body
		)

		return BackgroundTaskResponse(items = listOf(task))
	}

	data class BackgroundTaskResponse(val items: List<BackgroundTaskItem>)
}
