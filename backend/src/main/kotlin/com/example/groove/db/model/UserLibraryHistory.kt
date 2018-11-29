package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "user_library_history")
data class UserLibraryHistory(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "user_library_id", nullable = false)
		val userLibrary: UserLibrary,

		@Column(name = "created_at", nullable = false)
		val createdAt: Timestamp = Timestamp(Date().time)
)
