package com.example.groove.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.awt.Image
import java.util.*
import java.io.IOException
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File


// JPA repositories use Java's Optional class
// Kotlin has its own, better way of handling nullable values
// Here we extend Java's Optional type to return a Kotlin nullable type so it is easier to work with
fun <T> Optional<T>.unwrap(): T? = orElse(null)

fun createMapper(): ObjectMapper = ObjectMapper()
		.registerKotlinModule()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

fun String.blankAsNull(): String? {
	return if (this.isBlank()) null else this
}

fun Image.writeToFile(destination: File, imageType: String) {
	val bufferedImage = BufferedImage(this.getWidth(null), this.getHeight(null), BufferedImage.TYPE_INT_RGB)
	bufferedImage.graphics.drawImage(this, 0, 0, null)
	ImageIO.write(bufferedImage, imageType, destination)
}
