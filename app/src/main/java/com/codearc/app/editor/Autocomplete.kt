package com.codearc.app.editor

/** Prepared for LSP, per the Phase 7 plan ("Improve language-aware completion. Prepare the
 *  editor architecture for LSP... normal editor use must still work if LSP is unavailable").
 *  [KeywordCompletionProvider] is the real, working implementation today: language keywords
 *  plus identifiers already typed in the current file. A future LspCompletionProvider can
 *  implement this same interface and swap in without CodeEditorView or EditorActivity changing
 *  — that swap point is the entire point of the interface existing. */
interface CompletionProvider {
    /** [prefix] is the partial word being typed (never blank); [fileText] is the whole file, so
     *  an implementation can also suggest identifiers the user already defined. */
    fun complete(prefix: String, fileText: String, language: String): List<String>
}

object KeywordCompletionProvider : CompletionProvider {
    private val keywords = mapOf(
        "python" to listOf("def", "return", "if", "elif", "else", "for", "while", "in", "import", "from", "class", "self", "try", "except", "finally", "with", "as", "lambda", "print", "range", "len", "True", "False", "None", "break", "continue", "pass", "yield", "raise"),
        "javascript" to listOf("function", "return", "if", "else", "for", "while", "const", "let", "var", "class", "import", "export", "console", "log", "async", "await", "try", "catch", "finally", "new", "this", "null", "undefined", "true", "false", "break", "continue"),
        "java" to listOf("public", "private", "protected", "static", "void", "class", "interface", "return", "if", "else", "for", "while", "new", "import", "package", "extends", "implements", "try", "catch", "finally", "int", "String", "boolean", "true", "false", "null", "System", "out", "println"),
        "kotlin" to listOf("fun", "val", "var", "return", "if", "else", "for", "while", "class", "object", "interface", "import", "package", "when", "is", "in", "try", "catch", "finally", "null", "true", "false", "println", "readln"),
        "c" to listOf("int", "char", "float", "double", "void", "return", "if", "else", "for", "while", "struct", "typedef", "include", "define", "printf", "scanf", "main", "const", "static"),
        "cpp" to listOf("int", "char", "float", "double", "void", "return", "if", "else", "for", "while", "class", "struct", "public", "private", "namespace", "using", "std", "cout", "cin", "endl", "include", "const", "new", "delete"),
        "lua" to listOf("function", "return", "if", "then", "else", "elseif", "end", "for", "while", "do", "local", "print", "require", "nil", "true", "false", "io", "table")
    )
    override fun complete(prefix: String, fileText: String, language: String): List<String> {
        val lower = prefix.lowercase()
        val fromKeywords = (keywords[language.lowercase()] ?: emptyList()).filter { it.lowercase().startsWith(lower) && !it.equals(prefix, true) }
        val identifiers = Regex("[A-Za-z_][A-Za-z0-9_]{2,}").findAll(fileText).map { it.value }.toSet()
        val fromFile = identifiers.filter { it.lowercase().startsWith(lower) && !it.equals(prefix, true) }.sorted()
        return (fromKeywords + fromFile).distinct().take(8)
    }
}
