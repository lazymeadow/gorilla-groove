package com.example.groove.dto

import com.example.groove.db.model.RemoteSyncable
import com.example.groove.db.model.enums.SyncableEntityType
import java.sql.Timestamp

data class RemoteSyncResponseDTO(
		val content: EntityChangesDTO,
		val pageable: PageResponseDTO
)

data class LastModifiedResponseDTO(
		val lastModifiedTimestamps: Map<SyncableEntityType, Timestamp>
)

data class EntityChangesDTO(
		val new: List<RemoteSyncable>,
		val modified: List<RemoteSyncable>,
		val removed: List<Long>
)

data class PageResponseDTO(
		val offset: Long,
		val pageSize: Int,
		val pageNumber: Int,
		val totalPages: Int,
		val totalElements: Long
)
