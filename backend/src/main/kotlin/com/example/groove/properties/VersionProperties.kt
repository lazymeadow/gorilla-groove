package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class VersionProperties {
	@Value("\${info.build.version}")
	val version: String? = null
}
