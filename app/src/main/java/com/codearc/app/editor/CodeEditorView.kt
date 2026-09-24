package com.codearc.app.editor

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.codearc.app.data.EditorSettings

/** Line-number gutter + CodeInput side by side. The gutter is not itself scrollable — its
 *  drawn content is panned in lockstep with the EditText's own internal scroll via
 *  setOnScrollChangeListener, which is how it stays in sync without a shared ScrollView.
 *  Tapping the gutter toggles a breakpoint marker (Phase 7 prepared debugger scaffolding —
 *  visual only, doesn't affect execution). */
class CodeEditorView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    val input = CodeInput(context)
    private val gutter = TextView(context)
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var language = "Python"
    private var fileName = ""
    private var syntaxOn = true
    var onDirty: (() -> Unit)? = null
    var onProblems: ((List<Problem>) -> Unit)? = null
    // Phase 7: prepared (non-functional) debugger scaffolding — see EditorActivity.showDebugInfo
    // and PHASE7.md. In-memory only, per CodeEditorView instance (i.e. per open tab).
    private val breakpoints = mutableSetOf<Int>()
    var onBreakpointsChanged: ((Set<Int>) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gutter.typeface = Typeface.MONOSPACE
        gutter.setTextColor(0xFFA5B0C2.toInt())
        gutter.gravity = Gravity.END or Gravity.TOP
        gutter.setPadding(dp(8), dp(12), dp(8), dp(120))
        gutter.setSingleLine(false)
        gutter.isClickable = true
        gutter.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val layout = gutter.layout
                if (layout != null) {
                    val y = (event.y + gutter.scrollY).toInt().coerceAtLeast(0)
                    val line = (layout.getLineForVertical(y) + 1).coerceAtMost(input.text?.count { it == '\n' }?.plus(1) ?: 1)
                    toggleBreakpoint(line)
                }
            }
            true
        }
        addView(gutter, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        input.typeface = Typeface.MONOSPACE
        input.setTextColor(0xFFF5F7FA.toInt())
        input.setBackgroundColor(0)
        input.setPadding(dp(4), dp(12), dp(24), dp(120))
        input.gravity = Gravity.TOP or Gravity.START
        addView(input, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        input.setOnScrollChangeListener { _, _, scrollY, _, _ -> gutter.scrollTo(0, scrollY) }
        input.onChanged = { e -> updateGutter(e); onDirty?.invoke(); scheduleAnalysis() }
    }

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    fun open(file: String, language: String, content: String) {
        fileName = file; this.language = language
        input.onChanged = null
        input.setText(content)
        input.onChanged = { e -> updateGutter(e); onDirty?.invoke(); scheduleAnalysis() }
        updateGutter(input.text!!)
        analyze()
    }
    fun content(): String = input.text?.toString() ?: ""
    fun applySettings(s: EditorSettings) {
        input.textSize = s.fontSize.toFloat(); gutter.textSize = s.fontSize.toFloat()
        input.tabSize = s.tabSize; input.autoIndentEnabled = s.autoIndent; input.autoBracketsEnabled = s.autoBrackets; input.autoQuotesEnabled = s.autoQuotes
        input.setHorizontallyScrolling(!s.wordWrap)
        gutter.visibility = if (s.lineNumbers) VISIBLE else GONE
        syntaxOn = s.syntaxHighlighting
        input.text?.let { if (syntaxOn) SyntaxHighlighter.highlight(it, language) }
        input.requestLayout()
    }
    fun insertIndent() = input.insertIndent()
    /** Line numbers here are logical lines. With word wrap on, a long wrapped line still counts
     *  as one line, so the gutter can drift out of visual alignment — a known Phase 3 limit. */
    fun goTo(line: Int) {
        val str = content(); var idx = 0; var l = 1
        while (l < line && idx < str.length) { val next = str.indexOf('\n', idx); if (next < 0) { idx = str.length; break }; idx = next + 1; l++ }
        input.requestFocus(); input.setSelection(idx.coerceIn(0, str.length))
    }
    private fun updateGutter(e: Editable) {
        val lines = e.count { it == '\n' } + 1
        val sb = SpannableStringBuilder()
        for (i in 1..lines) {
            val start = sb.length
            sb.append(if (i in breakpoints) "●" else " ").append(i.toString())
            if (i in breakpoints) sb.setSpan(ForegroundColorSpan(0xFFFF5C69.toInt()), start, start + 1, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (i != lines) sb.append("\n")
        }
        gutter.text = sb
    }
    /** Toggles a breakpoint marker on [line] (1-based, logical line — same caveat as goTo()
     *  about word wrap). Never affects execution; see EditorActivity.showDebugInfo. */
    fun toggleBreakpoint(line: Int) {
        if (!breakpoints.remove(line)) breakpoints.add(line)
        input.text?.let { updateGutter(it) }
        onBreakpointsChanged?.invoke(breakpoints.toSet())
    }
    fun breakpointLines(): Set<Int> = breakpoints.toSet()
    private fun scheduleAnalysis() {
        pending?.let { handler.removeCallbacks(it) }
        val r = Runnable { analyze() }; pending = r; handler.postDelayed(r, 350)
    }
    fun analyze() {
        val e = input.text ?: return
        if (syntaxOn) SyntaxHighlighter.highlight(e, language)
        onProblems?.invoke(Linter.lint(fileName, e.toString()))
    }
}
