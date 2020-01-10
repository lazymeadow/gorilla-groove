package com.example.groove.dto

data class CurrentlyListeningUsersDTO(
		val currentlyListeningUsers: Map<Long, String>?,
		val lastUpdate: Int
)
