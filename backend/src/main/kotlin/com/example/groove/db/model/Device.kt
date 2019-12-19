package com.example.groove.db.model

import com.example.groove.db.model.enums.DeviceType
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "device")
data class Device(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "user_id")
		val user: User,

		@JsonIgnore
		@Enumerated
		val deviceType: DeviceType,

		@Column(name = "device_id")
		val deviceId: String,

		@Column(name = "application_version")
		var applicationVersion: String,

		@Column(name = "last_ip")
		var lastIp: String,

		@Column(name = "additional_data")
		var additionalData: String? = null,

		@Column(name = "created_at")
		val createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

		@Column(name = "updated_at")
		var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())
)
