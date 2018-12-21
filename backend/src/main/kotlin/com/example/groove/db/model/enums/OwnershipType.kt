package com.example.groove.db.model.enums

// The ordinal value of these is used for mapping to the DB
// Use caution when editing the order, and only add new ones to the end
enum class OwnershipType(val hasWritePrivilege: Boolean) {
	OWNER(true),
	WRITER(true),
	READER(false)
}
