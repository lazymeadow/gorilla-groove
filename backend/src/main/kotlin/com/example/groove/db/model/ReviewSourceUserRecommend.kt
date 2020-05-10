package com.example.groove.db.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "review_source_user_recommend")
@PrimaryKeyJoinColumn(name = "review_source_id")
data class ReviewSourceUserRecommend(
		@JsonIgnore
		@ManyToOne
		val user: User
) : ReviewSource()
