package com.example.groove.db.model

import com.example.groove.services.enums.AudioFormat
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

		@Enumerated
		val audioFormat: AudioFormat,

		@Column(name = "is_art", columnDefinition = "BIT")
		val isArt: Boolean,

		@Column(name = "expires_at")
		val expiresAt: Timestamp
)
