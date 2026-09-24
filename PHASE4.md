# Phase 4 — CodeArc 0.4.0

Continues Phase 3 unchanged in behavior; adds the execution architecture and the first real
offline runtime.

## Implemented

- **Execution architecture** (`execution/`): `ExecutionEngine` (interface), `ExecutionRequest`,
  `ExecutionResult` / `ExecutionStatus`, `LocalExecutionEngine`, `CloudExecutionEngine`
  (interface + a `notConfigured()` stub that reports `REQUIRES_INTERNET`), `RuntimeManager`,
  `LanguageDefinition` / `LanguageRegistry`, and `ExecutionManager`, which is the only thing the
  UI talks to. None of this is coupled to `EditorActivity` beyond the one call it makes.
- **Automatic mode decision logic**, exactly as specified: offline runtime installed → run
  offline; else internet available → cloud path (currently the Phase-6 stub); else report the
  runtime isn't installed. `Offline` and `Online` modes skip the decision and fail honestly
  instead of silently falling back to the other path.
- **Python offline execution — real, not faked.** `PythonRuntime` runs actual CPython via the
  bundled [Chaquopy](https://chaquo.com/chaquopy/) interpreter (`com.chaquo.python:gradle`
  plugin, added to `build.gradle`/`app/build.gradle`/`settings.gradle`). It compiles and
  `exec`s the project's main file with a fresh globals dict, adds the file's own folder to
  `sys.path` so sibling `.py` files in the project can be imported (the "multiple files"
  requirement), and redirects `sys.stdout` / `sys.stderr` / `sys.stdin` to in-memory buffers so
  real stdout, stderr and exit code come back — `SystemExit(n)` maps to that exit code,
  uncaught exceptions produce a real `traceback.format_exc()` in stderr and exit code 1.
- **Run is wired up for real.** The Editor's Run button now: saves all dirty tabs, asks for
  optional stdin (a short dialog — CodeArc doesn't have a live interactive terminal in Phase 4,
  so stdin is supplied up front and consumed by `input()` calls in order), calls
  `ExecutionManager.run(project, projectDir, stdin)`, and shows real stdout / stderr / exit code
  / execution time in the Output tab, matching the plan's example format.
- **Language Pack Manager** (`LanguagesActivity`, reachable from Settings, Home's "Installed
  Languages" card, and the editor's overflow menu): Installed / Available / Updates sections, a
  detail dialog per language (version, offline/online/package-support, storage size), Install
  and Remove actions. Python shows as installed and non-removable (it's bundled in the app, not
  downloaded separately). Every other language's "Install" button opens an honest dialog
  explaining that no real runtime exists yet — it does not toggle a fake "installed" flag.
- Output example format from the plan (`Running main.py...` / stdout / `Process finished with
  exit code 0` / `Execution time: 0.08s`) is reproduced in the Output tab, not just in docs.

## Intentional scope and limits

- **Only Python is real.** JavaScript, C, C++, Java, Kotlin and Lua are fully defined in
  `LanguageRegistry` (extensions, syntax id, capabilities) so the editor, Languages screen and
  `ExecutionManager` already know how to describe and route them, but `RuntimeManager` reports
  them as not installed and `LocalExecutionEngine` returns `RUNTIME_NOT_INSTALLED` for them,
  exactly as the plan requires ("Do not fake working runtimes").
- **No real sandboxing.** Chaquopy runs CPython in-process inside CodeArc's own app — there is
  no separate OS process, so there's no process-level kill on timeout and no filesystem jail
  beyond the app's own storage permissions. `ExecutionManager` applies a **soft** timeout via
  `withTimeoutOrNull`: if it fires, the coroutine stops waiting and reports `TIMEOUT`, but the
  underlying Python call may keep running on its worker thread until it finishes on its own (an
  infinite loop won't actually be killed). A real "restricted files / process limits" sandbox
  would need a genuine subprocess or isolated runtime and is a larger change than Phase 4 scope.
- **Stdin is pre-supplied, not interactive.** The Terminal tab still isn't a live shell; it
  explains that Run sends the file through the same engine shown in Output. A truly interactive
  console (reading `input()` calls one at a time from a live keyboard) needs a different
  execution model (a paused/resumable interpreter or a pipe to a real process) and is deferred.
- **Cloud execution is a stub.** `CloudExecutionEngine.notConfigured()` always returns
  `REQUIRES_INTERNET` with an explanatory message; the real HTTPS backend arrives in Phase 6.
  `ACCESS_NETWORK_STATE` was added to the manifest now (to detect connectivity for the
  Automatic-mode decision); `INTERNET` is deferred to Phase 6 when there's an actual endpoint to
  call.
- **APK size.** Chaquopy bundles a real Python distribution per target ABI. `abiFilters` is set
  to `arm64-v8a`, `armeabi-v7a`, `x86_64` to keep this from ballooning to include every ABI;
  this is still a meaningfully bigger APK than Phase 3's.

## Validation actually performed

Same constraint as every previous phase: this sandbox has no Android SDK, no Gradle network
access to Google's or Maven Central's binary artifacts, and no device. **None of this has been
compiled, and Chaquopy specifically has never been exercised against a real device by this
process** — the API calls above (`Python.start`, `getModule`, `callAttr`, `PyException`) are
written against Chaquopy's public documentation, not verified by a compiler. Treat
`PythonRuntime.kt` as the highest-risk file in this phase and prioritize checking it first.

## Required device checks

1. `./gradlew assembleDebug` with JDK 17, SDK 34, NDK available, and full internet access (first
   build needs to download the Chaquopy Python distribution — this can be large, budget extra
   time and a stable connection).
2. Create a Python project, write `print("hi")`, tap Run with no stdin — confirm stdout shows
   `hi` and exit code 0.
3. Write code that reads `input()` twice; supply two lines in the stdin dialog; confirm both
   values are consumed in order.
4. Write code that imports a sibling file in the same `src/` folder; confirm the import
   succeeds.
5. Raise an uncaught exception (e.g. `1/0`); confirm stderr shows a real traceback and the run
   reports a non-zero exit code.
6. Call `sys.exit(3)`; confirm exit code 3 is reported.
7. Create a project in a language other than Python, tap Run — confirm the honest "runtime not
   installed" message, not a crash or a fake success.
8. Open Languages from Settings, Home, and the editor's overflow menu; open each language's
   detail dialog; confirm Python's "Remove" explains it can't be removed, and every other
   language's "Install" explains it isn't real yet.
9. Turn off Wi-Fi/data and confirm Automatic mode still runs Python offline correctly (it
   shouldn't depend on connectivity at all).
10. Write an intentionally infinite loop, tap Run, and confirm the UI itself recovers (shows
    "Execution timed out.") rather than freezing — per the known limitation above, the
    background Python call may keep burning CPU after that message appears; note this if it's a
    problem worth escalating rather than treating it as a Phase 4 bug.

STOP at Phase 4. Next phase (5) should add Learn/Practice/lessons wired to this same
`ExecutionManager` for runnable examples, exactly as the plan specifies ("Use the SAME
ExecutionManager from the IDE... Do not create a separate fake compiler for learning").
