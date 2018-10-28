package com.example.groove.security

import org.springframework.security.web.RedirectStrategy
import javax.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest


@Component
class NoRedirectStrategy : RedirectStrategy {
	override fun sendRedirect(request: HttpServletRequest?, response: HttpServletResponse?, url: String?) {
		// This space intentionally left blank. We need no redirection in a pure REST API
	}

}