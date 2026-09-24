# Phase 3 — CodeArc 0.3.0

Continues the Phase 2 source unchanged in behavior; adds the real editor workspace.

## Implemented
- EditorActivity: toolbar (drawer, back+autosave, Run stub, overflow), scrollable file tabs, a Problems/Output/Terminal bottom panel with expand/collapse, and a lazy-loading file-explorer drawer.
- CodeEditorView/CodeInput: monospace multi-line editor with a synced line-number gutter, undo/redo and cut/copy/paste/select-all/find via the platform EditText, auto-indent (matches previous line, adds one tab after `:`/`{`/`(`), auto-closing brackets and quotes, horizontal scroll when word wrap is off.
- SyntaxHighlighter: debounced (350ms) regex highlighting for Python, JavaScript, C, C++, Java, Kotlin and Lua — comments and strings are masked first so keywords/numbers inside them aren't recolored.
- Linter: a genuine (not faked) offline check for unmatched brackets and unterminated strings, surfaced in the Problems tab; tapping an entry jumps to that line.
- Editor Settings (font size, tab size, word wrap, line numbers, auto-indent, auto-brackets, auto-quotes, syntax highlighting) persisted in DataStore and applied live to every open tab.
- Find & Replace (Find Next with wraparound, Replace All).
- Autosave: 700ms after typing stops, on tab close, on switching away from the activity, and on back/close.
- ProjectActivity file taps and new-file creation now open the real editor instead of the old read-only preview; all Phase 2 file management (rename/move/duplicate/export/delete/import) is untouched.

## Intentional scope and limits
- No compiler or runtime. Run, Output and Terminal are placeholders pointing at Phase 4.
- The linter is bracket/string matching only — it does not understand comments, so brackets written inside a comment can produce a false "Unexpected" entry. Real diagnostics arrive with execution in Phase 4.
- Line numbers count logical lines; with word wrap on, a long wrapped line still counts as one, so the gutter can drift out of visual alignment on very long lines. Word wrap defaults to off.
- Editing is capped at 8 MB per file (vs. the 256 KB read-only preview cap from Phase 2).
- `insertIndent()` exists on the editor for a future toolbar "insert tab" action but isn't wired to a button yet — most keyboards don't send a Tab key, autoindent covers the common case.
- Autosave on process death isn't journaled (same honest limitation as Phase 2's filesystem/SQLite write path).

## Validation actually performed
This host has no Android SDK, Kotlin compiler, or Gradle network access (same constraint as Phase 2's build attempt). No `assembleDebug`, no Kotlin compilation, no JUnit run, no emulator, no on-device check. Everything above was reasoned through against the exact Phase 1/2 source (verified by reading every touched file) and against the documented Android APIs (TextWatcher, Editable spans, View.OnScrollChangeListener, DataStore, TabLayout custom tabs, DrawerLayout), but nothing here has been compiled or run.

## Required device checks
1. `./gradlew assembleDebug` with JDK 17, SDK 34 and internet access; fix any compile errors before relying on this.
2. Open a file from Home/Projects, edit it, background the app, reopen — confirm the edit persisted.
3. Open several files as tabs, switch between them, close one with unsaved changes, confirm it saved before closing.
4. Type in Python/JS/Java/Kotlin/C/C++/Lua files and check keyword/string/comment/number colors look right; toggle syntax highlighting off in Editor Settings and confirm it clears.
5. Type an unmatched `(` or an unterminated string; confirm it shows up in Problems and tapping it jumps to the right line.
6. Try Find Next (including wraparound) and Replace All.
7. Toggle every Editor Settings switch and confirm it applies to already-open tabs immediately.
8. Rotate the screen mid-edit; open the drawer and expand a few folders; try on a small screen.

STOP at Phase 3. Next phase (4) should add the ExecutionManager/LocalExecutionEngine architecture and wire Run to a real offline interpreter, starting with Python.
