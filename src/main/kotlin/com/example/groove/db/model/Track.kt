package com.example.groove.db.model

import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
data class Track(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@Column(nullable = false)
		val name: String,

		@Column(nullable = false)
		val artist: String = "",

		@Column(nullable = false)
		val album: String = "",

		@Column(name = "file_name", nullable = false)
		val fileName: String,

		@Column(name = "play_count", nullable = false)
		val playCount: Int = 0,

		@Column(name = "bit_rate", nullable = false)
		val bitRate: Long,

		@Column(name = "sample_rate", nullable = false)
		val sampleRate: Int,

		@Column(nullable = false)
		val length: Int,

		@Column(name = "release_year")
		val releaseYear: Int?,

		@Column(name = "created_at", nullable = false)
		val createdAt: Timestamp = Timestamp(Date().time),

		@Column(name = "last_played")
		val lastPlayed: Timestamp?
)
