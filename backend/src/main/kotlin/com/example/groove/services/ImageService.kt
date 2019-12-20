package com.example.groove.services

import com.example.groove.properties.FileStorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.min

@Service
class ImageService(
		fileStorageProperties: FileStorageProperties
) {

	private val fileStorageLocation: Path = Paths.get(fileStorageProperties.tmpDir!!)
			.toAbsolutePath().normalize()

	fun cropToSquare(imageFile: File): File {
		val image = ImageIO.read(imageFile)

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

		val tmpImageName = UUID.randomUUID().toString() + ".png"
		val outputFile = fileStorageLocation.resolve(tmpImageName).toFile()
		ImageIO.write(copyOfImage, "png", outputFile)

		return outputFile
	}

	companion object {
		val logger = LoggerFactory.getLogger(ImageService::class.java)!!
	}
}