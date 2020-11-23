package com.example.groove.controllers

import com.example.groove.db.model.CrashReport
import com.example.groove.db.model.enums.PermissionType
import com.example.groove.services.CrashReportService
import com.example.groove.services.UserService
import com.example.groove.util.FileUtils
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletResponse


@RestController
@RequestMapping("api/crash-report")
class CrashReportController(
		private val crashReportService: CrashReportService,
		private val userService: UserService,
		private val fileUtils: FileUtils
) {

	// Example cURL command for uploading a file
	// curl -H "Content-Type: multipart/form-data" -H "Authorization: Bearer df86c467-d940-4239-889f-4d72329f0ba4"
	// -F "file=@C:/Users/user/Music/Song.mp3"  http://localhost:8080/api/crash-report
	// This Zip file should contain two files: a .txt file that contains the device logs, and a .db file for the device DB.
	// The files should not be nested within a directory or this will fail.
	@PostMapping
	fun uploadCrashReport(@RequestParam("file") zip: MultipartFile): ResponseEntity<String> {
		val user = loadLoggedInUser()
		logger.info("Beginning upload of crash info for user ${user.name}")

		crashReportService.saveCrashReport(user, zip)

		return ResponseEntity(HttpStatus.ACCEPTED)
	}

	@GetMapping
	fun getAllCrashReportInfo(): List<CrashReport> {
		userService.assertPermission(loadLoggedInUser(), PermissionType.VIEW_CRASH_REPORTS)

		return crashReportService.getCrashReports()
	}

	@GetMapping("/{id}/log")
	fun getLogForCrashReport(@PathVariable id: Long): Map<String, String> {
		userService.assertPermission(loadLoggedInUser(), PermissionType.VIEW_CRASH_REPORTS)

		return mapOf("deviceLog" to crashReportService.getCrashReportLog(id))
	}

	@GetMapping("/{id}/db")
	fun getDbForCrashReport(
			@PathVariable id: Long,
			response: HttpServletResponse
	) {
		userService.assertPermission(loadLoggedInUser(), PermissionType.VIEW_CRASH_REPORTS)

		val file = crashReportService.getCrashReportDb(id)

		fileUtils.writeFileToServlet(file, response, "application/x-sqlite3")

		file.delete()
	}

    companion object {
        private val logger = logger()
    }
}
