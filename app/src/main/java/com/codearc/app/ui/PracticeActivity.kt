package com.codearc.app.ui

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.codearc.app.execution.ExecutionManager
import com.codearc.app.execution.ExecutionStatus
import com.codearc.app.learn.CourseRegistry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/** Standalone Practice screen reached from a Course's Practice tab: instruction, starter code,
 *  Run Code, Reset, Hint, Solution — exactly the Phase 5 plan's exercise shape. Runs through
 *  ExecutionManager.runSnippet(), the same engine as the editor and the Lesson screen. Solutions
 *  are never shown automatically; the person has to tap for them. */
class PracticeActivity : AppCompatActivity() {
    private lateinit var page: LinearLayout
    private lateinit var execution: ExecutionManager
    private var working = false

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun text(value: String, size: Float = 15f, accent: Int = R.color.muted, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color(accent)); if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5)); setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun mono(value: String) = TextView(this).apply { text = value; setTextColor(color(R.color.text)); typeface = Typeface.MONOSPACE; textSize = 13f }
    private fun button(label: String, action: () -> Unit) = MaterialButton(this).apply { text = label; setOnClickListener { action() } }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_practice)
        val languageId = intent.getStringExtra("language") ?: "python"
        val lessonId = intent.getStringExtra("lessonId")
        execution = ExecutionManager(applicationContext)
        page = findViewById(R.id.page)
        findViewById<MaterialToolbar>(R.id.toolbar).apply { title = "Practice"; setNavigationOnClickListener { finish() } }
        val course = CourseRegistry.forLanguageId(languageId)
        val lesson = course?.beginner?.find { it.id == lessonId }
        if (course == null || lesson == null) { finish(); return }

        page.addView(text(lesson.title, 22f, R.color.text, true))
        page.addView(text(lesson.practice.instruction, 15f, R.color.text))
        val codeInput = EditText(this).apply {
            setText(lesson.practice.starterCode); typeface = Typeface.MONOSPACE; textSize = 13f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setTextColor(color(R.color.text))
        }
        val card = layoutInflater.inflate(R.layout.item_card, page, false) as MaterialCardView
        val output = text("Tap Run Code to try it.", 13f, R.color.muted)
        var hintShown = false
        val hintText = text("", 13f, R.color.cyan)
        var solutionShown = false
        val solutionLabel = text("", 13f, R.color.cyan)
        val solutionCode = mono("")
        card.findViewById<LinearLayout>(R.id.card_content).apply {
            addView(codeInput)
            val row1 = LinearLayout(this@PracticeActivity)
            row1.addView(button("Run Code") { run(languageId, codeInput.text.toString(), output) })
            row1.addView(button("Reset") { codeInput.setText(lesson.practice.starterCode); output.text = "Tap Run Code to try it." })
            addView(row1)
            addView(output)
            val row2 = LinearLayout(this@PracticeActivity)
            row2.addView(button("Hint") {
                hintShown = !hintShown
                hintText.text = if (hintShown) "Hint: ${lesson.practice.hint}" else ""
            })
            row2.addView(button("Solution") {
                solutionShown = !solutionShown
                solutionLabel.text = if (solutionShown) "Solution:" else ""
                solutionCode.text = if (solutionShown) lesson.practice.solution else ""
            })
            addView(row2); addView(hintText); addView(solutionLabel); addView(solutionCode)
        }
        page.addView(card)
    }

    private fun run(languageId: String, code: String, output: TextView) {
        if (working) return
        working = true
        output.text = "Running..."
        lifecycleScope.launch {
            try {
                val result = execution.runSnippet(languageId, code, "")
                output.text = when (result.status) {
                    ExecutionStatus.SUCCESS, ExecutionStatus.RUNTIME_ERROR -> buildString {
                        if (result.stdout.isNotBlank()) appendLine(result.stdout.trimEnd())
                        if (result.stderr.isNotBlank()) appendLine(result.stderr.trimEnd())
                        append("Process finished with exit code ${result.exitCode} · ${"%.2f".format(result.executionTimeMs / 1000f)}s")
                    }
                    ExecutionStatus.RUNTIME_NOT_INSTALLED -> "Runtime not installed."
                    ExecutionStatus.TIMEOUT -> "Execution timed out."
                    else -> result.stderr.ifBlank { "Could not run this." }
                }
            } catch (e: Exception) { output.text = e.message ?: "Could not run this." } finally { working = false }
        }
    }
}
