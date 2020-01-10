package com.example.groove.dto

data class CurrentlyListeningUsersDTO(
		val currentlyListeningUsers: List<Pair<Long, String>>?,
		val lastUpdate: Int
)