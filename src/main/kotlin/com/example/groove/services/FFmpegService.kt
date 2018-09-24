package com.example.groove.services

import com.example.groove.properties.FFmpegProperties
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.springframework.stereotype.Component
import net.bramp.ffmpeg.FFmpegExecutor
import org.springframework.beans.factory.annotation.Autowired

@Component
class FFmpegService(@Autowired val ffmpegProperties: FFmpegProperties) {

	fun test() {
		val ffmpeg = FFmpeg(ffmpegProperties.ffmpegBinaryLocation + "ffmpeg")
		val ffprobe = FFprobe(ffmpegProperties.ffmpegBinaryLocation + "ffprobe")

		val builder = FFmpegBuilder()
				.addInput(ffmpegProperties.ffmpegOutputLocation + "Besaid Island.mp3")
				.addOutput(ffmpegProperties.ffmpegOutputLocation + "Besaid Island.ogg")
				.done()

		val executor = FFmpegExecutor(ffmpeg, ffprobe)
		executor.createJob(builder).run()
	}
}
