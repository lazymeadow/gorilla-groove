package com.example.groove.services

import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class UserService(
        @Autowired val userRepository: UserRepository,
        @Autowired val entityManager: EntityManager
) {
    fun goodThings() {
        logger.info("Good things!")
        val user = User(0, "Test User")
        entityManager.persist(user)
        entityManager.flush()
    }

    companion object {
        val logger = LoggerFactory.getLogger(UserService::class.java)!!
    }
}
