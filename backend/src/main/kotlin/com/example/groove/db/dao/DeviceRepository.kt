package com.example.groove.db.dao

import com.example.groove.db.model.Device
import com.example.groove.db.model.User
import org.springframework.data.repository.CrudRepository

interface DeviceRepository : CrudRepository<Device, Long> {
	fun findByUser(user: User): List<Device>
	fun findByDeviceIdAndUser(deviceId: String, user: User): Device?
}
