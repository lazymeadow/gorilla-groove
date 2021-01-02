package com.example.groove.services

import com.example.groove.db.dao.*
import com.example.groove.db.model.RemoteSyncable
import com.example.groove.db.model.Track
import com.example.groove.db.model.User
import com.example.groove.db.model.enums.SyncableEntityType
import com.example.groove.dto.PageResponseDTO
import com.example.groove.dto.EntityChangesDTO
import com.example.groove.dto.RemoteSyncResponseDTO
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.domain.Page

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp


@Service
class SyncableEntityService(
		private val trackRepository: TrackRepository,
		private val userRepository: UserRepository,
		private val playlistRepository: PlaylistRepository,
		private val playlistTrackRepository: PlaylistTrackRepository,
		private val reviewSourceUserRepository: ReviewSourceUserRepository
) {

	@Transactional(readOnly = true)
	fun getChangesBetweenTimestamp(
			type: SyncableEntityType,
			minimum: Timestamp,
			maximum: Timestamp,
			page: Int,
			size: Int
	): RemoteSyncResponseDTO {
		// There are some fine differences in how we query each entity.
		// Do the specific query / transformation required for each
		val entities = when (type) {
			SyncableEntityType.TRACK -> trackRepository.getTracksUpdatedBetweenTimestamp(
					userId = loadLoggedInUser().id,
					minimum = minimum,
					maximum = maximum,
					pageable = PageRequest.of(page, size)
			)

			SyncableEntityType.USER -> userRepository.getUsersUpdatedBetweenTimestamp(
					minimum = minimum,
					maximum = maximum,
					pageable = PageRequest.of(page, size)
			).map {
				SyncableUser(
						id = it.id,
						name = it.name,
						lastLogin = it.lastLogin,
						createdAt = it.createdAt,
						updatedAt = it.updatedAt,
						deleted = it.deleted
				)
			}

			SyncableEntityType.PLAYLIST -> playlistRepository.getPlaylistsUpdatedBetweenTimestamp(
					userId = loadLoggedInUser().id,
					minimum = minimum,
					maximum = maximum,
					pageable = PageRequest.of(page, size)
			)

			SyncableEntityType.PLAYLIST_TRACK -> playlistTrackRepository.getPlaylistTracksUpdatedBetweenTimestamp(
					userId = loadLoggedInUser().id,
					minimum = minimum,
					maximum = maximum,
					pageable = PageRequest.of(page, size)
			).map {
				SyncablePlaylistTrack(
						id = it.id,
						track = it.track,
						playlistId = it.playlist.id,
						createdAt = it.createdAt,
						updatedAt = it.updatedAt,
						deleted = it.deleted
				)
			}

			SyncableEntityType.REVIEW_SOURCE -> reviewSourceUserRepository.getReviewSourcesUpdatedBetweenTimestamp(
					userId = loadLoggedInUser().id,
					minimum = minimum,
					maximum = maximum,
					pageable = PageRequest.of(page, size)
			)
		}

		return getEntitiesToSync(entities, minimum)
	}

	private fun getEntitiesToSync(entities: Page<out RemoteSyncable>, minimum: Timestamp): RemoteSyncResponseDTO {
		val (deletedEntities, aliveEntities) = entities.partition { it.deleted }
		val (newEntities, modifiedEntities) = aliveEntities.partition { it.createdAt > minimum }

		return RemoteSyncResponseDTO(
				content = EntityChangesDTO(
						new = newEntities.map { it.toSyncDTO() },
						modified = modifiedEntities.map { it.toSyncDTO() },
						removed = deletedEntities.map { it.toSyncDTO().id }
				),
				pageable = PageResponseDTO(
						offset = entities.pageable.offset,
						pageNumber = entities.pageable.pageNumber,
						pageSize = entities.pageable.pageSize,
						totalPages = entities.totalPages,
						totalElements = entities.totalElements
				)
		)
	}

	fun getLastModifiedTimestamps(user: User, entityTypes: List<SyncableEntityType>): Map<SyncableEntityType, Timestamp> {
		return entityTypes.map { entityType ->
			val repository: RemoteSyncableDao = when (entityType) {
				SyncableEntityType.TRACK -> trackRepository
				SyncableEntityType.USER -> userRepository
				SyncableEntityType.PLAYLIST -> playlistRepository
				SyncableEntityType.PLAYLIST_TRACK -> playlistTrackRepository
				SyncableEntityType.REVIEW_SOURCE -> reviewSourceUserRepository
			}

			// We return 1970 if there hasn't been anything. Could return null instead but
			// it'll just make things more inconvenient for clients than returning the minimum date...
			entityType to (repository.getLastModifiedRow(user.id) ?: Timestamp(0))
		}.toMap()
	}

	companion object {
		private val logger = logger()
	}

	// User entities have a lot of sensitive info in them built in as part of spring.
	// Create a new DTO to safely strip out all this information
	data class SyncableUser(
			override val id: Long,
			val name: String,
			val lastLogin: Timestamp,
			override val createdAt: Timestamp,
			override var updatedAt: Timestamp,

			@JsonIgnore
			override var deleted: Boolean
	) : RemoteSyncable

	// Playlist DB entities currently serialize without playlist data because they are fetched via
	// playlist ID and it's redundant. But for syncing we do care about playlist information.
	// Also return the full track object here- although at present there's no reason to send the
	// full track data for syncing, when shared playlists are eventually a thing, there is no
	// guarantee the apps will have already synced the songs of other users that put stuff on the playlist
	data class SyncablePlaylistTrack(
			override val id: Long,
			val track: Track,
			val playlistId: Long,
			override val createdAt: Timestamp,
			override var updatedAt: Timestamp,

			@JsonIgnore
			override var deleted: Boolean
	) : RemoteSyncable
}
