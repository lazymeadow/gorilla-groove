package com.example.groove.db.model.enums

// The ordinal value of these is used for mapping to the DB
// Use caution when editing the order, and only add new ones to the end
@Suppress("unused")
enum class PermissionType {
	WRITE_VERSION_HISTORY,
	EXPERIMENTAL,
	INVITE_USER,
	RUN_REVIEW_QUEUES,
	VIEW_CRASH_REPORTS
}
