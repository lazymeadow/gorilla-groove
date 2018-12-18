package com.example.groove.services

import com.example.groove.db.dao.UserLibraryHistoryRepository
import com.example.groove.db.dao.UserLibraryRepository
import com.example.groove.db.model.UserLibrary
import com.example.groove.db.model.UserLibraryHistory
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.slf4j.LoggerFactory

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.util.*


@Service
class UserLibraryService(
		private val userLibraryRepository: UserLibraryRepository,
		private val userLibraryHistoryRepository: UserLibraryHistoryRepository
) {

	@Transactional
	fun getUserLibrary(
			name: String?,
			artist: String?,
			album: String?,
			userId: Long?,
			pageable: Pageable
	): Page<UserLibrary> {
		val loggedInId = loadLoggedInUser().id
		val idToLoad = userId ?: loggedInId
		val loadHidden = loggedInId == idToLoad

		return userLibraryRepository.getLibrary(name, artist, album, idToLoad, loadHidden, pageable)
	}

	fun markSongListenedTo(userLibraryId: Long) {
		val userLibrary = userLibraryRepository.findById(userLibraryId).unwrap()

		if (userLibrary == null || userLibrary.user != loadLoggedInUser()) {
			throw IllegalArgumentException("No user library found by ID $userLibraryId!")
		}

		// May want to do some sanity checks / server side validation here to prevent this incrementing too often.
		// We know the last played date of a track and can see if it's even possible to have listened to this song
		userLibrary.playCount++
		userLibrary.lastPlayed = Timestamp(Date().time)

		val userLibraryHistory = UserLibraryHistory(userLibrary = userLibrary)
		userLibraryHistoryRepository.save(userLibraryHistory)
	}

	// I think this should be reworked to be "clone track" or "fork track" or something
	/*
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
	*/

	companion object {
		val logger = LoggerFactory.getLogger(UserLibraryService::class.java)!!
	}
}
