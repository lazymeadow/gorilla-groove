package com.example.groove.db.model

import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "track_review_info")
data class TrackReviewInfo(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@OneToOne
		@JoinColumn(name = "track_id")
		val track: Track,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "review_source_id")
		val reviewSource: ReviewSource,

		@Column(name = "last_reviewed")
		var lastReviewed: Timestamp = now()
)
