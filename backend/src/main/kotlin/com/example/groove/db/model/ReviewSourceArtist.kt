package com.example.groove.db.model

import com.example.groove.db.model.enums.ReviewSourceType
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "review_source_artist")
@PrimaryKeyJoinColumn(name = "review_source_id")
class ReviewSourceArtist(
		@Column(name = "artist_id")
		val artistId: String,

		@Column(name = "artist_name")
		val artistName: String,

		@Column(name = "search_newer_than")
		var searchNewerThan: Timestamp? = null
) : ReviewSource(sourceType = ReviewSourceType.ARTIST) {
	override val displayName: String
		get() = artistName
}
