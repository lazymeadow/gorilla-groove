package com.example.groove.db.dao

import com.example.groove.db.model.Track
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.Pageable
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@DataJpaTest
class TrackRepositoryTest(@Autowired val entityManager: TestEntityManager,
                        @Autowired val trackRepository: TrackRepository) {

    // This method --should-- be able to be named with backticks + spaces, as is standard Kotlin. But there is a problem
    // with Java 10 + @DataJpaTest issue. https://github.com/spring-guides/tut-spring-boot-kotlin/issues/8
    @Test
    fun i_can_persist_an_entity_and_pull_it_back_out() {
        val track = createTestTrack()
        entityManager.persist(track)
        entityManager.flush()

        val resultPage = trackRepository.findTracks(null, null, null, Pageable.unpaged())

        assertThat(resultPage.content[0]).isEqualTo(track)
    }

    @Test
    fun i_can_use_filters_to_grab_the_entity_i_want() {
        val track1 = createTestTrack()
		val track2 = createTestTrack("My First Love", "Mikkas", "")
        entityManager.persist(track1)
		entityManager.persist(track2)
        entityManager.flush()

        val resultPage1 = trackRepository.findTracks(null, null, null, Pageable.unpaged())
        assertThat(resultPage1.totalElements).isEqualTo(2)

		val resultPage2 = trackRepository.findTracks("Besaid", null, null, Pageable.unpaged())
		assertThat(resultPage2.content[0]).isEqualTo(track1)
		assertThat(resultPage2.totalElements).isEqualTo(1)

		val resultPage3 = trackRepository.findTracks("Nothing! Absolutely Nothing! You so stupid!", null, null, Pageable.unpaged())
		assertThat(resultPage3.totalElements).isEqualTo(0)

		val resultPage4 = trackRepository.findTracks(null, "Mikkas", null, Pageable.unpaged())
		assertThat(resultPage4.content[0]).isEqualTo(track2)
		assertThat(resultPage4.totalElements).isEqualTo(1)

		val resultPage5 = trackRepository.findTracks(null, null, "Final Fantasy", Pageable.unpaged())
		assertThat(resultPage5.content[0]).isEqualTo(track1)
		assertThat(resultPage5.totalElements).isEqualTo(1)
    }

	private fun createTestTrack(
			name: String = "Besaid Island",
			artist: String = "Nobuo Uematsu",
			album: String = "Final Fantasy X OST"
	): Track {
		return Track(
				name = name,
				artist = artist,
				album = album,
				fileName = "Besaid Island",
				bitRate = 112,
				sampleRate = 44100,
				length = 284
		)
	}
}
