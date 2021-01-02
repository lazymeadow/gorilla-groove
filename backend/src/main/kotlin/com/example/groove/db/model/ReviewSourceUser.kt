@file:Suppress("unused")

package com.example.groove.db.model

import com.example.groove.db.model.enums.OfflineAvailabilityType
import com.example.groove.db.model.enums.ReviewSourceType
import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "review_source_user")
class ReviewSourceUser(
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		override val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "review_source_id", nullable = false)
		val reviewSource: ReviewSource,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "user_id", nullable = false)
		val user: User,

		@Column(name = "offline_availability")
		val offlineAvailabilityType: OfflineAvailabilityType = OfflineAvailabilityType.NORMAL,

		@Column(name = "created_at")
		override val createdAt: Timestamp = now(),

		@JsonIgnore
		@Column(name = "updated_at")
		override var updatedAt: Timestamp = now(),

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		override var deleted: Boolean = false
) : RemoteSyncable {
	override fun toSyncDTO() = ReviewSourceUserDTO(
			id = this.reviewSource.id,
			offlineAvailabilityType = this.offlineAvailabilityType,
			sourceType = this.reviewSource.sourceType,
			displayName = this.reviewSource.displayName,
			updatedAt = this.updatedAt
	)
}

class ReviewSourceUserDTO(
		val id: Long,
		val offlineAvailabilityType: OfflineAvailabilityType,
		val sourceType: ReviewSourceType,
		val displayName: String,
		val updatedAt: Timestamp
)
