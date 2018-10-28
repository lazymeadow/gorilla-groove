package com.example.groove.services

import com.example.groove.properties.FFmpegProperties
import com.example.groove.properties.FileStorageProperties
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.springframework.stereotype.Component
import net.bramp.ffmpeg.FFmpegExecutor
import org.springframework.beans.factory.annotation.Autowired


@Component

class FFmpegService @Autowired constructor(
        val ffmpegProperties: FFmpegProperties,
        val fileStorageProperties: FileStorageProperties
) {

    fun convertTrack(fileName: String): String {
        val convertedFileName = fileName.substringBeforeLast('.') + ".ogg"

        val ffmpeg = FFmpeg(ffmpegProperties.ffmpegBinaryLocation + "ffmpeg")

        val ffprobe = FFprobe(ffmpegProperties.ffmpegBinaryLocation + "ffprobe")

        val builder = FFmpegBuilder()
                .addInput(fileStorageProperties.uploadDir + fileName)
                .addOutput(ffmpegProperties.ffmpegOutputLocation + convertedFileName)
                .done()

        val executor = FFmpegExecutor(ffmpeg, ffprobe)
        executor.createJob(builder).run()

		return convertedFileName
    }

}
