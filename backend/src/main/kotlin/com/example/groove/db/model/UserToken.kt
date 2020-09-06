package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "user_token")
data class UserToken(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@ManyToOne
		@JoinColumn(name = "user_id")
		val user: User,

		@ManyToOne
		@JoinColumn(name = "device_id")
		val device: Device?,

		@Column
		val token: String = "",

		@Column(name = "created_at")
		val createdAt: Timestamp = now()
)
