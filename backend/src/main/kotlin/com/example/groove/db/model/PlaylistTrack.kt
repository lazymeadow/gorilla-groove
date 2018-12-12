package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "playlist_track")
data class PlaylistTrack(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "playlist_id", nullable = false)
		val playlist: Playlist,

		@ManyToOne
		@JoinColumn(name = "track_id", nullable = false)
		val track: Track
)
