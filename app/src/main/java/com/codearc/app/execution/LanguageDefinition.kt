package com.codearc.app.execution

/** Everything CodeArc knows about a language, independent of whether it's installed yet. */
data class LanguageDefinition(
    val id: String,
    val displayName: String,
    val extensions: List<String>,
    val syntaxIdentifier: String,
    val runtimeDescription: String,
    val offlineCapable: Boolean,
    val onlineCapable: Boolean,
    val packageSupport: Boolean,
    val storageSizeMb: Int
)

/** The seven languages from the Phase 1 template list. Only Python has a real offline runtime
 *  in Phase 4 (bundled CPython via Chaquopy) — the rest are defined so the editor, Learn and
 *  Languages screens have something real to point at, without pretending they can run. */
object LanguageRegistry {
    val all = listOf(
        LanguageDefinition("python", "Python", listOf("py"), "python", "CPython 3.8 (bundled, offline)", offlineCapable = true, onlineCapable = true, packageSupport = false, storageSizeMb = 28),
        LanguageDefinition("javascript", "JavaScript", listOf("js"), "javascript", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("c", "C", listOf("c"), "c", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("cpp", "C++", listOf("cpp", "cc", "cxx"), "cpp", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("java", "Java", listOf("java"), "java", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("kotlin", "Kotlin", listOf("kt"), "kotlin", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("lua", "Lua", listOf("lua"), "lua", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        // Phase 7: the plan's "expand language support" list. Same honesty rule as every
        // language above — defined so Languages/Learn/the editor can describe them, but
        // RuntimeManager still reports them as not installed until a real runtime exists.
        LanguageDefinition("go", "Go", listOf("go"), "go", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("rust", "Rust", listOf("rs"), "rust", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("php", "PHP", listOf("php"), "php", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("ruby", "Ruby", listOf("rb"), "ruby", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0),
        LanguageDefinition("csharp", "C#", listOf("cs"), "csharp", "Not yet implemented", offlineCapable = false, onlineCapable = true, packageSupport = false, storageSizeMb = 0)
    )
    fun forId(id: String) = all.find { it.id == id }
    fun forDisplayName(name: String) = all.find { it.displayName.equals(name, ignoreCase = true) }
}
