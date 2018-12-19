package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "track_history")
data class TrackHistory(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "track_id", nullable = false)
		val track: Track,

		@Column(name = "created_at", nullable = false)
		val createdAt: Timestamp = Timestamp(Date().time)
)
