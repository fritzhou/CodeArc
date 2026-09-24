package com.codearc.app.ui
import android.os.Bundle
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import com.codearc.app.projects.ProjectRepository
import com.codearc.app.learn.CourseRegistry
import com.codearc.app.learn.LearningRepository
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.codearc.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
class PageFragment : Fragment(R.layout.fragment_page) {
 private lateinit var page: LinearLayout
 private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
 private fun color(id: Int) = ContextCompat.getColor(requireContext(), id)
 private fun text(value: String, size: Float = 15f, accent: Int = R.color.muted, bold: Boolean = false): TextView = TextView(requireContext()).apply {
 text = value; textSize = size; setTextColor(color(accent)); if(bold) setTypeface(typeface, Typeface.BOLD)
 setPadding(0, dp(5), 0, dp(5)); setLineSpacing(dp(3).toFloat(), 1f)
 }
 private fun section(title: String) { page.addView(text(title, 18f, R.color.text, true).apply { setPadding(0,dp(20),0,dp(12)) }) }
 private fun card(title: String, description: String, accent: Int = R.color.cyan, click: (() -> Unit)? = null): MaterialCardView {
 val card = layoutInflater.inflate(R.layout.item_card, page, false) as MaterialCardView
 card.findViewById<LinearLayout>(R.id.card_content).apply {
 addView(text(title, 17f, accent, true)); addView(text(description))
 }
 if(click != null) { card.isClickable = true; card.isFocusable = true; card.setOnClickListener { click() } }
 page.addView(card); return card
 }
 private fun info(title: String, message: String) { MaterialAlertDialogBuilder(requireContext()).setTitle(title).setMessage(message).setPositiveButton("Got it", null).show() }
 private fun create() { CreationSheet().show(parentFragmentManager, "create") }
 private fun go(name: String) { (requireActivity() as MainActivity).select(name) }
 private fun button(label: String, action: () -> Unit) = MaterialButton(requireContext()).apply { text = label; setOnClickListener { action() } }
 private fun openCourse(languageId: String) { startActivity(Intent(requireContext(), CourseActivity::class.java).putExtra("language", languageId)) }
 override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 page = view.findViewById(R.id.page)
 when(arguments?.getString("page") ?: "Home") { "Home" -> home(); "Projects" -> projects(); "Learn" -> learn(); else -> settings() }
 }
 private fun home() {
 val header = LinearLayout(requireContext()).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
 header.addView(ImageView(requireContext()).apply { setImageResource(R.drawable.ic_logo); contentDescription = "CodeArc logo" }, LinearLayout.LayoutParams(dp(36), dp(36)))
 header.addView(text("  CodeArc",24f,R.color.text,true), LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
 fun icon(drawable: Int, label: String, action: () -> Unit) { header.addView(ImageButton(requireContext()).apply { setImageResource(drawable); contentDescription=label; setBackgroundColor(android.graphics.Color.TRANSPARENT); setOnClickListener { action() } }, LinearLayout.LayoutParams(dp(48),dp(48))) }
 icon(R.drawable.ic_search,"Search") { search() }; icon(R.drawable.ic_settings,"Settings") { go("Settings") }; page.addView(header)
 page.addView(text("YOUR DEVELOPMENT WORKSPACE",11f,R.color.cyan,true))
 val hero = card("Build your next idea.","A workspace for curious minds.\nLearn. Code. Compile. Build.",R.color.text)
 hero.setCardBackgroundColor(color(R.color.elevated))
 hero.findViewById<LinearLayout>(R.id.card_content).apply { addView(text("</>  endless possibilities",18f,R.color.cyan).apply { typeface=Typeface.MONOSPACE }); addView(button("＋  New Project") { startActivity(Intent(requireContext(),ProjectActivity::class.java)) }) }
 val actions = listOf("Quick Code", "Learn", "Open Project", "Templates")
 actions.chunked(2).forEach { row ->
 val layout = LinearLayout(requireContext())
 row.forEach { label -> layout.addView(button(label) { when(label) { "Learn" -> go("Learn"); "Open Project" -> go("Projects"); "Templates" -> startActivity(Intent(requireContext(),ProjectActivity::class.java)); else -> info("Quick Code", "The code editor arrives in Phase 3.") } }, LinearLayout.LayoutParams(0,dp(60),1f).apply { setMargins(dp(3),0,dp(3),0) }) }; page.addView(layout)
 }
 section("Continue Learning")
 val continueLearning = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }; page.addView(continueLearning)
 viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
 renderContinueLearning(continueLearning)
 } }
 section("Recent Projects")
 val recent=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL }; page.addView(recent)
 viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
 ProjectRepository(requireContext()).projects.collect { projects ->
 recent.removeAllViews()
 if(projects.isEmpty()) recent.addView(text("No projects yet. Create your first CodeArc project."))
 projects.take(4).forEach { project ->
 recent.addView(button("${project.name} — ${project.language}") { startActivity(Intent(requireContext(),ProjectActivity::class.java).putExtra("project",project.id)) })
 }
 }
 } }
 section("Installed Languages")
 card("Python — installed", "Offline CPython runtime. Tap Run inside any project to execute it.", R.color.success) { startActivity(Intent(requireContext(),LanguagesActivity::class.java)) }
 }
 private suspend fun renderContinueLearning(container: LinearLayout) {
 val recent = LearningRepository(requireContext()).mostRecent()
 container.removeAllViews()
 val card = layoutInflater.inflate(R.layout.item_card, container, false) as MaterialCardView
 if (recent == null) {
 card.findViewById<LinearLayout>(R.id.card_content).apply { addView(text("Your first line starts here", 17f, R.color.cyan, true)); addView(text("Explore Python, JavaScript and more with real, runnable lessons.")) }
 } else {
 val course = CourseRegistry.forLanguageId(recent.language)
 val title = course?.beginner?.find { it.id == recent.lessonId }?.title ?: recent.lessonId
 card.findViewById<LinearLayout>(R.id.card_content).apply { addView(text("Continue: ${course?.displayName ?: recent.language}", 17f, R.color.cyan, true)); addView(text("Last opened: $title")) }
 }
 card.isClickable = true; card.isFocusable = true
 card.setOnClickListener { openCourse(recent?.language ?: "python") }
 container.addView(card)
 }
 private fun projects() {
 page.addView(text("Projects",28f,R.color.text,true)); page.addView(text("Your ideas, organized in one place."))
 section("Local workspace")
 card("No projects yet.","Create your first CodeArc project. Project storage and file management arrive in Phase 2.")
 page.addView(button("＋  New Project") { startActivity(Intent(requireContext(),ProjectActivity::class.java)) })
 }
 private fun learn() {
 page.addView(text("Learn",28f,R.color.text,true)); page.addView(text("One concept. One program. One step forward."))
 section("Continue Learning")
 val continueLearning = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }; page.addView(continueLearning)
 viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { renderContinueLearning(continueLearning) } }
 section("Explore Languages")
 CourseRegistry.all.forEach { course ->
 val status = if (course.available) "Beginner → Intermediate → Advanced" else "Runtime not installed yet"
 card(course.displayName, status, if (course.available) R.color.cyan else R.color.muted) { openCourse(course.languageId) }
 }
 }
 private fun settings() {
 page.addView(text("Settings",28f,R.color.text,true)); page.addView(text("Make room for your workflow."))
 section("Editor")
 card("Editor", "Font, indentation and wrapping · Phase 3") { info("Editor", "Font, indentation and wrapping · Phase 3") }
 section("Execution")
 card("Execution", "Offline runtime active (Python) · cloud execution needs a configured endpoint", R.color.cyan) { cloudSettingsDialog() }
 section("Languages"); card("Manage language packs", "Python runs offline · Package Management and 12 defined languages live here", R.color.cyan) { startActivity(Intent(requireContext(),LanguagesActivity::class.java)) }
 section("Learning")
 card("Learning", "Lessons, practice and guided projects for Python are live · more languages arrive later") { info("Learning", "Lessons, practice and guided projects for Python are live · more languages arrive later") }
 section("AI")
 card("AI", "Editor assistant (explain, fix, generate, refactor…) · needs the same cloud endpoint", R.color.cyan) { cloudSettingsDialog() }
 section("Storage")
 card("Manage storage", "Real per-category sizes for Projects, Language Packs and Cache, with a safe Clear Cache action.", R.color.cyan) { startActivity(Intent(requireContext(),StorageActivity::class.java)) }
 section("Appearance"); card("Midnight developer theme", "CodeArc's dark palette is active.")
 section("Account"); card("Account", "Optional. No login required.") { info("Account", "Optional. No login required.") }
 section("About"); card("CodeArc 0.7.0", "Learn. Code. Compile. Build.\nPhase 7 · Advanced Features, Optimization & Release Polish",R.color.cyan) { info("CodeArc", "Offline first. Online enhanced. Learning integrated.\n\nPython runs offline via a bundled interpreter; 11 more languages are defined and honestly marked not-yet-implemented rather than faked. Automatic mode falls back to real HTTPS cloud execution and the AI assistant once a cloud endpoint is configured. Source Control (Init/Commit/View Changes, Clone) is real, backed by JGit. Editor autocomplete offers real keyword/identifier suggestions with an architecture prepared for a future LSP. Breakpoints can be toggled in the editor gutter as prepared debugger scaffolding — stepping isn't available yet for any language.") }
 }
 private fun cloudSettingsDialog() {
 val ctx = requireContext()
 val current = com.codearc.app.data.Preferences(ctx)
 val endpoint = TextInputEditText(ctx)
 val apiKey = TextInputEditText(ctx)
 val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
 col.addView(text("Used by cloud Run (Automatic/Online mode) and the editor's AI assistant. Both stay off — and honestly say so — until an https:// endpoint is set here."))
 col.addView(TextInputLayout(ctx).apply { hint = "Cloud endpoint (https://...)"; addView(endpoint) })
 col.addView(TextInputLayout(ctx).apply { hint = "API key (optional)"; addView(apiKey) })
 viewLifecycleOwner.lifecycleScope.launch {
 val saved = current.cloudSettings.first()
 endpoint.setText(saved.endpoint); apiKey.setText(saved.apiKey)
 }
 MaterialAlertDialogBuilder(ctx).setTitle("Cloud & AI").setView(col).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
 viewLifecycleOwner.lifecycleScope.launch {
 current.saveCloudSettings(com.codearc.app.data.CloudSettings(endpoint.text.toString(), apiKey.text.toString(), true))
 }
 }.show()
 }
 private fun search() {
 val input = TextInputEditText(requireContext()).apply { hint="Home, Projects, Learn, Settings"; isSingleLine=true }
 val wrapper = TextInputLayout(requireContext()).apply { setPadding(dp(20),dp(8),dp(20),0); addView(input) }
 val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle("Find a screen").setView(wrapper).setPositiveButton("Open",null).setNegativeButton("Cancel",null).create()
 dialog.setOnShowListener { dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
 val match=listOf("Home","Projects","Learn","Settings").firstOrNull { it.equals(input.text.toString().trim(),true) }
 if(match==null) wrapper.error="Enter Home, Projects, Learn or Settings" else { dialog.dismiss(); go(match) }
 } }; dialog.show()
 }
 companion object { fun newInstance(page: String) = PageFragment().apply { arguments=Bundle().apply { putString("page",page) } } }
}
