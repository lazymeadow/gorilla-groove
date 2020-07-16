package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "review_source_artist_download")
data class ReviewSourceArtistDownload(
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@ManyToOne
		@JoinColumn(name = "review_source_id")
		val reviewSource: ReviewSourceArtist,

		@Column(name = "track_name")
		val trackName: String,

		@Column(name = "discovered_at")
		val discoveredAt: Timestamp = now(),

		@Column(name = "last_download_attempt")
		var lastDownloadAttempt: Timestamp? = null,

		@Column(name = "downloaded_at")
		var downloadedAt: Timestamp? = null
)
