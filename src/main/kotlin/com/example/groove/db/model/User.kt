package com.example.groove.db.model

import javax.persistence.*

@Entity
data class User(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long,

		@Column
		val name: String
)