package com.example.groove.services

import com.example.groove.properties.FFmpegProperties
import com.example.groove.properties.FileStorageProperties
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.logger
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.springframework.stereotype.Component
import net.bramp.ffmpeg.FFmpegExecutor
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*


@Component
class FFmpegService(
		private val ffmpegProperties: FFmpegProperties,
		private val fileStorageProperties: FileStorageProperties
) {

	fun convertTrack(file: File, audioFormat: AudioFormat, volume: Double? = null): File {
		val convertedFileName = UUID.randomUUID().toString() + audioFormat.extension

		val ffmpeg = FFmpeg(ffmpegProperties.ffmpegBinaryLocation + "ffmpeg")

		val ffprobe = FFprobe(ffmpegProperties.ffmpegBinaryLocation + "ffprobe")

		val builder = FFmpegBuilder()
				.addInput(file.absolutePath)
				.addOutput(fileStorageProperties.tmpDir + convertedFileName)

		volume?.let { builder.setAudioFilter("volume=$it") }

		// For whatever reason, if we don't set the quality explicitly the mp3 file doubles in length.
		// And I don't mean file size, I mean audio duration. Very weird. There is supposedly another flag
		// we could use called "write_xing 0", but it needs to be placed in front of the output in the
		// command string, and this ffmpeg library has no support to do this, so we'd have to rip out this
		// library and go straight command line. Might not be a bad idea, but I don't want to right now
		if (audioFormat == AudioFormat.MP3) {
			builder.audio_quality = 3.0
		}

		val executor = FFmpegExecutor(ffmpeg, ffprobe)
		executor.createJob(builder.done()).run()

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
		val logger = logger()
	}
}
