package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class FFmpegProperties {
	@Value("\${spring.data.ffmpeg.binary.location}")
	val ffmpegBinaryLocation: String? = null

	@Value("\${spring.data.ffmpeg.output.location}")
	val ffmpegOutputLocation: String? = null
}