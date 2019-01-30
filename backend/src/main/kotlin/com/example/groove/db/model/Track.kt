package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "track")
data class Track(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "user_id", nullable = false)
		val user: User,

		@Column(nullable = false)
		var name: String,

		@Column(nullable = false)
		var artist: String = "",

		@Column(nullable = false)
		var album: String = "",

		@Column(name = "track_number")
		var trackNumber: Int? = null,

		@Column(name = "file_name", nullable = false)
		var fileName: String,

		@Column(name = "bit_rate", nullable = false)
		var bitRate: Long,

		@Column(name = "sample_rate", nullable = false)
		var sampleRate: Int,

		@Column(nullable = false)
		var length: Int,

		@Column(name = "release_year")
		var releaseYear: Int? = null,

		@Column
		var genre: String? = null,

		@Column(name = "play_count", nullable = false)
		var playCount: Int = 0,

		@Column(columnDefinition = "BIT") // MySQL lacks a Boolean type. Need to label it as a BIT column
		var hidden: Boolean = false,

		@Column(name = "last_played")
		var lastPlayed: Timestamp? = null,

		@Column(name = "created_at", nullable = false)
		var createdAt: Timestamp = Timestamp(Date().time),

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		var deleted: Boolean = false,

		@Column
		var note: String? = null
)
