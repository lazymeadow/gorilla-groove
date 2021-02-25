package com.example.groove.config

import com.example.groove.properties.FileStorageProperties
import com.example.groove.services.TrackLinkHtmlTransformer
import com.example.groove.util.endWith
import com.example.groove.util.logger
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths


@Configuration
class StaticResourceConfiguration(
		private val fileStorageProperties: FileStorageProperties,
		private val environment: Environment,
		private val transformer: TrackLinkHtmlTransformer
): WebMvcConfigurer {
	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		// Create an authenticated path for the frontend to grab songs from
		logger.info("Setting music directory: ${fileStorageProperties.musicDirectoryLocation}")
		registry
				.addResourceHandler("/music/**")
				.addResourceLocations("file:${fileStorageProperties.musicDirectoryLocation}")

		logger.info("Setting album art directory: ${fileStorageProperties.albumArtDirectoryLocation}")
		registry
				.addResourceHandler("/album-art/**")
				.addResourceLocations("file:${fileStorageProperties.albumArtDirectoryLocation}")

		// We don't want to do this for prod builds as the static content is bundled into the .war
		// This is purely for easier local development
		if (!environment.activeProfiles.contains("prod")) {
			// user.dir seems to go to the root of the backend
			val frontendProjectPath = System.getProperty("user.dir") + "/../frontend/"
			val normalizedPath = Paths.get(frontendProjectPath)
					.normalize()
					.toString()
					.endWith("/")

			logger.info("Setting static content directory: $normalizedPath")

			registry
					.addResourceHandler("/**")
					.addResourceLocations("file:$normalizedPath")
			registry
					.addResourceHandler("/index.html")
					.addResourceLocations("file:$normalizedPath")
					.resourceChain(false)
					.addTransformer(transformer)

		} else {
			registry
					.addResourceHandler("/index.html")
					.addResourceLocations("classpath:/static/index.html")
					.resourceChain(false)
					.addTransformer(transformer)
		}
	}

	companion object {
		private val logger = logger()
	}
}
