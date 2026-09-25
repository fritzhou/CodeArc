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
    private lateinit var tabRow: com.google.android.material.tabs.TabLayout
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
        listOf(Tab.LEARN to "Learn", Tab.PRACTICE to "Practice", Tab.PROJECTS to "Projects", Tab.REFERENCE to "Reference").forEach { (t, label) ->
            tabRow.addTab(tabRow.newTab().setText(label).setTag(t))
        }
        tabRow.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(t: com.google.android.material.tabs.TabLayout.Tab) { tab = t.tag as Tab; render() }
            override fun onTabUnselected(t: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(t: com.google.android.material.tabs.TabLayout.Tab) {}
        })
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
    private fun section(title: String) { page.addView(text(title, 18f, R.color.text, true).apply { setPadding(0, dp(14), 0, dp(8)) }) }
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

    private fun render() {
        page.removeAllViews()
        if (!course.available) {
            page.addView(text(course.displayName, 22f, R.color.text, true))
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
                isAllCaps = false; letterSpacing = 0f; isSingleLine = true
                insetTop = 0; insetBottom = 0; minimumWidth = 0
                setPadding(dp(4), paddingTop, dp(4), paddingBottom)
                setOnClickListener { level = lvl; render() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        val scroll = android.widget.HorizontalScrollView(this).apply { isFillViewport = true; isHorizontalScrollBarEnabled = false; addView(row) }
        page.addView(scroll)
    }

    private fun renderLearn() {
        page.addView(text(course.displayName, 22f, R.color.text, true))
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
        page.addView(text("Practice", 22f, R.color.text, true))
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
        page.addView(text("Guided Projects", 22f, R.color.text, true))
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
        page.addView(text("Reference", 22f, R.color.text, true))
        when (course.languageId) {
            "python" -> { section("Python quick reference"); PYTHON_REFERENCE.forEach { (title, snippet) -> card(title, snippet.replace("\\n", "\n"), R.color.text) } }
            "html" -> { section("HTML quick reference"); HTML_REFERENCE.forEach { (title, snippet) -> card(title, snippet.replace("\\n", "\n"), R.color.text) } }
            "css" -> { section("CSS quick reference"); CSS_REFERENCE.forEach { (title, snippet) -> card(title, snippet.replace("\\n", "\n"), R.color.text) } }
            "javascript" -> { section("JavaScript quick reference"); JS_REFERENCE.forEach { (title, snippet) -> card(title, snippet.replace("\\n", "\n"), R.color.text) } }
            else -> card("Coming soon", "A quick reference for ${course.displayName} isn't written yet.", R.color.muted)
        }
    }

    private fun openLesson(lessonId: String) {
        val ids = ArrayList((if (level == "Beginner") course.beginner else emptyList()).sortedBy { it.order }.map { it.id })
        startActivity(Intent(this, LessonActivity::class.java)
            .putExtra("language", course.languageId).putExtra("level", level)
            .putExtra("lessonId", lessonId).putStringArrayListExtra("lessonIds", ids))
    }
}

private val PYTHON_REFERENCE = listOf(
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
)
private val HTML_REFERENCE = listOf(
    "Heading" to "<h1>...</h1>  through <h6>",
    "Paragraph" to "<p>text</p>",
    "Link" to "<a href=\"url\">text</a>",
    "Image" to "<img src=\"path\" alt=\"description\">",
    "List" to "<ul>\\n  <li>item</li>\\n</ul>",
    "Div / span" to "<div>block</div>  <span>inline</span>",
    "Form input" to "<input id=\"x\" type=\"text\">",
    "Button" to "<button id=\"x\">label</button>",
    "Comment" to "<!-- comment -->",
    "Link a stylesheet" to "<link rel=\"stylesheet\" href=\"style.css\">",
    "Link a script" to "<script src=\"script.js\"></script>"
)
private val CSS_REFERENCE = listOf(
    "Select by class" to ".card { ... }",
    "Select by id" to "#header { ... }",
    "Select by tag" to "p { ... }",
    "Text color" to "color: #147bff;",
    "Background" to "background-color: white;",
    "Box model" to "margin: 8px; border: 1px solid #ccc; padding: 8px;",
    "Font" to "font-family: sans-serif; font-size: 16px;",
    "Flexbox" to "display: flex;\\njustify-content: center;\\nalign-items: center;",
    "Border radius" to "border-radius: 8px;",
    "Comment" to "/* comment */"
)
private val JS_REFERENCE = listOf(
    "Log" to "console.log(value1, value2, ...)",
    "Variable" to "let x = 1;  const y = 2;",
    "String" to "`Hello, ${'$'}{name}`  (template literal)",
    "Condition" to "if (x) {\\n  ...\\n} else if (y) {\\n  ...\\n} else {\\n  ...\\n}",
    "For loop" to "for (let i = 0; i < n; i++) { ... }",
    "While loop" to "while (condition) { ... }",
    "Function" to "function name(params) {\\n  return value;\\n}",
    "Arrow function" to "const name = (params) => value;",
    "Array" to "const items = [1, 2, 3]; items.push(4);",
    "Get element" to "document.getElementById(\"id\")",
    "Event listener" to "el.addEventListener(\"click\", () => { ... });"
)
