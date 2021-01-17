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
    fun getUnfinishedTasks(): List<BackgroundTaskItem> {
		return backgroundTaskProcessor.getUnfinishedTasksForUser(loadLoggedInUser())
    }

	@GetMapping
	fun getTasksWithIds(@RequestParam("ids") ids: Set<Long>): List<BackgroundTaskItem> {
		require(ids.isNotEmpty()) {
			"At least one ID is required to get task status"
		}
		return backgroundTaskProcessor.getTasksForUserWithIds(ids, loadLoggedInUser())
	}

	@PostMapping("/youtube-dl")
    fun enqueueYoutubeDl(@RequestBody body: YoutubeDownloadDTO): BackgroundTaskItem {
			if (body.url.contains("&list")) {
				throw IllegalArgumentException("Playlist downloads are not allowed")
			}

			return backgroundTaskProcessor.addBackgroundTask(
					type = BackgroundProcessType.YT_DOWNLOAD,
					payload = body
			)
    }

	// This is a download based off a prior request to get Metadata from probably the SearchController
	@PostMapping("/metadata-dl")
	fun enqueueNamedDl(@RequestBody body: MetadataDTO): BackgroundTaskItem {
		return backgroundTaskProcessor.addBackgroundTask(
				type = BackgroundProcessType.NAMED_IMPORT,
				payload = body
		)
	}
}
