package com.example.groove.controllers

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.services.FFmpegService
import com.example.groove.services.FileMetadataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("track")
class TrackController(
		@Autowired val ffmpegService: FFmpegService,
		@Autowired val fileMetadataService: FileMetadataService,
		@Autowired val trackRepository: TrackRepository
) {

    @Transactional(readOnly = true)
	@GetMapping
    fun getTracks(@RequestParam(value = "name") name: String): List<Track> {
		return trackRepository.findTracks(name)
    }

    @Transactional
    @GetMapping("/convert")
    fun getSup(): String {
        ffmpegService.test()
        return "donezo"
    }

	@Transactional
	@PostMapping("/add-track")
	fun addTrack(@RequestBody addTrackDTO: AddTrackDTO): ResponseEntity<String> {
		val track = fileMetadataService.createTrackFromFileName(addTrackDTO.fileName)
		trackRepository.save(track)
		return ResponseEntity(HttpStatus.CREATED)
	}


    @GetMapping("/metadata")
    fun someTest(
			@RequestParam(value = "fileName") fileName: String
	): Track {
        return fileMetadataService.createTrackFromFileName(fileName)
    }

	data class AddTrackDTO(
			val fileName: String
	)
}
