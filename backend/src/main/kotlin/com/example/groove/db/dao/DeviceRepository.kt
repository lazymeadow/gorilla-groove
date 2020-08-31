package com.example.groove.db.dao

import com.example.groove.db.model.Device
import com.example.groove.db.model.User
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface DeviceRepository : CrudRepository<Device, Long> {
	fun findByUser(user: User): List<Device>

	@Query("""
			SELECT d
			FROM Device d
			WHERE d.user.id = :userId
			AND d.deviceId = :deviceId
		""")
	fun findByDeviceIdAndUser(
			@Param("deviceId") deviceId: String,
			@Param("userId") userId: Long
	): Device?
}
