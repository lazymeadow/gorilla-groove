package com.example.groove.services

import com.example.groove.db.dao.*
import com.example.groove.db.model.*
import com.example.groove.db.model.enums.DeviceType
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp


@Service
class DeviceService(
		private val deviceRepository: DeviceRepository
) {

	@Transactional(readOnly = true)
	fun getDevices(user: User): List<Device> {
		return deviceRepository.findByUser(user)
	}

	@Transactional
	fun createOrUpdateDevice(
			user: User,
			deviceId: String,
			deviceType: DeviceType,
			version: String,
			ipAddress: String?
	) {
		val device = deviceRepository.findByDeviceIdAndUser(deviceId, user)
				?: run {
					logger.info("First time seeing device $deviceId for user ${user.name}")
					Device(
							user = user,
							deviceId = deviceId,
							deviceType = deviceType,
							applicationVersion = version,
							lastIp = ipAddress ?: "0.0.0.0"
					)
				}

		device.applicationVersion = version
		device.updatedAt = Timestamp(System.currentTimeMillis())
		ipAddress?.let { device.lastIp = ipAddress }

		deviceRepository.save(device)
	}

	companion object {
		val logger = LoggerFactory.getLogger(DeviceService::class.java)!!
	}
}
