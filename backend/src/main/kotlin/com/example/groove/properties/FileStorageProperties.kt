package com.example.groove.properties


import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "file")
@Component
class FileStorageProperties {
	// TODO Spring gets mad when this is a val, but the other property files are vals
	// This file should be altered to be in line with the other property files
    var uploadDir: String? = null // This is basically a tmp directory for song processing
}
