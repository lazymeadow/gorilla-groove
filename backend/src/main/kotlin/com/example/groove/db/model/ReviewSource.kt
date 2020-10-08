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
		override val id: Long = 0,

		@JsonIgnore
		@ManyToMany
		@JoinTable(
				name = "review_source_user",
				joinColumns = [JoinColumn(name = "review_source_id")],
				inverseJoinColumns = [JoinColumn(name = "user_id")])
		open var subscribedUsers: MutableList<User> = mutableListOf(),

		@Column(name = "source_type")
		open val sourceType: ReviewSourceType,

		@Column(name = "created_at")
		override val createdAt: Timestamp = now(),

		@JsonIgnore
		@Column(name = "updated_at")
		override var updatedAt: Timestamp = now(),

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		override var deleted: Boolean = false
): RemoteSyncable {
	abstract val displayName: String

	fun isUserSubscribed(user: User): Boolean {
		return subscribedUsers.find { it.id == user.id } != null
	}
}
