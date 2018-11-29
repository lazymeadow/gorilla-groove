package com.example.groove.config

import com.example.groove.properties.MusicProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class StaticResourceConfiguration(
		private val musicProperties: MusicProperties
): WebMvcConfigurer {
	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		// Create an authenticated path for the frontend to grab songs from
		registry
				.addResourceHandler("/music/**")
				.addResourceLocations("file:${musicProperties.musicDirectoryLocation}")
		registry
				.addResourceHandler("/album-art/**")
				.addResourceLocations("file:${musicProperties.albumArtDirectoryLocation}")
	}
}
