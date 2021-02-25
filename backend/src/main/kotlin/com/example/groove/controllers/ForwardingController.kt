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

@Configuration
class MainPageTransformer(
		private val environment: Environment,
		private val trackService: TrackService
) : ResourceTransformer {
	override fun transform(request: HttpServletRequest, resource: Resource, transformerChain: ResourceTransformerChain): Resource {
		val originalUri = request.getAttribute("javax.servlet.forward.request_uri") as? String

		if (originalUri?.startsWith("/track-link/") != true) {
			return resource
		}

		val trackId = originalUri.split('/').lastOrNull()?.toLong() ?: run {
			logger.error("Could not parse a track ID from request URI! URI was: '$originalUri'")
			return resource
		}

		val trackInfo = try {
			trackService.getPublicTrackInfo(trackId, true, AudioFormat.MP3)
		} catch (e: Throwable) {
			logger.warn("No track info found for oembed link for track ID: $trackId. It may have expired.")
			return resource
		}

		val html = IOUtils.toString(resource.inputStream, "UTF-8")

		val ogpHtml = transformForOgp(html, trackId, trackInfo)
		val finalHtml = transformForOembed(ogpHtml, originalUri, request)

		return TransformedResource(resource, finalHtml.toByteArray())
	}

	private fun transformForOgp(html: String, trackId: Long, trackInfo: PublicTrackInfoDTO): String {
		return html.replace("<head>", """
			<head>
		${createMetaTag("og:title", trackInfo.name)}
		${createMetaTag("og:type", "music.song")}
		${createMetaTag("og:url", "https://gorillagroove.net/track-link/$trackId")}
		${createMetaTag("og:image", trackInfo.albumArtLink ?: "")}
		${createMetaTag("og:image:width", "250")}
		${createMetaTag("og:image:height", "250")}
		${createMetaTag("og:image:alt", "Album art for ${trackInfo.album}")}
		${createMetaTag("music:duration", trackInfo.length.toString())}
		${createMetaTag("music:musician", trackInfo.artist)}
		${createMetaTag("og:audio", trackInfo.trackLink)}
		${createMetaTag("og:audio:type", "audio/mpeg")}
		""".trimIndent())
	}

	private fun createMetaTag(tag: String, content: String): String {
		return if (content.isNotBlank()) {
			return """<meta property="$tag" content="$content">"""
		} else {
			""
		}
	}

	private fun transformForOembed(html: String, originalUri: String, request: HttpServletRequest): String {
		val isProd = environment.activeProfiles.contains("prod")
		val port = if (isProd) "" else ":" + request.serverPort
		val serverName = if (isProd) "gorillagroove.net" else "localhost"
		val scheme = if (isProd) "https://" else "http://"

		val ggUrl = scheme + serverName + port
		val userUrl = URLEncoder.encode(ggUrl + originalUri, "UTF-8")
		val oembedUrl = "$ggUrl/api/oembed?url=$userUrl"
		val linkElement = """<link href="$oembedUrl" rel="alternate" type="application/json+oembed" title="Gorilla Groove Track Link">"""

		return html.replace("<head>", "<head>$linkElement")
	}

	companion object {
		private val logger = logger()
	}
}
