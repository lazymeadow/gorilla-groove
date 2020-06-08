package com.example.groove.db.model

import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "review_source_artist")
@PrimaryKeyJoinColumn(name = "review_source_id")
data class ReviewSourceArtist(
		@Column(name = "artist_id")
		val artistId: String,

		@Column(name = "artist_name")
		val artistName: String,

		@Column(name = "search_newer_than")
		val searchNewerThan: Timestamp? = null
) : ReviewSource()
