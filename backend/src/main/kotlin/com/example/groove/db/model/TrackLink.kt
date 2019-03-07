package com.example.groove.db.model

import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "track_link")
data class TrackLink(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@ManyToOne
		@JoinColumn(name = "track_id")
		val track: Track,

		@Column
		val link: String = "",

		@Column(name = "expires_at")
		val expiresAt: Timestamp
)
