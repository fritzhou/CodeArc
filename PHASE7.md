# Phase 7 — CodeArc 0.7.0

Continues Phase 6 unchanged in behavior; adds the "advanced features, optimization and release
polish" items from the plan. Every existing offline capability (editor, Python execution, Learn,
progress) keeps working exactly as before and without internet.

## Bug fixes in this phase

**1. `error: Duplicate class 'org.intellij.lang.annotations.Identifier'` (D8, build failed).**
Reported from a device build (screenshot): 1 error, 3 warnings, `Failed`. The error means two
different jars on the classpath both define the same class. Two build.gradle-level fixes
(`configurations.all { exclude ...; resolutionStrategy { force ... } }`, then per-dependency
`exclude` closures on the likeliest candidates) were tried first and **both did nothing** —
confirmed by rebuilding between each. Root cause, found only by reading CodeAssist's own
Dependency Manager (app module → ⋯ → Dependency Manager → Dependencies tab), not the build log:
CodeAssist does not run a real Gradle dependency resolver against `build.gradle` at all — it
reads the `group:artifact:version` coordinates off each `implementation` line and resolves the
actual tree itself into `module.toml`, silently ignoring Groovy `exclude` closures and
`configurations.all` blocks. That's why fixes aimed at the file did nothing. The Dependency
Manager's tree view showed the real duplicate: `chaquopy_java:15.0.1` transitively pulls
`org.jetbrains:annotations-java5:15.0` — a *different* Maven artifact from
`org.jetbrains:annotations` (which CodeAssist's resolver did correctly dedupe on its own,
13.0/23.0.0 → 23.0.0) that happens to ship the same `org.intellij.lang.annotations.*` classes,
`Identifier` included. Fixed via CodeAssist's own exclude action on that transitive entry in
Dependency Manager, not in `app/build.gradle` — the file's `dependencies {}` block is back to
plain `implementation` lines with no exclude closures, since those have no effect here. If this
recurs, check Dependency Manager's tree directly rather than guessing from the build log; a
weaker second candidate spotted in the same tree, `com.intellij:annotations:12.0` under
`room-compiler`, is KSP/compiler-only ("C" badge) and likely isn't on the actual dex classpath.

**2–4. Three warnings, same screenshot.** None of these failed the build, but all three were
real: `EditorActivity.onSaveInstanceState`/`ProjectActivity.onSaveInstanceState` named their
`Bundle` parameter `out` instead of `outState`, which the Kotlin compiler flags because it
breaks calling the override with a named argument (`super.onSaveInstanceState(outState = ...)`
would not resolve) — renamed both. `MainActivity.kt` called the deprecated
`Lifecycle.whenResumed`, which the warning correctly explains runs on a *pausing* dispatcher
(the block can suspend indefinitely through pause/resume cycles) rather than being cancelled —
replaced with `Lifecycle.withResumed`, which is the direct non-deprecated replacement for
one-shot, non-suspending work gated on the Resumed state (exactly what that call site does).

**5. `runProject()` never tried cloud execution for non-Python projects (found while working on
this phase, not from a screenshot).** Since Phase 4, `EditorActivity.runProject()` checked
`RuntimeManager.isInstalled(lang.id)` *before* calling `ExecutionManager.run()` and showed
"Runtime not installed" immediately if that was false — which, for every language except Python,
is always false. Phase 6 added real cloud execution and taught `ExecutionManager.run()` the
correct offline/cloud/neither decision (it already checked `RuntimeManager` itself, plus
connectivity and whether a cloud endpoint is configured), but the editor's own early check
upstream of that call meant cloud execution could never actually be reached for a non-Python
project, even with a cloud endpoint configured in Settings and a live connection. Fixed by
deleting the early check and letting `ExecutionManager.run()` make the decision, same as it
already does correctly for Learn's `runSnippet()` path.

**6. Offline Python execution: `FileNotFoundException: chaquopy/build.json` on Run (confirmed
on-device, not just reasoned about).** PHASE6.md's bug #4 flagged this as a risk it couldn't
confirm from a compile-error screenshot alone ("Chaquopy's plugin also registers custom tasks
that download and bundle the actual Python interpreter and standard library into the APK; if
CodeAssist's engine skips those... Python execution could still fail at runtime"). It does.
`build.json`, the native interpreter, and the bundled stdlib are all generated/packaged by the
Chaquo Gradle plugin's own tasks at build time — CodeAssist doesn't run them, same root cause as
bug #4/#5 in Phase 6, just one layer deeper (past compiling, into what actually ships in the
APK). There is no build.gradle fix for this: the missing pieces are Chaquopy's proprietary
per-ABI native binaries and a full Python stdlib archive, fetched from `chaquo.com/maven` by
tasks this build engine skips entirely. **Offline Python execution cannot work under CodeAssist
as this project is structured** — it needs a real Gradle+AGP build (Android Studio, CI) where
the Chaquo plugin's tasks actually run. Until/unless this project is built that way, `Run` on a
Python project should route to Cloud Run (Settings > Execution) once an endpoint is configured.
Fixed `PythonRuntime.kt` to match: `Python.start()` failure is now caught and reported as a
clear one-line message (matching `CloudExecutionEngine`'s `CLOUD_NOT_CONFIGURED` honesty
pattern) instead of surfacing Chaquopy's raw stack trace to the person as an error dialog.

## Implemented

- **Language support expanded**: Go, Rust, PHP, Ruby and C# added to `LanguageRegistry`,
  bringing the total to twelve. Same honesty rule as every language before them — defined so
  Languages/Learn/the editor can name and describe them, but `RuntimeManager` still reports them
  as not installed. Nothing here is "labeled as working" that isn't.
- **Git — real, local, via JGit** (`git/GitManager.kt`, a pure-Java git implementation, so no
  native git binary is needed on a CodeAssist device): `Initialize Repository`, `Commit` (stages
  everything, including deletions, then commits with a message), and `View Changes` (real
  `git status`, split into added/modified/removed/untracked/missing) plus commit history — all
  in a new `GitActivity` ("Source Control"), reachable from a project's file-explorer screen and
  from the editor's overflow menu. Branches and authenticated push/pull are left out, per the
  plan ("Branches later", GitHub push/pull "optional" and not required to use CodeArc).
- **Clone Repository — wired up for real.** The creation sheet's "Clone Repository" option (a
  stub since Phase 1) now prompts for an HTTPS URL and performs a real `Git.cloneRepository()`
  call. `ProjectRepository` gained `allocate()`/`adopt()`/`discard()` so a project folder can be
  reserved before the network call and either registered as a real project (once a recognizable
  main file — `main.py`, `Main.java`, etc., found by a bounded scan of the cloned tree) or
  cleanly discarded on any failure, mirroring the rollback shape `create()`/`duplicate()` already
  used. Deep or unconventional repo layouts that the scan misses can still be imported — just set
  the main file afterward via the existing Project Configuration screen.
- **Package Management — explained honestly, not faked.** The plan asks for a screen showing
  "download size, internet requirement, status, errors" for language packages. Chaquopy (Python's
  bundled runtime) only supports declaring pip packages in `app/build.gradle`'s `python { pip {
  } }` block at *build* time — there is no supported runtime installer to build a real version of
  that screen around. `LanguagesActivity` now has a Package Management section that says this
  plainly instead of drawing a plausible-looking installer UI with nothing behind it.
- **Storage Management — real, computed sizes** (`StorageActivity`, reachable from Settings):
  Projects, Recently Deleted (the trash folder `ProjectRepository.delete()` already used),
  Language Packs (Chaquopy's actual extracted-interpreter directory), Learning Content (always
  0 — lessons are compiled into the app, not stored as files, and the screen says so), and
  Compiler Cache & Temporary Files (the app's cache directory, which is also where Learn's
  `runSnippet()` scratch files live). "Clear Cache & Temporary Files" only ever touches
  `cacheDir` — Projects and the trash are shown but have no clear action here, per the plan's
  "Never accidentally delete projects during cleanup."
- **Autocomplete — real keyword/identifier completion, architecture prepared for LSP**
  (`editor/Autocomplete.kt`). `CompletionProvider` is the swap point: `KeywordCompletionProvider`
  (language keyword lists plus identifiers already typed in the current file) is the real,
  working implementation today; a future LSP-backed implementation can satisfy the same
  interface without `CodeEditorView` or `EditorActivity` changing. Wired into the editor as a
  suggestion strip above the code area, shown once a 2+ character word prefix is being typed;
  tapping a suggestion inserts it.
- **Debugger — prepared scaffolding, explicitly not a working debugger.** Tapping a line number
  in the editor's gutter toggles a real breakpoint marker (visible as a red dot), backed by an
  actual `Set<Int>` in `CodeEditorView` — but it's in-memory per open tab only, and doesn't
  affect execution at all. A new "Debugger" entry in the editor's overflow menu says this
  outright: stepping, variables and a call stack all need a real debugger hook into a runtime,
  and none of CodeArc's runtimes expose one (Python runs in-process via Chaquopy, which doesn't
  wire up `pdb` here). This is the plan's "prepare... only enable for languages with real
  debugger support" read literally — since no language qualifies yet, nothing is enabled.
- **Final Settings structure** reordered to match the plan exactly: Editor, Execution, Languages,
  Learning, AI, Storage, Appearance, Account, About. Storage now links to the real
  `StorageActivity` instead of a static dialog.
- A few smaller honesty/accuracy fixes made while touching these files: `ProjectActivity`'s
  configuration screen and class doc comment, and the creation sheet's stale "arrives in Phase
  X" copy for options that had already shipped, no longer reference phases that are done.

## Intentional scope and limits

- **Git has no partial staging, branches, or authenticated push/pull.** Commit always stages the
  entire working tree. Clone supports public HTTPS repositories (JGit's built-in HTTP transport,
  no extra dependency needed); a credentials parameter exists in `GitManager.clone()` for a
  future "private repo" UI but nothing in this phase's UI collects a token yet.
- **Debugging is scaffolding, not a debugger**, as described above — this is a deliberate reading
  of "prepare... only enable for languages with real debugger support," not an oversight.
- **Autocomplete has no real language semantics** — it's prefix-matched keywords and identifiers,
  not type-aware or scope-aware completion. That's what "prepared for LSP, not LSP itself" means
  in practice.
- **UI audit / performance / reliability testing** from the plan are validation activities, not
  code changes, and this sandbox still can't compile, install or run CodeArc on a device (no
  Android SDK, no emulator, and the network here can't reach Google's or Maven Central's binary
  artifacts, or JGit's, either). The device checklist below is what actually needs running.

## Validation actually performed

Same constraint as every previous phase: nothing in this drop has been compiled or run. The two
highest-risk additions this phase are new external dependencies exercised for the first time —
**JGit** (`GitManager.kt` — init/status/commit/log/clone, all written against JGit's public
`org.eclipse.jgit.api.Git` surface, not verified by a compiler) and the `configurations.all`
block fixing bug #1 above (the exact exclude/force syntax is standard Gradle, but its effect on
*this* dependency graph, on *this* build system, is unverified the same way PHASE6.md flagged
Chaquopy's runtime behavor as unverified).

## Required device checks

1. Rebuild after pulling this drop and confirm the duplicate-class error and all three warnings
   from the screenshots are gone.
2. Git: on a project with no `.git` folder, open Source Control, tap Initialize Repository,
   confirm a `.git` folder appears and the screen shows a clean working tree with no history.
3. Edit a file, save, reopen Source Control — confirm it shows up as Modified or Untracked as
   appropriate, commit it, confirm it appears in History with the right message/time and the
   working tree shows clean again.
4. Clone Repository with a small public HTTPS repo URL — confirm a new project appears with a
   sensible main file and language, and that its files match the repository. Try a repo whose
   entry point the scan won't find and confirm the honest error message, then confirm Project
   Configuration can still be used to set a main file by hand afterward.
5. Storage: confirm the reported sizes look plausible against what's actually on disk (e.g. via
   a file manager), tap Clear Cache & Temporary Files, confirm projects and progress are
   untouched.
6. Autocomplete: type a 2+ character prefix matching a keyword or an identifier already in the
   file, confirm the strip appears above the editor and tapping a suggestion inserts it correctly
   at the cursor.
7. Debugger: tap several line numbers in the gutter, confirm red dot markers toggle on/off;
   switch tabs and back, confirm markers persist for that tab; close and reopen the file, confirm
   they're gone (documented in-memory-only limitation); open the Debugger menu entry and confirm
   the breakpoint count matches and the message reads sensibly.
8. Run a Python project end to end again (Phase 4/5/6 regression) to confirm bug fix #5 above
   didn't change Python's own offline behavior.
9. With a cloud endpoint configured in Settings > Execution and a non-Python project, tap Run —
   confirm it now actually attempts cloud execution (per bug fix #5) instead of immediately
   showing "Runtime not installed" regardless of connectivity/configuration.
10. Confirm rotation on `GitActivity`, `StorageActivity` and the editor's new autocomplete strip
    doesn't crash or lose state in ways worse than the rest of the app already tolerates.

STOP at Phase 7. Per the plan, this is the final phase — CodeArc should now read as a real (if
still honestly-scoped) Android IDE, offline compiler, learning platform and optional AI
assistant, not a code playground. Anything still marked "not yet implemented" throughout this
codebase (JavaScript/C/C++/Java/Kotlin/Lua/Go/Rust/PHP/Ruby/C# execution, LSP, real stepping
debugging, branches, authenticated Git) is exactly that — named, scoped, and never faked.
