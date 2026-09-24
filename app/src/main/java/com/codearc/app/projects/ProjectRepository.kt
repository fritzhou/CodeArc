package com.codearc.app.projects

import android.content.Context
import com.codearc.app.data.CodeArcDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.*
import java.util.UUID

class ProjectRepository(context: Context) {
 private val base = File(context.applicationContext.filesDir, "projects").apply { mkdirs() }
 private val trash = File(context.applicationContext.filesDir, "project-trash").apply { mkdirs() }
 private val dao = CodeArcDatabase.get(context).projects()
 val projects = dao.observe()
 companion object { private val lock = Mutex() }
 suspend fun get(id: String) = dao.get(id) ?: error("Project no longer exists.")
 private suspend fun <T> work(block: suspend () -> T): T = withContext(Dispatchers.IO) { lock.withLock { withContext(NonCancellable) { block() } } }
 fun root(p: Project): File {
  val root = File(p.path).canonicalFile
  require(root.parentFile == base.canonicalFile && root.name == p.id) { "Invalid project location." }
  return root
 }
 private fun config(p: Project) {
  val data = JSONObject().put("id",p.id).put("name",p.name).put("language",p.language).put("mainFile",p.mainFile).put("executionMode",p.executionMode).put("runtimePreference",p.runtimePreference).put("projectDirectory",p.path).put("created",p.created).put("modified",p.modified).put("favorite",p.favorite)
  val out = File(root(p),"project.json")
  val atomic = android.util.AtomicFile(out)
  val stream = atomic.startWrite()
  try { stream.write(data.toString(2).toByteArray()); atomic.finishWrite(stream) } catch(e: Exception) { atomic.failWrite(stream); throw e }
 }
 private suspend fun save(p: Project) {
  val previous=dao.get(p.id)
  config(p)
  try { dao.update(p) } catch(e: Exception) { if(previous!=null) config(previous); throw e }
 }
 suspend fun create(name: String, language: String, template: String, mode: String): Project = work {
  require(language in Templates.languages && template in Templates.names && mode in listOf("Automatic","Offline","Online"))
  val id = UUID.randomUUID().toString(); val now = System.currentTimeMillis()
  val p = Project(id,SafeFiles.name(name),language,File(base,id).path,now,now,mainFile="src/${Templates.file(language)}",executionMode=mode)
  val folder = root(p); check(folder.mkdir())
  try {
   File(folder,"src").mkdir(); File(folder,"assets").mkdir()
   File(folder,p.mainFile).writeText(Templates.code(language,template)); config(p); dao.insert(p); p
  } catch(e: Exception) { folder.deleteRecursively(); throw e }
 }
 suspend fun rename(id: String, name: String) = work { val p=get(id); save(p.copy(name=SafeFiles.name(name),modified=System.currentTimeMillis())) }
 // Phase 7: Clone Repository. allocate() reserves an empty project folder (JGit's clone needs
 // an existing-and-empty or not-yet-existing target directory) before any network call happens;
 // adopt() registers it as a real project afterwards once a main file has been found in the
 // cloned content, and discard() cleans up on any failure in between — same rollback shape as
 // create()/duplicate() above, just split across the network call in the middle.
 suspend fun allocate(): Pair<String, File> = work {
  val newId = UUID.randomUUID().toString(); val folder = File(base, newId); check(folder.mkdir()); newId to folder
 }
 suspend fun adopt(id: String, folder: File, name: String, language: String, mainFile: String, mode: String): Project = work {
  require(language in Templates.languages) { "Unrecognized project language." }
  require(mode in listOf("Automatic","Offline","Online"))
  val now = System.currentTimeMillis()
  val p = Project(id, SafeFiles.name(name), language, folder.path, now, now, mainFile = mainFile, executionMode = mode)
  try { config(p); dao.insert(p); p } catch (e: Exception) { folder.deleteRecursively(); throw e }
 }
 suspend fun discard(folder: File) = work { folder.deleteRecursively(); Unit }
 suspend fun favorite(id: String) = work { val p=get(id); save(p.copy(favorite=!p.favorite)) }
 suspend fun duplicate(id: String): Project = work {
  val old=get(id); val newId=UUID.randomUUID().toString(); val now=System.currentTimeMillis()
  val p=old.copy(id=newId,name=old.name.take(70)+" copy",path=File(base,newId).path,created=now,modified=now,favorite=false)
  try { SafeFiles.copy(root(old),root(p)); config(p); dao.insert(p); p } catch(e: Exception) { root(p).deleteRecursively(); throw e }
 }
 suspend fun delete(id: String) = work {
  val p=get(id); val source=root(p); val target=File(trash,"${p.id}-${System.currentTimeMillis()}")
  check(source.renameTo(target)) { "Could not move project to internal trash. Nothing was deleted." }
  try { dao.delete(id) } catch(e: Exception) { target.renameTo(source); throw e }
 }
 suspend fun entries(id: String, relative: String): List<File> = work {
  val folder=SafeFiles.resolve(root(get(id)),relative); require(folder.isDirectory)
  folder.listFiles()?.filter { it.name != "project.json" && it.name != ".trash" }?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
 }
 private fun editable(p: Project, relative: String): File {
  val f=SafeFiles.resolve(root(p),relative)
  require(f != root(p) && relative.split('/').none { it == ".trash" } && f != File(root(p),"project.json")) { "This is a protected project path." }
  return f
 }
 suspend fun createEntry(id: String, parent: String, name: String, folder: Boolean) = work {
  val p=get(id); val dir=SafeFiles.resolve(root(p),parent); require(dir.isDirectory)
  val f=editable(p, listOf(parent,SafeFiles.name(name)).filter { it.isNotEmpty() }.joinToString("/"))
  require(!f.exists()) { "That name already exists." }
  check(if(folder) f.mkdir() else f.createNewFile()) { "Could not create entry." }; save(p.copy(modified=System.currentTimeMillis()))
 }
 suspend fun changeEntry(id: String, relative: String, destination: String, duplicate: Boolean) = work {
  val p=get(id); val source=editable(p,relative); val target=editable(p,destination)
  require(source.exists() && target.parentFile!!.isDirectory) { "Choose an existing destination folder." }
  require(!target.exists()) { "Destination already exists." }
  require(!target.canonicalPath.startsWith(source.canonicalPath + File.separator)) { "Cannot move a folder into itself." }
  if(duplicate) { try { SafeFiles.copy(source,target) } catch(e: Exception) { target.deleteRecursively(); throw e } }
  else check(source.renameTo(target)) { "Move failed. Source was retained." }
  val normalizedTarget=target.relativeTo(root(p)).invariantSeparatorsPath
  val main=if(!duplicate && (p.mainFile==relative || p.mainFile.startsWith("$relative/"))) normalizedTarget+p.mainFile.removePrefix(relative) else p.mainFile
  try { save(p.copy(mainFile=main,modified=System.currentTimeMillis())) }
  catch(e: Exception) { if(duplicate) target.deleteRecursively() else target.renameTo(source); throw e }
 }
 suspend fun deleteEntry(id: String, relative: String) = work {
  val p=get(id)
  require(p.mainFile != relative && !p.mainFile.startsWith("$relative/")) { "Set a different main file in project configuration before deleting this entry." }
  val source=editable(p,relative); require(source.exists())
  val bin=File(root(p),".trash").apply { mkdirs() }
  check(source.renameTo(File(bin,UUID.randomUUID().toString()+"-"+source.name))) { "Could not move entry to internal trash." }
  save(p.copy(modified=System.currentTimeMillis()))
 }
 suspend fun configure(id: String, main: String, mode: String, runtime: String) = work {
  val p=get(id); require(editable(p,main).isFile) { "Main file must be an existing file." }
  require(mode in listOf("Automatic","Offline","Online")); save(p.copy(mainFile=editable(p,main).relativeTo(root(p)).invariantSeparatorsPath,executionMode=mode,runtimePreference=runtime.ifBlank { "Default" },modified=System.currentTimeMillis()))
 }
 suspend fun preview(id: String, relative: String): String = work {
  val f=editable(get(id),relative); require(f.isFile && f.length() <= 256*1024) { "Preview supports text files up to 256 KB." }; f.readText()
 }
 suspend fun read(id: String, relative: String): String = work {
  val f=editable(get(id),relative); require(f.isFile && f.length() <= 8L*1024*1024) { "This file is too large to edit (8 MB limit)." }; f.readText()
 }
 suspend fun write(id: String, relative: String, content: String) = work {
  val p=get(id); val f=editable(p,relative); require(f.isFile) { "File no longer exists." }
  val atomic=android.util.AtomicFile(f); val stream=atomic.startWrite()
  try { stream.write(content.toByteArray()); atomic.finishWrite(stream) } catch(e: Exception) { atomic.failWrite(stream); throw e }
  save(p.copy(modified=System.currentTimeMillis()))
 }
 suspend fun importFile(id: String, parent: String, name: String, input: InputStream) = work {
  val p=get(id); val f=editable(p,listOf(parent,SafeFiles.name(name)).filter { it.isNotEmpty() }.joinToString("/"))
  require(!f.exists()) { "File already exists. Rename it before importing." }
  try { input.use { src -> f.outputStream().use { out -> val b=ByteArray(8192); var total=0L; while(true) { val n=src.read(b); if(n<0) break; total+=n; require(total<=50L*1024*1024) { "File exceeds 50 MB." }; out.write(b,0,n) } } }; save(p.copy(modified=System.currentTimeMillis())) }
  catch(e: Exception) { f.delete(); throw e }
 }
 suspend fun export(id: String, relative: String?, output: OutputStream) = work {
  val p=get(id); val file=if(relative==null) root(p) else editable(p,relative)
  if(relative!=null && file.isFile) output.use { out -> file.inputStream().use { it.copyTo(out) } }
  else SafeFiles.zip(file,output)
 }
 suspend fun importProject(input: InputStream): Project = work {
  val id=UUID.randomUUID().toString(); val folder=File(base,id); check(folder.mkdir())
  try {
   SafeFiles.unzip(input,folder)
   val configFile=File(folder,"project.json"); require(configFile.isFile && configFile.length()<65536) { "Choose a CodeArc project ZIP with project.json at its root." }
   val json=JSONObject(configFile.readText()); val language=json.getString("language"); require(language in Templates.languages)
   val main=json.getString("mainFile"); require(SafeFiles.resolve(folder,main).isFile && main!="project.json" && !main.startsWith(".trash/"))
   val mode=json.optString("executionMode","Automatic"); require(mode in listOf("Automatic","Offline","Online"))
   val now=System.currentTimeMillis(); val p=Project(id,SafeFiles.name(json.getString("name")),language,folder.path,now,now,executionMode=mode,mainFile=main,runtimePreference=json.optString("runtimePreference","Default"))
   config(p); dao.insert(p); p
  } catch(e: Exception) { folder.deleteRecursively(); throw e }
 }
}
