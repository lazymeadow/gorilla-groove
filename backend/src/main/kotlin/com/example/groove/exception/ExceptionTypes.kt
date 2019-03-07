package com.example.groove.exception


class FileStorageException : RuntimeException {
    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}
}

class ResourceNotFoundException(message: String) : RuntimeException(message)
