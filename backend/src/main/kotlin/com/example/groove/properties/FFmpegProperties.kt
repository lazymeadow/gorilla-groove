package com.example.groove.properties

import com.example.groove.util.endWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class FFmpegProperties {
	@Value("\${spring.data.ffmpeg.binary.location:#{null}}")
	val ffmpegBinaryLocation: String? = null
		get() = field?.endWith("/")
}
