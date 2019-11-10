package com.example.groove.services

import com.example.groove.db.model.User
import com.example.groove.db.model.Track
import com.example.groove.properties.FileStorageProperties
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.datatype.Artwork
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.File


@Component
class FileMetadataService(
		private val fileStorageProperties: FileStorageProperties
) {

	// Removing the album artwork (and getting the image out of it) is useful for a couple of reasons
	// 1) FFmpeg, at least when converting from mp3 to ogg vorbis, seems to be corrupting the metadata
	//    on songs that are converted with album artwork. This makes the metadata extraction fail
	// 2) Storing the album artwork on the track itself is not a useful place to store the data. It's
	//    more convenient to save it to disk, and allow clients to directly link to the artwork
	fun removeAlbumArtFromFile(fileName: String): BufferedImage? {
		val path = "${fileStorageProperties.tmpDir}$fileName"
        val file = File(path)
        if (!file.exists()) {
            logger.error("File was not found using the path '$path'")
            throw IllegalArgumentException("File by name '$fileName' does not exist!")
        }
        val audioFile = AudioFileIO.read(file)

		val artwork = audioFile.tag.artworkList
		if (artwork.size > 1) {
			logger.warn("While removing album artwork from the file $fileName, more than one piece " +
					"of art was found (${artwork.size} pieces). Using the first piece of album art.")
		}

		return if (artwork.isNotEmpty()) {
			// Destroy the existing album art on the file. We don't need it, and it can cause file conversion problems
			audioFile.tag.deleteArtworkField()
			audioFile.commit()

			// Check that the image data actually exists....
			// Encountered Alestorm songs with album data that existed, but was empty
			if (artwork[0].binaryData != null) {
				// Return the first album art that we found
				artwork[0].image
			} else {
				null
			}
		} else {
			null
		}
	}

    fun createTrackFromSongFile(song: File, user: User): Track {
        if (!song.exists()) {
            logger.error("File was not found using the path '${song.path}'")
            throw IllegalArgumentException("File by name '${song.name}' does not exist!")
        }
        val audioFile = AudioFileIO.read(song)

		return Track(
				user = user,
				fileName = song.name,
				name = audioFile.tag.getFirst(FieldKey.TITLE),
				artist = audioFile.tag.getFirst(FieldKey.ARTIST),
				album = audioFile.tag.getFirst(FieldKey.ALBUM),
				trackNumber = parseTrackNumber(audioFile.tag.getFirst(FieldKey.TRACK)),
				releaseYear = audioFile.tag.getFirst(FieldKey.YEAR).toIntOrNull(),
				genre = audioFile.tag.getFirst(FieldKey.GENRE),
				length = audioFile.audioHeader.trackLength,
				bitRate = audioFile.audioHeader.bitRateAsNumber,
				sampleRate = audioFile.audioHeader.sampleRateAsNumber
		)
	}

	fun getTrackLength(song: File): Int {
		if (!song.exists()) {
			logger.error("File was not found using the path '${song.path}'")
			throw IllegalArgumentException("File by name '${song.name}' does not exist!")
		}
		val audioFile = AudioFileIO.read(song)

		return audioFile.audioHeader.trackLength
	}

	fun addMetadataToFile(song: File, track: Track, albumArt: File?) {
		val file = AudioFileIO.read(song)
		val tag = file.tag

		tag.setField(FieldKey.TITLE, track.name)
		tag.setField(FieldKey.ARTIST, track.artist)
		tag.setField(FieldKey.ALBUM, track.album)
		tag.setField(FieldKey.TRACK, track.trackNumber?.toString() ?: "")
		tag.setField(FieldKey.YEAR, track.releaseYear?.toString() ?: "")
		tag.setField(FieldKey.GENRE, track.genre ?: "")

		albumArt?.let {
			// This doesn't seem to actually work... Not sure if it's because it's .ogg. But oh well
			tag.artworkList.add(Artwork.createArtworkFromFile(it))
		}

		AudioFileIO.write(file)

		logger.info("Wrote metadata to file")
	}

	private fun parseTrackNumber(trackNumber: String?): Int? {
		if (trackNumber == null) {
			return null
		}
		val trimTrackNumber = trackNumber.trim()

		return when {
			trimTrackNumber.isBlank() -> null
			trimTrackNumber.toIntOrNull() != null -> trimTrackNumber.toIntOrNull()
			trimTrackNumber.contains('/') -> trimTrackNumber.split('/')[0].toIntOrNull()
			else -> null
		}
	}

    companion object {
        val logger = LoggerFactory.getLogger(FileMetadataService::class.java)!!
    }
}
