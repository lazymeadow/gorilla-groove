package com.example.groove.controllers

import com.example.groove.services.CrashReportService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("api/crash-report")
class CrashReportController(
		private val crashReportService: CrashReportService
) {

	// Example cURL command for uploading a file
	// curl -H "Content-Type: multipart/form-data" -H "Authorization: Bearer df86c467-d940-4239-889f-4d72329f0ba4"
	// -F "file=@C:/Users/user/Music/Song.mp3"  http://localhost:8080/api/crash-report
	// This Zip file should contain two files: a .txt file that contains the device logs, and a .db file for the device DB.
	// The files should not be nested within a directory or this will fail.
	@PostMapping
	fun uploadCrashInfo(@RequestParam("file") zip: MultipartFile): ResponseEntity<String> {
		val user = loadLoggedInUser()
		logger.info("Beginning upload of crash info for user ${user.name}")

		crashReportService.saveCrashReport(user, zip)

		return ResponseEntity(HttpStatus.ACCEPTED)
	}

    companion object {
        private val logger = logger()
    }
}
