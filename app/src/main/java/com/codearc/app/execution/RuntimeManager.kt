package com.codearc.app.execution

/** Which offline runtimes actually exist right now. Python ships as a real bundled CPython
 *  (Chaquopy), not a placeholder. HTML and CSS are genuinely offline too — rendered/applied via
 *  Android's built-in WebView, nothing to download, so they're honestly "installed" as well.
 *  "javascript" deliberately stays false here: that id is shared with the pre-existing
 *  standalone (Node-flavored) JavaScript project template, whose Run goes through
 *  ExecutionManager.run()/LocalExecutionEngine and must keep falling back to cloud exactly as
 *  before. JavaScript genuinely does run offline inside HTML projects and Learn — see
 *  ExecutionManager.runSnippet and EditorActivity.runWebProject — entirely independent of this
 *  flag, so nothing here is misleading, just scoped to what this particular flag gates. */
object RuntimeManager {
    fun isInstalled(languageId: String): Boolean = languageId == "python" || languageId == "html" || languageId == "css"
    fun isRemovable(languageId: String): Boolean = false // nothing here is downloaded separately
    fun storageSizeMb(languageId: String): Int = if (languageId == "python") 28 else 0
}
