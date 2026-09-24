package com.codearc.app.ui

import android.os.Bundle
import android.graphics.Typeface
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.codearc.app.execution.ExecutionManager
import com.codearc.app.execution.ExecutionStatus
import com.codearc.app.learn.CourseRegistry
import com.codearc.app.learn.Lesson
import com.codearc.app.learn.LearningRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** Lesson Screen from the Phase 5 plan: objective, explanation, an editable + runnable code
 *  example, notes, common mistakes, a practice challenge and a quiz, with Previous/Next and
 *  Bookmark. Interactive Examples run through ExecutionManager.runSnippet() — the SAME engine
 *  the IDE's Run button uses (Phase 4) — never a separate fake compiler for learning. */
class LessonActivity : AppCompatActivity() {
    private lateinit var page: LinearLayout
    private lateinit var repo: LearningRepository
    private lateinit var execution: ExecutionManager
    private lateinit var languageId: String
    private lateinit var level: String
    private lateinit var lessonIds: List<String>
    private var lesson: Lesson? = null
    private var working = false
    private var bookmarked = false
    private var solvedQuiz = false
    private var hintShown = false
    private var solutionShown = false

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_lesson)
        languageId = intent.getStringExtra("language") ?: "python"
        level = intent.getStringExtra("level") ?: "Beginner"
        lessonIds = intent.getStringArrayListExtra("lessonIds") ?: arrayListOf()
        repo = LearningRepository(applicationContext)
        execution = ExecutionManager(applicationContext)
        page = findViewById(R.id.page)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        load(intent.getStringExtra("lessonId") ?: return)
    }

    private fun load(lessonId: String) {
        val course = CourseRegistry.forLanguageId(languageId) ?: return
        val found = course.beginner.find { it.id == lessonId } ?: return
        lesson = found
        solvedQuiz = false
        hintShown = false
        solutionShown = false
        findViewById<MaterialToolbar>(R.id.toolbar).title = found.title
        lifecycleScope.launch {
            bookmarked = repo.isBookmarked(languageId, level, found.id)
            render()
        }
    }

    private fun text(value: String, size: Float = 15f, accent: Int = R.color.muted, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color(accent)); if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5)); setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun section(title: String) { page.addView(text(title, 16f, R.color.cyan, true).apply { setPadding(0, dp(18), 0, dp(8)) }) }
    private fun card(): MaterialCardView { val c = layoutInflater.inflate(R.layout.item_card, page, false) as MaterialCardView; page.addView(c); return c }
    private fun mono(value: String): TextView = TextView(this).apply { text = value; setTextColor(color(R.color.text)); typeface = Typeface.MONOSPACE; textSize = 13f; setPadding(0, dp(6), 0, dp(6)) }
    private fun button(label: String, action: () -> Unit) = MaterialButton(this).apply { text = label; setOnClickListener { action() } }

    private fun render() {
        val l = lesson ?: return
        page.removeAllViews()
        val index = lessonIds.indexOf(l.id)
        page.addView(text("${l.level} · Lesson ${index + 1} of ${lessonIds.size.coerceAtLeast(1)} · ${l.durationMinutes} min", 12f, R.color.muted))
        val titleRow = LinearLayout(this)
        titleRow.addView(text(l.title, 24f, R.color.text, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(button(if (bookmarked) "★" else "☆") { toggleBookmark() })
        page.addView(titleRow)

        section("Objective"); page.addView(text(l.objective))
        section("Explanation"); page.addView(text(l.explanation))

        section("Code Example")
        val codeCard = card()
        val codeInput = EditText(this).apply {
            setText(l.codeExample); typeface = Typeface.MONOSPACE; textSize = 13f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setTextColor(color(R.color.text)); background = null
        }
        val exampleOutput = text("Tap Run to execute this example.", 13f, R.color.muted)
        codeCard.findViewById<LinearLayout>(R.id.card_content).apply {
            addView(codeInput)
            addView(button("▶ Run Example") { runSnippet(codeInput.text.toString(), "") { result -> exampleOutput.text = result } })
            addView(exampleOutput)
        }

        section("Important Notes"); page.addView(text(l.notes))
        section("Common Mistakes"); page.addView(text(l.commonMistakes))

        section("Practice Challenge")
        val practiceCard = card()
        val practiceInput = EditText(this).apply {
            setText(l.practice.starterCode); typeface = Typeface.MONOSPACE; textSize = 13f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setTextColor(color(R.color.text)); background = null
        }
        val practiceOutput = text("Tap Run Code to try it.", 13f, R.color.muted)
        practiceCard.findViewById<LinearLayout>(R.id.card_content).apply {
            addView(text(l.practice.instruction, 15f, R.color.text))
            addView(practiceInput)
            val row1 = LinearLayout(this@LessonActivity)
            row1.addView(button("Run Code") { runSnippet(practiceInput.text.toString(), "") { result -> practiceOutput.text = result } })
            row1.addView(button("Reset") { practiceInput.setText(l.practice.starterCode); practiceOutput.text = "Tap Run Code to try it." })
            addView(row1)
            addView(practiceOutput)
            val row2 = LinearLayout(this@LessonActivity)
            row2.addView(button("Hint") { hintShown = !hintShown; render() })
            row2.addView(button("Solution") { solutionShown = !solutionShown; render() })
            addView(row2)
            if (hintShown) addView(text("Hint: ${l.practice.hint}", 13f, R.color.cyan))
            if (solutionShown) { addView(text("Solution:", 13f, R.color.cyan)); addView(mono(l.practice.solution)) }
        }

        section("Quiz")
        val quizCard = card()
        quizCard.findViewById<LinearLayout>(R.id.card_content).apply {
            addView(text(l.quiz.prompt, 15f, R.color.text))
            val group = RadioGroup(this@LessonActivity)
            l.quiz.choices.forEachIndexed { i, choice -> group.addView(RadioButton(this@LessonActivity).apply { text = choice; id = i; setTextColor(color(R.color.text)) }) }
            addView(group)
            val quizResult = text("", 13f, R.color.muted)
            addView(button("Submit") {
                if (group.checkedRadioButtonId == -1) { quizResult.text = "Choose an answer first."; quizResult.setTextColor(color(R.color.warning)); return@button }
                val correct = group.checkedRadioButtonId == l.quiz.correctIndex
                quizResult.text = if (correct) "Correct!" else "Not quite — try again."
                quizResult.setTextColor(color(if (correct) R.color.success else R.color.error))
                lifecycleScope.launch {
                    repo.recordQuiz(languageId, level, l.id, correct)
                    if (correct) { repo.markCompleted(languageId, level, l.id); solvedQuiz = true }
                }
            })
            addView(quizResult)
        }

        val navRow = LinearLayout(this)
        val prev = index - 1; val next = index + 1
        navRow.addView(button("← Previous") { if (prev >= 0) load(lessonIds[prev]) }.apply { isEnabled = prev >= 0 }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        navRow.addView(button("Next →") { if (next < lessonIds.size) load(lessonIds[next]) else finish() }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        page.addView(navRow)
    }

    private fun toggleBookmark() { lifecycleScope.launch { bookmarked = repo.toggleBookmark(languageId, level, lesson?.id ?: return@launch); render() } }

    private fun runSnippet(code: String, stdin: String, onResult: (String) -> Unit) {
        if (working) return
        working = true
        onResult("Running...")
        lifecycleScope.launch {
            try {
                val result = execution.runSnippet(languageId, code, stdin)
                val summary = when (result.status) {
                    ExecutionStatus.SUCCESS, ExecutionStatus.RUNTIME_ERROR -> buildString {
                        if (result.stdout.isNotBlank()) appendLine(result.stdout.trimEnd())
                        if (result.stderr.isNotBlank()) appendLine(result.stderr.trimEnd())
                        append("Process finished with exit code ${result.exitCode} · ${"%.2f".format(result.executionTimeMs / 1000f)}s")
                    }
                    ExecutionStatus.RUNTIME_NOT_INSTALLED -> "Runtime not installed."
                    ExecutionStatus.TIMEOUT -> "Execution timed out."
                    else -> result.stderr.ifBlank { "Could not run this example." }
                }
                onResult(summary)
            } catch (e: Exception) {
                if (!isFinishing && !isDestroyed) MaterialAlertDialogBuilder(this@LessonActivity).setTitle("Error").setMessage(e.message ?: "Please try again.").setPositiveButton("OK", null).show()
                onResult("Could not run this example.")
            } finally { working = false }
        }
    }
}
