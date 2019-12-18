package com.example.groove.db.model

import com.example.groove.db.model.enums.DeviceType
import com.example.groove.util.DateUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "version_history")
data class VersionHistory(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@Enumerated
		val deviceType: DeviceType,

		@Column
		val version: String,

		@Column
		val notes: String,

		@Column(name = "created_at")
		val createdAt: Timestamp = DateUtils.now()
)
