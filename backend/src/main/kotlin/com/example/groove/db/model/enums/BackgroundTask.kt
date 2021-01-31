package com.example.groove.db.model.enums

enum class BackgroundProcessType {
	YT_DOWNLOAD,
	NAMED_IMPORT,
	USER_LIBRARY_IMPORT,
}

enum class BackgroundProcessStatus {
	PENDING, RUNNING, COMPLETE, FAILED
}
