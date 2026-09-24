package com.codearc.app.editor

import android.graphics.Typeface
import android.text.Editable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

/** Regex-based highlighting. Comments/strings are matched first and masked off so keywords
 *  and numbers inside them are never recolored. Call only from a debounced caller —
 *  this walks the full text on every call and is not meant to run per keystroke. */
object SyntaxHighlighter {
    private class HL(color: Int) : ForegroundColorSpan(color)

    private val KEYWORDS = mapOf(
        "Python" to setOf("def","return","if","elif","else","for","while","in","import","from","as","class","try","except","finally","with","pass","break","continue","lambda","yield","global","nonlocal","not","and","or","is","None","True","False","raise","assert","del","async","await"),
        "JavaScript" to setOf("function","return","if","else","for","while","in","of","var","let","const","class","try","catch","finally","new","this","typeof","instanceof","null","undefined","true","false","break","continue","switch","case","default","import","export","from","async","await","yield","throw","extends","super","static"),
        "Java" to setOf("public","private","protected","class","interface","extends","implements","static","final","void","int","long","double","float","boolean","char","byte","short","new","return","if","else","for","while","do","switch","case","default","break","continue","try","catch","finally","throw","throws","import","package","this","super","null","true","false","enum","abstract","synchronized","volatile","transient","instanceof"),
        "Kotlin" to setOf("fun","val","var","return","if","else","for","while","when","class","interface","object","package","import","is","in","as","null","true","false","try","catch","finally","throw","break","continue","this","super","override","private","protected","public","internal","companion","data","sealed","enum","suspend","inline","lateinit","init","by","typealias"),
        "C" to setOf("int","char","float","double","void","long","short","unsigned","signed","struct","union","enum","typedef","if","else","for","while","do","switch","case","default","break","continue","return","sizeof","static","const","extern","volatile","goto","include","define"),
        "C++" to setOf("int","char","float","double","void","long","short","unsigned","signed","struct","union","enum","typedef","class","public","private","protected","virtual","new","delete","namespace","using","template","typename","if","else","for","while","do","switch","case","default","break","continue","return","sizeof","static","const","extern","volatile","try","catch","throw","this","nullptr","true","false","include","auto"),
        "Lua" to setOf("function","end","if","then","else","elseif","for","while","do","repeat","until","local","return","break","in","and","or","not","nil","true","false","goto")
    )
    private val NUMBER = Regex("\\b\\d+(\\.\\d+)?\\b")
    private val IDENT = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")

    private fun stringRegex(language: String) = if (language == "Python")
        Regex("(\"\"\".*?\"\"\"|'''.*?'''|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*')", RegexOption.DOT_MATCHES_ALL)
    else
        Regex("(\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*')")

    private fun commentRegexes(language: String): List<Regex> = when (language) {
        "Python" -> listOf(Regex("#.*"))
        "Lua" -> listOf(Regex("--\\[\\[.*?]]", RegexOption.DOT_MATCHES_ALL), Regex("--.*"))
        else -> listOf(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), Regex("//.*"))
    }

    fun highlight(editable: Editable, language: String) {
        editable.getSpans(0, editable.length, HL::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { editable.removeSpan(it) }
        val text = editable.toString()
        if (text.isEmpty()) return
        val masked = BooleanArray(text.length)
        fun mark(range: IntRange, color: Int, bold: Boolean = false) {
            editable.setSpan(HL(color), range.first, range.last + 1, Editable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (bold) editable.setSpan(StyleSpan(Typeface.BOLD), range.first, range.last + 1, Editable.SPAN_EXCLUSIVE_EXCLUSIVE)
            for (i in range) masked[i] = true
        }
        commentRegexes(language).forEach { rx -> rx.findAll(text).forEach { m -> if (m.range.none { masked[it] }) mark(m.range, 0xFF6B7A99.toInt()) } }
        stringRegex(language).findAll(text).forEach { m -> if (m.range.none { masked[it] }) mark(m.range, 0xFF25C975.toInt()) }
        val kw = KEYWORDS[language] ?: emptySet()
        if (kw.isNotEmpty()) IDENT.findAll(text).forEach { m -> if (m.value in kw && m.range.none { masked[it] }) mark(m.range, 0xFF7B4DFF.toInt(), bold = true) }
        NUMBER.findAll(text).forEach { m -> if (m.range.none { masked[it] }) mark(m.range, 0xFF14C8FF.toInt()) }
    }
}
