package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "playlist")
class Playlist(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		override val id: Long = 0,

		@JsonIgnore
		@OneToMany(mappedBy = "playlist", fetch = FetchType.LAZY)
		val tracks: List<PlaylistTrack> = emptyList(),

		@JsonIgnore
		@OneToMany(mappedBy = "playlist", fetch = FetchType.LAZY)
		val users: List<PlaylistUser> = emptyList(),

		@Column(nullable = false)
		var name: String,

		@Column(name = "created_at", nullable = false)
		override var createdAt: Timestamp = now(),

		@Column(name = "updated_at", nullable = false)
		override var updatedAt: Timestamp = now(),

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		override var deleted: Boolean = false
) : RemoteSyncable
