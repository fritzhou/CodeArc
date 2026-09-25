package com.codearc.app.ui
import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.codearc.app.projects.ProjectRepository
import com.codearc.app.projects.SafeFiles
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreationSheet : BottomSheetDialogFragment() {
 override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
 val root = inflater.inflate(R.layout.fragment_page, container, false)
 val page = root.findViewById<LinearLayout>(R.id.page)
 listOf("New Project", "New File", "Quick Code", "Import Project", "Clone Repository").forEach { title ->
 page.addView(MaterialButton(requireContext()).apply { text = title; setOnClickListener {
 when (title) {
 "New Project", "Import Project" -> { startActivity(Intent(requireContext(), ProjectActivity::class.java).putExtra("import",title == "Import Project")); dismiss() }
 "New File" -> newFileDialog()
 "Clone Repository" -> cloneDialog()
 else -> { viewLifecycleOwner.lifecycleScope.launch { openQuickCode(requireContext()) }; dismiss() }
 }
 } }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,0,0,(6*resources.displayMetrics.density).toInt()) })
 }
 return root
 }
 /** Drops the new file straight into the Quick Code scratch project (same one the "Quick Code"
  *  action above uses) and opens it — a real, immediate "new file" rather than just navigating
  *  to the Projects tab and leaving the person to create one manually. */
 private fun newFileDialog() {
 val input = EditText(requireContext()).apply { hint = "File name (e.g. sketch.py)" }
 MaterialAlertDialogBuilder(requireContext()).setTitle("New File").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Create") { _, _ ->
 val name = input.text.toString().trim()
 if (name.isEmpty()) return@setPositiveButton
 viewLifecycleOwner.lifecycleScope.launch {
 val ctx = requireContext()
 val repo = ProjectRepository(ctx)
 val prefs = com.codearc.app.data.Preferences(ctx)
 val existingId = prefs.quickCodeProjectId.first()
 val project = existingId?.let { id -> runCatching { repo.get(id) }.getOrNull() }
 ?: repo.create("Quick Code", "Python", "Empty Project", "Automatic").also { prefs.setQuickCodeProjectId(it.id) }
 runCatching { repo.createEntry(project.id, "", name, false) }
 startActivity(Intent(ctx, EditorActivity::class.java).putExtra("project", project.id).putExtra("file", SafeFiles.name(name)))
 dismiss()
 }
 }.show()
 }
 private fun cloneDialog() {
 val input = android.widget.EditText(requireContext()).apply { hint = "https://github.com/owner/repo.git" }
 MaterialAlertDialogBuilder(requireContext()).setTitle("Clone Repository").setMessage("Public HTTPS repositories only — CodeArc doesn't store credentials.").setView(input)
 .setNegativeButton("Cancel", null)
 .setPositiveButton("Clone") { _, _ ->
 val url = input.text.toString().trim()
 if (url.isNotEmpty()) { startActivity(Intent(requireContext(), ProjectActivity::class.java).putExtra("clone", true).putExtra("url", url)); dismiss() }
 }.show()
 }
}
