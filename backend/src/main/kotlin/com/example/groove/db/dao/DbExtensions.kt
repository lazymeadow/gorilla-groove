package com.example.groove.db.dao

import org.springframework.data.jpa.repository.JpaRepository

// findAllById returns things in whatever order it wants. It's wasteful to do a sort
// when we already have the desired order. This will preserve the sort order with
// minimal overhead as it's all in memory and has constant time lookups with no comparisons
fun<T: GGEntity> JpaRepository<T, Long>.findAllByIdOrdered(ids: List<Long>): List<T> {
	val idToType = mutableMapOf<Long, T>()
	findAllById(ids).forEach { entity ->
		idToType[entity.id] = entity
	}

	return ids.mapNotNull { idToType[it] }
}

interface GGEntity {
	val id: Long
}
