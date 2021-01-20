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

		// This basically just means whether or not a client device should be told that this exists
		@JsonIgnore
		@Column(columnDefinition = "BIT")
		override var deleted: Boolean = false,

		// This currently only means something for Artist sources. Someone can add music to review from an artist
		// without being subscribed to a source. So it works out like:
		// 1) !deleted && active -> user is subscribed and is getting new music released by this artist
		// 2) !deleted && !active -> user has music to review by a given artist, but is not seeing new music
		// 3) deleted -> user has no reason to even know that this source exists and it will not be returned
		// That being said, I don't actually set "deleted" to true after you finish reviewing all tracks on an
		// "inactive" queue. I should, but I am too lazy and it doesn't REALLY cause issues. Just makes the payload larger
		@JsonIgnore
		@Column(columnDefinition = "BIT")
		var active: Boolean = true
) : RemoteSyncable {
	override fun toSyncDTO() = ReviewSourceUserDTO(
			id = this.reviewSource.id,
			offlineAvailabilityType = this.offlineAvailabilityType,
			sourceType = this.reviewSource.sourceType,
			displayName = this.reviewSource.displayName,
			updatedAt = this.updatedAt,
			active = this.active
	)
}

class ReviewSourceUserDTO(
		override val id: Long,
		val offlineAvailabilityType: OfflineAvailabilityType,
		val sourceType: ReviewSourceType,
		val displayName: String,
		val updatedAt: Timestamp,
		val active: Boolean
) : SyncDTO
