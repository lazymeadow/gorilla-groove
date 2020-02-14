package com.example.groove.services

import com.example.groove.db.dao.UserInviteLinkRepository
import com.example.groove.db.model.UserInviteLink
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.toTimestamp
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class UserInviteLinkService(
		private val userInviteLinkRepository: UserInviteLinkRepository
) {

	@Transactional
	fun createLink(): String {
		val userInviteLink = UserInviteLink(
				invitingUser = loadLoggedInUser(),
				expiresAt = now().toLocalDateTime().plusWeeks(1).toTimestamp()
		)
		userInviteLinkRepository.save(userInviteLink)

		return userInviteLink.getLink()
	}

	@Transactional(readOnly = true)
	fun getLink(linkIdentifier: String): UserInviteLink {
		val link = userInviteLinkRepository.findByLinkIdentifier(linkIdentifier)

		if (link == null || link.expiresAt < now()) {
			throw ResourceNotFoundException("The invitation has either expired or never existed!")
		}

		return link
	}

	fun UserInviteLink.getLink(): String {
		return "/create-account/${this.linkIdentifier}"
	}
}
