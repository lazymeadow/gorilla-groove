package com.example.groove.services

import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.User
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


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
	fun getAllUsers(): List<User> {
		return userRepository.findAll().toList()
	}
}
