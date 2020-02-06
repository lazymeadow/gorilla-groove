package com.example.groove.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class CurrentlyListeningUsersDTO(
		val currentlyListeningUsers: Map<Long, SongListenResponse>?,
		val lastUpdate: Int
)

data class SongListenResponse(
		val song: String,
		val deviceName: String?,

		// Damn Jackson stripping out the "is". No, I want it there
		@get:JsonProperty("isPhone")
		val isPhone: Boolean?
)
