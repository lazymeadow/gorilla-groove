package com.example.groove.db.dao

import com.example.groove.db.model.PasswordReset
import org.springframework.data.repository.CrudRepository

interface PasswordResetRepository : CrudRepository<PasswordReset, Long> {

	fun findByUniqueKey(uniqueKey: String): PasswordReset?
}
