package com.codearc.app.execution

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Genuine offline HTML/CSS/JavaScript execution via Android's built-in WebView (a real
 *  Chromium JS engine, bundled with the OS — never a simulation). This object handles the
 *  Learn side: rendering a lesson/practice snippet off-screen and turning its console output
 *  into an [ExecutionResult], so Lesson/Practice's existing text output panel works for HTML,
 *  CSS and JavaScript with no UI changes — the same "route through the real engine, capture
 *  what actually happened" principle Python's Run already used. The editor's own visible
 *  Run/Preview (EditorActivity.runWebProject) manages its own on-screen WebView directly, since
 *  that one needs to be seen and needs to load the project's real files from disk — it doesn't
 *  go through here. WebView only works on the main thread; [runSnippet] hops there itself so an
 *  IO-dispatcher caller (ExecutionManager) doesn't need to know that. */
object WebRuntime {
    private const val TIMEOUT_MS = 6_000L

    /** Extra settle time after onPageFinished — deferred console output (setTimeout, promise
     *  callbacks, etc.) can arrive slightly after the page finishes loading. */
    private const val SETTLE_MS = 350L

    /** [languageId] is "html", "css" or "javascript". CSS and JavaScript snippets alone aren't
     *  valid documents on their own, so they're wrapped in a minimal shell so their effect is
     *  actually visible/testable — a small demo body for CSS, a bare <script> for JavaScript. */
    suspend fun runSnippet(context: Context, languageId: String, code: String): ExecutionResult = withContext(Dispatchers.Main) {
        val html = when (languageId) {
            "html" -> code
            "css" -> DEMO_SHELL.replace("/*__CSS__*/", code)
            else -> "<!DOCTYPE html><html><head></head><body>\n<script>\n$code\n</script>\n</body></html>"
        }
        renderAndCapture(context, html)
    }

    private suspend fun renderAndCapture(context: Context, html: String): ExecutionResult {
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val logs = StringBuilder()
                val errors = StringBuilder()
                val webView = WebView(context.applicationContext)
                var finished = false
                fun finish() {
                    if (finished) return
                    finished = true
                    if (cont.isActive) {
                        cont.resumeWith(Result.success(ExecutionResult(
                            stdout = logs.toString().trimEnd(),
                            stderr = errors.toString().trimEnd(),
                            exitCode = if (errors.isEmpty()) 0 else 1,
                            executionTimeMs = 0,
                            status = if (errors.isEmpty()) ExecutionStatus.SUCCESS else ExecutionStatus.RUNTIME_ERROR
                        )))
                    }
                    runCatching { webView.destroy() }
                }
                webView.settings.javaScriptEnabled = true
                webView.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        val line = message.message() + (message.lineNumber().takeIf { it > 0 }?.let { " (line $it)" } ?: "")
                        when (message.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR -> errors.appendLine(line)
                            ConsoleMessage.MessageLevel.WARNING -> logs.appendLine("Warning: $line")
                            else -> logs.appendLine(line)
                        }
                        return true
                    }
                }
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Handler(Looper.getMainLooper()).postDelayed({ finish() }, SETTLE_MS)
                    }
                }
                cont.invokeOnCancellation { runCatching { webView.destroy() } }
                webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        }
        return result ?: ExecutionResult("", "Rendering timed out.", -1, TIMEOUT_MS, ExecutionStatus.TIMEOUT)
    }

    private val DEMO_SHELL = """
        <!DOCTYPE html><html><head><style>/*__CSS__*/</style></head>
        <body>
          <h1>Heading</h1>
          <p>A paragraph of body text.</p>
          <button>Button</button>
          <div class="box">A div with class "box"</div>
          <ul><li>List item one</li><li>List item two</li></ul>
        </body></html>
    """.trimIndent()
}
