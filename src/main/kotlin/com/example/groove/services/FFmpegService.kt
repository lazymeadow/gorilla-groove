package com.example.groove.services

import com.example.groove.properties.FFmpegProperties
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.springframework.stereotype.Component
import net.bramp.ffmpeg.FFmpegExecutor
import org.aspectj.weaver.tools.cache.SimpleCacheFactory.path
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

@Component
class FFmpegService(@Autowired val ffmpegProperties: FFmpegProperties) {

	fun convertTrack(fileName:String) {

//		val file = File("C:\\Program Files\\FFmpeg\\bin\\ffmpeg")
//		if (!file.exists()) {
//			logger.error("File was not found using the path '$file'")
//			throw IllegalArgumentException("File by name '$file' does not exist!")
//		}
		val ffmpeg = FFmpeg(ffmpegProperties.ffmpegBinaryLocation + "ffmpeg")

		val ffprobe = FFprobe(ffmpegProperties.ffmpegBinaryLocation + "ffprobe")

		val builder = FFmpegBuilder()
				.addInput(ffmpegProperties.ffmpegBinaryLocation + fileName)
				.addOutput(ffmpegProperties.ffmpegOutputLocation + "Dichotomy.ogg")
				.done()

		val executor = FFmpegExecutor(ffmpeg, ffprobe)
		executor.createJob(builder).run()
	}

	companion object {
		val logger = LoggerFactory.getLogger(FFmpegService::class.java)!!
	}
}
