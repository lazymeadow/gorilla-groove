package com.gorilla.gorillagroove.model

import java.io.Serializable

data class PlaylistKey(
    var id: Long,
    var name: String,
    var createdAt: String,
    var updatedAt: String
) : Serializable
