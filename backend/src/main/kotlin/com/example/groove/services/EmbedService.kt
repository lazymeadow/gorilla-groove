@file:Suppress("unused")

package com.example.groove.services

import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.logger
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.io.IOUtils
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.servlet.resource.ResourceTransformer
import org.springframework.web.servlet.resource.ResourceTransformerChain
import org.springframework.web.servlet.resource.TransformedResource
import java.lang.IllegalArgumentException
import java.net.URLEncoder
import javax.servlet.http.HttpServletRequest


// A good read:
// https://medium.com/slack-developer-blog/everything-you-ever-wanted-to-know-about-unfurling-but-were-afraid-to-ask-or-how-to-make-your-e64b4bb9254
// Currently only using OGP as it's supported everywhere I tested, and oEmbed is not.
// The advantage of oEmbed is a lot of customization, but Slack strips audio tags out of HTML so why bother?
// This could change at any time though if they decide to stop being rude.
@Service
class EmbedService(
		private val trackService: TrackService
) {
	fun getOembedResponse(url: String): OembedResponse? {
		val trackId = url.split('/').lastOrNull()?.toLong() ?: run {
			logger.error("Could not parse a track ID from oembed URL! URL was: '$url'")
			throw IllegalArgumentException("Invalid link specified")
		}

		// It's probably safer to just always use MP3 here. As much as I want to use an OGG, not all clients may support it
		// and we have no way of knowing what our client necessarily is. Could be a damn Safari web browser
		val trackInfo = try {
			trackService.getPublicTrackInfo(trackId, true, AudioFormat.MP3)
		} catch (e: Throwable) {
			logger.warn("No track info found for oembed link for track ID: $trackId. It may have expired.")
			return null
		}

		return OembedResponse(
				title = listOfNotNull(trackInfo.name, trackInfo.artist.takeIf { it.isNotBlank() }).joinToString(" - "),
				url = trackInfo.trackLink
		)
	}

	companion object {
		private val logger = logger()
	}
}

data class OembedResponse(
		val version: String = "1.0",
		val type: String = "video",
		val width: Int = 240,
		val height: Int = 180,
		val title: String = "This is a test",

		@JsonProperty("provider_name")
		val providerName: String = "Gorilla Groove",

		@JsonProperty("provider_url")
		val providerUrl: String = "https://gorillagroove.net",

		@JsonIgnore
		val url: String,
) {
	val html: String = """
			<div>
			<audio src="$url" controls></audio>
			Damn this is a sick video player
			</div>
		""".trimIndent()
}


// This is used to add tags to the <head> of the HTML document that we serve up for the track link page.
// We can't do this in React because it needs to happen without JS involvement. So the server needs to
// transform the HTML on its way out. This is only for track-link pages.
@Configuration
class TrackLinkHtmlTransformer(
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

		// Currently not using oembed as there doesn't seem to be any point. But there might be in the future.
//		val finalHtml = transformForOembed(ogpHtml, originalUri, request)

		return TransformedResource(resource, ogpHtml.toByteArray())
	}

	private fun transformForOgp(html: String, trackId: Long, trackInfo: PublicTrackInfoDTO): String {
		val title = listOfNotNull(trackInfo.name, trackInfo.artist.takeIf { it.isNotBlank() }).joinToString(" - ")
		return html.replace("<head>", """
			<head>
		${createMetaTag("og:title", title)}
		${createMetaTag("og:type", "music.song")}
		${createMetaTag("og:url", "https://gorillagroove.net/track-link/$trackId")}
		${createMetaTag("og:image", trackInfo.albumArtLink ?: "")}
		${createMetaTag("og:image:width", "250")}
		${createMetaTag("og:image:height", "250")}
		${createMetaTag("og:image:alt", "Album art for ${trackInfo.album}")}
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
