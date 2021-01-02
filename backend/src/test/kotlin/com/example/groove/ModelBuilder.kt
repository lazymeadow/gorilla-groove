package com.example.groove

import com.example.groove.db.model.*
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.db.model.enums.OfflineAvailabilityType
import com.example.groove.util.DateUtils.now
import java.sql.Timestamp

@Suppress("UNUSED_PARAMETER", "unused", "MemberVisibilityCanBePrivate")
object ModelBuilder {
	fun track(
			id: Long = 0L,
			user: User = user(),
			name: String = "Laser Sharks",
			artist: String = "Savant",
			featuring: String = "",
			album: String = "Laser Sharks",
			note: String? = null,
			trackNumber: Int? = 1,
			length: Int = 180,
			releaseYear: Int? = 2013,
			genre: String? = null,
			playCount: Int = 0,
			hidden: Boolean = false,
			private: Boolean = false,
			deleted: Boolean = false,
			lastPlayed: Timestamp? = null,
			addedToLibrary: Timestamp? = null,
			offlineAvailability: OfflineAvailabilityType = OfflineAvailabilityType.NORMAL,
			reviewSource: ReviewSource? = null,
			inReview: Boolean = false,
			lastReviewed: Timestamp? = null,
			originalTrack: Track? = null,
			songUpdatedAt: Timestamp = now(),
			artUpdatedAt: Timestamp = now(),
			fileName: String = "$id.ogg",
			filesizeSongOgg: Long = 1337,
			filesizeSongMp3: Long = 13370,
			filesizeArtPng: Long = 420,
			filesizeThumbnail64x64Png: Long = 42
	) = Track(
			id = id,
			user = user,
			name = name,
			artist = artist,
			featuring = featuring,
			album = album,
			trackNumber = trackNumber,
			length = length,
			releaseYear = releaseYear,
			genre = genre,
			playCount = playCount,
			hidden = hidden,
			private = private,
			lastPlayed = lastPlayed,
			addedToLibrary = addedToLibrary,
			deleted = deleted,
			note = note,
			offlineAvailability = offlineAvailability,
			reviewSource = reviewSource,
			inReview = inReview,
			lastReviewed = lastReviewed,
			originalTrack = originalTrack,
			songUpdatedAt = songUpdatedAt,
			artUpdatedAt = artUpdatedAt,
			fileName = fileName,
			filesizeSongOgg = filesizeSongOgg,
			filesizeSongMp3 = filesizeSongMp3,
			filesizeArtPng = filesizeArtPng,
			filesizeThumbnail64x64Png = filesizeThumbnail64x64Png
	)

	fun trackHistory(
			id: Long = 0,
			track: Track = track(),
			device: Device? = device(),
			ipAddress: String? = "0.0.0.0",
			deleted: Boolean = false,
			listenedInReview: Boolean = false,
			createdAt: Timestamp = now(),
			utcListenedAt: Timestamp = createdAt,
			localTimeListenedAt: String = "2020-11-16 00:56:55",
			ianaTimezone: String = "America/Boise",
			latitude: Double? = null,
			longitude: Double? = null
	) = TrackHistory(
			id = id,
			track = track,
			device = device,
			ipAddress = ipAddress,
			deleted = deleted,
			listenedInReview = listenedInReview,
			createdAt = createdAt,
			utcListenedAt = utcListenedAt,
			localTimeListenedAt = localTimeListenedAt,
			ianaTimezone = ianaTimezone,
			latitude = latitude,
			longitude = longitude
	)

	fun user(
			id: Long = 0L,
			name: String = "Billy",
			email: String = "dude@dude.dude",
			encryptedPassword: String = "${'$'}2a${'$'}10${'$'}njisMJt50QC34SpUIi9iX.UwF94zRtjaqLIMOMSoSH2GvT.HV3USm",
			lastLogin: Timestamp = now(),
			deleted: Boolean = false,
			partyDevices: MutableList<Device> = mutableListOf(),
			currentAuthToken: UserToken? = null
	) = User(
			id = id,
			name = name,
			email = email,
			encryptedPassword = encryptedPassword,
			lastLogin = lastLogin,
			deleted = deleted,
			partyDevices = partyDevices,
			currentAuthToken = currentAuthToken
	)

	fun userToken(
			id: Long = 0L,
			user: User = user(),
			device: Device = device(user = user),
			token: String = ""
	) = UserToken(
			id = id,
			user = user,
			device = device,
			token = token
	)

	fun device(
			id: Long = 0,
			user: User = user(),
			mergedDevice: Device? = null,
			mergedDevices: List<Device> = mutableListOf(),
			deviceType: DeviceType = DeviceType.WEB,
			deviceId: String = "6a3bf01a-96a9-48b0-9a8b-1199d427fc5f",
			deviceName: String = "Fertile Frodo",
			archived: Boolean = false,
			applicationVersion: String = "4.5.2-414f723",
			lastIp: String = "0.0.0.0",
			additionalData: String? = null,
			partyEnabledUntil: Timestamp? = null,
			partyUsers: MutableList<User> = mutableListOf(),
			createdAt: Timestamp = now(),
			updatedAt: Timestamp = now()
	) = Device(
			id = id,
			user = user,
			mergedDevice = mergedDevice,
			mergedDevices = mergedDevices,
			deviceType = deviceType,
			deviceId = deviceId,
			deviceName = deviceName,
			archived = archived,
			applicationVersion = applicationVersion,
			lastIp = lastIp,
			additionalData = additionalData,
			partyEnabledUntil = partyEnabledUntil,
			partyUsers = partyUsers,
			createdAt = createdAt,
			updatedAt = updatedAt
	)
}
