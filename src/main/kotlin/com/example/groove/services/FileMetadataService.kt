package com.example.groove.services

import com.example.groove.db.model.Track
import com.example.groove.properties.MusicProperties
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@Component
class FileMetadataService(
		@Autowired val musicProperties: MusicProperties
) {

	fun createTrackFromFileName(fileName: String): Track {
		val path = "${musicProperties.musicDirectoryLocation}$fileName.ogg"
		val file = File(path)
		if (!file.exists()) {
			logger.error("File was not found using the path '$path'")
			throw IllegalArgumentException("File by name '$fileName' does not exist!")
		}
		val audioFile = AudioFileIO.read(file)

		return Track(
				fileName = fileName,
				name = audioFile.tag.getFirst(FieldKey.TITLE),
				artist = audioFile.tag.getFirst(FieldKey.ARTIST),
				album = audioFile.tag.getFirst(FieldKey.ALBUM),
				releaseYear = audioFile.tag.getFirst(FieldKey.YEAR).toIntOrNull(),
				length = audioFile.audioHeader.trackLength,
				bitRate = audioFile.audioHeader.bitRateAsNumber,
				sampleRate = audioFile.audioHeader.sampleRateAsNumber,
				lastPlayed = null
		)
	}

    companion object {
        val logger = LoggerFactory.getLogger(FileMetadataService::class.java)!!
    }
}
