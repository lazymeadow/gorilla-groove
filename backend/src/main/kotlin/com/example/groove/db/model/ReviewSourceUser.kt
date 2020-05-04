package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "review_source_user")
data class ReviewSourceUser(
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		val user: User
) : ReviewSourceImplementation
