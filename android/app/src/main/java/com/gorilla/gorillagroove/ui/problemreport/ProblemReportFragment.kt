package com.gorilla.gorillagroove.ui.problemreport

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.GGLog.logWarn
import com.gorilla.gorillagroove.service.GGSettings
import com.gorilla.gorillagroove.util.GGToast
import com.gorilla.gorillagroove.util.getNullableLong
import com.gorilla.gorillagroove.util.sharedPreferences
import kotlinx.android.synthetic.main.fragment_problem_report.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val LAST_MANUAL_REPORT_KEY = "last_manual_report"
private const val LAST_AUTOMATED_REPORT_KEY = "last_automated_report"

class ProblemReportFragment : Fragment(R.layout.fragment_problem_report) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Problem Report view")

        sendReportButton.setOnClickListener { sendProblemReport() }
        viewLogsButton.setOnClickListener { findNavController().navigate(R.id.logViewFragment) }

        displayLastSentReportMessage()
    }

    private fun sendProblemReport() {
        sendReportButton.isEnabled = false

        lifecycleScope.launch {
            val success = ProblemReportSender.sendProblemReport(true)

            if (success) {
                displayLastSentReportMessage()
                GGToast.show("Problem report uploaded")
            } else {
                sendReportButton.isEnabled = true
                GGToast.show("Could not upload problem report")
            }
        }
    }

    private fun displayLastSentReportMessage() {
        val currentTime = System.currentTimeMillis()

        val lastManualReportMillis = sharedPreferences.getNullableLong(LAST_MANUAL_REPORT_KEY)
        val lastAutomatedReportMillis = sharedPreferences.getNullableLong(LAST_AUTOMATED_REPORT_KEY)

        val lastManualReport = lastManualReportMillis?.let { "Your last manual problem report was sent ${it.toTimeAgoString(currentTime)}" }
        val lastAutomatedReport = lastAutomatedReportMillis?.let { "The last automated problem report was sent ${it.toTimeAgoString(currentTime)}" }

        val displayString = listOfNotNull(lastManualReport, lastAutomatedReport).joinToString(". ")

        lastLogsSentText.text = displayString

        val layout = viewLogsButton.layoutParams as ConstraintLayout.LayoutParams
        layout.topToBottom = if (displayString.isBlank()) {
            explanationText.id
        } else {
            lastLogsSentText.id
        }

        viewLogsButton.requestLayout()
    }
}

private fun Long.toTimeAgoString(currentTimeMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(currentTimeMillis - this).toInt()
    val timeString = when {
        hours < 1 -> "less than one hour"
        hours == 1 -> "one hour"
        hours < 24 -> "${hours.toEnglishWord()} hours"
        else -> {
            val daysAgo = hours / 24

            when {
                daysAgo == 1 -> "one day"
                daysAgo < 30 -> "${daysAgo.toEnglishWord()} days"
                else -> "more than a month"
            }
        }
    }

    return "$timeString ago"
}

// Because I spent most of my life learning you're "supposed" to spell out numbers under 10 in most situations. Anal, sure. But here we are.
private fun Int.toEnglishWord(): String {
    return when (this) {
        1 -> "one"
        2 -> "two"
        3 -> "three"
        4 -> "four"
        5 -> "five"
        6 -> "six"
        7 -> "seven"
        8 -> "eight"
        9 -> "nine"
        else -> this.toString()
    }
}

object ProblemReportSender {

    private val cacheDirPath: String by lazy { "${GGApplication.application.cacheDir.absolutePath}/tmp" }

    val lastSentAutomatedReport get() = sharedPreferences.getNullableLong(LAST_AUTOMATED_REPORT_KEY)

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendProblemReport(isManual: Boolean): Boolean = withContext(Dispatchers.IO) {
        logInfo("Uploading problem report")

        if (GGSettings.offlineModeEnabled) {
            logWarn("Not uploading crash report due to offline mode being enabled")
            return@withContext false
        }

        val cacheDir = File(cacheDirPath)
        cacheDir.mkdirs()

        val tmpLogFile = File("$cacheDirPath/log.txt")
        tmpLogFile.createNewFile()

        val logContent = GGLog.getLogContent()
        tmpLogFile.printWriter().use { writer ->
            logContent.forEach { writer.append(it + "\n") }
        }

        val database = GorillaDatabase.getDatabaseFile()

        val tmpDatabase = File("$cacheDirPath/db.sqlite")
        database.copyTo(tmpDatabase, overwrite = true)

        val zip = createZipWithFiles(tmpLogFile, tmpDatabase)

        try {
            val multipartFile = MultipartBody.Part.createFormData(
                "file",
                "crash-report.zip",
                zip.asRequestBody()
            )

            try {
                Network.api.uploadCrashReport(multipartFile)
            } catch (e: Throwable) {
                logError("Could not upload crash report!", e)
                throw e
            }

            val key = if (isManual) LAST_MANUAL_REPORT_KEY else LAST_AUTOMATED_REPORT_KEY
            sharedPreferences.edit().putLong(key, System.currentTimeMillis()).apply()

            logInfo("Problem report uploaded successfully")

            return@withContext true
        } catch (e: Throwable) {
            logError("Failed to upload problem report")

            return@withContext false
        } finally {
            tmpLogFile.delete()
            tmpDatabase.delete()
            zip.delete()
        }
    }

    private fun createZipWithFiles(vararg files: File): File {
        val outputPath = "$cacheDirPath/crash-report.zip"
        File(outputPath).delete()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputPath))).use { out ->
            files.forEach { file ->
                FileInputStream(file).use { fi ->
                    BufferedInputStream(fi).use { origin ->
                        val filePath = file.absolutePath
                        val entry = ZipEntry(filePath.substring(filePath.lastIndexOf("/")))
                        out.putNextEntry(entry)
                        origin.copyTo(out, 1024)
                    }
                }
            }
        }

        return File(outputPath)
    }
}
