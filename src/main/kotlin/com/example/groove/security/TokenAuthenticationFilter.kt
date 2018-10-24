package com.example.groove.security

import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletException
import java.io.IOException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.apache.commons.lang3.StringUtils.removeStart
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.web.util.matcher.RequestMatcher
import javax.servlet.FilterChain


class TokenAuthenticationFilter(requiresAuth: RequestMatcher) : AbstractAuthenticationProcessingFilter(requiresAuth) {

	override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication? {
		val param = request.getHeader(AUTHORIZATION) ?: request.getParameter("t")

		val token = param?.let {
			removeStart(param, BEARER).trim()
		} ?: throw BadCredentialsException("Missing Authentication Token")

		val auth = UsernamePasswordAuthenticationToken(token, token)
		return authenticationManager.authenticate(auth)
	}

	@Throws(IOException::class, ServletException::class)
	override fun successfulAuthentication(
			request: HttpServletRequest,
			response: HttpServletResponse,
			chain: FilterChain,
			authResult: Authentication) {
		super.successfulAuthentication(request, response, chain, authResult)
		chain.doFilter(request, response)
	}

	companion object {
		private const val BEARER = "Bearer"
	}
}