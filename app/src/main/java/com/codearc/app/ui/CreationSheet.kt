package com.codearc.app.ui
import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.codearc.app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
class CreationSheet : BottomSheetDialogFragment() {
 override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
 val root = inflater.inflate(R.layout.fragment_page, container, false)
 val page = root.findViewById<LinearLayout>(R.id.page)
 listOf("New Project", "New File", "Quick Code", "Import Project", "Clone Repository").forEach { title ->
 page.addView(MaterialButton(requireContext()).apply { text = title; setOnClickListener {
 if(title == "New Project" || title == "Import Project") {
 startActivity(Intent(requireContext(), ProjectActivity::class.java).putExtra("import",title == "Import Project")); dismiss()
 } else if(title == "New File") {
 (requireActivity() as MainActivity).select("Projects"); dismiss()
 } else if(title == "Clone Repository") {
 cloneDialog()
 } else MaterialAlertDialogBuilder(requireContext()).setTitle(title).setMessage(
 "The editor arrives in Phase 3 and offline execution in Phase 4."
 ).setPositiveButton("Got it", null).show()
 } })
 }
 return root
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
