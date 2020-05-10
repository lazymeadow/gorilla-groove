package com.example.groove.util

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object DateUtils {
	fun now(): Timestamp {
		return Timestamp(System.currentTimeMillis())
	}
}

fun Timestamp.isNewerThan(timeUnit: ChronoUnit, timeAmount: Long): Boolean {
	val comparison = DateUtils
			.now()
			.toInstant()
			.minus(timeAmount, timeUnit)

	return this.after(comparison.toTimestamp())
}

fun LocalDateTime.toTimestamp(): Timestamp {
	return Timestamp.valueOf(this)
}
fun Instant.toTimestamp(): Timestamp {
	return Timestamp.from(this)
}
