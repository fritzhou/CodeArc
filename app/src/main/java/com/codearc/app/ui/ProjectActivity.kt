package com.codearc.app.ui

import android.os.Bundle
import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.widget.*
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codearc.app.R
import com.codearc.app.projects.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import java.io.File

/** Real file explorer, project configuration and (Phase 7) Source Control — file *editing*
 *  itself lives in EditorActivity, opened from here. */
class ProjectActivity : AppCompatActivity() {
 private lateinit var page: LinearLayout
 private lateinit var repo: ProjectRepository
 private var id: String? = null
 private var folder = ""
 private var exportRelative: String? = null
 private var importFolder = ""
 private var working = false
 private val importFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if(uri!=null) task {
   var name="imported.txt"
   contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { if(it.moveToFirst()) name=it.getString(0) }
   repo.importFile(id!!,importFolder,name,contentResolver.openInputStream(uri) ?: error("Cannot open document.")); explorer()
  }
 }
 private val importProject = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if(uri!=null) task { id=repo.importProject(contentResolver.openInputStream(uri) ?: error("Cannot read ZIP.")).id; folder=""; explorer() }
  else if(id==null) finish()
 }
 private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
  if(uri!=null) task { repo.export(id!!,exportRelative,contentResolver.openOutputStream(uri,"w") ?: error("Cannot write document.")); message("Export complete", "Saved to your selected location.") }
 }
 override fun onCreate(state: Bundle?) {
  super.onCreate(state); setContentView(R.layout.fragment_page)
  page=findViewById(R.id.page); repo=ProjectRepository(applicationContext)
  onBackPressedDispatcher.addCallback(this,object: OnBackPressedCallback(true) {
   override fun handleOnBackPressed() { if(folder.isNotEmpty()) { folder=folder.substringBeforeLast('/',""); task { explorer() } } else finish() }
  })
  id=state?.getString("project") ?: intent.getStringExtra("project")
  folder=state?.getString("folder") ?: ""
  exportRelative=state?.getString("exportRelative"); importFolder=state?.getString("importFolder") ?: ""
  if(id!=null) task { explorer(); if(state==null && intent.getBooleanExtra("export",false)) { exportRelative=null; export.launch("${repo.get(id!!).name}.zip") } }
  else if(intent.getBooleanExtra("import",false)) { if(state==null) importProject.launch(arrayOf("application/zip","application/octet-stream")) }
  else if(intent.getBooleanExtra("clone",false)) { if(state==null) task { cloneFlow(intent.getStringExtra("url") ?: error("No repository URL was given.")) } }
  else createForm(state)
 }
 override fun onSaveInstanceState(outState: Bundle) {
  outState.putString("project",id); outState.putString("folder",folder); outState.putString("exportRelative",exportRelative); outState.putString("importFolder",importFolder)
  if(id==null && ::nameInput.isInitialized) { outState.putString("draftName",nameInput.text.toString()); outState.putInt("language",languageSpinner.selectedItemPosition); outState.putInt("template",templateSpinner.selectedItemPosition); outState.putInt("mode",modeSpinner.selectedItemPosition) }
  super.onSaveInstanceState(outState)
 }
 private fun task(block: suspend () -> Unit) {
  if(working) return
  working=true
  lifecycleScope.launch {
   try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) { message("Could not complete action",e.message ?: "Please try again.") } finally { working=false }
  }
 }
 private fun message(title: String, body: String) { if(!isFinishing && !isDestroyed) MaterialAlertDialogBuilder(this).setTitle(title).setMessage(body).setPositiveButton("OK",null).show() }
 private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
 private fun title(value: String) { page.addView(TextView(this).apply { text=value; textSize=22f; setTextColor(getColor(R.color.text)); setPadding(0,dp(4),0,dp(14)); setTypeface(typeface,Typeface.BOLD) }) }
 private fun label(value: String) { page.addView(TextView(this).apply { text=value; textSize=13f; setTextColor(getColor(R.color.muted)); setPadding(0,dp(6),0,dp(10)); setLineSpacing(dp(3).toFloat(),1f) }) }
 private fun button(value: String, action: () -> Unit) { page.addView(MaterialButton(this).apply { text=value; setOnClickListener { action() } }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,0,0,dp(8)) }) }
 /** Builds a labeled outlined text field. The value is set only after the field is fully
  *  attached to [box] and [box] to [page], which — together with forcing the outlined box style
  *  via [outlined] — is what keeps the hint from overlapping the value (see UiKit.kt). */
 private fun field(hint: String, value: String=""): TextInputEditText {
  val box=TextInputLayout(this).outlined().apply { this.hint=hint; setPadding(0,dp(4),0,dp(2)) }
  val input=TextInputEditText(box.context).apply { isSingleLine=true }
  box.addView(input)
  page.addView(box, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,0,0,dp(10)) })
  input.setText(value)
  return input
 }
 private fun spinner(caption: String, values: List<String>): Spinner {
  page.addView(TextView(this).apply { text=caption; textSize=13f; setTextColor(getColor(R.color.muted)); setPadding(0,dp(6),0,dp(4)) })
  return Spinner(this).also {
   it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,values)
   it.setPadding(dp(4),dp(10),dp(4),dp(10))
   page.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,0,0,dp(10)) })
  }
 }
 private lateinit var nameInput: TextInputEditText
 private lateinit var languageSpinner: Spinner
 private lateinit var templateSpinner: Spinner
 private lateinit var modeSpinner: Spinner
 private fun createForm(state: Bundle?) {
  page.removeAllViews(); title("New Project")
  nameInput=field("Project name",state?.getString("draftName") ?: intent.getStringExtra("suggestedName") ?: "")
  languageSpinner=spinner("Programming language",Templates.languages)
  templateSpinner=spinner("Template",Templates.names)
  modeSpinner=spinner("Execution mode",listOf("Automatic","Offline","Online"))
  languageSpinner.setSelection(state?.getInt("language") ?: intent.getIntExtra("suggestedLanguage",0)); templateSpinner.setSelection(state?.getInt("template") ?: intent.getIntExtra("suggestedTemplate",1)); modeSpinner.setSelection(state?.getInt("mode") ?: 0)
  label("Project location: CodeArc private storage\n${filesDir.path}/projects/<project-id>/\nExport a ZIP to save a copy elsewhere. App data is removed on uninstall.")
  button("Create") { task { id=repo.create(nameInput.text.toString(),languageSpinner.selectedItem.toString(),templateSpinner.selectedItem.toString(),modeSpinner.selectedItem.toString()).id; folder=""; explorer() } }
  button("Cancel") { finish() }
 }
 private fun openInEditor(relative: String) { startActivity(Intent(this, EditorActivity::class.java).putExtra("project", id).putExtra("file", relative)) }
 private suspend fun cloneFlow(url: String) {
  val (newId, folder) = repo.allocate()
  try {
   com.codearc.app.git.GitManager.clone(url, folder)
   val main = guessMainFile(folder) ?: run { repo.discard(folder); error("Couldn't find a recognizable source file (e.g. main.py, Main.java) in this repository. Clone it elsewhere and import the file you want instead.") }
   val relative = main.relativeTo(folder).invariantSeparatorsPath
   val language = Templates.languageFor(main.name)
   val name = url.substringAfterLast('/').removeSuffix(".git").ifBlank { "Cloned Project" }
   id = repo.adopt(newId, folder, name, language, relative, "Automatic").id
   explorer()
  } catch (e: Exception) { repo.discard(folder); throw e }
 }
 /** Shallow, bounded scan (root + two levels of subfolders, capped at 400 entries) for a
  *  recognizable entry point. Good enough for typical small repos; anything deeper can have its
  *  main file set afterward via Project configuration, which already exists below. */
 private fun guessMainFile(root: File): File? {
  val canonicalNames = setOf("main.py","Main.java","main.kt","main.js","index.js","main.c","main.cpp","main.lua")
  val sourceExtensions = setOf("py","js","c","cpp","cc","cxx","java","kt","lua")
  var scanned = 0
  fun scan(dir: File, depth: Int, matches: (File) -> Boolean): File? {
   if (scanned > 400) return null
   val children = dir.listFiles()?.sortedBy { it.name } ?: return null
   for (f in children) { scanned++; if (f.isFile && matches(f)) return f }
   if (depth > 0) for (f in children) { if (f.isDirectory && f.name != ".git") scan(f, depth - 1, matches)?.let { return it } }
   return null
  }
  scan(root, 2) { it.name in canonicalNames }?.let { return it }
  scanned = 0
  return scan(root, 2) { it.extension.lowercase() in sourceExtensions }
 }
 private suspend fun explorer() {
  val project=repo.get(id!!); val entries=repo.entries(project.id,folder)
  page.removeAllViews(); title(project.name)
  label("${project.language} · ${project.executionMode}\n/${folder}\nMain: ${project.mainFile}")
  button(if(folder.isEmpty()) "Back to Projects" else "↑ Parent folder") { if(folder.isEmpty()) finish() else { folder=folder.substringBeforeLast('/',""); task { explorer() } } }
  button("＋ Create / Import") {
   MaterialAlertDialogBuilder(this).setTitle("Add to /$folder").setItems(arrayOf("New File","New Folder","Import File")) { _,which ->
    if(which==2) { importFolder=folder; importFile.launch(arrayOf("*/*")) }
    else if(which==0) prompt("File name","") { name -> task { repo.createEntry(id!!,folder,name,false); val relative=listOf(folder,SafeFiles.name(name)).filter{it.isNotEmpty()}.joinToString("/"); openInEditor(relative) } }
    else prompt("Folder name","") { name -> task { repo.createEntry(id!!,folder,name,true); explorer() } }
   }.show()
  }
  button("Project configuration") { configuration(project) }
  button("Source Control") { startActivity(Intent(this, GitActivity::class.java).putExtra("project", project.id)) }
  button("Export Project ZIP") { exportRelative=null; export.launch("${project.name}.zip") }
  if(entries.isEmpty()) label("This folder is empty. Create a file or folder to get started.")
  entries.forEach { file ->
   val relative=file.relativeTo(repo.root(project)).invariantSeparatorsPath
   val card=layoutInflater.inflate(R.layout.item_card,page,false) as MaterialCardView
   val content=card.findViewById<LinearLayout>(R.id.card_content)
   content.addView(MaterialButton(this).apply { text=(if(file.isDirectory) "▸  " else "</>  ")+file.name; setOnClickListener { if(file.isDirectory) { folder=relative; task { explorer() } } else openInEditor(relative) } })
   content.addView(MaterialButton(this,null,com.google.android.material.R.attr.borderlessButtonStyle).apply { text="Manage ${file.name}"; setOnClickListener { entryMenu(relative,file) } })
   page.addView(card)
  }
 }
 private fun entryMenu(relative: String, file: File) {
  val options=arrayOf("Rename","Move","Duplicate","Export","Delete")
  MaterialAlertDialogBuilder(this).setTitle(file.name).setItems(options) { _,which ->
   when(which) {
    0 -> prompt("Rename",file.name) { name -> task { val destination=listOf(relative.substringBeforeLast('/',""),SafeFiles.name(name)).filter { it.isNotEmpty() }.joinToString("/"); repo.changeEntry(id!!,relative,destination,false); explorer() } }
    1 -> prompt("Move: destination path from project root",relative) { path -> task { repo.changeEntry(id!!,relative,path,false); explorer() } }
    2 -> prompt("Duplicate: new path from project root",relative+".copy") { path -> task { repo.changeEntry(id!!,relative,path,true); explorer() } }
    3 -> { exportRelative=relative; export.launch(file.name+if(file.isDirectory) ".zip" else "") }
    4 -> MaterialAlertDialogBuilder(this).setTitle("Delete ${file.name}?").setMessage("This removes the entry from your workspace. A private trash copy is retained; there is no restore screen yet.").setNegativeButton("Cancel",null).setPositiveButton("Delete") { _,_ -> task { repo.deleteEntry(id!!,relative); explorer() } }.show()
   }
  }.show()
 }
 private fun prompt(title: String, initial: String, action: (String)->Unit) {
  val input=EditText(this).apply { setText(initial); isSingleLine=true }
  MaterialAlertDialogBuilder(this).setTitle(title).setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save") { _,_ -> action(input.text.toString().trim()) }.show()
 }
 private fun configuration(p: Project) {
  page.removeAllViews(); title("Project configuration"); label("Language: ${p.language}\nDirectory: ${p.path}")
  val main=field("Main file",p.mainFile); val mode=spinner("Execution mode",listOf("Automatic","Offline","Online")); mode.setSelection(listOf("Automatic","Offline","Online").indexOf(p.executionMode))
  val runtime=field("Runtime preference",p.runtimePreference)
  label("Runtime preference is a free-text hint (e.g. a specific interpreter version) saved with the project; only Python actually runs offline right now — see Languages for what's installed.")
  button("Save configuration") { task { repo.configure(p.id,main.text.toString(),mode.selectedItem.toString(),runtime.text.toString()); explorer() } }
  button("Cancel") { task { explorer() } }
 }
}
