package com.example.groove.properties


import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "file")
@Component
class FileStorageProperties {
    var uploadDir: String? = null
}