package com.example.groove.services

import com.example.groove.properties.FFmpegProperties
import com.example.groove.properties.FileStorageProperties
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.springframework.stereotype.Component
import net.bramp.ffmpeg.FFmpegExecutor
import java.io.File
import java.util.*


@Component
class FFmpegService(
        private val ffmpegProperties: FFmpegProperties,
        private val fileStorageProperties: FileStorageProperties
) {

    fun convertTrack(fileName: String): File {
        val convertedFileName = UUID.randomUUID().toString() + ".ogg"

        val ffmpeg = FFmpeg(ffmpegProperties.ffmpegBinaryLocation + "ffmpeg")

        val ffprobe = FFprobe(ffmpegProperties.ffmpegBinaryLocation + "ffprobe")

        val builder = FFmpegBuilder()
                .addInput(fileStorageProperties.tmpDir + fileName)
                .addOutput(fileStorageProperties.tmpDir + convertedFileName)
                .done()

		// TODO clean up old file

        val executor = FFmpegExecutor(ffmpeg, ffprobe)
        executor.createJob(builder).run()

		return File(fileStorageProperties.tmpDir + convertedFileName)
    }

}
