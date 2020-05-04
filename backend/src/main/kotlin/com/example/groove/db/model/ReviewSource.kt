package com.example.groove.db.model

import com.example.groove.db.model.enums.ReviewSourceType
import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "review_source")
data class ReviewSource(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@Enumerated
		val reviewSourceType: ReviewSourceType,

		@JsonIgnore
		@Column(name = "review_source_implementation_id")
		val reviewSourceImplementationId: Long, // Points to a ReviewSourceImplementation interface, not entity

		@JsonIgnore
		@ManyToMany
		@JoinTable(
				name = "review_source_user",
				joinColumns = [JoinColumn(name = "review_source_id")],
				inverseJoinColumns = [JoinColumn(name = "user_id")])
		var subscribedUsers: MutableList<User> = mutableListOf(),

		@Column(name = "created_at")
		val createdAt: Timestamp = now()
)
