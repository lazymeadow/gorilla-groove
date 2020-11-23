package com.example.groove.util


fun String.blankAsNull(): String? {
	return if (this.isBlank()) null else this
}

fun String.endWith(ending: String): String {
	return if (this.endsWith(ending)) {
		this
	} else {
		this + ending
	}
}

fun String.withNewExtension(extension: String): String {
	val extensionWithoutDot = if (extension.first() == '.') extension.substring(1) else extension

	if (!this.contains(".")) {
		return "$this.$extensionWithoutDot"
	}

	return this.split('.')
			.dropLast(1)
			.plus(extensionWithoutDot)
			.joinToString(".")
}

fun String.withoutExtension(): String {
	return this.split('.')
			.dropLast(1)
			.joinToString(".")
}

fun String.extension(): String {
	return this.split('.')
			.last()
			.toLowerCase()
}

// Windows is the worst offending OS, so this is just the list of characters that Windows won't let you put in a file name
val sensitiveCharacters = setOf('/', '\\', '<', '>', ':', '"', '|', '?', '*')
fun String.withoutReservedFileSystemCharacters(): String {
	return this.filter { !sensitiveCharacters.contains(it) }
}

fun String.urlDecode(): String {
	return this
			.replace("%2C", ",")
			.replace("%3A", ":")
			.replace("%22", "\"")
			.replace("+", " ")
}

fun String.splitFirst(delimiter: String): Pair<String, String> {
	val index = this.indexOf(delimiter)
	val first = this.substring(0, index)
	val second = this.substring(index + 1)

	return first to second
}

fun String.isUuid(): Boolean {
	val uuidRegex = Regex("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")

	return uuidRegex.matches(this)
}

