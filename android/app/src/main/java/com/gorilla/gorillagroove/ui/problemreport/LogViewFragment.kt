@file:Suppress("MoveVariableDeclarationIntoWhen")

package com.gorilla.gorillagroove.ui.problemreport

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.service.GGLog
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.LogLevel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_log_view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val LOG_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[.*\\] \\[.*\\]:.*"

@AndroidEntryPoint
class LogViewFragment : Fragment(R.layout.fragment_log_view) {

    private val regex by lazy { Regex(LOG_REGEX) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Log view")
    }

    override fun onStart() {
        super.onStart()

        CoroutineScope(Dispatchers.IO).launch {
            val logContent = GGLog.getLogContent()
                .takeLast(300)
                .map { logLine ->
                    if (regex.matches(logLine)) {
                        // Example log looks like
                        // 2021-03-26 13:36:30.369 [LogViewFragment] [Info]: Loading Log view
                        val (_, time, _, logLevel, message) = logLine.split(" ", limit = 5)
                        val stringToShow = "$time: $message"

                        // It looks like [info]: (which is backed up by the regex match, so we know this to be true)
                        val logLevelWord = logLevel.drop(1).dropLast(2)

                        val color = when (logLevelWord) {
                            LogLevel.VERBOSE.logName, LogLevel.DEBUG.logName -> R.color.debugGrey
                            LogLevel.INFO.logName -> R.color.foreground
                            LogLevel.WARNING.logName -> R.color.warningYellow
                            else -> R.color.dangerRed
                        }

                        stringToShow.withColor(color)
                    } else {
                        // This most likely means we're logging an exception if it doesn't match the regex
                        logLine.withColor(R.color.dangerRed)
                    }
                }

            launch(Dispatchers.Main) {
                logContent.forEach {
                    logContentText.append(it)
                }

                scrollView.post {
                    scrollView.scrollToBottom()
                }

                loadingIndicator.visibility = View.GONE
            }
        }
    }
}

private fun CharSequence.withColor(colorId: Int): SpannableString {
    val color = GGApplication.application.getColor(colorId)

    val spannableString = SpannableString("$this\n")
    spannableString.setSpan(ForegroundColorSpan(color), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    return spannableString
}

// This scrolls better for me than scrollView.fullScroll
// https://stackoverflow.com/a/34866634
fun ScrollView.scrollToBottom() {
    val lastChild = getChildAt(childCount - 1)
    val bottom = lastChild.bottom + paddingBottom
    val delta = bottom - (scrollY+ height)
    smoothScrollBy(0, delta)
}
