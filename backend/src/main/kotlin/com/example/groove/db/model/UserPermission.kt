package com.example.groove.db.model

import com.example.groove.db.model.enums.PermissionType
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "user_permission")
data class UserPermission(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "user_id", nullable = false)
		val user: User,

		@Enumerated
		val permissionType: PermissionType,

		@Column(name = "created_at", nullable = false)
		var createdAt: Timestamp = Timestamp(Date().time)
)
