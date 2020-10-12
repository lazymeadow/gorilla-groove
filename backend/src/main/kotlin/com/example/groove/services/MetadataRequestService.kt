package com.example.groove.services

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.dto.MetadataUpdateRequestDTO
import com.example.groove.services.enums.MetadataOverrideType
import com.example.groove.util.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO

@Service
class MetadataRequestService(
		private val trackRepository: TrackRepository,
		private val spotifyApiClient: SpotifyApiClient,
		private val fileStorageService: FileStorageService,
		private val fileUtils: FileUtils,
		private val songIngestionService: SongIngestionService,
		private val trackLinkRepository: TrackLinkRepository
) {

	@Transactional
	fun requestTrackMetadata(request: MetadataUpdateRequestDTO): Pair<List<Track>, List<Long>> {
		val trackIds = request.trackIds

		val updatedSuccessTracks = findUpdatableTracks(trackIds).map { (track, metadataResponse) ->
			if (track.album.shouldBeUpdated(request.changeAlbum)) {
				track.album = metadataResponse.album
			}
			if (track.genre.shouldBeUpdated(request.changeGenre)) {
				track.genre = metadataResponse.genre
			}

			// We only want to update this info if we are actually using the same album as the response
			if (track.album.toLowerCase() == metadataResponse.album.toLowerCase()) {
				if (track.releaseYear.shouldBeUpdated(request.changeReleaseYear)) {
					track.releaseYear = metadataResponse.releaseYear
				}
				if (track.trackNumber.shouldBeUpdated(request.changeTrackNumber)) {
					track.trackNumber = metadataResponse.trackNumber
				}
				saveAlbumArt(track, metadataResponse.albumArtUrl, request.changeAlbumArt)
			}

			trackRepository.save(track)
		}

		val successfulIds = updatedSuccessTracks.map { it.id }.toSet()
		val failedIds = trackIds - successfulIds

		return Pair(updatedSuccessTracks, failedIds)
	}

	private fun Any?.shouldBeUpdated(metadataOverrideType: MetadataOverrideType): Boolean {
		return when (metadataOverrideType) {
			MetadataOverrideType.NEVER -> false
			MetadataOverrideType.ALWAYS -> true
			MetadataOverrideType.IF_EMPTY -> if (this is String) {
				isNullOrBlank()
			} else {
				this == null
			}
		}
	}

	private fun saveAlbumArt(track: Track, newAlbumArtUrl: String, overrideType: MetadataOverrideType) {
		if (overrideType == MetadataOverrideType.NEVER) {
			return
		}

		if (overrideType == MetadataOverrideType.IF_EMPTY) {
			fileStorageService.loadAlbumArt(track.id, ArtSize.LARGE)?.run {
				logger.info("Updating album art conditionally and existing album art was found for track ID ${track.id}. " +
						"Skipping album art save")
				return
			}
		}

		val artImage = try {
			ImageIO.read(URL(newAlbumArtUrl))
		} catch (e: IOException) {
			logger.error("Failed to read in image URL! $newAlbumArtUrl")
			return
		}

		val file = fileUtils.createTemporaryFile(".png")
		artImage.writeToFile(file, "png")
		logger.info("Writing new album art for track $track to storage...")
		songIngestionService.storeAlbumArtForTrack(file, track, false)
		file.delete()

		trackLinkRepository.forceExpireLinksByTrackId(track.id)
	}

	private fun findUpdatableTracks(trackIds: List<Long>): List<Pair<Track, MetadataResponseDTO>> {
		val currentUser = loadLoggedInUser()

		val validTracks = trackIds
				.map { trackRepository.get(it) }
				.filterNot { track ->
					track == null
							|| track.user.id != currentUser.id
							|| track.artist.isBlank()
							|| track.name.isBlank()
				}

		return validTracks
				.map {
					val metadataResponse = spotifyApiClient.getMetadataByTrackArtistAndName(
							artist = it!!.artist,
							name = it.name
					)
					Pair(it, metadataResponse)
				}.filter { (_, metadataResponse) -> metadataResponse != null }
				.map { Pair(it.first, it.second!!)}
	}

	companion object {
		private val logger = logger()
	}
}
