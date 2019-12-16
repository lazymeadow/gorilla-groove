package com.example.groove.services

import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.User
import com.example.groove.util.isNewerThan
import com.example.groove.util.loadLoggedInUser
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.temporal.ChronoUnit


@Service
class UserService(
		private val userRepository: UserRepository
) {

	@Transactional(readOnly = true)
	fun getUserByEmail(email: String): User? {
		return userRepository.findByEmail(email)
	}

	@Transactional
	fun saveUser(user: User) {
		user.encryptedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
		userRepository.save(user)
	}

	@Transactional(readOnly = true)
	fun getUsers(showAll: Boolean): List<User> {
		val self = loadLoggedInUser()

		return userRepository.findAll()
				.toList()
				.filter {
					showAll || it.id == self.id || it.lastLogin.isNewerThan(ChronoUnit.DAYS, 60)
				}
	}

	@Transactional
	fun updateOwnLastLogin() {
		val user = loadLoggedInUser()
		user.lastLogin = Timestamp(System.currentTimeMillis())

		userRepository.save(user)
	}
}
