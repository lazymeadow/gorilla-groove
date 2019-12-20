package com.example.groove.db.dao

import com.example.groove.db.model.User
import com.example.groove.db.model.UserPermission
import com.example.groove.db.model.enums.PermissionType
import org.springframework.data.repository.CrudRepository

interface UserPermissionRepository : CrudRepository<UserPermission, Long> {
	fun findByUser(user: User): List<UserPermission>
	fun findByUserAndPermissionType(User: User, permissionType: PermissionType): UserPermission?
}
