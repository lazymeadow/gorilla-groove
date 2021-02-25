package com.example.groove.controllers

import com.example.groove.services.PublicTrackInfoDTO
import com.example.groove.services.TrackService
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.logger
import org.apache.commons.io.IOUtils
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.resource.ResourceTransformer
import org.springframework.web.servlet.resource.ResourceTransformerChain
import org.springframework.web.servlet.resource.TransformedResource
import java.net.URLEncoder
import javax.servlet.http.HttpServletRequest


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
