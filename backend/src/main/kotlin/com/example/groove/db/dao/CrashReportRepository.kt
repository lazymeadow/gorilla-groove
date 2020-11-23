package com.example.groove.db.dao

import com.example.groove.db.model.CrashReport
import org.springframework.data.repository.CrudRepository

interface CrashReportRepository : CrudRepository<CrashReport, Long>
