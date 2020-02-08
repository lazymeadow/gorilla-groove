package com.example.groove.config

import com.example.groove.properties.FileStorageProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class StaticResourceConfiguration(
		private val fileStorageProperties: FileStorageProperties
): WebMvcConfigurer {
	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		// Create an authenticated path for the frontend to grab songs from
		registry
				.addResourceHandler("/music/**")
				.addResourceLocations("file:${fileStorageProperties.musicDirectoryLocation}")
		registry
				.addResourceHandler("/album-art/**")
				.addResourceLocations("file:${fileStorageProperties.albumArtDirectoryLocation}")
	}
}
