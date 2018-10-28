package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserLibraryRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.User
import com.example.groove.db.model.UserLibrary
import com.example.groove.util.unwrap
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class UserLibraryService @Autowired constructor(
		private val userLibraryRepository: UserLibraryRepository,
		private val trackRepository: TrackRepository
) {

	@Transactional
	fun addTrack(user: User, trackId: Long) {
		val track = trackRepository.findById(trackId)
				.unwrap() ?: throw IllegalArgumentException("No track found by ID $trackId!")

		val existingLibraryEntry = userLibraryRepository.findByTrackAndUser(track, user)
		if (existingLibraryEntry != null) {
			throw IllegalArgumentException("The track with ID $trackId already exists in this library")
		}

		val librariesWithTrack = userLibraryRepository.findByTrack(track)
		if (librariesWithTrack.isNotEmpty() && librariesWithTrack.all { it.hidden }) {
			logger.info("The Track with ID $trackId was attempted to be added by User ID ${user.id}. " +
					"However, this track is already owned by other users and none of them have the " +
					" track listed with public visibility.")
			throw IllegalArgumentException("No track found by ID $trackId!")
		}

		val userLibrary = UserLibrary(user = user, track = track)
		userLibraryRepository.save(userLibrary)
	}

	companion object {
		val logger = LoggerFactory.getLogger(UserLibraryService::class.java)!!
	}
}
