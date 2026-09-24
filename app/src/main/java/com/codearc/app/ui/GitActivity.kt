package com.codearc.app.ui

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.codearc.app.git.CommitInfo
import com.codearc.app.git.GitManager
import com.codearc.app.git.GitStatusSnapshot
import com.codearc.app.projects.ProjectRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

/** Source Control from the Phase 7 plan: Initialize Repository, Commit (stages everything and
 *  commits, since CodeArc has no partial-staging UI yet), and View Changes — backed by real
 *  JGit calls against the project's own folder, not a simulation. Branches, push and
 *  authenticated pull are out of scope here, per the plan. */
class GitActivity : AppCompatActivity() {
    private lateinit var page: LinearLayout
    private lateinit var repo: ProjectRepository
    private lateinit var projectId: String
    private lateinit var projectDir: File
    private var working = false
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_git)
        projectId = intent.getStringExtra("project") ?: return finish()
        repo = ProjectRepository(applicationContext)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        page = findViewById(R.id.page)
        task { projectDir = repo.root(repo.get(projectId)); render() }
    }

    private fun task(block: suspend () -> Unit) {
        if (working) return
        working = true
        lifecycleScope.launch {
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { message("Couldn't complete that", e.message ?: "Please try again.") } finally { working = false }
        }
    }
    private fun message(title: String, body: String) { if (!isFinishing && !isDestroyed) MaterialAlertDialogBuilder(this).setTitle(title).setMessage(body).setPositiveButton("OK", null).show() }
    private fun text(value: String, size: Float = 15f, accent: Int = R.color.muted, bold: Boolean = false, mono: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color(accent)); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        if (mono) typeface = android.graphics.Typeface.MONOSPACE
        setPadding(0, dp(5), 0, dp(5)); setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun section(title: String) { page.addView(text(title, 17f, R.color.cyan, true).apply { setPadding(0, dp(18), 0, dp(6)) }) }
    private fun card(): MaterialCardView { val c = layoutInflater.inflate(R.layout.item_card, page, false) as MaterialCardView; page.addView(c); return c }

    private suspend fun render() {
        page.removeAllViews()
        page.addView(text("Source Control", 26f, R.color.text, true))
        if (!GitManager.isRepo(projectDir)) {
            page.addView(text("This project isn't a Git repository yet."))
            page.addView(MaterialButton(this).apply {
                text = "Initialize Repository"
                setOnClickListener { task { GitManager.init(projectDir); render() } }
            })
            return
        }
        val status = GitManager.status(projectDir)
        section("Changes")
        if (status.isClean) page.addView(text("Nothing to commit — working tree clean.", 15f, R.color.success))
        else {
            fun list(label: String, files: List<String>, accent: Int) { if (files.isNotEmpty()) { page.addView(text(label, 13f, accent, true)); files.forEach { page.addView(text("  $it", 13f, R.color.text, mono = true)) } } }
            list("Staged (added)", status.added, R.color.success)
            list("Modified", status.modified, R.color.cyan)
            list("Removed", status.removed, R.color.error)
            list("Untracked", status.untracked, R.color.muted)
            list("Missing (deleted, not staged)", status.missing, R.color.error)
            page.addView(MaterialButton(this).apply {
                text = "Commit All Changes"
                setOnClickListener { commitDialog() }
            })
        }
        section("History")
        val commits = GitManager.log(projectDir)
        if (commits.isEmpty()) page.addView(text("No commits yet.")) else commits.forEach { commitCard(it) }
    }

    private fun commitCard(c: CommitInfo) {
        card().apply {
            findViewById<LinearLayout>(R.id.card_content).apply {
                addView(text(c.message, 15f, R.color.text, true))
                addView(text("${c.shortId} · ${c.author} · ${DateFormat.format("MMM d, yyyy h:mm a", c.time)}", 12f, R.color.muted))
            }
        }
    }

    private fun commitDialog() {
        val input = EditText(this).apply { hint = "Commit message"; isSingleLine = true }
        MaterialAlertDialogBuilder(this).setTitle("Commit changes").setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Commit") { _, _ ->
                task {
                    GitManager.commitAll(projectDir, input.text.toString().trim(), "CodeArc User", "user@codearc.local")
                    render()
                }
            }.show()
    }
}
