package com.codearc.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.codearc.app.ai.AiAction
import com.codearc.app.ai.AiManager
import com.codearc.app.ai.AiRequest
import com.codearc.app.ai.AiResult
import com.codearc.app.ai.AiStatus
import com.codearc.app.data.EditorSettings
import com.codearc.app.data.Preferences
import com.codearc.app.editor.CodeEditorView
import com.codearc.app.editor.KeywordCompletionProvider
import com.codearc.app.editor.Problem
import com.codearc.app.editor.Severity
import com.codearc.app.execution.ExecutionManager
import com.codearc.app.execution.ExecutionResult
import com.codearc.app.execution.ExecutionStatus
import com.codearc.app.execution.LanguageRegistry
import com.codearc.app.projects.Project
import com.codearc.app.projects.ProjectRepository
import com.codearc.app.projects.SafeFiles
import com.codearc.app.projects.Templates
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

private enum class Panel { OUTPUT, TERMINAL, PROBLEMS }

private class EditorTab(val relative: String, val view: CodeEditorView) {
    var dirty = false
    var problems: List<Problem> = emptyList()
    var labelView: TextView? = null
}

/** IDE workspace: real editing, tabs, a file drawer and a Problems panel backed by a genuine
 *  (if basic) offline linter (Phase 3), real Run via ExecutionManager (Phase 4/6, offline
 *  Python + cloud fallback), an AI assistant (Phase 6), and — Phase 7 — real keyword/identifier
 *  autocomplete, a link out to Source Control, and prepared (non-functional) breakpoint
 *  scaffolding. The Terminal tab is still not a live interactive shell. */
class EditorActivity : AppCompatActivity() {
    private lateinit var repo: ProjectRepository
    private lateinit var prefs: Preferences
    private lateinit var execution: ExecutionManager
    private lateinit var ai: AiManager
    private var aiWorking = false
    private lateinit var projectId: String
    private lateinit var drawer: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabStrip: TabLayout
    private lateinit var editorContainer: FrameLayout
    private lateinit var panelTabs: LinearLayout
    private lateinit var panelScroll: View
    private lateinit var panelContent: LinearLayout
    private lateinit var drawerTree: LinearLayout
    private lateinit var autocompleteScroll: View
    private lateinit var autocompleteStrip: LinearLayout
    private val tabs = mutableListOf<EditorTab>()
    private var current: EditorTab? = null
    private var settings = EditorSettings()
    private var panelMode = Panel.PROBLEMS
    private var panelExpanded = false
    private val saveHandlers = mutableMapOf<String, Runnable>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var working = false
    private var running = false
    private var outputText: String = "Nothing has run yet.\nTap ▶ Run to execute this project's main file."
    private var lastResult: ExecutionResult? = null
    // Web (HTML) project preview — a real, visible WebView stacked in editorContainer next to
    // the file tabs, shown only while previewing (see runWebProject/showPreview). Everything
    // else in this class is completely unaffected by/unaware of this: Python and every other
    // language still runs through execution.run() exactly as before.
    private var previewView: WebView? = null
    private val consoleLog = StringBuilder()
    private val consoleErr = StringBuilder()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_editor)
        val pid = state?.getString("project") ?: intent.getStringExtra("project")
        if (pid == null) { finish(); return }
        projectId = pid
        repo = ProjectRepository(applicationContext); prefs = Preferences(applicationContext); execution = ExecutionManager(applicationContext); ai = AiManager(applicationContext)
        drawer = findViewById(R.id.drawer)
        toolbar = findViewById(R.id.toolbar)
        tabStrip = findViewById(R.id.tabs)
        editorContainer = findViewById(R.id.editorContainer)
        panelTabs = findViewById(R.id.panelTabs)
        panelScroll = findViewById(R.id.panelScroll)
        panelContent = findViewById(R.id.panelContent)
        drawerTree = findViewById(R.id.drawerTree)
        autocompleteScroll = findViewById(R.id.autocompleteScroll)
        autocompleteStrip = findViewById(R.id.autocompleteStrip)
        setupToolbar()
        setupPanelTabs()
        collapsePanel()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START) else saveAllAndFinish()
            }
        })
        tabStrip.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { selectTab(tab.tag as String) }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        task {
            settings = prefs.editorSettings.first()
            val p = repo.get(projectId)
            toolbar.title = p.name
            refreshTree(drawerTree, "", 0)
            val start = state?.getString("file") ?: intent.getStringExtra("file") ?: p.mainFile
            openFileInternal(start)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("project", projectId); current?.let { outState.putString("file", it.relative) }; super.onSaveInstanceState(outState) }
    override fun onPause() { super.onPause(); tabs.forEach { t -> if (t.dirty) lifecycleScope.launch { runCatching { repo.write(projectId, t.relative, t.view.content()) } } } }
    override fun onDestroy() { previewView?.destroy(); super.onDestroy() }

    private fun task(block: suspend () -> Unit) {
        if (working) return
        working = true
        lifecycleScope.launch {
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { message("Error", e.message ?: "Please try again.") } finally { working = false }
        }
    }
    private fun message(title: String, body: String) { if (!isFinishing && !isDestroyed) MaterialAlertDialogBuilder(this).setTitle(title).setMessage(body).setPositiveButton("OK", null).show() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun setupToolbar() {
        fun btn(label: String, action: () -> Unit) {
            toolbar.addView(MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = label; setTextColor(getColor(R.color.text))
                setOnClickListener { action() }
            }, Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT))
        }
        btn("☰") { drawer.openDrawer(GravityCompat.START) }
        btn("←") { saveAllAndFinish() }
        btn("▶ Run") { runProject() }
        btn("✦ AI") { showAiMenu() }
        btn("⋮") { showMoreMenu() }
    }
    private fun showMoreMenu() {
        val options = arrayOf("Find & Replace", "Editor Settings", "Language Packs", "Source Control", "Debugger", "Save All", "New File Here", "Close Tab")
        MaterialAlertDialogBuilder(this).setItems(options) { _, which ->
            when (which) {
                0 -> findReplace()
                1 -> editorSettingsDialog()
                2 -> startActivity(android.content.Intent(this, LanguagesActivity::class.java))
                3 -> startActivity(android.content.Intent(this, GitActivity::class.java).putExtra("project", projectId))
                4 -> showDebugInfo()
                5 -> task { tabs.forEach { t -> if (t.dirty) { repo.write(projectId, t.relative, t.view.content()); t.dirty = false; updateTabTitle(t) } } }
                6 -> newFileDialog()
                7 -> current?.let { closeTab(it.relative) }
            }
        }.show()
    }
    // Phase 7: prepared debugger scaffolding. Tapping a line number in the gutter toggles a
    // real (if in-memory-only, per open tab) breakpoint; there's no stepping/variables/call
    // stack yet because no runtime here exposes a real debugger hook to attach one to — see
    // PHASE7.md for why that's a bigger change than this phase covers, and why faking one would
    // violate "Only enable debugging for languages with real debugger support."
    private fun showDebugInfo() {
        val tab = current
        if (tab == null) { message("Debugger", "Open a file first."); return }
        val count = tab.view.breakpointLines().size
        MaterialAlertDialogBuilder(this).setTitle("Debugger")
            .setMessage("Tap a line number in the gutter to toggle a breakpoint — $count set in this file right now.\n\nThat's as far as debugging goes in this build. Stepping, variables and the call stack need a real debugger hook into a runtime, and none of CodeArc's runtimes expose one yet (Python runs in-process via Chaquopy, which doesn't wire up pdb here). Breakpoints aren't saved when you close this file — this is prepared scaffolding, not a working debugger.")
            .setPositiveButton("Got it", null).show()
    }

    // ---- Phase 6: AI Assistant. Every action here goes through AiManager, which honestly
    // reports REQUIRES_INTERNET / NOT_CONFIGURED instead of faking a response, and a
    // suggestion is never written into the file except through the explicit Apply button in
    // showAiResult() — see the plan's "Never modify source code automatically" instruction.
    private fun aiTask(block: suspend () -> Unit) {
        if (aiWorking) return
        aiWorking = true
        lifecycleScope.launch {
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { message("AI Error", e.message ?: "Please try again.") } finally { aiWorking = false }
        }
    }
    private fun currentLanguageId(): String {
        val name = current?.relative?.let { Templates.languageFor(File(it).name) } ?: "Python"
        return LanguageRegistry.forDisplayName(name)?.id ?: "python"
    }
    private fun selectedOrWholeCode(): String {
        val tab = current ?: return ""
        val input = tab.view.input
        val start = input.selectionStart.coerceAtLeast(0)
        val end = input.selectionEnd.coerceAtLeast(0)
        return if (end > start) input.text?.substring(start, end) ?: "" else tab.view.content()
    }
    private fun showAiMenu() {
        if (current == null) { message("AI Assistant", "Open a file first."); return }
        val hasSelection = current!!.view.input.let { it.selectionEnd > it.selectionStart }
        val actions = AiAction.values().toList()
        val scoped = setOf(AiAction.EXPLAIN_CODE, AiAction.FIX_CODE, AiAction.REFACTOR, AiAction.SIMPLIFY, AiAction.ADD_COMMENTS)
        val labels = actions.map { a -> a.label + if (a in scoped) (if (hasSelection) " (selection)" else " (whole file)") else "" }
        MaterialAlertDialogBuilder(this).setTitle("AI Assistant").setItems(labels.toTypedArray()) { _, which -> runAiAction(actions[which]) }.show()
    }
    private fun runAiAction(action: AiAction) {
        when (action) {
            AiAction.GENERATE_CODE -> promptThenAsk(action, "Generate Code", "Describe what to generate")
            AiAction.ASK_AI -> promptThenAsk(action, "Ask AI", "Ask about this code")
            AiAction.EXPLAIN_ERROR -> {
                val problems = current?.problems.orEmpty()
                if (problems.isEmpty()) message("Explain Error", "No problems detected in this file.") else explainProblem(problems.first())
            }
            else -> requestAi(AiRequest(action, currentLanguageId(), selectedOrWholeCode()))
        }
    }
    private fun promptThenAsk(action: AiAction, title: String, hintText: String) {
        val input = EditText(this).apply { hint = hintText; minLines = 2; maxLines = 5 }
        MaterialAlertDialogBuilder(this).setTitle(title).setView(input).setNegativeButton("Cancel", null)
            .setPositiveButton(title.substringBefore(" ")) { _, _ ->
                val question = input.text.toString().trim()
                if (question.isNotEmpty()) requestAi(AiRequest(action, currentLanguageId(), selectedOrWholeCode(), question = question))
            }.show()
    }
    private fun explainProblem(p: Problem) {
        requestAi(AiRequest(AiAction.EXPLAIN_ERROR, currentLanguageId(), current?.view?.content() ?: "", errorContext = "Line ${p.line}:${p.column} — ${p.message}"))
    }
    private fun requestAi(request: AiRequest) { aiTask { showAiResult(request, ai.ask(request)) } }
    private fun showAiResult(request: AiRequest, result: AiResult) {
        if (isFinishing || isDestroyed) return
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        body.addView(TextView(this).apply { text = result.explanation; setTextColor(getColor(R.color.text)) })
        if (result.suggestedCode != null) {
            body.addView(TextView(this).apply { text = "Suggested code"; setTextColor(getColor(R.color.muted)); setPadding(0, dp(12), 0, dp(4)) })
            body.addView(TextView(this).apply { text = result.suggestedCode; setTextColor(getColor(R.color.cyan)); typeface = android.graphics.Typeface.MONOSPACE; setPadding(dp(8), dp(8), dp(8), dp(8)); setBackgroundColor(getColor(R.color.card)) })
        }
        val scroll = android.widget.ScrollView(this).apply { addView(body) }
        val builder = MaterialAlertDialogBuilder(this).setTitle(request.action.label).setView(scroll).setNegativeButton("Dismiss", null)
        if (result.status == AiStatus.SUCCESS && result.suggestedCode != null) {
            builder.setNeutralButton("Copy") { _, _ -> copyToClipboard(result.suggestedCode) }
            builder.setPositiveButton("Apply") { _, _ -> applySuggestedCode(result.suggestedCode) }
        } else if (result.status == AiStatus.SUCCESS) {
            builder.setNeutralButton("Copy") { _, _ -> copyToClipboard(result.explanation) }
        }
        builder.show()
    }
    private fun copyToClipboard(text: String) {
        val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("CodeArc", text))
    }
    private fun applySuggestedCode(code: String) {
        val tab = current ?: return
        val input = tab.view.input
        val start = input.selectionStart.coerceAtLeast(0)
        val end = input.selectionEnd.coerceAtLeast(0)
        if (end > start) input.text?.replace(start, end, code) else input.setText(code)
        tab.dirty = true; updateTabTitle(tab); scheduleSave(tab)
    }

    private fun setupPanelTabs() {
        panelTabs.removeAllViews()
        fun tabBtn(name: String, mode: Panel) = MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = name; setTextColor(getColor(if (panelMode == mode) R.color.primary else R.color.muted))
            setOnClickListener { panelMode = mode; if (!panelExpanded) expandPanel(); renderPanel() }
        }
        panelTabs.addView(tabBtn("Output", Panel.OUTPUT))
        panelTabs.addView(tabBtn("Terminal", Panel.TERMINAL))
        panelTabs.addView(tabBtn("Problems", Panel.PROBLEMS))
        panelTabs.addView(MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = if (panelExpanded) "⌄" else "⌃"; setTextColor(getColor(R.color.text))
            setOnClickListener { togglePanel() }
        })
    }
    private fun collapsePanel() { panelExpanded = false; panelScroll.layoutParams = panelScroll.layoutParams.apply { height = 0 }; panelScroll.requestLayout(); setupPanelTabs() }
    private fun expandPanel() { panelExpanded = true; panelScroll.layoutParams = panelScroll.layoutParams.apply { height = dp(260) }; panelScroll.requestLayout(); setupPanelTabs() }
    private fun togglePanel() { if (panelExpanded) collapsePanel() else expandPanel(); renderPanel() }
    private fun label(parent: LinearLayout, text: String) { parent.addView(TextView(this).apply { this.text = text; setTextColor(getColor(R.color.muted)); setPadding(0, 8, 0, 8) }) }
    private fun renderPanel() {
        panelContent.removeAllViews()
        when (panelMode) {
            Panel.OUTPUT -> {
                label(panelContent, outputText)
                lastResult?.let { r ->
                    if (r.stdout.isNotBlank()) label(panelContent, r.stdout)
                    if (r.stderr.isNotBlank()) label(panelContent, r.stderr)
                    if (r.status == ExecutionStatus.SUCCESS || r.status == ExecutionStatus.RUNTIME_ERROR) {
                        label(panelContent, "Process finished with exit code ${r.exitCode}\nExecution time: ${"%.2f".format(r.executionTimeMs / 1000f)}s")
                    }
                }
            }
            Panel.TERMINAL -> label(panelContent, "An interactive terminal isn't available yet — Run sends the project's main file through the same offline/online execution engine and shows its output here.")
            Panel.PROBLEMS -> {
                val list = current?.problems.orEmpty()
                if (list.isEmpty()) label(panelContent, "No problems detected in this file.")
                else list.forEach { p ->
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                    row.addView(MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                        text = "${if (p.severity == Severity.ERROR) "⛔" else "⚠"} Line ${p.line}:${p.column} — ${p.message}"
                        gravity = Gravity.START
                        setOnClickListener { current?.view?.goTo(p.line) }
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    row.addView(MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                        text = "Explain with AI"; setTextColor(getColor(R.color.cyan))
                        setOnClickListener { explainProblem(p) }
                    })
                    panelContent.addView(row)
                }
            }
        }
    }

    private fun runProject() {
        if (running) return
        task {
            running = true
            try {
                tabs.forEach { t -> if (t.dirty) { repo.write(projectId, t.relative, t.view.content()); t.dirty = false; updateTabTitle(t) } }
                val project = repo.get(projectId)
                if (project.language == "HTML") {
                    runWebProject(project)
                } else {
                    panelMode = Panel.OUTPUT
                    if (!panelExpanded) expandPanel() else setupPanelTabs()
                    val stdin = askForStdin(project.name)
                    outputText = "Running ${project.mainFile}..."
                    lastResult = null
                    renderPanel()
                    // Let ExecutionManager make the offline/cloud decision — it already knows
                    // whether Python's offline runtime is installed and whether a cloud endpoint
                    // is configured; short-circuiting here on RuntimeManager alone (as earlier
                    // phases did) would have skipped cloud execution entirely for every
                    // non-Python project, even with a cloud endpoint configured in Settings.
                    val result = execution.run(project, repo.root(project), stdin)
                    outputText = when (result.status) {
                        ExecutionStatus.REQUIRES_INTERNET -> "Requires internet."
                        ExecutionStatus.CLOUD_NOT_CONFIGURED -> "Cloud execution isn't configured yet. Add a cloud endpoint in Settings > Execution, or run offline with Python."
                        ExecutionStatus.RUNTIME_NOT_INSTALLED -> "Runtime not installed.\n\nOnly Python runs offline in this build, and there's no internet connection (or no cloud endpoint configured) to fall back to. See Languages for what's honestly supported."
                        ExecutionStatus.TIMEOUT -> "Execution timed out."
                        else -> "Running ${project.mainFile}..."
                    }
                    lastResult = result
                    renderPanel()
                }
            } finally { running = false }
        }
    }

    // ---- Web (HTML) project Run/Preview. A real Android WebView renders the project's actual
    // files straight off disk (file://...), so <link>/<script> tags to sibling style.css/
    // script.js resolve exactly like a normal static site — no wrapping or injection needed.
    // Console output (console.log/warn/error, and uncaught JS exceptions, which Chromium also
    // reports through the console) is captured live and fed into the SAME Output panel Python's
    // Run already uses, via the same outputText/lastResult fields and renderPanel(). Tapping Run
    // again just reloads the same WebView from the current files on disk — editing and rerunning
    // never recreates the project.
    private fun runWebProject(project: Project) {
        panelMode = Panel.OUTPUT
        if (!panelExpanded) expandPanel() else setupPanelTabs()
        consoleLog.clear(); consoleErr.clear()
        outputText = "Loading ${project.mainFile}..."
        lastResult = null
        renderPanel()
        showPreview(project)
    }
    private fun showPreview(project: Project) {
        val webView = previewView ?: WebView(this).also { wv ->
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            wv.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean { appendConsoleMessage(message); return true }
            }
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { refreshWebOutput() }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) { consoleErr.appendLine("Failed to load ${request.url}: ${error?.description}"); refreshWebOutput() }
                }
            }
            editorContainer.addView(wv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            previewView = wv
        }
        tabs.forEach { it.view.visibility = View.GONE }
        webView.visibility = View.VISIBLE
        webView.bringToFront()
        val file = File(repo.root(project), project.mainFile)
        if (!file.isFile) {
            outputText = "Main file not found: ${project.mainFile}"
            lastResult = ExecutionResult("", "Main file not found: ${project.mainFile}", -1, 0, ExecutionStatus.RUNTIME_ERROR)
            renderPanel()
            return
        }
        webView.loadUrl("file://" + file.absolutePath)
    }
    private fun appendConsoleMessage(message: ConsoleMessage) {
        val where = message.sourceId()?.let { File(it).name }?.takeIf { it.isNotBlank() }?.let { "$it:${message.lineNumber()}" } ?: "console"
        when (message.messageLevel()) {
            ConsoleMessage.MessageLevel.ERROR -> consoleErr.appendLine("[$where] ${message.message()}")
            ConsoleMessage.MessageLevel.WARNING -> consoleLog.appendLine("[$where] Warning: ${message.message()}")
            else -> consoleLog.appendLine("[$where] ${message.message()}")
        }
        refreshWebOutput()
    }
    private fun refreshWebOutput() {
        outputText = "Previewing this project. Console output (if any) is below."
        lastResult = ExecutionResult(
            stdout = consoleLog.toString().trimEnd(),
            stderr = consoleErr.toString().trimEnd(),
            exitCode = if (consoleErr.isEmpty()) 0 else 1,
            executionTimeMs = 0,
            status = if (consoleErr.isEmpty()) ExecutionStatus.SUCCESS else ExecutionStatus.RUNTIME_ERROR
        )
        if (panelMode == Panel.OUTPUT) renderPanel()
    }
    private suspend fun askForStdin(projectName: String): String = suspendCancellableCoroutine { cont ->
        if (isFinishing || isDestroyed) { if (cont.isActive) cont.resume(""); return@suspendCancellableCoroutine }
        val input = EditText(this).apply { hint = "stdin — one value per line (optional)"; minLines = 3; maxLines = 6 }
        val dialog = MaterialAlertDialogBuilder(this).setTitle("Run $projectName")
            .setMessage("If the program reads input, provide it here. Leave blank if not needed.")
            .setView(input)
            .setNegativeButton("Cancel") { _, _ -> if (cont.isActive) cont.resume("") }
            .setPositiveButton("Run") { _, _ -> if (cont.isActive) cont.resume(input.text.toString()) }
            .setOnCancelListener { if (cont.isActive) cont.resume("") }
            .create()
        cont.invokeOnCancellation { runCatching { dialog.dismiss() } }
        dialog.show()
    }

    private fun openFile(relative: String) { task { openFileInternal(relative) } }
    private suspend fun openFileInternal(relative: String) {
        val existing = tabs.find { it.relative == relative }
        if (existing != null) { selectTabInStrip(relative); return }
        val content = repo.read(projectId, relative)
        val language = Templates.languageFor(File(relative).name)
        val view = CodeEditorView(this@EditorActivity)
        view.open(relative, language, content)
        view.applySettings(settings)
        val tab = EditorTab(relative, view)
        view.onDirty = { tab.dirty = true; updateTabTitle(tab); scheduleSave(tab); updateAutocomplete(tab) }
        view.onProblems = { list -> tab.problems = list; if (panelMode == Panel.PROBLEMS) renderPanel() }
        tabs.add(tab)
        editorContainer.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        view.visibility = View.GONE
        addTabStripEntry(tab)
        selectTabInStrip(relative)
    }
    private fun addTabStripEntry(tab: EditorTab) {
        val t = tabStrip.newTab(); t.tag = tab.relative
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val lbl = TextView(this).apply { text = File(tab.relative).name; setTextColor(getColor(R.color.text)); setPadding(dp(4), dp(8), dp(4), dp(8)) }
        val close = TextView(this).apply { text = " × "; setTextColor(getColor(R.color.muted)); setOnClickListener { closeTab(tab.relative) } }
        row.addView(lbl); row.addView(close)
        t.customView = row
        tabStrip.addTab(t)
        tab.labelView = lbl
    }
    private fun updateTabTitle(tab: EditorTab) { tab.labelView?.text = File(tab.relative).name + if (tab.dirty) " •" else "" }
    private fun selectTabInStrip(relative: String) {
        for (i in 0 until tabStrip.tabCount) { val t = tabStrip.getTabAt(i); if (t?.tag == relative) { if (tabStrip.selectedTabPosition != i) tabStrip.selectTab(t) else selectTab(relative); return } }
    }
    private fun selectTab(relative: String) {
        tabs.forEach { it.view.visibility = if (it.relative == relative) View.VISIBLE else View.GONE }
        previewView?.visibility = View.GONE
        current = tabs.find { it.relative == relative }
        current?.view?.applySettings(settings)
        autocompleteScroll.visibility = View.GONE
        renderPanel()
    }
    private fun closeTab(relative: String) {
        val tab = tabs.find { it.relative == relative } ?: return
        task {
            if (tab.dirty) repo.write(projectId, tab.relative, tab.view.content())
            editorContainer.removeView(tab.view)
            tabs.remove(tab)
            for (i in 0 until tabStrip.tabCount) { val t = tabStrip.getTabAt(i); if (t?.tag == relative) { tabStrip.removeTab(t); break } }
            if (tabs.isEmpty()) { finish(); return@task }
            if (current?.relative == relative) selectTabInStrip(tabs.last().relative)
        }
    }
    private fun scheduleSave(tab: EditorTab) {
        saveHandlers[tab.relative]?.let { mainHandler.removeCallbacks(it) }
        val r = Runnable { task { repo.write(projectId, tab.relative, tab.view.content()); tab.dirty = false; updateTabTitle(tab) } }
        saveHandlers[tab.relative] = r
        mainHandler.postDelayed(r, 700)
    }
    private fun saveAllAndFinish() { task { tabs.forEach { if (it.dirty) repo.write(projectId, it.relative, it.view.content()) }; finish() } }

    // Phase 7: real keyword/identifier autocomplete (see editor/Autocomplete.kt) — no fake
    // completions, and no LSP either; this is the honest "prepare the architecture" version.
    private fun currentWordPrefix(input: EditText): String {
        val text = input.text ?: return ""
        val cursor = input.selectionStart.coerceIn(0, text.length)
        var start = cursor
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_')) start--
        return text.substring(start, cursor)
    }
    private fun updateAutocomplete(tab: EditorTab) {
        if (tab != current) return
        val prefix = currentWordPrefix(tab.view.input)
        if (prefix.length < 2) { autocompleteScroll.visibility = View.GONE; return }
        val language = Templates.languageFor(File(tab.relative).name)
        val suggestions = KeywordCompletionProvider.complete(prefix, tab.view.content(), language)
        if (suggestions.isEmpty()) { autocompleteScroll.visibility = View.GONE; return }
        autocompleteStrip.removeAllViews()
        suggestions.forEach { s ->
            autocompleteStrip.addView(MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = s; setTextColor(getColor(R.color.cyan))
                setOnClickListener { insertCompletion(tab, s) }
            })
        }
        autocompleteScroll.visibility = View.VISIBLE
    }
    private fun insertCompletion(tab: EditorTab, completion: String) {
        val input = tab.view.input
        val text = input.text ?: return
        val prefix = currentWordPrefix(input)
        val cursor = input.selectionStart.coerceIn(0, text.length)
        val start = (cursor - prefix.length).coerceAtLeast(0)
        text.replace(start, cursor, completion)
        input.setSelection(start + completion.length)
        autocompleteScroll.visibility = View.GONE
        tab.dirty = true; updateTabTitle(tab); scheduleSave(tab)
    }

    private fun newFileDialog() {
        val folder = current?.relative?.substringBeforeLast('/', "") ?: ""
        val input = EditText(this).apply { hint = "File name (e.g. helper.py)" }
        MaterialAlertDialogBuilder(this).setTitle("New File").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Create") { _, _ ->
            val name = input.text.toString().trim()
            task {
                repo.createEntry(projectId, folder, name, false)
                refreshTree(drawerTree, "", 0)
                openFileInternal(listOf(folder, SafeFiles.name(name)).filter { it.isNotEmpty() }.joinToString("/"))
            }
        }.show()
    }
    private fun findReplace() {
        val tab = current ?: return
        val find = TextInputEditText(this); val findBox = TextInputLayout(this).apply { hint = "Find"; addView(find) }
        val replace = TextInputEditText(this); val replaceBox = TextInputLayout(this).apply { hint = "Replace with"; addView(replace) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(findBox); addView(replaceBox) }
        var searchFrom = 0
        MaterialAlertDialogBuilder(this).setTitle("Find & Replace").setView(col)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Replace All") { _, _ ->
                val f = find.text.toString()
                if (f.isNotEmpty()) {
                    val text = tab.view.input.text?.toString()?.replace(f, replace.text.toString()) ?: return@setNeutralButton
                    tab.view.input.setText(text); tab.dirty = true; updateTabTitle(tab); scheduleSave(tab)
                }
            }
            .setPositiveButton("Find Next") { _, _ ->
                val f = find.text.toString(); val e = tab.view.input.text?.toString() ?: ""
                if (f.isNotEmpty()) {
                    val idx = e.indexOf(f, searchFrom).let { if (it < 0) e.indexOf(f, 0) else it }
                    if (idx >= 0) { tab.view.input.requestFocus(); tab.view.input.setSelection(idx, idx + f.length); searchFrom = idx + f.length }
                    else message("Not found", "\"$f\" was not found in this file.")
                }
            }.show()
    }
    private fun editorSettingsDialog() {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val font = TextInputEditText(this).apply { setText(settings.fontSize.toString()) }
        col.addView(TextInputLayout(this).apply { hint = "Font size (10-24)"; addView(font) })
        col.addView(TextView(this).apply { text = "Tab size"; setTextColor(getColor(R.color.muted)); setPadding(0, dp(12), 0, dp(4)) })
        val tabSizeSpinner = Spinner(this).apply { adapter = ArrayAdapter(this@EditorActivity, android.R.layout.simple_spinner_dropdown_item, listOf("2", "4", "8")); setSelection(listOf(2, 4, 8).indexOf(settings.tabSize).coerceAtLeast(0)) }
        col.addView(tabSizeSpinner)
        fun switchRow(text: String, initial: Boolean): Switch {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply { this.text = text; setTextColor(getColor(R.color.text)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            val sw = Switch(this).apply { isChecked = initial }; row.addView(sw); col.addView(row); return sw
        }
        val wordWrap = switchRow("Word wrap", settings.wordWrap)
        val lineNumbers = switchRow("Line numbers", settings.lineNumbers)
        val autoIndent = switchRow("Auto-indent", settings.autoIndent)
        val autoBrackets = switchRow("Auto-close brackets", settings.autoBrackets)
        val autoQuotes = switchRow("Auto-close quotes", settings.autoQuotes)
        val syntax = switchRow("Syntax highlighting", settings.syntaxHighlighting)
        MaterialAlertDialogBuilder(this).setTitle("Editor Settings").setView(col).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
            val fontSize = font.text.toString().toIntOrNull()?.coerceIn(10, 24) ?: settings.fontSize
            val newSettings = EditorSettings(fontSize, listOf(2, 4, 8)[tabSizeSpinner.selectedItemPosition], wordWrap.isChecked, lineNumbers.isChecked, autoIndent.isChecked, autoBrackets.isChecked, autoQuotes.isChecked, syntax.isChecked)
            settings = newSettings
            task { prefs.saveEditorSettings(newSettings) }
            tabs.forEach { it.view.applySettings(newSettings) }
        }.show()
    }

    private suspend fun refreshTree(container: LinearLayout, folder: String, depth: Int) {
        container.removeAllViews()
        val project = repo.get(projectId)
        val entries = repo.entries(projectId, folder)
        entries.forEach { file ->
            val relative = file.relativeTo(repo.root(project)).invariantSeparatorsPath
            val row = MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = "  ".repeat(depth) + (if (file.isDirectory) "▸ " else "</> ") + file.name
                setTextColor(getColor(R.color.text)); gravity = Gravity.START
            }
            container.addView(row)
            if (file.isDirectory) {
                val childContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
                container.addView(childContainer)
                var expanded = false
                row.setOnClickListener {
                    expanded = !expanded; childContainer.visibility = if (expanded) View.VISIBLE else View.GONE
                    if (expanded && childContainer.childCount == 0) task { refreshTree(childContainer, relative, depth + 1) }
                }
            } else {
                row.setOnClickListener { openFile(relative); drawer.closeDrawer(GravityCompat.START) }
            }
        }
        if (entries.isEmpty() && depth == 0) container.addView(TextView(this).apply { text = "No files yet."; setTextColor(getColor(R.color.muted)); setPadding(dp(12), dp(12), dp(12), dp(12)) })
    }
}
