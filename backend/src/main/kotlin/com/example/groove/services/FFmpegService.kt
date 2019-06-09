package com.example.groove.services

import com.example.groove.properties.FFmpegProperties
import com.example.groove.properties.FileStorageProperties
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.springframework.stereotype.Component
import net.bramp.ffmpeg.FFmpegExecutor
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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

		// Delete the non-converted file, as we no longer need it
		File(fileStorageProperties.tmpDir + fileName).delete()

		return File(fileStorageProperties.tmpDir + convertedFileName)
    }

	// Start and End time must be strings like "00:01:02.500"
	fun trimSong(song: File, startTime: String?, duration: String?): File {
		// The latest version of the ffmpeg wrapper DOES support the -t flag, but for whatever reason,
		// they have not published a new version to maven in two years. So those flags are not available

		logger.info("Trimming song ${song.name} from $startTime to $duration")

		val destinationFile = File(song.parent + "/" + UUID.randomUUID().toString() + ".ogg")

		val args = mutableListOf("ffmpeg", "-i", song.path)

		startTime?.let {
			args.add("-ss")
			args.add(it)
		}

		duration?.let {
			args.add("-t")
			args.add(it)
		}

		args.add("-c")
		args.add("copy")
		args.add(destinationFile.path)

		val pb = ProcessBuilder(args)
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		pb.redirectError(ProcessBuilder.Redirect.INHERIT)

		val p = pb.start()
		p.waitFor()

		Files.copy(destinationFile.toPath(), song.toPath(), StandardCopyOption.REPLACE_EXISTING)

		destinationFile.delete()

		// I'm not sure if I could just reuse the original song file object but w/e
		return File(song.path)
	}

	companion object {
		val logger = LoggerFactory.getLogger(FFmpegService::class.java)!!
	}
}
