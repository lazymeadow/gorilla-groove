package com.example.groove.controllers

import com.example.groove.db.dao.TrackRepository
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
import java.sql.Timestamp
import java.util.*

@RestController
@RequestMapping("track")
class TrackController(
		@Autowired val ffmpegService: FFmpegService,
		@Autowired val fileMetadataService: FileMetadataService,
		@Autowired val trackRepository: TrackRepository
) {

	//example: http://localhost:8080/track?page=0&size=1&sort=name,asc
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
	@PostMapping("/mark-track-as-listened-to")
	fun markTrackAsListenedTo(@RequestBody markTrackAsReadDTO: MarkTrackAsListenedToDTO): ResponseEntity<String> {
		val track = trackRepository.findById(markTrackAsReadDTO.trackId)
				.unwrap() ?: throw IllegalArgumentException("No track found by ID ${markTrackAsReadDTO.trackId}!")

		// May want to do some sanity checks / server side validation here to prevent this incrementing too often.
		// We know the last played date of a track and can see if it's even possible to have listened to this song again
		// FIXME need to get the current user, grab this from the UserLibrary, and increment it
//		track.playCount++
//		track.lastPlayed = Timestamp(Date().time)

		return ResponseEntity(HttpStatus.OK)
	}

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

	data class MarkTrackAsListenedToDTO(
			val trackId: Long
	)
}
