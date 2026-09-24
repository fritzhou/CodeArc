package com.codearc.app.projects

import org.junit.Test
import org.junit.Assert.*
import java.io.*
import java.nio.file.Files
import java.util.zip.*

class SafeFilesTest {
 private fun root() = Files.createTempDirectory("codearc-test").toFile()
 private fun rejects(block: () -> Unit) { try { block(); fail("Expected rejection") } catch(expected: IllegalArgumentException) {} }
 @Test fun traversalCannotEscapeProject() {
  val dir=root()
  try { rejects { SafeFiles.resolve(dir,"../outside.txt") }; rejects { SafeFiles.resolve(dir,"/tmp/outside.txt") }; rejects { SafeFiles.resolve(dir,"..\\outside.txt") } } finally { dir.deleteRecursively() }
 }
 @Test fun archiveTraversalIsRejected() {
  val bytes=ByteArrayOutputStream(); ZipOutputStream(bytes).use { it.putNextEntry(ZipEntry("../escape.txt")); it.write(1); it.closeEntry() }
  val dir=root(); try { rejects { SafeFiles.unzip(ByteArrayInputStream(bytes.toByteArray()),dir) } } finally { dir.deleteRecursively() }
 }
 @Test fun copyNeverOverwritesExistingFile() {
  val dir=root(); try {
   val a=File(dir,"a").apply { writeText("source") }; val b=File(dir,"b").apply { writeText("keep") }
   rejects { SafeFiles.copy(a,b) }; assertEquals("keep",b.readText()); assertEquals("source",a.readText())
  } finally { dir.deleteRecursively() }
 }
 @Test fun zipRoundTripPreservesFilesButExcludesTrash() {
  val a=root(); val b=root()
  try {
   File(a,"src").mkdir(); File(a,"src/main.py").writeText("print('hello')\n")
   File(a,".trash").mkdir(); File(a,".trash/secret").writeText("deleted")
   val bytes=ByteArrayOutputStream(); SafeFiles.zip(a,bytes); SafeFiles.unzip(ByteArrayInputStream(bytes.toByteArray()),b)
   assertEquals("print('hello')\n",File(b,"src/main.py").readText()); assertFalse(File(b,".trash").exists())
  } finally { a.deleteRecursively(); b.deleteRecursively() }
 }
 @Test fun folderCannotBeCopiedIntoItself() { val dir=root(); try { rejects { SafeFiles.copy(dir,File(dir,"child")) } } finally { dir.deleteRecursively() } }
 @Test fun allTemplatesCoverAllLanguages() { Templates.languages.forEach { language -> Templates.names.forEach { template -> val code=Templates.code(language,template); if(template=="Empty Project") assertEquals("",code) else assertTrue(code.isNotBlank()) } } }
}
