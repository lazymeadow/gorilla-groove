package com.example.groove.exception


class FileStorageException(message: String, cause: Throwable) : RuntimeException(message, cause)

class ResourceNotFoundException(message: String) : RuntimeException(message)

class PermissionDeniedException(message: String) : RuntimeException(message)

// I don't like the build in javax one. Just keep it simple with a string
class ConstraintViolationException(message: String) : RuntimeException(message)
