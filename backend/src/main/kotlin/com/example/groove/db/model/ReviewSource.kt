package com.example.groove.db.model

import com.example.groove.db.model.enums.ReviewSourceType
import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "review_source")
abstract class ReviewSource(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Access(AccessType.FIELD) // This (and the open modifier) let this ID be accessed lazily without fetching the entire entity
		open val id: Long = 0,

		@JsonIgnore
		@OneToMany(mappedBy = "reviewSource", fetch = FetchType.LAZY)
		open val reviewSourceUsers: List<ReviewSourceUser> = mutableListOf(),

		@Column(name = "source_type")
		open val sourceType: ReviewSourceType,

		@Column(name = "created_at")
		open val createdAt: Timestamp = now(),

		@JsonIgnore
		@Column(name = "updated_at")
		open var updatedAt: Timestamp = now()
) {
	abstract val displayName: String

	fun isUserSubscribed(user: User): Boolean {
		val userSource = reviewSourceUsers.find { it.user.id == user.id } ?: return false
		return !userSource.deleted && userSource.active
	}

	fun isActive(): Boolean {
		return reviewSourceUsers.isNotEmpty() && reviewSourceUsers.any { !it.deleted && it.active }
	}

	fun getActiveUsers(): List<User> {
		return reviewSourceUsers.filter { !it.deleted && it.active }.map { it.user }
	}
}
