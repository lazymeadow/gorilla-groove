package com.example.groove.services

import com.example.groove.db.dao.TrackHistoryRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.Device
import com.example.groove.db.model.Track
import com.example.groove.db.model.TrackHistory
import com.example.groove.exception.ConstraintViolationException
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.util.get
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import com.example.groove.util.toTimestamp
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant


@Service
class TrackHistoryService(
		private val trackRepository: TrackRepository,
		private val trackHistoryRepository: TrackHistoryRepository,
		private val userRepository: UserRepository
) {

	@Transactional(readOnly = true)
	fun getTrackHistory(userId: Long?, startDate: Timestamp, endDate: Timestamp): List<TrackHistory> {
		val currentUser = loadLoggedInUser()
		val targetUser = userId?.let { userRepository.get(it) }

		val loadPrivate = targetUser != null && targetUser.id == currentUser.id

		return trackHistoryRepository.findAllByUserAndTimeRange(targetUser?.id, loadPrivate, startDate, endDate)
	}

	@Transactional(readOnly = true)
	fun checkValidListeningTimestampForDevice(newPlayTime: Instant, listenedTrack: Track, device: Device) {
		val descSort = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"))
		trackHistoryRepository.findPlayHistoryNearInstantForDevice(newPlayTime.toTimestamp(), device, descSort).firstOrNull()?.let { historyBefore ->
			val beforePlayIsValid = priorPlayIsValidForTrack(
					priorPlay = historyBefore.utcListenedAt.toInstant(),
					laterPlay = newPlayTime,
					track = listenedTrack
			)
			if (!beforePlayIsValid) {
				val trackString = historyBefore.track.artist + " - " + historyBefore.track.name
				throw ConstraintViolationException("Target play time of $newPlayTime is too recent to prior play of '$trackString' at ${historyBefore.utcListenedAt.toInstant()}!")
			}
		}

		val ascSort = PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "createdAt"))
		trackHistoryRepository.findPlayHistoryNearInstantForDevice(newPlayTime.toTimestamp(), device, ascSort).firstOrNull()?.let { historyAfter ->
			val afterPlayIsValid = priorPlayIsValidForTrack(
					priorPlay = newPlayTime,
					laterPlay = historyAfter.utcListenedAt.toInstant(),
					track = historyAfter.track
			)
			if (!afterPlayIsValid) {
				val trackString = historyAfter.track.artist + " - " + historyAfter.track.name
				throw ConstraintViolationException("Target play time of $newPlayTime is too near the next play of '$trackString' at ${historyAfter.utcListenedAt.toInstant()}!")
			}
		}
	}

	private fun priorPlayIsValidForTrack(priorPlay: Instant, laterPlay: Instant, track: Track): Boolean {
		// Clients should be requiring that 60% of a song is listened to in order for it to be "listened" to. So if the
		// previous play for this device was not 60% of the track's time from the new play time, then it means that the
		// play couldn't have happened under the rules established. I use (50% -2) here to give some wiggle room.
		// We don't take into account the duration of the last song, as it could be skipped immediately after the 60%
		// play mark and this listen would still be valid.
		val requiredTrackLength = (track.length * 0.50) - 2

		// If someone has a song in their library that is hella short, then just disable this check. The potential timing
		// issues with validating a song that is like 6 seconds long don't seem worth it.
		if (requiredTrackLength < 2) {
			return true
		}

		// TODO this does not take into account a play that is sent in the past too near a time in the future.
		// e.g. There are listens at 6:00 and 6:10. A 3 minute song is listened to in the past at 6:09.
		// It would be valid, even though 6:10 is too near 6:09 (assuming the 6:10 song isn't hella short)
		// I consider this to be OK right now, because this is mostly to guard against async issues with clients.

		return priorPlay
				.plusSeconds(requiredTrackLength.toLong())
				.isBefore(laterPlay)
	}

	@Transactional
	fun deleteTrackHistory(id: Long) {
		val trackHistory = trackHistoryRepository.get(id)
		if (trackHistory == null || trackHistory.deleted || trackHistory.track.user.id != loadLoggedInUser().id) {
			throw ResourceNotFoundException("No track history found with ID $id")
		}

		trackHistory.deleted = true

		trackHistoryRepository.save(trackHistory)

		// It's entirely possible that this track update stuff gets out of sync if someone listens to the same song
		// while we are in the middle of doing this.... but it's real unlikely and it's nothing that can't be fixed
		val track = trackHistory.track
		val mostRecentHistory = trackHistoryRepository.findMostRecentHistoryForTrack(track, PageRequest.of(0, 1))

		track.lastPlayed = mostRecentHistory.firstOrNull()?.createdAt
		track.playCount = track.playCount - 1

		trackRepository.save(track)
	}

	companion object {
		val logger = logger()
	}
}
