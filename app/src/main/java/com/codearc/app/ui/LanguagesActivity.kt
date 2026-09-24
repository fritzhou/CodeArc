package com.codearc.app.ui

import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.codearc.app.R
import com.codearc.app.execution.LanguageDefinition
import com.codearc.app.execution.LanguageRegistry
import com.codearc.app.execution.RuntimeManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Language Pack Manager from the Phase 4 plan: Installed / Available / Updates, with a detail
 *  dialog per language. Only Python is real; every other "Install" honestly explains why it
 *  can't run yet instead of pretending to install something. */
class LanguagesActivity : AppCompatActivity() {
    private lateinit var page: LinearLayout
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_languages)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        page = findViewById(R.id.page)
        render()
    }

    private fun text(value: String, size: Float = 15f, accent: Int = R.color.muted, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color(accent)); if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5)); setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun section(title: String) { page.addView(text(title, 18f, R.color.text, true).apply { setPadding(0, dp(20), 0, dp(12)) }) }
    private fun card(lang: LanguageDefinition, status: String, accent: Int): MaterialCardView {
        val card = layoutInflater.inflate(R.layout.item_card, page, false) as MaterialCardView
        card.findViewById<LinearLayout>(R.id.card_content).apply {
            addView(text(lang.displayName, 17f, R.color.text, true))
            addView(text(status, 13f, accent))
            addView(text(lang.runtimeDescription, 13f, R.color.muted))
        }
        card.isClickable = true; card.isFocusable = true
        card.setOnClickListener { showDetail(lang) }
        page.addView(card)
        return card
    }

    private fun render() {
        page.removeAllViews()
        page.addView(text("Languages", 28f, R.color.text, true))
        page.addView(text("Manage offline runtimes and see what CodeArc can run right now."))
        val installed = LanguageRegistry.all.filter { RuntimeManager.isInstalled(it.id) }
        val available = LanguageRegistry.all.filter { !RuntimeManager.isInstalled(it.id) }
        section("Installed")
        if (installed.isEmpty()) page.addView(text("No languages installed yet.")) else installed.forEach { card(it, "Installed · offline", R.color.success) }
        section("Available")
        available.forEach { card(it, if (it.onlineCapable) "Not installed offline · cloud execution runs once a cloud endpoint is configured in Settings" else "Not installed", R.color.cyan) }
        section("Updates")
        page.addView(text("Everything installed is up to date."))
        section("Package Management")
        val packagesCard = layoutInflater.inflate(R.layout.item_card, page, false) as MaterialCardView
        packagesCard.findViewById<LinearLayout>(R.id.card_content).apply {
            addView(text("Python packages", 17f, R.color.text, true))
            addView(text("No third-party packages bundled", 13f, R.color.muted))
        }
        packagesCard.isClickable = true; packagesCard.isFocusable = true
        packagesCard.setOnClickListener { showPackages() }
        page.addView(packagesCard)
    }

    private fun showPackages() {
        MaterialAlertDialogBuilder(this).setTitle("Python packages")
            .setMessage("Chaquopy (CodeArc's bundled Python runtime) only installs pip packages at build time, listed in app/build.gradle's python { pip { ... } } block — there's no runtime installer to hit here, the way this screen's \"download size / internet requirement / status\" language might suggest for a language with real dynamic packages. Nothing is bundled beyond the standard library right now. Adding a package means declaring it there and rebuilding the app; CodeArc won't pretend to fetch or install one from this screen.")
            .setPositiveButton("Got it", null).show()
    }

    private fun showDetail(lang: LanguageDefinition) {
        val installed = RuntimeManager.isInstalled(lang.id)
        val body = buildString {
            appendLine("Version: ${lang.runtimeDescription}")
            appendLine("Offline: ${if (lang.offlineCapable) "Supported" else "Not implemented in this build"}")
            appendLine("Online: ${if (lang.onlineCapable) "Runs via cloud execution once a cloud endpoint is configured in Settings" else "Not planned"}")
            appendLine("Package support: ${if (lang.packageSupport) "Yes" else "Not yet"}")
            if (installed) append("Storage: ${RuntimeManager.storageSizeMb(lang.id)} MB")
        }
        val builder = MaterialAlertDialogBuilder(this).setTitle(lang.displayName).setMessage(body.trim())
        if (installed) {
            builder.setNeutralButton("Remove") { _, _ ->
                MaterialAlertDialogBuilder(this).setTitle("Can't remove ${lang.displayName}")
                    .setMessage("${lang.displayName} is bundled inside the app itself, not downloaded separately, so it can't be removed without uninstalling CodeArc.")
                    .setPositiveButton("OK", null).show()
            }
            builder.setPositiveButton("Close", null)
        } else {
            builder.setPositiveButton("Install") { _, _ ->
                MaterialAlertDialogBuilder(this).setTitle("Runtime not installed")
                    .setMessage("A real offline ${lang.displayName} runtime isn't implemented yet, so CodeArc won't pretend to install one. It's architected for — see ExecutionManager and LanguageDefinition — and will arrive in a later update, or run through cloud execution once a cloud endpoint is configured in Settings.")
                    .setPositiveButton("Got it", null).show()
            }
            builder.setNegativeButton("Cancel", null)
        }
        builder.show()
    }
}
