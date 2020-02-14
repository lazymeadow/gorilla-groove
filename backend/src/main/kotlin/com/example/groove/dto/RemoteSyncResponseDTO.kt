package com.example.groove.dto

import com.example.groove.db.model.RemoteSyncable

data class RemoteSyncResponseDTO(
		val content: EntityChangesDTO,
		val pageable: PageResponseDTO
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
