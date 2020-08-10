package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.ReviewSourceArtistDownload
import org.springframework.data.repository.CrudRepository

interface ReviewSourceArtistDownloadRepository : CrudRepository<ReviewSourceArtistDownload, Long> {
	fun findByReviewSource(reviewSource: ReviewSource): List<ReviewSourceArtistDownload>
	fun findByReviewSourceAndTrackName(reviewSource: ReviewSource, trackName: String): ReviewSourceArtistDownload?
}
