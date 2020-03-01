package com.example.groove.services

import com.example.groove.db.dao.UserInviteLinkRepository
import com.example.groove.db.dao.UserPermissionRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.User
import com.example.groove.db.model.UserPermission
import com.example.groove.db.model.enums.PermissionType
import com.example.groove.exception.PermissionDeniedException
import com.example.groove.util.DateUtils
import com.example.groove.util.isNewerThan
import com.example.groove.util.loadLoggedInUser
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit


@Service
class UserService(
		private val userRepository: UserRepository,
		private val userPermissionRepository: UserPermissionRepository,
		private val userInviteLinkService: UserInviteLinkService,
		private val userInviteLinkRepository: UserInviteLinkRepository
) {

	@Transactional(readOnly = true)
	fun getUserByEmail(email: String): User? {
		return userRepository.findByEmail(email)
	}

	@Transactional
	fun saveUser(user: User, inviteLinkIdentifier: String) {
		val inviteLink = userInviteLinkService.getLink(inviteLinkIdentifier)
		require(inviteLink.createdUser == null) { "This invite link has already been used!" }

		inviteLink.createdUser = user
		userInviteLinkRepository.save(inviteLink)

		user.encryptedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
		userRepository.save(user)
	}

	@Transactional(readOnly = true)
	fun getUsers(showAll: Boolean): List<User> {
		val self = loadLoggedInUser()

		return userRepository.findAll()
				.toList()
				.filterNot { it.deleted }
				.filter {
					showAll || it.id == self.id || it.lastLogin.isNewerThan(ChronoUnit.DAYS, 60)
				}
	}

	@Transactional(readOnly = true)
	fun getUserPermissions(user: User): List<UserPermission> {
		return userPermissionRepository.findByUser(user)
	}

	@Transactional(readOnly = true)
	fun assertPermission(user: User, permissionType: PermissionType) {
		userPermissionRepository.findByUserAndPermissionType(user, permissionType)
				?: throw PermissionDeniedException("User ${user.email} does not have access to ${permissionType}!")
	}

	@Transactional
	fun updateOwnLastLogin() {
		val user = loadLoggedInUser()
		user.lastLogin = DateUtils.now()

		userRepository.save(user)
	}
}
