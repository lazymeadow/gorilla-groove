package com.example.groove.db.model

import java.sql.Timestamp
import javax.persistence.*

@Entity
data class Track(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long,

		@Column(nullable = false)
		val name: String,

		@Column
		val artist: String,

		@Column
		val album: String,

		@Column(name = "file_name", nullable = false)
		val fileName: String,

		@Column(name = "play_count", nullable = false)
		val playCount: Int,

		@Column(name = "bit_rate")
		val bitRate: Int,

		@Column(nullable = false)
		val length: Int,

		@Column(name = "release_date")
		val releaseDate: Int,

		@Column(name = "created_at", nullable = false)
		val createdAt: Timestamp,

		@Column(name = "last_played")
		val lastPlayed: Timestamp
)