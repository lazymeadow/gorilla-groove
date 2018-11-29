package com.example.groove.db.dao

import com.example.groove.db.model.Track
import com.example.groove.db.model.User
import com.example.groove.db.model.UserLibrary
import com.example.groove.util.unwrap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional

@ExtendWith(SpringExtension::class)
@DataJpaTest
class UserLibraryRepositoryTest(
		private val entityManager: TestEntityManager,
		private val userLibraryRepository: UserLibraryRepository
) {

	// This method --should-- be able to be named with backticks + spaces, as is standard Kotlin. But there is a problem
	// with Java 10 + @DataJpaTest issue. https://github.com/spring-guides/tut-spring-boot-kotlin/issues/8
	@Test
	fun i_can_persist_an_entity_and_pull_it_back_out() {
		val track = createPersistedTestTrack()
		val user = createPersistedTestUser()
		val userLibraryTrack = createPersistedUserLibrary(user = user, track = track)

		val resultPage = userLibraryRepository.getLibrary(userId = user.id)

		assertThat(resultPage.content[0]).isEqualTo(userLibraryTrack)
	}

	// This method --should-- be able to be named with backticks + spaces, as is standard Kotlin. But there is a problem
	// with Java 10 + @DataJpaTest issue. https://github.com/spring-guides/tut-spring-boot-kotlin/issues/8
	@Test
	fun i_can_grab_tracks_by_user() {
		val track = createPersistedTestTrack()
		val user1 = createPersistedTestUser()
		val user2 = createPersistedTestUser()
		createPersistedUserLibrary(user = user1, track = track)

		val user1Result = userLibraryRepository.getLibrary(userId = user1.id)
		val user2Result = userLibraryRepository.getLibrary(userId = user2.id)
		assertThat(user1Result.totalElements).isEqualTo(1)
		assertThat(user2Result.totalElements).isEqualTo(0)
	}

	@Test
	fun i_can_use_filters_to_grab_the_entity_i_want() {
		val track1 = createPersistedTestTrack()
		val track2 = createPersistedTestTrack("My First Love", "Mikkas", "")

		val user = createPersistedTestUser()
		val userLibraryTrack1 = createPersistedUserLibrary(user = user, track = track1)
		val userLibraryTrack2 = createPersistedUserLibrary(user = user, track = track2)

		val resultPage1 = userLibraryRepository.getLibrary(userId = user.id)
		assertThat(resultPage1.totalElements).isEqualTo(2)

		val resultPage2 = userLibraryRepository.getLibrary(userId = user.id, name = "Besaid")
		assertThat(resultPage2.content[0]).isEqualTo(userLibraryTrack1)
		assertThat(resultPage2.totalElements).isEqualTo(1)

		val resultPage3 = userLibraryRepository.getLibrary(userId = user.id, name = "Nothing! Absolutely Nothing! You so stupid!")
		assertThat(resultPage3.totalElements).isEqualTo(0)

		val resultPage4 = userLibraryRepository.getLibrary(userId = user.id, artist = "Mikkas")
		assertThat(resultPage4.content[0]).isEqualTo(userLibraryTrack2)
		assertThat(resultPage4.totalElements).isEqualTo(1)

		val resultPage5 = userLibraryRepository.getLibrary(userId = user.id, album = "Final Fantasy")
		assertThat(resultPage5.content[0]).isEqualTo(userLibraryTrack1)
		assertThat(resultPage5.totalElements).isEqualTo(1)
	}

	@Test
	fun i_can_set_hidden_values_of_my_own_tracks() {
		val track1 = createPersistedTestTrack()
		val user = createPersistedTestUser()

		val userLibrary = createPersistedUserLibrary(user, track1)

		assertThat(userLibrary.hidden).isFalse()

		userLibraryRepository.setHiddenForUser(listOf(userLibrary.id), user.id, true)
		entityManager.clear() // Clear the cache so the track we grab is the updated one

		val recoveredLibrary = userLibraryRepository.findById(userLibrary.id)
		assertThat(recoveredLibrary.unwrap()!!.hidden).isTrue()
	}

	@Transactional
	fun testyTest(userLibraryId: Long, userId: Long) {
	}

	private fun createPersistedTestTrack(
			name: String = "Besaid Island",
			artist: String = "Nobuo Uematsu",
			album: String = "Final Fantasy X OST"
	): Track {
		val track =  Track(
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

	private fun createPersistedUserLibrary(
			user: User,
			track: Track
	): UserLibrary {
		val userLibrary =  UserLibrary(
				user = user,
				track = track
		)
		entityManager.persist(userLibrary)
		return userLibrary
	}
}
