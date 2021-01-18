package com.example.groove.db.dao

import com.example.groove.db.model.BackgroundTaskItem
import com.example.groove.db.model.enums.BackgroundProcessStatus
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface BackgroundTaskItemRepository : CrudRepository<BackgroundTaskItem, Long> {
	@Query("""
			SELECT bti
			FROM BackgroundTaskItem bti
			WHERE bti.id IN (:ids)
		""")
	fun findAllByIdIn(
			@Param("ids") ids: List<Long>
	): List<BackgroundTaskItem>

	@Query("""
			SELECT bti
			FROM BackgroundTaskItem bti
			WHERE (:userId IS NULL OR bti.user.id = :userId)
			AND status IN (:statuses)
		""")
	fun findWhereStatusIsIn(
			@Param("statuses") statuses: List<BackgroundProcessStatus>,
			@Param("userId") userId: Long?
	): List<BackgroundTaskItem>
}
