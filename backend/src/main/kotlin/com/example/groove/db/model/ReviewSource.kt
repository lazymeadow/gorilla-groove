package com.example.groove.db.model

import com.example.groove.db.model.enums.ReviewSourceType
import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "review_source")
open class ReviewSource(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToMany
		@JoinTable(
				name = "review_source_user",
				joinColumns = [JoinColumn(name = "review_source_id")],
				inverseJoinColumns = [JoinColumn(name = "user_id")])
		var subscribedUsers: MutableList<User> = mutableListOf(),

		@Column(name = "source_type")
		val sourceType: ReviewSourceType,

		@Column(name = "created_at")
		val createdAt: Timestamp = now()
)
