package com.example.groove.controllers

import com.example.groove.db.model.enums.PermissionType
import com.example.groove.services.UserInviteLinkService
import com.example.groove.services.UserService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/user-invite-link")
class UserInviteLinkController(
		private val userInviteLinkService: UserInviteLinkService,
		private val userService: UserService
) {

	@PostMapping
	fun createUserInviteLink(): CreateLinkResponse {
		userService.assertPermission(loadLoggedInUser(), PermissionType.INVITE_USER)

		logger.info(loadLoggedInUser().name + " created a new user invite link")

		return CreateLinkResponse(link = userInviteLinkService.createLink())
	}

	// NOTE: This function is available anonymously
	@GetMapping("/public/{identifier}")
	fun getExistingUserInviteLink(@PathVariable(value = "identifier") identifier: String): UserInviteLinkResponse {
		val link = userInviteLinkService.getLink(identifier)
		return UserInviteLinkResponse(
				invitingUserName = link.invitingUser.name,
				alreadyUsed = link.createdUser != null
		)
	}

	data class CreateLinkResponse(val link: String)
	data class UserInviteLinkResponse(val invitingUserName: String, val alreadyUsed: Boolean)

	companion object {
		val logger = logger<UserInviteLinkController>()
	}
}
