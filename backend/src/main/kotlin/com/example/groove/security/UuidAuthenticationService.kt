package com.example.groove.security

import com.example.groove.db.dao.UserRepository
import com.example.groove.db.dao.UserTokenRepository
import com.example.groove.db.model.User
import com.example.groove.db.model.UserToken
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Suppress("unused")
@Service
class UuidAuthenticationService @Autowired constructor(
		private val userRepository: UserRepository,
		private val userTokenRepository: UserTokenRepository
) : UserAuthenticationService {

	@Transactional
	override fun login(email: String, password: String): String {
		val user = userRepository.findByEmail(email)
		if (user == null) {
			logger.info("A user attempted to log in with the email address $email but it was not found")
			throw BadCredentialsException("No user found with the email $email")
		}

		if (!BCrypt.checkpw(password, user.encryptedPassword)) {
			logger.info("A user attempted to log in to the email address $email but provided the wrong password")
			logger.info("$password ${user.encryptedPassword}")
			throw BadCredentialsException("The password provided was invalid")
		}

		val uuid = UUID.randomUUID().toString()

		// Now that the user has created a token, save it for later so that we can authenticate against it
		val userToken = UserToken(user = user, token = uuid)
		userTokenRepository.save(userToken)

		return uuid
	}

	override fun findByToken(token: String): User {
		val dbToken = userTokenRepository.findByToken(token)
				?: throw BadCredentialsException("User not found with the provided token $token")
		return dbToken.user
	}

	override fun logout() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	companion object {
		val logger = LoggerFactory.getLogger(UuidAuthenticationService::class.java)!!
	}
}