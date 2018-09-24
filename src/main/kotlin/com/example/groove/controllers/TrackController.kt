package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.services.FFmpegService
import com.example.groove.services.FileMetadataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("track")
class TrackController(
		@Autowired val ffmpegService: FFmpegService,
		@Autowired val fileMetadataService: FileMetadataService
) {

    @Transactional
    @GetMapping("/convert")
    fun getSup(): String {
        ffmpegService.test()
        return "donezo"
    }

	@GetMapping("/greeting")
	fun greeting(@RequestParam(value = "name", defaultValue = "World") name: String) =
			"Hello, $name"


    @GetMapping("/metadata")
    fun someTest(
			@RequestParam(value = "fileName") fileName: String
	): Track {
        return fileMetadataService.createTrackFromFileName(fileName)
    }

}
