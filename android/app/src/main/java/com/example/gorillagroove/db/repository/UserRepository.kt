package com.example.gorillagroove.db.repository

import android.util.Log
import com.example.gorillagroove.db.dao.UserDao
import com.example.gorillagroove.db.model.User
import com.example.gorillagroove.utils.logger

const val userRepositoryTag = "User Repository"

class UserRepository(private val userDao: UserDao) {

    val logger = logger()

    fun findUser(email: String): User? {
        logger.debug("Finding user for email: $email")
        val user = userDao.findByEmail(email)
        logger.debug("findByEmail returned $user")
        return user
    }

    fun updateToken(user: User, token: String) {
        logger.info(userRepositoryTag, "Updating token for userId: ${user.id}")
        userDao.updateToken(user.id, token)

        user.token = token
        user.loggedIn = 1
    }

    fun updateDeviceId(userId: Long, deviceId: String) {
        logger.info(userRepositoryTag, "Updating deviceId for userId: $userId")
        userDao.updateDeviceId(userId, deviceId)
    }

    fun createUser(userName: String, email: String, token: String?, deviceId: String?): User {
        logger.info(userRepositoryTag, "Creating user with username: $userName and email: $email")
        val user = User(
            id = 0,
            userName = userName,
            email = email,
            token = token,
            loggedIn = 1,
            deviceId = deviceId
        )
        userDao.createUser(user)

        return user
    }

    fun lastLoggedInUser(): User? {
        logger.debug(userRepositoryTag, "Fetching the user that last logged in")
        val user = userDao.findLastLoggedIn()
        if (user != null) {
            logger.debug(userRepositoryTag, "The last logged in user was: $user")
        }
        return user
    }

    fun logout(userId: Long) {
        logger.info(userRepositoryTag, "Logging out user")
        userDao.logout(userId)
    }

}

