package com.example.groove.services.review

import com.example.groove.ModelBuilder
import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.services.YoutubeApiClient
import com.example.groove.services.YoutubeDownloadService
import com.example.groove.shouldBe
import com.example.groove.util.toTimestamp
import org.junit.jupiter.api.Test
import java.time.Instant

class ReviewSourceArtistServiceTest {
	@Test
	fun `YouTube videos which should match artist searches do`() {
		val video1 = createVideo(title = "Slow Magic - Breathless (feat. Runn) [Official Audio]", duration = 199)
		val song1 = createSong(name = "Breathless", artist = "Slow Magic, RUNN", length = 198)

		val video2 = createVideo(title = "Gareth Emery & Ashley Wallbridge - CVNT5 Of The Caribbean", duration = 214)
		val song2 = createSong(name = "CVNT5 Of The Caribbean", artist = "Gareth Emery, Ashley Wallbridge, CVNT5", length = 214)

		video1.isValidForSong(song1) shouldBe true
		video2.isValidForSong(song2) shouldBe true
	}

	private fun createVideo(title: String, duration: Int) = YoutubeDownloadService.VideoProperties(
			title = title,
			duration = duration,
			id = "",
			videoUrl = ""
	)

	private fun createSong(name: String, artist: String, length: Int) = MetadataResponseDTO(
			name = name,
			artist = artist,
			length = length,
			albumArtLink = "",
			album = "",
			trackNumber = 0,
			releaseYear = 0,
			previewUrl = "",
			sourceId = ""
	)
}