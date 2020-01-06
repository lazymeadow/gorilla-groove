package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
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

		@Column
		var name: String,

		@Column
		var artist: String = "",

		@Column
		var featuring: String = "",

		@Column
		var album: String = "",

		@Column(name = "track_number")
		var trackNumber: Int? = null,

		@Column(name = "file_name")
		var fileName: String,

		@Column(name = "bit_rate")
		var bitRate: Long,

		@Column(name = "sample_rate")
		var sampleRate: Int,

		@Column
		var length: Int,

		@Column(name = "release_year")
		var releaseYear: Int? = null,

		@Column
		var genre: String? = null,

		@Column(name = "play_count")
		var playCount: Int = 0,

		@Column(columnDefinition = "BIT") // MySQL lacks a Boolean type. Need to label it as a BIT column
		var private: Boolean = false,

		@Column(columnDefinition = "BIT")
		var hidden: Boolean = false,

		@Column(name = "last_played")
		var lastPlayed: Timestamp? = null,

		@Column(name = "created_at")
		var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

		@JsonIgnore // Currently breaks GG app to include this and offers no current benefit to be exposed
		@Column(name = "updated_at")
		var updatedAt: Timestamp = Timestamp(System.currentTimeMillis()),

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		var deleted: Boolean = false,

		@Column
		var note: String? = null
)
