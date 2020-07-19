package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "track")
data class Track(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		override val id: Long = 0,

		@JsonIgnore
		@ManyToOne(fetch = FetchType.LAZY)
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

		@JsonIgnore
		@Column(name = "created_at")
		override var createdAt: Timestamp = now(),

		@JsonIgnore
		@Column(name = "updated_at")
		override var updatedAt: Timestamp = now(),

		// We originally used the createdAt property to mean "addedToLibrary" because for years that's what it was.
		// With the introduction of the ReviewQueue stuff we now need to use a dedicated property for this.
		// However, to avoid having to update all the API's consumers, keep the JsonProperty name the same as it was
		@JsonProperty("createdAt")
		@Column(name = "added_to_library")
		var addedToLibrary: Timestamp? = null,

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		override var deleted: Boolean = false,

		@Column
		var note: String? = null,

		@JsonIgnore
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "review_source_id")
		var reviewSource: ReviewSource? = null,

		@JsonIgnore
		@Column(name = "last_reviewed")
		var lastReviewed: Timestamp? = null,

		@Column(columnDefinition = "BIT")
		var inReview: Boolean = false
) : RemoteSyncable
