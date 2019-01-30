package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "playlist")
data class Playlist(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@OneToMany(mappedBy = "playlist", fetch = FetchType.LAZY)
		val tracks: List<PlaylistTrack> = emptyList(),

		@JsonIgnore
		@OneToMany(mappedBy = "playlist", fetch = FetchType.LAZY)
		val users: List<PlaylistUser> = emptyList(),

		@Column(nullable = false)
		var name: String,

		@Column(name = "created_at", nullable = false)
		var createdAt: Timestamp = Timestamp(Date().time)
)
