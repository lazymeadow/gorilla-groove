@file:Suppress("unused")

package com.example.groove.controllers.exceptionhandler

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


/**
 * When we return IllegalArgumentException from a controller, we should default to
 * telling the client that it is a Bad Request (400)
 */
@ControllerAdvice
class IllegalArgumentExceptionHandler : ResponseEntityExceptionHandler() {

	@ExceptionHandler(IllegalArgumentException::class)
	protected fun handleIllegalArgument(ex: RuntimeException, request: WebRequest): ResponseEntity<Any> {
		return handleExceptionInternal(ex, ex.message, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
	}
}
