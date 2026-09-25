package com.codearc.app.ui

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.codearc.app.R
import com.codearc.app.projects.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import java.text.DateFormat
import java.util.Date

class ProjectsFragment : Fragment(R.layout.fragment_page) {
 private lateinit var repo: ProjectRepository
 private lateinit var list: LinearLayout
 private var all=emptyList<Project>()
 private var query=""; private var language="All languages"; private var sort="Recent"; private var favorites=false
 override fun onViewCreated(view: View, state: Bundle?) {
  repo=ProjectRepository(requireContext())
  query=state?.getString("query") ?: ""; language=state?.getString("language") ?: "All languages"; sort=state?.getString("sort") ?: "Recent"; favorites=state?.getBoolean("favorites") ?: false
  val page=view.findViewById<LinearLayout>(R.id.page)
  page.addView(TextView(requireContext()).apply { text="Projects"; textSize=22f; setTextColor(resources.getColor(R.color.text,null)); setPadding(0,0,0,(8*resources.displayMetrics.density).toInt()) })
  page.addView(com.google.android.material.textfield.TextInputLayout(requireContext()).outlined().apply { hint="Search projects"; setPadding(0,0,0,(6*resources.displayMetrics.density).toInt())
   addView(com.google.android.material.textfield.TextInputEditText(context).apply { isSingleLine=true; setText(query); doAfterTextChanged { query=it.toString(); render() } })
  })
  fun spinner(values: List<String>, selected: String, change: (String)->Unit) { page.addView(Spinner(requireContext()).apply {
   adapter=ArrayAdapter(requireContext(),android.R.layout.simple_spinner_dropdown_item,values); setSelection(values.indexOf(selected).coerceAtLeast(0)); onItemSelectedListener=object: AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: AdapterView<*>?) {} ; override fun onItemSelected(parent: AdapterView<*>?,v:View?,position:Int,id:Long) { change(values[position]); render() } }
  }) }
  spinner(listOf("All languages")+Templates.languages,language) { language=it }; spinner(listOf("Recent","Name","Oldest"),sort) { sort=it }
  page.addView(CheckBox(requireContext()).apply { text="Favorites only"; isChecked=favorites; setOnCheckedChangeListener { _,checked -> favorites=checked; render() } })
  page.addView(MaterialButton(requireContext()).apply { text="＋ New Project"; setOnClickListener { startActivity(Intent(requireContext(),ProjectActivity::class.java)) } })
  page.addView(MaterialButton(requireContext()).apply { text="Import Project ZIP"; setOnClickListener { startActivity(Intent(requireContext(),ProjectActivity::class.java).putExtra("import",true)) } })
  list=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL }; page.addView(list)
  viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { repo.projects.collect { all=it; render() } } }
 }
 override fun onSaveInstanceState(out: Bundle) { out.putString("query",query); out.putString("language",language); out.putString("sort",sort); out.putBoolean("favorites",favorites); super.onSaveInstanceState(out) }
 private fun action(block: suspend ()->Unit) { viewLifecycleOwner.lifecycleScope.launch { try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) { if(isAdded) MaterialAlertDialogBuilder(requireContext()).setTitle("Action failed").setMessage(e.message).setPositiveButton("OK",null).show() } } }
 private fun open(p: Project) { startActivity(Intent(requireContext(),ProjectActivity::class.java).putExtra("project",p.id)) }
 private fun render() {
  if(!::list.isInitialized) return
  list.removeAllViews()
  val filtered=all.filter { it.name.contains(query,true) && (language=="All languages" || language==it.language) && (!favorites || it.favorite) }
  val items=when(sort) { "Name" -> filtered.sortedBy { it.name.lowercase() }; "Oldest" -> filtered.sortedBy { it.created }; else -> filtered.sortedByDescending { it.modified } }
  if(items.isEmpty()) list.addView(TextView(requireContext()).apply { text=if(all.isEmpty()) "No projects yet.\nCreate your first CodeArc project." else "No matching projects."; setPadding(0,24,0,24) })
  items.forEach { p ->
   val card=layoutInflater.inflate(R.layout.item_card,list,false) as MaterialCardView
   val content=card.findViewById<LinearLayout>(R.id.card_content)
   content.addView(MaterialButton(requireContext()).apply { text="${symbol(p.language)}  ${p.name}"; setOnClickListener { open(p) } })
   content.addView(TextView(requireContext()).apply { text="${p.language} · ${p.executionMode}\nModified ${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(p.modified))}" })
   content.addView(MaterialButton(requireContext(),null,com.google.android.material.R.attr.borderlessButtonStyle).apply { text=if(p.favorite) "★ Favorited" else "☆ Favorite"; setOnClickListener { action { repo.favorite(p.id) } } })
   content.addView(MaterialButton(requireContext(),null,com.google.android.material.R.attr.borderlessButtonStyle).apply { text="⋮ Options"; setOnClickListener { menu(p) } })
   list.addView(card)
  }
 }
 private fun symbol(language: String)=when(language) { "Python"->"Py"; "JavaScript"->"JS"; "Kotlin"->"Kt"; "Java"->"J"; "Lua"->"Lu"; else->language }
 private fun menu(p: Project) {
  MaterialAlertDialogBuilder(requireContext()).setTitle(p.name).setItems(arrayOf("Open","Rename","Duplicate","Export","Delete")) { _,which ->
   when(which) {
    0 -> open(p)
    1 -> { val input=EditText(requireContext()).apply { setText(p.name); isSingleLine=true }; MaterialAlertDialogBuilder(requireContext()).setTitle("Rename project").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Rename") { _,_ -> action { repo.rename(p.id,input.text.toString()) } }.show() }
    2 -> action { repo.duplicate(p.id) }
    3 -> startActivity(Intent(requireContext(),ProjectActivity::class.java).putExtra("project",p.id).putExtra("export",true))
    4 -> MaterialAlertDialogBuilder(requireContext()).setTitle("Delete ${p.name}?").setMessage("Remove this project from your workspace? A private trash copy is retained; there is no restore screen yet.").setNegativeButton("Cancel",null).setPositiveButton("Delete") { _,_ -> action { repo.delete(p.id) } }.show()
   }
  }.show()
 }
}
