package com.example.groove.controllers

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserLibraryRepository
import com.example.groove.db.model.Track
import com.example.groove.services.FFmpegService
import com.example.groove.services.FileMetadataService
import com.example.groove.util.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("track")
class TrackController @Autowired constructor(
		val ffmpegService: FFmpegService,
		val fileMetadataService: FileMetadataService,
		val trackRepository: TrackRepository
) {

	// TODO this is a test endpoint. Not intended to be used forever
    @Transactional
    @GetMapping("/convert")
    fun getSup(): String {
        ffmpegService.test()
        return "donezo"
    }

	// TODO this isn't really intended to stick around. Mostly for testing
	// When song upload is put into place we no longer need this.
	@Transactional
	@PostMapping("/add-track")
	fun addTrack(@RequestBody addTrackDTO: AddTrackDTO): ResponseEntity<String> {
		val track = fileMetadataService.createTrackFromFileName(addTrackDTO.fileName)
		trackRepository.save(track)
		return ResponseEntity(HttpStatus.CREATED)
	}


	data class AddTrackDTO(
			val fileName: String
	)

}
