package com.example.groove.services

import java.io.File

interface FileStorageService {
	fun storeSong(song: File, trackId: Long)
	fun storeAlbumArt(tmpAlbumArt: File, trackId: Long)

	fun getSongLink(trackId: Long): String
	fun getAlbumArtLink(trackId: Long): String?
}