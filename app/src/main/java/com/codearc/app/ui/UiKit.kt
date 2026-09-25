package com.codearc.app.ui

import android.content.Context
import android.content.Intent
import com.codearc.app.data.Preferences
import com.codearc.app.projects.ProjectRepository
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first

/** Every field() / labeled-input in this app is built purely in code (no XML), which leaves
 *  [TextInputLayout] to resolve its box style from the theme's `textInputStyle` default. That
 *  path doesn't reliably reserve space for the floating label in this project's environment —
 *  the label and the field's value end up drawn on top of each other (the "Main file" /
 *  "src/main.py" overlap). Forcing the outlined box style explicitly makes the label's reserved
 *  space unambiguous regardless of the theme's default, fixing the overlap everywhere a labeled
 *  field is built programmatically. */
fun TextInputLayout.outlined(): TextInputLayout = apply {
    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
    isHintAnimationEnabled = true
}

/** Quick Code: reuses the real project + editor architecture (no second editor). Opens — or
 *  creates once, then remembers — a private scratch "Quick Code" project and jumps straight into
 *  EditorActivity for it. Shared by the Home screen action and the "+" creation sheet so both
 *  entry points behave identically. */
suspend fun openQuickCode(context: Context) {
    val prefs = Preferences(context)
    val repo = ProjectRepository(context)
    val existingId = prefs.quickCodeProjectId.first()
    val project = existingId?.let { id -> runCatching { repo.get(id) }.getOrNull() }
        ?: repo.create("Quick Code", "Python", "Empty Project", "Automatic").also { prefs.setQuickCodeProjectId(it.id) }
    context.startActivity(Intent(context, EditorActivity::class.java).putExtra("project", project.id))
}
