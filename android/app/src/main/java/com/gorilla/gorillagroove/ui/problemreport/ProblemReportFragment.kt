package com.gorilla.gorillagroove.ui.problemreport

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.getNullableLong
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_problem_report.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val LAST_MANUAL_REPORT_KEY = "last_manual_report"
private const val LAST_AUTOMATED_REPORT_KEY = "last_automated_report"

@AndroidEntryPoint
class ProblemReportFragment : Fragment(R.layout.fragment_problem_report) {

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Problem Report view")

        sharedPrefs.edit().remove(LAST_MANUAL_REPORT_KEY).apply()

        sendReportButton.setOnClickListener { sendProblemReport() }

        displayLastSentReportMessage()
    }

    private fun displayLastSentReportMessage() {
        val currentTime = System.currentTimeMillis()

        val lastManualReportMillis = sharedPrefs.getNullableLong(LAST_MANUAL_REPORT_KEY)
        val lastAutomatedReportMillis = sharedPrefs.getNullableLong(LAST_AUTOMATED_REPORT_KEY)

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

    private fun sendProblemReport() {
        sharedPrefs.edit().putLong(LAST_MANUAL_REPORT_KEY, System.currentTimeMillis()).apply()

        displayLastSentReportMessage()
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
