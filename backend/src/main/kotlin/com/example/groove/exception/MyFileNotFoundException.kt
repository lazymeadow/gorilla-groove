package com.example.groove.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus


// Should map to an ExceptionHandler instead
@ResponseStatus(HttpStatus.NOT_FOUND)
class MyFileNotFoundException : RuntimeException {
    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}
}
