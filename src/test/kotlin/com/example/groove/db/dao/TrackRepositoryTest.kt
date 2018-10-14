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
import java.sql.Timestamp
import java.util.*

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

	private fun createTestTrack(): Track {
		return Track(
				0,
				"Besaid Island",
				"Nobuo Uematsu",
				"Final Fantasy X OST",
				"Besaid Island",
				0,
				112,
				44100,
				284,
				null,
				Timestamp(Date().time),
				null
		)
	}
}