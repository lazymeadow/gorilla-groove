package com.example.groove.controllers

import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


// This controller maps routes that the UI uses to the root which goes to index.html
// This will allow the frontend routing to handle them, since, they are frontend routes
@Controller
class ForwardingController(
		private val environment: Environment,
) {
	// In theory, I could combine these using the same function by using a regex
	// inside of the @RequestMapping. But I can only fight Spring for so long
	@RequestMapping("/login")
	fun forwardLogin(): String {
		return "forward:/"
	}

	@RequestMapping("/track-link/{trackId}")
	fun forwardTrackLink(response: HttpServletResponse, request: HttpServletRequest): String {
		val isProd = environment.activeProfiles.contains("prod")
		val port = if (isProd) "" else ":" + request.serverPort
		val serverName = if (isProd) "gorillagroove.net" else "localhost"
		val scheme = if (isProd) "https://" else "http://"

		val ggUrl = scheme + serverName + port
		val userUrl = ggUrl + request.servletPath
		val oembedUrl = "$ggUrl/api/oembed?url=$userUrl"
		val header = """<$oembedUrl>; rel="alternate"; type="application/json+oembed"; title="Gorilla Groove Track Link""""

		response.addHeader("Link", header)

		return "forward:/"
	}

	@RequestMapping("/create-account/{inviteIdentifier}")
	fun forwardUserCreationInvite(): String {
		return "forward:/"
	}

	@RequestMapping("/password-reset/{resetIdentifier}")
	fun forwardPasswordReset(): String {
		return "forward:/"
	}

	@RequestMapping("/privacy-policy")
	fun forwardPrivacyPolicy(): String {
		return "forward:/"
	}

	@RequestMapping("/")
	fun forwardSlash(): String {
		return "forward:/index.html"
	}
}
