package com.example.groove.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Suppress("unused")
@Configuration
@EnableWebMvc
class WebMvcConfig : WebMvcConfigurer {

	override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
		// Prevent clients of the API from needing to specify the Content-Type header since everything will be JSON
		// Actually maybe this didn't actually accomplish that goal... Dangit Spring
		configurer.defaultContentType(MediaType.APPLICATION_JSON)
	}
}

