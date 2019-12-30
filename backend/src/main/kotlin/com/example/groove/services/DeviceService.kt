package com.example.groove.services

import com.example.groove.db.dao.*
import com.example.groove.db.model.*
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp


@Service
class DeviceService(
		private val deviceRepository: DeviceRepository,
		private val trackHistoryRepository: TrackHistoryRepository
) {

	@Transactional(readOnly = true)
	fun getDevices(user: User): List<Device> {
		return deviceRepository.findByUser(user)
	}

	@Transactional(readOnly = true)
	fun getDevice(deviceId: String): Device {
		return deviceRepository.findByDeviceIdAndUser(deviceId, loadLoggedInUser())
				?: throw ResourceNotFoundException("No device found with id $deviceId")
	}

	@Transactional
	fun createOrUpdateDevice(
			user: User,
			deviceId: String,
			deviceType: DeviceType,
			version: String,
			ipAddress: String?,
			additionalData: String?
	) {
		val device = deviceRepository.findByDeviceIdAndUser(deviceId, user)
				?: run {
					logger.info("First time seeing device $deviceId for user ${user.name}")
					Device(
							user = user,
							deviceId = deviceId,
							deviceName = deviceId,
							deviceType = deviceType,
							applicationVersion = version,
							lastIp = ipAddress ?: "0.0.0.0"
					)
				}

		// If this device has been merged, we need to update the version of the parent
		val deviceToUpdate = device.mergedDevice ?: device

		deviceToUpdate.applicationVersion = version
		deviceToUpdate.updatedAt = Timestamp(System.currentTimeMillis())
		deviceToUpdate.additionalData = additionalData
		ipAddress?.let { deviceToUpdate.lastIp = ipAddress }

		deviceRepository.save(deviceToUpdate)
	}

	private fun findActiveDevice(id: Long): Device {
		val device = deviceRepository.findById(id).unwrap()
		if (device == null || device.user.id != loadLoggedInUser().id) {
			throw ResourceNotFoundException("No device found with ID $id")
		}

		return device
	}

	@Transactional
	fun updateDevice(id: Long, deviceName: String) {
		val device = findActiveDevice(id)

		device.deviceName = deviceName
		deviceRepository.save(device)
	}

	@Transactional
	fun archiveDevice(id: Long, archived: Boolean) {
		val device = findActiveDevice(id)

		device.archived = archived
		deviceRepository.save(device)
	}

	@Transactional
	fun deleteDevice(id: Long) {
		val device = findActiveDevice(id)

		// Database handles nulling the references to this in TrackHistory
		deviceRepository.delete(device)
	}

	@Transactional
	fun mergeDevices(id: Long, targetId: Long) {
		val user = loadLoggedInUser()

		logger.info("Merging device $id into $targetId for user ${user.name}")

		val device = findActiveDevice(id)
		val targetDevice = findActiveDevice(targetId)

		if (device.deviceType != targetDevice.deviceType) {
			throw IllegalArgumentException("Cannot merge two devices that have a different device type!")
		}

		device.mergedDevice = targetDevice
		deviceRepository.save(device)

		// Update all our old references so that they point to just one device- the one we merged into
		trackHistoryRepository.updateDevice(newDevice = targetDevice, oldDevice = device)

		if (device.mergedDevices.isNotEmpty()) {
			logger.info("Merging ${device.mergedDevices.size} additional device(s) into $targetId")
			device.mergedDevices.forEach {
				it.mergedDevice = targetDevice
				deviceRepository.save(it)
			}
		}
	}

	companion object {
		val logger = LoggerFactory.getLogger(DeviceService::class.java)!!
	}
}
