package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "playlist_track")
data class PlaylistTrack(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		override val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "playlist_id", nullable = false)
		val playlist: Playlist,

		@ManyToOne
		@JoinColumn(name = "track_id", nullable = false)
		val track: Track,

		@Column(name = "created_at", nullable = false)
		override var createdAt: Timestamp = now(),

		@Column(name = "updated_at", nullable = false)
		override var updatedAt: Timestamp = now(),

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		override var deleted: Boolean = false
) : RemoteSyncable
