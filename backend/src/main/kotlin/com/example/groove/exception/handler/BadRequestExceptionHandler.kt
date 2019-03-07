@file:Suppress("unused")

package com.example.groove.exception.handler

import com.example.groove.exception.ResourceNotFoundException
import org.apache.tomcat.util.http.fileupload.FileUploadBase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException


@ControllerAdvice
class BadRequestExceptionHandler : ResponseEntityExceptionHandler() {

	// These exceptions should return a 400 to the client
	@ExceptionHandler(
			IllegalArgumentException::class,
			FileUploadBase.FileSizeLimitExceededException::class,
			MaxUploadSizeExceededException::class
	)
	protected fun handleIllegalArgument(ex: RuntimeException, request: WebRequest): ResponseEntity<Any> {
		return handleExceptionInternal(ex, ex.message, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
	}

	// These exceptions should return a 404 to the client
	@ExceptionHandler(ResourceNotFoundException::class)
	protected fun handleNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity<Any> {
		return handleExceptionInternal(ex, ex.message, HttpHeaders(), HttpStatus.NOT_FOUND, request)
	}
}
