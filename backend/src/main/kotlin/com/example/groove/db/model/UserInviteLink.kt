package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "user_invite_link")
data class UserInviteLink(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@ManyToOne
		@JoinColumn(name = "inviting_user_id")
		val invitingUser: User,

		@ManyToOne
		@JoinColumn(name = "created_user_id")
		var createdUser: User? = null, // Null before the link is used. Filled in after

		@Column(name = "link_identifier")
		val linkIdentifier: String = UUID.randomUUID().toString(),

		@Column(name = "created_at")
		val createdAt: Timestamp = now(),

		@Column(name = "expires_at")
		val expiresAt: Timestamp
)
