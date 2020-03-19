package com.example.groove.db.model

import com.example.groove.db.model.enums.DeviceType
import com.example.groove.util.DateUtils.now
import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "device")
data class Device(

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		val id: Long = 0,

		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "user_id")
		val user: User,

		// Multiple devices can be merged together to act as one device
		// Should only ever go 1 level deep as references are updated if multiple merges happen
		@JsonIgnore
		@ManyToOne
		@JoinColumn(name = "merged_device_id")
		var mergedDevice: Device? = null,

		@JsonIgnore
		@OneToMany(mappedBy = "mergedDevice", fetch = FetchType.LAZY)
		val mergedDevices: List<Device> = mutableListOf(),

		@Enumerated
		val deviceType: DeviceType,

		@Column(name = "device_id")
		val deviceId: String,

		@Column(name = "device_name")
		var deviceName: String,

		@JsonIgnore
		@Column(columnDefinition = "BIT")
		var archived: Boolean = false,

		@Column(name = "application_version")
		var applicationVersion: String,

		@Column(name = "last_ip")
		var lastIp: String,

		@Column(name = "additional_data")
		var additionalData: String? = null,

		@Column(name = "party_enabled_until")
		var partyEnabledUntil: Timestamp? = null,

		@JsonIgnore
		@ManyToMany
		@JoinTable(
				name = "device_party_user",
				joinColumns = [JoinColumn(name = "device_id")],
				inverseJoinColumns = [JoinColumn(name = "user_id")])
		var partyUsers: MutableList<User> = mutableListOf(),

		@Column(name = "created_at")
		val createdAt: Timestamp = now(),

		@Column(name = "updated_at")
		var updatedAt: Timestamp = now()
)
