package com.codearc.app.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.codearc.app.learn.CourseRegistry
import com.codearc.app.learn.GuidedProject
import com.codearc.app.learn.LanguageCourse
import com.codearc.app.learn.Lesson
import com.codearc.app.learn.LearningRepository
import com.codearc.app.projects.Templates
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** Course screen from the Phase 5 plan: Learn / Practice / Projects / Reference tabs for one
 *  language, plus a Beginner / Intermediate / Advanced level switch inside Learn. Only Python
 *  has real content (see LearnContent.kt) — every other language explains honestly that it's
 *  waiting on a real offline runtime, the same principle Phase 4 used in LanguagesActivity. */
class CourseActivity : AppCompatActivity() {
    private enum class Tab { LEARN, PRACTICE, PROJECTS, REFERENCE }
    private lateinit var page: LinearLayout
    private lateinit var tabRow: LinearLayout
    private lateinit var repo: LearningRepository
    private lateinit var course: LanguageCourse
    private var tab = Tab.LEARN
    private var level = "Beginner"
    private var completed: Set<String> = emptySet()

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_course)
        val languageId = intent.getStringExtra("language") ?: "python"
        val found = CourseRegistry.forLanguageId(languageId)
        if (found == null) { finish(); return }
        course = found
        repo = LearningRepository(applicationContext)
        page = findViewById(R.id.page)
        tabRow = findViewById(R.id.tabRow)
        findViewById<MaterialToolbar>(R.id.toolbar).apply { title = course.displayName; setNavigationOnClickListener { finish() } }
        renderTabRow()
        lifecycleScope.launch {
            repo.observeProgress(course.languageId).collect { list ->
                completed = list.filter { it.completed }.map { it.lessonId }.toSet()
                render()
            }
        }
    }

    override fun onResume() { super.onResume(); render() }

    private fun text(value: String, size: Float = 15f, accent: Int = R.color.muted, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color(accent)); if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5)); setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun section(title: String) { page.addView(text(title, 18f, R.color.text, true).apply { setPadding(0, dp(20), 0, dp(12)) }) }
    private fun card(title: String, description: String, accent: Int = R.color.cyan, enabled: Boolean = true, click: (() -> Unit)? = null): MaterialCardView {
        val card = layoutInflater.inflate(R.layout.item_card, page, false) as MaterialCardView
        card.findViewById<LinearLayout>(R.id.card_content).apply {
            addView(text(title, 17f, if (enabled) accent else R.color.muted, true))
            addView(text(description))
        }
        card.alpha = if (enabled) 1f else 0.6f
        if (click != null) { card.isClickable = true; card.isFocusable = true; card.setOnClickListener { click() } }
        page.addView(card); return card
    }
    private fun info(title: String, message: String) { MaterialAlertDialogBuilder(this).setTitle(title).setMessage(message).setPositiveButton("Got it", null).show() }

    private fun tabButton(label: String, active: Boolean, action: () -> Unit) = MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
        text = label; setTextColor(color(if (active) R.color.primary else R.color.muted)); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }
    private fun renderTabRow() {
        tabRow.removeAllViews()
        tabRow.addView(tabButton("Learn", tab == Tab.LEARN) { tab = Tab.LEARN; renderTabRow(); render() })
        tabRow.addView(tabButton("Practice", tab == Tab.PRACTICE) { tab = Tab.PRACTICE; renderTabRow(); render() })
        tabRow.addView(tabButton("Projects", tab == Tab.PROJECTS) { tab = Tab.PROJECTS; renderTabRow(); render() })
        tabRow.addView(tabButton("Reference", tab == Tab.REFERENCE) { tab = Tab.REFERENCE; renderTabRow(); render() })
    }

    private fun render() {
        page.removeAllViews()
        if (!course.available) {
            page.addView(text(course.displayName, 28f, R.color.text, true))
            card("Offline runtime not installed", "${course.displayName} doesn't have a real offline runtime yet, so Learn content for it isn't available. See the Languages screen for what's installed.", R.color.muted) { startActivity(Intent(this, LanguagesActivity::class.java)) }
            return
        }
        when (tab) {
            Tab.LEARN -> renderLearn()
            Tab.PRACTICE -> renderPractice()
            Tab.PROJECTS -> renderProjects()
            Tab.REFERENCE -> renderReference()
        }
    }

    private fun levelRow() {
        val row = LinearLayout(this)
        listOf("Beginner", "Intermediate", "Advanced").forEach { lvl ->
            row.addView(MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = lvl; setTextColor(color(if (level == lvl) R.color.primary else R.color.muted))
                setOnClickListener { level = lvl; render() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        page.addView(row)
    }

    private fun renderLearn() {
        page.addView(text(course.displayName, 28f, R.color.text, true))
        page.addView(text("Beginner → Intermediate → Advanced"))
        levelRow()
        when (level) {
            "Beginner" -> {
                if (course.beginner.isEmpty()) { card("Coming soon", "Beginner content for ${course.displayName} isn't written yet.", R.color.muted); return }
                val done = course.beginner.count { it.id in completed }
                section("Your Progress"); page.addView(text("$done of ${course.beginner.size} lessons completed"))
                section("Lessons")
                course.beginner.sortedBy { it.order }.forEach { lesson ->
                    val status = if (lesson.id in completed) "✓ Completed" else "${lesson.durationMinutes} min"
                    card(lesson.title, status, if (lesson.id in completed) R.color.success else R.color.cyan) { openLesson(lesson.id) }
                }
            }
            "Intermediate" -> {
                if (course.intermediateTitles.isEmpty()) { card("Coming soon", "Intermediate content for ${course.displayName} isn't written yet.", R.color.muted); return }
                section("Lessons")
                course.intermediateTitles.forEach { title -> card(title, "Coming soon", R.color.muted, enabled = false) { info(title, "This Intermediate lesson's full content is coming in a later update. The syllabus is set — see PHASE5.md.") } }
            }
            else -> card("Coming soon", "Advanced content arrives in a later update, for every language.", R.color.muted)
        }
    }

    private fun renderPractice() {
        page.addView(text("Practice", 28f, R.color.text, true))
        page.addView(text("Short, focused exercises — jump straight in without the lesson."))
        if (course.beginner.isEmpty()) { card("Coming soon", "No practice exercises yet for ${course.displayName}.", R.color.muted); return }
        section("Beginner")
        course.beginner.sortedBy { it.order }.forEach { lesson ->
            card(lesson.title, lesson.practice.instruction, R.color.cyan) {
                startActivity(Intent(this, PracticeActivity::class.java).putExtra("language", course.languageId).putExtra("lessonId", lesson.id))
            }
        }
    }

    private fun renderProjects() {
        page.addView(text("Guided Projects", 28f, R.color.text, true))
        page.addView(text("Build something real using what you've learned."))
        section("Beginner")
        course.beginnerProjects.forEach { projectCard(it) }
        section("Intermediate")
        course.intermediateProjects.forEach { projectCard(it) }
    }
    private fun projectCard(project: GuidedProject) {
        card(project.title, project.description, if (project.template != null) R.color.cyan else R.color.muted, enabled = project.template != null) {
            if (project.template == null) { info(project.title, "This guided project is coming in a later update."); return@card }
            val language = course.displayName
            lifecycleScope.launch { repo.markProjectStarted(course.languageId, project.id) }
            startActivity(Intent(this, ProjectActivity::class.java)
                .putExtra("suggestedName", project.title)
                .putExtra("suggestedLanguage", Templates.languages.indexOf(language).coerceAtLeast(0))
                .putExtra("suggestedTemplate", Templates.names.indexOf(project.template).coerceAtLeast(0)))
        }
    }

    private fun renderReference() {
        page.addView(text("Reference", 28f, R.color.text, true))
        if (course.languageId != "python") { card("Coming soon", "A quick reference for ${course.displayName} isn't written yet.", R.color.muted); return }
        section("Python quick reference")
        listOf(
            "Print" to "print(value1, value2, ...)",
            "Input" to "input(\"prompt\")  → always returns a string",
            "Types" to "int, float, str, bool, list, tuple, dict",
            "Condition" to "if x:\\n    ...\\nelif y:\\n    ...\\nelse:\\n    ...",
            "For loop" to "for i in range(n):\\n    ...",
            "While loop" to "while condition:\\n    ...",
            "Function" to "def name(params):\\n    return value",
            "List" to "items = [1, 2, 3]; items.append(4)",
            "Dict" to "d = {\"key\": \"value\"}; d[\"key\"]",
            "File" to "with open(path, \"r\") as f:\\n    f.read()"
        ).forEach { (title, snippet) -> card(title, snippet.replace("\\n", "\n"), R.color.text) }
    }

    private fun openLesson(lessonId: String) {
        val ids = ArrayList((if (level == "Beginner") course.beginner else emptyList()).sortedBy { it.order }.map { it.id })
        startActivity(Intent(this, LessonActivity::class.java)
            .putExtra("language", course.languageId).putExtra("level", level)
            .putExtra("lessonId", lessonId).putStringArrayListExtra("lessonIds", ids))
    }
}
