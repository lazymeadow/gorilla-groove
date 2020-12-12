package com.example.groove.service

import com.example.groove.ModelBuilder.device
import com.example.groove.ModelBuilder.track
import com.example.groove.ModelBuilder.trackHistory
import com.example.groove.db.dao.TrackHistoryRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.Track
import com.example.groove.db.model.TrackHistory
import com.example.groove.exception.ConstraintViolationException
import com.example.groove.services.TrackHistoryService
import com.example.groove.util.toTimestamp
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class TrackHistoryServiceTest {

	@Test
	fun `isValidListeningTimestampForDevice throws when a prior play was too near the target play`() {
		val newListenTime = Instant.ofEpochMilli(1_606_591_312_000)

		val listenedTrack = track(length = 60)
		val lastListenTime = newListenTime.minusSeconds(listenedTrack.length / 3L)

		val lastListen = trackHistory(utcListenedAt = lastListenTime.toTimestamp())

		testValidSongListen(
				newListenTime = newListenTime,
				newListenedTrack = listenedTrack,
				priorListenedTrackHistory = lastListen,
				shouldThrow = true
		)
	}

	@Test
	fun `isValidListeningTimestampForDevice does not throw when enough time has passed between current and prior plays`() {
		val newListenTime = Instant.ofEpochMilli(1_606_591_312_000)

		val listenedTrack = track(length = 60)
		val lastListenTime = newListenTime.minusSeconds(listenedTrack.length - 1L)

		val lastListen = trackHistory(utcListenedAt = lastListenTime.toTimestamp())

		testValidSongListen(
				newListenTime = newListenTime,
				newListenedTrack = listenedTrack,
				priorListenedTrackHistory = lastListen
		)
	}

	@Test
	fun `isValidListeningTimestampForDevice throws when not enough time has passed between current and future plays`() {
		val newListenTime = Instant.ofEpochMilli(1_606_591_312_000)

		val nextListenedTrack = track(length = 60)
		val nextListenTime = newListenTime.plusSeconds(nextListenedTrack.length / 3L)

		val nextListen = trackHistory(track = nextListenedTrack, utcListenedAt = nextListenTime.toTimestamp())

		testValidSongListen(
				newListenTime = newListenTime,
				nextListenedTrackHistory = nextListen,
				shouldThrow = true
		)
	}

	@Test
	fun `isValidListeningTimestampForDevice does not throw when enough time has passed between current and future plays`() {
		val newListenTime = Instant.ofEpochMilli(1_606_591_312_000)

		val nextListenedTrack = track(length = 60)
		val nextListenTime = newListenTime.plusSeconds(nextListenedTrack.length + 1L)

		val nextListen = trackHistory(track = nextListenedTrack, utcListenedAt = nextListenTime.toTimestamp())

		testValidSongListen(
				newListenTime = newListenTime,
				nextListenedTrackHistory = nextListen
		)
	}

	private fun testValidSongListen(
			newListenTime: Instant,
			newListenedTrack: Track = track(),
			priorListenedTrackHistory: TrackHistory? = null,
			nextListenedTrackHistory: TrackHistory? = null,
			shouldThrow: Boolean = false
	) {
		val trackHistoryRepository: TrackHistoryRepository = mockk()
		every { trackHistoryRepository.findPlayHistoryNearInstantForDevice(any(), "DESC", any()) } returns priorListenedTrackHistory
		every { trackHistoryRepository.findPlayHistoryNearInstantForDevice(any(), "ASC", any()) } returns nextListenedTrackHistory

		val service = createService(trackHistoryRepository = trackHistoryRepository)

		if (shouldThrow) {
			assertThrows<ConstraintViolationException> {
				service.checkValidListeningTimestampForDevice(newListenTime, newListenedTrack, device())
			}
		} else {
			service.checkValidListeningTimestampForDevice(newListenTime, newListenedTrack, device())

			// Lack of thrown exception verifies test run
		}
	}

	private fun createService(
			trackHistoryRepository: TrackHistoryRepository = mockk(),
			trackRepository: TrackRepository = mockk(),
			userRepository: UserRepository = mockk()
	) = TrackHistoryService(
			trackHistoryRepository = trackHistoryRepository,
			trackRepository = trackRepository,
			userRepository = userRepository
	)
}