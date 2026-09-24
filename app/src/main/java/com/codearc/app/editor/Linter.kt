package com.codearc.app.editor

/** Lightweight, language-agnostic offline checks: unmatched brackets and unterminated strings.
 *  This is not a compiler or parser — real diagnostics arrive with execution in Phase 4.
 *  It does not understand comments, so brackets inside comments can produce false positives. */
object Linter {
    private val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}')
    private val CLOSERS = PAIRS.values.toSet()

    fun lint(file: String, text: String): List<Problem> {
        val problems = mutableListOf<Problem>()
        val stack = ArrayDeque<Triple<Char, Int, Int>>()
        var line = 1; var col = 1
        var inString: Char? = null
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\n') { line++; col = 1; i++; continue }
            if (inString != null) {
                if (c == '\\') { i += 2; col += 2; continue }
                if (c == inString) inString = null
                i++; col++; continue
            }
            when {
                c == '"' || c == '\'' -> inString = c
                c in PAIRS.keys -> stack.addLast(Triple(c, line, col))
                c in CLOSERS -> {
                    if (stack.isEmpty() || PAIRS[stack.last().first] != c) problems.add(Problem(file, line, col, Severity.ERROR, "Unexpected '$c'."))
                    else stack.removeLast()
                }
            }
            i++; col++
        }
        if (inString != null) problems.add(Problem(file, line, col, Severity.ERROR, "Unterminated string literal."))
        stack.forEach { (ch, l, c) -> problems.add(Problem(file, l, c, Severity.ERROR, "Unmatched '$ch'.")) }
        return problems
    }
}
