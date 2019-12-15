package com.example.groove.db.model

import com.example.groove.db.model.enums.DeviceType
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
		@JoinColumn(name = "track_id")
		val track: Track,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "device_id")
		val device: Device?,

		@JsonIgnore
		@Column(name = "ip_address")
		val ipAddress: String?,

		@Column(name = "created_at")
		val createdAt: Timestamp = Timestamp(System.currentTimeMillis())
)
