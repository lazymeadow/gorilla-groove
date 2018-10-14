package com.example.groove.controllers

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.services.FFmpegService
import com.example.groove.services.FileMetadataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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

	//http://localhost:8080/track?page=0&size=1&sort=name,asc
    @Transactional(readOnly = true)
	@GetMapping
    fun getTracks(
			@RequestParam(value = "name") name: String?,
			@RequestParam(value = "artist") artist: String?,
			@RequestParam(value = "album") album: String?,
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<Track> {
		return trackRepository.findTracks(name, artist, album, pageable)
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
