package com.example.groove.db.model

import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "user_token")
data class UserToken(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@ManyToOne
		@JoinColumn(name = "user_id", nullable = false)
		val user: User,

		@Column(nullable = false)
		val token: String = "",

		@Column(name = "created_at", nullable = false)
		val createdAt: Timestamp = Timestamp(Date().time)
)
