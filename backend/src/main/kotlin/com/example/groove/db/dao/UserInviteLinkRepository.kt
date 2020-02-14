package com.example.groove.db.dao

import com.example.groove.db.model.UserInviteLink
import org.springframework.data.repository.CrudRepository

interface UserInviteLinkRepository : CrudRepository<UserInviteLink, Long> {

	fun findByLinkIdentifier(linkIdentifier: String): UserInviteLink?
}
