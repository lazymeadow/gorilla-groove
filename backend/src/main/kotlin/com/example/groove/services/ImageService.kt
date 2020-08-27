package com.example.groove.services

import com.example.groove.util.FileUtils
import com.example.groove.util.logger
import org.springframework.stereotype.Service
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.min

@Service
class ImageService(
		private val fileStorageService: FileStorageService,
		private val fileUtils: FileUtils
) {

	private fun cropToSquare(image: BufferedImage): BufferedImage? {
		logger.info("Converting image to square")
		val smallerEdge = min(image.width, image.height)

		// We want to crop equally from both sides.
		// Say an image is 1000px wide and 500px tall, we need to drop 500px from the width to square it.
		// To do this evenly, we want to drop 250px from each side.
		// So "(total width - the minimum edge) / 2" or "(1000 - 500) / 2" to get us our offset.
		val startX = (image.width - smallerEdge) / 2
		val startY = (image.height - smallerEdge) / 2

		val subImage = image.getSubimage(startX, startY, smallerEdge, smallerEdge)

		// The subImage maintains pointers to the original data. Just for sanity, copy it so we can't alter the original
		val copyOfImage = BufferedImage(subImage.width, subImage.height, BufferedImage.TYPE_INT_RGB)

		val g = copyOfImage.createGraphics()

		g.drawImage(subImage, 0, 0, null)

		return copyOfImage
	}

	private fun resizeImage(image: BufferedImage, artSize: ArtSize): BufferedImage {
		// Album art isn't always square, so we need to constrain EITHER the larger of the width or the height to the max,
		// and then scale the smaller size by the same ratio to keep the image looking normal
		val (targetWidth, targetHeight) = if (image.width > image.height) {
			if (image.width > artSize.size) {
				val shrinkRatio = image.width / artSize.size.toDouble()
				artSize.size to (image.height / shrinkRatio).toInt()
			} else {
				image.width to image.height
			}
		} else {
			if (image.height > artSize.size) {
				val shrinkRatio = image.height / artSize.size.toDouble()
				(image.width / shrinkRatio).toInt() to artSize.size
			} else {
				image.width to image.height
			}
		}

		val resultingImage = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH)
		val outputImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)

		outputImage.graphics.drawImage(resultingImage, 0, 0, null)

		return outputImage
	}

	fun downloadFromUrl(url: String): File? {
		return try {
			val image = ImageIO.read(URL(url)) ?: run {
				logger.error("Failed to read in image from URL $url!")
				return null
			}

			val imageFile = fileStorageService.generateTmpFilePath().toFile()

			// Might not be a PNG but...... it probably doesn't matter??
			ImageIO.write(image, "png", imageFile)

			imageFile
		} catch (e: IOException) {
			logger.error("Failed to read in image from URL $url!")
			null
		}
	}

	fun convertToStandardArtFile(imageFile: File, size: ArtSize, cropToSquare: Boolean): File? {
		val image = try {
			ImageIO.read(imageFile)
		} catch (e: Exception) {
			logger.error("Failed to read in album art for conversion!", e)
			return null
		} ?: run {
			logger.warn("Album art was null when converting!")
			return null
		}

		val resizedImage = resizeImage(image, size)

		val finalImage = if (cropToSquare) {
			cropToSquare(resizedImage)
		} else {
			resizedImage
		}

		val outputFile = fileUtils.createTemporaryFile("png")
		ImageIO.write(finalImage, "png", outputFile)

		return outputFile
	}

	companion object {
		val logger = logger()
	}
}

enum class ArtSize(val size: Int, val s3Directory: String, val systemFileExtension: String) {
	SMALL(64, "art-64x64", "-64x64.png"),
	LARGE(500, "art", ".png")
}
