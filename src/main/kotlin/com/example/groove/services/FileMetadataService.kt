package com.example.groove.services

import com.example.groove.db.model.Track
import com.example.groove.properties.MusicProperties
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@Component
class FileMetadataService(
		@Autowired val musicProperties: MusicProperties
) {

	fun createTrackFromFileName(fileName: String): Track {
		val file = File("${musicProperties.musicDirectoryLocation}$fileName.ogg")
		if (!file.exists()) {
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
}
