package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "user_library")
data class UserLibrary(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "user_id", nullable = false)
		val user: User,

		@ManyToOne
		@JoinColumn(name = "track_id", nullable = false)
		val track: Track,

		@Column(name = "play_count", nullable = false)
		var playCount: Int = 0,

		@Column(columnDefinition="BIT") // MySQL lacks a Boolean type. Need to label it as a BIT column
		var hidden: Boolean = false,

		@Column(name = "last_played")
		var lastPlayed: Timestamp? = null,

		@Column(name = "created_at", nullable = false)
		var createdAt: Timestamp = Timestamp(Date().time)
)
