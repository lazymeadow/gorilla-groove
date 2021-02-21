package com.example.groove.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping


// This controller maps routes that the UI uses to the root which goes to index.html
// This will allow the frontend routing to handle them, since, they are frontend routes
@Controller
class ForwardingController {
	// In theory, I could combine these using the same function by using a regex
	// inside of the @RequestMapping. But I can only fight Spring for so long
	@RequestMapping("/login")
	fun forwardLogin(): String {
		return "forward:/"
	}

	@RequestMapping("/track-link/{trackId}")
	fun forwardTrackLink(): String {
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
