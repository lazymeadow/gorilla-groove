package com.example.groove.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.CrudRepository
import org.springframework.http.HttpHeaders
import org.springframework.util.LinkedMultiValueMap
import java.awt.Image
import java.util.*
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File


// JPA repositories use Java's Optional class
// Kotlin has its own, better way of handling nullable values
// Here we extend Java's Optional type to return a Kotlin nullable type so it is easier to work with
fun <T> Optional<T>.unwrap(): T? = orElse(null)

// Screw the previous solution above this. Just add an extension function directly to CrudRepository.
fun<T> CrudRepository<T, Long>.get(id: Long): T? {
	return this.findById(id).unwrap()
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
