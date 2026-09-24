package com.codearc.app.projects

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** All user-supplied paths are resolved inside an explicit project root. */
object SafeFiles {
 fun name(value: String): String {
  val v = value.trim()
  require(v.isNotEmpty() && v.length <= 80 && v != "." && v != ".." && v.none { it == '/' || it == '\\' || it == ':' || it.code < 32 }) { "Use a name of 1–80 characters without slashes or control characters." }
  return v
 }
 fun resolve(root: File, relative: String): File {
  require(!File(relative).isAbsolute && !relative.contains('\\')) { "Use a relative project path." }
  val file = File(root, relative).canonicalFile
  require(file == root.canonicalFile || file.path.startsWith(root.canonicalPath + File.separator)) { "Path is outside the project." }
  return file
 }
 fun copy(source: File, target: File) {
  require(!target.exists()) { "A file or folder already exists with that name." }
  require(source.exists()) { "Source no longer exists." }
  require(target.canonicalFile != source.canonicalFile && !target.canonicalPath.startsWith(source.canonicalPath + File.separator)) { "Cannot copy a folder into itself." }
  check(source.copyRecursively(target, overwrite = false)) { "Copy failed." }
 }
 fun zip(root: File, output: OutputStream) {
  ZipOutputStream(output).use { zip ->
   val files = if(root.isDirectory) root.walkTopDown().onEnter { it.name != ".trash" }.drop(1) else sequenceOf(root)
   files.forEach { file ->
    require(file.canonicalPath.startsWith(if(root.isDirectory) root.canonicalPath + File.separator else root.canonicalPath))
    val relative = if(root.isDirectory) file.relativeTo(root).invariantSeparatorsPath else file.name
    zip.putNextEntry(ZipEntry(relative + if(file.isDirectory) "/" else ""))
    if(file.isFile) file.inputStream().use { it.copyTo(zip) }
    zip.closeEntry()
   }
  }
 }
 fun unzip(input: InputStream, root: File) {
  var total = 0L; var count = 0
  ZipInputStream(input).use { zip ->
   while(true) {
    val entry = zip.nextEntry ?: break
    require(++count <= 5000) { "Archive has too many entries." }
    val out = resolve(root, entry.name)
    require(out != root.canonicalFile) { "Invalid archive entry." }
    require(!out.exists() || (entry.isDirectory && out.isDirectory)) { "Archive contains duplicate paths." }
    if(entry.isDirectory) out.mkdirs() else {
     out.parentFile!!.mkdirs()
     out.outputStream().use { stream ->
      val buffer = ByteArray(8192)
      while(true) { val n = zip.read(buffer); if(n < 0) break; total += n; require(total <= 50L * 1024 * 1024) { "Archive exceeds 50 MB." }; stream.write(buffer,0,n) }
     }
    }
    zip.closeEntry()
   }
  }
 }
}
