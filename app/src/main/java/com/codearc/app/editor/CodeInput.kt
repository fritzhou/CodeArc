package com.codearc.app.editor

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/** The actual text surface. Auto-indent and auto-closing run from a TextWatcher guarded by
 *  `editing` so our own inserts don't recurse back into themselves. */
class CodeInput @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AppCompatEditText(context, attrs) {
    var tabSize = 4
    var autoIndentEnabled = true
    var autoBracketsEnabled = true
    var autoQuotesEnabled = true
    var onChanged: ((Editable) -> Unit)? = null
    private var editing = false
    private val OPEN = mapOf('(' to ')', '[' to ']', '{' to '}')
    private val QUOTES = setOf('"', '\'')

    init {
        isSingleLine = false
        setHorizontallyScrolling(true)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        addTextChangedListener(object : TextWatcher {
            var insertedAt = -1
            var insertedChar: Char? = null
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                insertedAt = if (count == 1 && before == 0) start else -1
                insertedChar = if (insertedAt >= 0) s?.get(start) else null
            }
            override fun afterTextChanged(e: Editable) {
                if (editing) return
                if (insertedAt >= 0) {
                    val ch = insertedChar
                    editing = true
                    try {
                        if (ch == '\n' && autoIndentEnabled) handleNewline(e, insertedAt)
                        else if (ch != null && ch in OPEN.keys && autoBracketsEnabled) { e.insert(insertedAt + 1, OPEN[ch].toString()); setSelection(insertedAt + 1) }
                        else if (ch != null && ch in QUOTES && autoQuotesEnabled && !isClosingQuote(e, insertedAt, ch)) { e.insert(insertedAt + 1, ch.toString()); setSelection(insertedAt + 1) }
                    } finally { editing = false }
                }
                onChanged?.invoke(e)
            }
        })
    }

    private fun isClosingQuote(e: Editable, at: Int, ch: Char) = at + 1 < e.length && e[at + 1] == ch

    private fun handleNewline(e: Editable, at: Int) {
        val str = e.toString()
        val prevNewline = str.lastIndexOf('\n', (at - 1).coerceAtLeast(0))
        val lineStart = if (prevNewline < 0) 0 else prevNewline + 1
        val prevLine = if (at > lineStart) str.substring(lineStart, at) else ""
        val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
        val trimmed = prevLine.trim()
        val extra = if (trimmed.endsWith(':') || trimmed.endsWith('{') || trimmed.endsWith('(')) " ".repeat(tabSize) else ""
        e.insert(at + 1, indent + extra)
        setSelection(at + 1 + indent.length + extra.length)
    }

    /** Not yet wired to a toolbar button — architecture is ready for a future "insert tab" action. */
    fun insertIndent() { val s = selectionStart.coerceAtLeast(0); text?.insert(s, " ".repeat(tabSize)) }
}
