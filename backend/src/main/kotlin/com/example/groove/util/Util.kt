package com.example.groove.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.*

// JPA repositories use Java's Optional class
// Kotlin has its own, better way of handling nullable values
// Here we extend Java's Optional type to return a Kotlin nullable type so it is easier to work with
fun <T> Optional<T>.unwrap(): T? = orElse(null)

fun createMapper(): ObjectMapper = ObjectMapper()
		.registerKotlinModule()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
