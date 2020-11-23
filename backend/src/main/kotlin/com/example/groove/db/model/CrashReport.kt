package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "crash_report")
class CrashReport(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "user_id")
		val user: User,

		@Column
		val version: String,

		@JsonIgnore
		@Column(name = "size_kb")
		val sizeKb: Int,

		@Column(name = "created_at")
		val createdAt: Timestamp = now()
)
