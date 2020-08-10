package com.example.groove.db.model

import com.example.groove.db.model.enums.ReviewSourceType
import com.example.groove.util.DateUtils.now
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "review_source_youtube_channel")
@PrimaryKeyJoinColumn(name = "review_source_id")
class ReviewSourceYoutubeChannel(
		@Column(name = "channel_id")
		val channelId: String,

		@Column(name = "channel_name")
		val channelName: String,

		@Column(name = "last_searched")
		var lastSearched: Timestamp = now()
) : ReviewSource(sourceType = ReviewSourceType.YOUTUBE_CHANNEL) {
	override val displayName: String
		get() = channelName
}
