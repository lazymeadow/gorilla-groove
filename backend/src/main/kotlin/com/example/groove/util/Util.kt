package com.example.groove.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.http.client.utils.URIBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.CrudRepository
import org.springframework.http.HttpHeaders
import org.springframework.util.LinkedMultiValueMap
import java.awt.Image
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File


// JPA repositories use Java's Optional class
// Kotlin has its own, better way of handling nullable values
// Here we extend CrudRepository with another method that returns a Kotlin nullable instead
fun<T> CrudRepository<T, Long>.get(id: Long): T? {
	return this.findById(id).orElse(null)
}

fun createMapper(): ObjectMapper = ObjectMapper()
		.registerKotlinModule()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

fun Image.writeToFile(destination: File, imageType: String) {
	val bufferedImage = BufferedImage(this.getWidth(null), this.getHeight(null), BufferedImage.TYPE_INT_RGB)
	bufferedImage.graphics.drawImage(this, 0, 0, null)
	ImageIO.write(bufferedImage, imageType, destination)
}

@Suppress("unused")
inline fun <reified T> T.logger(): Logger {
	return LoggerFactory.getLogger(T::class.java)
}

// Idk why RestTemplate doesn't work with normal maps. So swap to one of these instead
fun<T, E> Map<T, E>.toPostBody(): LinkedMultiValueMap<T, E> {
	val body = LinkedMultiValueMap<T, E>()
	this.forEach { (k, v) ->
		body.add(k, v)
	}

	return body
}

fun Map<String, String>.toHeaders(): HttpHeaders {
	val headers = HttpHeaders()
	this.forEach { (k, v) ->
		headers.set(k, v)
	}

	return headers
}

// The combination of URIBuilder and RestTemplate seem to double-encode things and make things very upset.
// I didn't see a way to disable one of them encoding... so just have a way to undo I guess
fun URIBuilder.toUnencodedString(): String {
	return this.toString().urlDecode()
}
