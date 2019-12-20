package com.example.groove.exception


class FileStorageException(message: String, cause: Throwable) : RuntimeException(message, cause)

class ResourceNotFoundException(message: String) : RuntimeException(message)

class PermissionDeniedException(message: String) : RuntimeException(message)
