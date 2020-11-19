package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "track_history")
data class TrackHistory(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "track_id")
		val track: Track,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "device_id")
		val device: Device?,

		@JsonIgnore
		@Column(name = "ip_address")
		val ipAddress: String?,

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		var deleted: Boolean = false,

		@Column(name = "listened_in_review", columnDefinition = "BIT")
		var listenedInReview: Boolean = false,

		@Column(name = "created_at")
		val createdAt: Timestamp = now(),

		// This is the datetime that the USER experienced when they listened to a song. It has no concept of
		// a time zone. If they listened to the song at 9AM, it was at 9AM. Doesn't matter where they were.
		// This is purely for stats tracking on letting people know what time of day they listen to music.
		// This could be saved as a LocalDateTime, but then Hibernate will try to convert between the JVM
		// timezone and the DB timezone, which can be problematic if they aren't the same since I want the
		// time reflected in the database to ALSO be the local time- not some conversion.
		@Column(name = "local_time_listened_at")
		val localTimeListenedAt: String,

		@Column(name = "iana_timezone")
		val ianaTimezone: String,

		@Column
		val latitude: Double? = null,

		@Column
		val longitude: Double? = null
)
