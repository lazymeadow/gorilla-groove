package com.example.groove.util

import com.example.groove.db.model.User
import org.springframework.security.core.context.SecurityContextHolder

fun loadLoggedInUser(): User {
	return SecurityContextHolder.getContext().authentication.principal as User
}