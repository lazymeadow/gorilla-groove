package com.example.groove.db.dao

import com.example.groove.db.model.CrashReport
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface CrashReportRepository : CrudRepository<CrashReport, Long> {

	@Query("""
			SELECT cr
			FROM CrashReport cr
			ORDER BY cr.id DESC
		""")
	fun findAllNewestFirst(): List<CrashReport>

}
