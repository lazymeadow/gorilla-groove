package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
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

		@Column(name = "last_searched")
		var lastSearched: Timestamp = now()
) : ReviewSource()
