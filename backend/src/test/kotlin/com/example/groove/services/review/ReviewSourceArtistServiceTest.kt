package com.example.groove.services.review

import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.services.YoutubeDownloadService
import com.example.groove.services.isValidForSong
import com.example.groove.shouldBe
import org.junit.jupiter.api.Test

class ReviewSourceArtistServiceTest {
	@Test
	fun `YouTube videos which should match artist searches do`() {
        // Good
		val video1 = createVideo(title = "Slow Magic - Breathless (feat. Runn) [Official Audio]", duration = 199)
		val song1 = createSong(name = "Breathless", artist = "Slow Magic, RUNN", length = 198)

		// Good
		val video2 = createVideo(title = "Gareth Emery & Ashley Wallbridge - CVNT5 Of The Caribbean", duration = 214)
		val song2 = createSong(name = "CVNT5 Of The Caribbean", artist = "Gareth Emery, Ashley Wallbridge, CVNT5", length = 214)

		// Bad; Track name isn't found in video title
		val video3 = createVideo(title = "Gareth Emery & Ashley Wallbridge - CVNT5 Of The High Seas", duration = 214)
		val song3 = createSong(name = "CVNT5 Of The Caribbean", artist = "Gareth Emery, Ashley Wallbridge, CVNT5", length = 214)

		// Bad; One artist isn't found in video title
		val video4 = createVideo(title = "Slow Magic - Breathless [Official Audio]", duration = 199)
		val song4 = createSong(name = "Breathless", artist = "Slow Magic, RUNN", length = 198)

		// Bad; Duration doesn't match
		val video5 = createVideo(title = "Slow Magic - Breathless (feat. Runn) [Official Audio]", duration = 210)
		val song5 = createSong(name = "Breathless", artist = "Slow Magic, RUNN", length = 198)

		video1.isValidForSong(song1.artist, song1.name, song1.length) shouldBe true
		video2.isValidForSong(song2.artist, song2.name, song2.length) shouldBe true
		video3.isValidForSong(song3.artist, song3.name, song3.length) shouldBe false
		video4.isValidForSong(song4.artist, song4.name, song4.length) shouldBe false
		video5.isValidForSong(song5.artist, song5.name, song5.length) shouldBe false
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