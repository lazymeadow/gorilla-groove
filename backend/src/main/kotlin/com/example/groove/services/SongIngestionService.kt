package com.example.groove.services

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.User
import com.example.groove.db.model.Track
import com.example.groove.exception.FileStorageException
import com.example.groove.properties.FileStorageProperties
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import com.example.groove.util.withNewExtension
import com.example.groove.util.withoutExtension
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import javax.imageio.ImageIO

@Service
class SongIngestionService(
		fileStorageProperties: FileStorageProperties,
		private val ffmpegService: FFmpegService,
		private val fileMetadataService: FileMetadataService,
		private val trackRepository: TrackRepository,
		private val trackLinkRepository: TrackLinkRepository,
		private val fileStorageService: FileStorageService,
		private val imageService: ImageService
) {

	private val fileStorageLocation: Path = Paths.get(fileStorageProperties.tmpDir!!)
			.toAbsolutePath().normalize()

	init {
		try {
			Files.createDirectories(this.fileStorageLocation)
		} catch (ex: Exception) {
			throw FileStorageException("Could not create the directory where the uploaded files will be stored.", ex)
		}
	}

	private fun storeMultipartFile(file: MultipartFile): String {
		// Discard the file's original name; but keep the extension
		val extension = file.originalFilename!!.split(".").last()
		val fileName = UUID.randomUUID().toString() + "." + extension

		// Copy the song to a temporary location for further processing
		val targetLocation = fileStorageLocation.resolve(fileName)
		Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

		return fileName
	}

	fun storeSongForUser(songFile: MultipartFile, user: User): Track {
		logger.info("Storing song ${songFile.originalFilename} for user: ${user.name}")

		val fileName = storeMultipartFile(songFile)
		val tmpImageFile = ripAndSaveAlbumArt(fileName)

		val track = convertAndSaveTrackForUser(fileName, user, songFile.originalFilename!!)

		if (tmpImageFile != null) {
			fileStorageService.storeAlbumArt(tmpImageFile, track.id)
			// We have stored the songFile in its permanent home. We can delete this tmp songFile
			tmpImageFile.delete()
		}

		return track
	}

	fun storeAlbumArtForTrack(albumArt: MultipartFile, track: Track, cropImageToSquare: Boolean) {
		logger.info("Storing album artwork ${albumArt.originalFilename} for track ID: ${track.id}")
		val fileName = storeMultipartFile(albumArt)

		val imageFile = fileStorageLocation.resolve(fileName).toFile()
		if (!imageFile.exists()) {
			throw IllegalStateException("Could not store album art for track ID: ${track.id}")
		}

		storeAlbumArtForTrack(imageFile, track, cropImageToSquare)

		imageFile.delete()
	}

	fun storeAlbumArtForTrack(albumArt: File, track: Track, cropImageToSquare: Boolean) {
		if (!cropImageToSquare) {
			logger.info("Storing album art track ID: ${track.id} unaltered")
			fileStorageService.storeAlbumArt(albumArt, track.id)
			return
		}

		logger.info("Beginning album art crop for track ID: ${track.id}")
		// Crop the image and save the cropped file
		val croppedImage = imageService.cropToSquare(albumArt)

		fileStorageService.storeAlbumArt(croppedImage, track.id)
		logger.info("Cropped album art was stored for track ID: ${track.id}")

		croppedImage.delete()
	}

	// It's important to rip the album art out PRIOR to running the song
	// through FFmpeg to be converted to an .ogg. If you don't, you will
	// get the error "Cannot find comment block (no vorbiscomment header)"
	private fun ripAndSaveAlbumArt(fileName: String): File? {
		val image = fileMetadataService.removeAlbumArtFromFile(fileName)
		return if (image != null) {
			val tmpImageName = UUID.randomUUID().toString() + ".png"
			val outputFile = File(fileStorageLocation.toString() + tmpImageName)
			ImageIO.write(image, "png", outputFile)

			outputFile
		} else {
			null
		}
	}

	@Transactional
	fun convertAndSaveTrackForUser(fileName: String, user: User, originalFileName: String): Track {
		logger.info("Saving file $fileName for user ${user.name}")

		val oggFile = ffmpegService.convertTrack(fileName, AudioFormat.OGG)
		// iOS does not support OGG playback. So at least for now, we have to store everything in both formats...
		val mp3File = ffmpegService.convertTrack(fileName, AudioFormat.MP3)

		// add the track to database
		val track = fileMetadataService.createTrackFromSongFile(oggFile, user, originalFileName)
		trackRepository.save(track)
		track.fileName = "${track.id}.ogg"

		fileStorageService.storeSong(oggFile, track.id, AudioFormat.OGG)
		fileStorageService.storeSong(mp3File, track.id, AudioFormat.MP3)

		// We have stored the file in S3, (or copied it to its final home)
		// We no longer need these files and can clean it up to save space
		oggFile.delete()
		mp3File.delete()

		return track
	}

	@Transactional
	fun trimSong(track: Track, startTime: String?, duration: String?): Int {
		renameSharedTracks(track)

		val oggFile = fileStorageService.loadSong(track, AudioFormat.OGG)

		val trimmedSong = ffmpegService.trimSong(oggFile, startTime, duration)

		fileStorageService.storeSong(trimmedSong, track.id, AudioFormat.OGG)

		val newLength = fileMetadataService.getTrackLength(trimmedSong)
		// The new file we save is always specific to this song, so name it with the song's ID as we do
		// with any song file names that aren't borrowed / shared
		val newFileName = "${track.id}.ogg"
		if (newFileName != track.fileName) {
			trackLinkRepository.forceExpireLinksByTrackId(track.id)
			track.fileName = newFileName
		}

		// It might make more sense to download the mp3 and trim it instead of re-converting the OGG to mp3
		// Maybe less loss in quality? But hard to say. Would require experimentation. At least this way we
		// save ourselves a trip to s3 and only download a single file
		val mp3File = ffmpegService.convertTrack(oggFile.name, AudioFormat.MP3)
		fileStorageService.storeSong(mp3File, track.id, AudioFormat.MP3)

		oggFile.delete()
		mp3File.delete()
		trimmedSong.delete()

		return newLength
	}

	// Other users could be sharing this track. We don't want to let another user trim the track for all users
	// Make sure all the tracks sharing the filename of this track are no longer using it
	private fun renameSharedTracks(track: Track) {
		// If the track we are trimming has a different ID than its current name, then we don't have to do anything.
		// We will be giving this track a new file using its ID as its name, and all existing shared tracks can continue
		// with their life as is
		if (track.fileName.withoutExtension().toLong() != track.id) {
			return
		}

		val otherTracksUsingFile = trackRepository.findAllByFileName(track.fileName)
				.filter { it.id != track.id }
		// Nothing is sharing this file name. We don't have to worry
		if (otherTracksUsingFile.isEmpty()) {
			return
		}

		// We have at least one other song sharing the file with this song. Pick the first song of the shared songs,
		// and use its ID as the basis for the new song shares.
		val idForNewName = otherTracksUsingFile.first().id

		// For each supported file format, copy it to give it a new name that won't be stomped out with the new trim
		listOf(AudioFormat.OGG, AudioFormat.MP3).forEach { audioExtension ->
			val newSharedFileName = idForNewName.toString() + audioExtension.extension
			fileStorageService.copySong(
					track.fileName.withNewExtension(audioExtension.extension),
					newSharedFileName,
					audioExtension
			)
		}

		// Now that we have a new file name, update our DB entities to point to the new one
		otherTracksUsingFile.forEach {
			// File name was created at a time when only .ogg existed. Kind of awkward, but just use the OGG name here
			// MP3 name is always derived by just dropping the extension and replacing it
			it.fileName = idForNewName.toString() + AudioFormat.OGG.extension
			trackRepository.save(it)
			// We are using a new file name, and our track link will still link to the old file name unless we force expire it
			trackLinkRepository.forceExpireLinksByTrackId(it.id)
		}
	}

	fun createTrackFileWithMetadata(trackId: Long): File {
		logger.info("About to create a downloadable track for track ID: $trackId")
		val track = trackRepository.findById(trackId).unwrap()

		if (track == null || (track.private && track.user != loadLoggedInUser())) {
			throw IllegalArgumentException("No track found by ID $trackId!")
		}

		val songFile = fileStorageService.loadSong(track, AudioFormat.OGG)
		val songArtist = if (track.artist.isBlank()) "Unknown" else track.artist
		val songName = if (track.name.isBlank()) "Unknown" else track.name
		val newName = "$songArtist - $songName.ogg"

		val artworkFile = fileStorageService.loadAlbumArt(trackId)

		logger.info("Creating temporary track with name $newName")
		val renamedFile = File("${songFile.parent}/$newName")
		songFile.renameTo(renamedFile)

		logger.info("Adding metadata to temporary track $newName")
		fileMetadataService.addMetadataToFile(renamedFile, track, artworkFile)

		return renamedFile
	}

	companion object {
        private val logger = LoggerFactory.getLogger(SongIngestionService::class.java)
    }
}
