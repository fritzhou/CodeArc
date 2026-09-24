package com.codearc.app.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Storage Management from the Phase 7 plan, computed for real from what's actually on disk —
 *  no placeholder numbers. Projects and the trash (still project data — see delete() in
 *  ProjectRepository) are shown but never offered a Clear action here, matching the plan's
 *  "Never accidentally delete projects during cleanup." Clearing only ever touches the app's
 *  own cache directory (Chaquopy's extraction scratch space and Learn's runnable-example
 *  scratch files both live there), which the OS itself is free to wipe at any time anyway. */
class StorageActivity : AppCompatActivity() {
    private lateinit var page: LinearLayout
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun mb(bytes: Long) = if (bytes < 1024 * 1024) "${(bytes / 1024.0).roundToInt()} KB" else "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_storage)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        page = findViewById(R.id.page)
        refresh()
    }

    private fun dirSize(f: File): Long = if (!f.exists()) 0 else runCatching { f.walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0)

    private fun refresh() {
        page.removeAllViews()
        page.addView(text("Storage", 26f, R.color.text, true))
        page.addView(text("Computed from what's actually on this device right now.", 13f, R.color.muted))
        page.addView(text("Loading…", 13f, R.color.muted))
        lifecycleScope.launch {
            val projects = File(filesDir, "projects")
            val trash = File(filesDir, "project-trash")
            val languagePacks = filesDir.listFiles()?.filter { it.isDirectory && it.name.contains("chaquopy", true) } ?: emptyList()
            val sizes = withContext(Dispatchers.IO) {
                mapOf(
                    "projects" to dirSize(projects),
                    "trash" to dirSize(trash),
                    "packs" to languagePacks.sumOf { dirSize(it) },
                    "cache" to dirSize(cacheDir)
                )
            }
            page.removeAllViews()
            page.addView(text("Storage", 26f, R.color.text, true))
            page.addView(text("Computed from what's actually on this device right now.", 13f, R.color.muted))
            section("Projects", sizes["projects"] ?: 0, R.color.success, "Your projects — Export ZIP from a project's screen before deleting anything here.")
            section("Recently Deleted", sizes["trash"] ?: 0, R.color.muted, "Deleted projects wait here as a safety net. There's no restore screen yet, so this is informational only for now.")
            section("Language Packs", sizes["packs"] ?: 0, R.color.cyan, "Python's bundled interpreter (Chaquopy) — this is where its ~28 MB actually lives on disk.")
            section("Learning Content", 0, R.color.muted, "Lessons are compiled into the app itself, not stored as files — there's nothing here to measure or clear.")
            section("Compiler Cache & Temporary Files", sizes["cache"] ?: 0, R.color.error, "Learn's scratch files for runnable examples and other short-lived extraction data. Safe to clear any time; CodeArc rebuilds what it needs.")
            page.addView(MaterialButton(this@StorageActivity).apply {
                text = "Clear Cache & Temporary Files"
                setOnClickListener {
                    MaterialAlertDialogBuilder(this@StorageActivity).setTitle("Clear cache?")
                        .setMessage("This only clears the app's cache directory — your projects, language packs and progress are never touched.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Clear") { _, _ ->
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) { cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                                refresh()
                            }
                        }.show()
                }
            })
        }
    }

    private fun text(value: String, size: Float = 15f, accent: Int = R.color.muted, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color(accent)); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5)); setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun section(title: String, bytes: Long, accent: Int, note: String) {
        val card = layoutInflater.inflate(R.layout.item_card, page, false) as MaterialCardView
        card.findViewById<LinearLayout>(R.id.card_content).apply {
            addView(text(title, 17f, R.color.text, true))
            addView(text(mb(bytes), 15f, accent, true))
            addView(text(note, 13f, R.color.muted))
        }
        page.addView(card)
    }
}
