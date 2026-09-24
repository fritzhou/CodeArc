package com.codearc.app.execution

/** Which offline runtimes actually exist right now. Phase 4 ships exactly one — Python — as a
 *  real bundled CPython (Chaquopy), not a placeholder. Nothing here reports a language as
 *  installed unless it truly can run; see PHASE4.md for why the other six aren't faked. */
object RuntimeManager {
    fun isInstalled(languageId: String): Boolean = languageId == "python"
    fun isRemovable(languageId: String): Boolean = false // Python ships inside the app itself
    fun storageSizeMb(languageId: String): Int = if (languageId == "python") 28 else 0
}
