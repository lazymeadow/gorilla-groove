package com.example.groove.services

import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class UserService @Autowired constructor(
		private val userRepository: UserRepository
) {

	@Transactional
	fun saveUser(user: User) {
		// TODO maybe something called encryption
		userRepository.save(user)
	}

}
