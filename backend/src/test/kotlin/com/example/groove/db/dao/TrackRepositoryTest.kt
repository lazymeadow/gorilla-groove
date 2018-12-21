package com.example.groove.db.dao

import com.example.groove.db.model.User
import com.example.groove.db.model.Track
import com.example.groove.util.unwrap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@DataJpaTest
class TrackRepositoryTest(
		private val entityManager: TestEntityManager,
		private val trackRepository: TrackRepository
) {

	// This method --should-- be able to be named with backticks + spaces, as is standard Kotlin. But there is a problem
	// with Java 10 + @DataJpaTest issue. https://github.com/spring-guides/tut-spring-boot-kotlin/issues/8
	@Test
	fun i_can_persist_an_entity_and_pull_it_back_out() {
		val user = createPersistedTestUser()
		val track = createPersistedTrack(user = user)

		val resultPage = trackRepository.getTrack(userId = user.id)

		assertThat(resultPage.content[0]).isEqualTo(track)
	}

	@Test
	fun i_can_grab_tracks_by_user() {
		val user1 = createPersistedTestUser()
		val user2 = createPersistedTestUser()
		createPersistedTrack(user = user1)

		val user1Result = trackRepository.getTrack(userId = user1.id)
		val user2Result = trackRepository.getTrack(userId = user2.id)
		assertThat(user1Result.totalElements).isEqualTo(1)
		assertThat(user2Result.totalElements).isEqualTo(0)
	}

	@Test
	fun i_can_use_filters_to_grab_the_entity_i_want() {
		val user = createPersistedTestUser()
		val track1 = createPersistedTrack(user)
		val track2 = createPersistedTrack(user, "My First Love", "Mikkas", "")

		val resultPage1 = trackRepository.getTrack(userId = user.id)
		assertThat(resultPage1.totalElements).isEqualTo(2)

		val resultPage2 = trackRepository.getTrack(userId = user.id, name = "Besaid")
		assertThat(resultPage2.content[0]).isEqualTo(track1)
		assertThat(resultPage2.totalElements).isEqualTo(1)

		val resultPage3 = trackRepository.getTrack(userId = user.id, name = "Nothing! Absolutely Nothing! You so stupid!")
		assertThat(resultPage3.totalElements).isEqualTo(0)

		val resultPage4 = trackRepository.getTrack(userId = user.id, artist = "Mikkas")
		assertThat(resultPage4.content[0]).isEqualTo(track2)
		assertThat(resultPage4.totalElements).isEqualTo(1)

		val resultPage5 = trackRepository.getTrack(userId = user.id, album = "Final Fantasy")
		assertThat(resultPage5.content[0]).isEqualTo(track1)
		assertThat(resultPage5.totalElements).isEqualTo(1)
	}

	@Test
	fun i_can_set_hidden_values_of_my_own_tracks() {
		val user = createPersistedTestUser()
		val track = createPersistedTrack(user = user)

		assertThat(track.hidden).isFalse()

		trackRepository.setHiddenForUser(listOf(track.id), user.id, true)
		entityManager.clear() // Clear the cache so the track we grab is the updated one

		val recoveredTrack = trackRepository.findById(track.id)
		assertThat(recoveredTrack.unwrap()!!.hidden).isTrue()
	}

	private fun createPersistedTrack(
			user: User,
			name: String = "Besaid Island",
			artist: String = "Nobuo Uematsu",
			album: String = "Final Fantasy X OST"
	): Track {
		val track =  Track(
				user = user,
				name = name,
				artist = artist,
				album = album,
				fileName = "Besaid Island",
				bitRate = 112,
				sampleRate = 44100,
				length = 284
		)
		entityManager.persist(track)
		return track
	}

	private fun createPersistedTestUser(
			name: String = "Elon Musk",
			email: String = "i-will-invest-in-your@media.player",
			password: String = "very-secure"
	): User {
		val user =  User(
				name = name,
				email = email,
				encryptedPassword = password
		)
		entityManager.persist(user)
		return user
	}

}
