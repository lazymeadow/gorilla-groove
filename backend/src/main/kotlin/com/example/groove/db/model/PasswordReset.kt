package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "password_reset")
data class PasswordReset(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@ManyToOne
		@JoinColumn(name = "user_id")
		val user: User,

		@Column(name = "unique_key")
		val uniqueKey: String = UUID.randomUUID().toString(),

		@Column(name = "created_at")
		val createdAt: Timestamp = now(),
)
