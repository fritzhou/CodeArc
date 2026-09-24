# Phase 6 — CodeArc 0.6.0

Continues Phase 5 unchanged in behavior; adds the online compiler backend and the AI assistant
from the plan. Offline CodeArc — Python execution, the editor, Learn, Practice and progress —
keeps working exactly as before and without internet.

## Bug fixes in this phase

**1. Chaquo plugin repository (build-time, config phase).** The Phase 5 drop failed to build at
`:app:generateSourcesDebug` with every task above it green and everything below it `SKIPPED`.
`settings.gradle` declared `id 'com.chaquo.python'` (used since Phase 4 for the bundled Python
runtime) but never added Chaquo's own Maven repository (`https://chaquo.com/maven`) to either
`pluginManagement.repositories` or `dependencyResolutionManagement.repositories`. Chaquopy isn't
published to the Gradle Plugin Portal or Maven Central, so its plugin/runtime artifacts couldn't
resolve. Fixed by adding `maven { url 'https://chaquo.com/maven' }` to both repository blocks.

**2. Room/KSP version mismatch (`ByteArrayWrapper`).** After fix #1, the build log (Phase 6,
first drop) showed the real underlying failure at the same step:
`ksp: crashed for app: cannot find required type XTypeName[androidx.room.util.ByteArrayWrapper /
androidx.room.util.ByteArrayWrapper]`. This is a known Room/KSP mismatch: newer versions of the
`room-compiler` annotation processor generate code that references
`androidx.room.util.ByteArrayWrapper`, a class that only exists in `room-runtime`/`room-ktx` from
a matching later Room release — it does not exist in 2.6.1, which is what this project had
`room-runtime`/`room-ktx` pinned to while the processing environment resolved a newer compiler.
The project's `kotlin-kapt` + `kapt 'androidx.room:room-compiler:2.6.1'` declaration was also
already stale: Google's own Room setup docs now recommend KSP over kapt, and the build log
(`dev.ide.ksp.KspSourceGenerator`) shows this environment runs Room's processor via KSP
regardless of the kapt declaration, which is exactly the kind of declared/actual version drift
that produces this crash. Fixed by switching fully to KSP and pinning every Room artifact to the
same current stable version, 2.8.4, so the compiler and the runtime it generates code against are
never out of sync again:
- root `build.gradle`: replaced the `org.jetbrains.kotlin.kapt` plugin with
  `com.google.devtools.ksp` version `1.9.24-1.0.20` (matched to this project's Kotlin 1.9.24).
- `app/build.gradle`: applies `com.google.devtools.ksp` instead of the kapt plugin;
  `room-runtime`, `room-ktx` and `room-compiler` (now `ksp 'androidx.room:room-compiler:2.8.4'`
  instead of `kapt`) are all `2.8.4`.

**4. `PythonRuntime.kt`: "Unresolved reference 'chaquo'" (12 errors, `compileKotlinDebug`).**
After fixes #1–#2, `:app:generateSources OK (ksp)` — Room's KSP crash is gone — but
`compileKotlinDebug` then failed because `com.chaquo.python.Python`, `PyException` and
`AndroidPlatform` (imported by `PythonRuntime.kt`, unchanged since Phase 4) couldn't resolve at
all. Standard Gradle + AGP normally never hits this: applying the official `com.chaquo.python`
plugin automatically injects its own Java API artifact
(`com.chaquo.python.runtime:chaquopy_java`, matching the plugin version) onto the app's compile
classpath as part of the plugin's `afterEvaluate` configuration. The build log's task names
(`dev.ide.build.engine.*`, `dev.ide.ksp.*`) confirm this project is being built by CodeAssist's
own on-device build engine, not stock Gradle/AGP — and that engine evidently doesn't execute a
third-party plugin's own dependency-injection code, only the plain `dependencies {}` block it can
read declaratively. Fixed by adding
`implementation 'com.chaquo.python.runtime:chaquopy_java:15.0.1'` explicitly to
`app/build.gradle`, matching the already-pinned plugin version. This is redundant (and harmless)
on a real Gradle+AGP build where the plugin adds the same artifact itself, but required for
CodeAssist. **Caveat, stated honestly:** this fixes the *compile-time* error only. Chaquopy's
plugin also registers custom tasks that download and bundle the actual Python interpreter and
standard library into the APK; if CodeAssist's engine skips those the same way it skipped the
dependency injection, Python execution could still fail at runtime even though it now compiles.
That would need confirming on-device — it isn't visible from a compile-error screenshot.

**5. Generated `*Binding.java` classes: "androidx.viewbinding cannot be resolved to a type"
(20 errors across `ActivityCourseBinding`, `ActivityEditorBinding`, `ActivityLanguagesBinding`
and every other screen).** After fix #4, `compileKotlinDebug` got past PythonRuntime.kt, and the
build reached `ecj` (the Eclipse Compiler for Java that CodeAssist uses for the generated Java
ViewBinding sources — another sign this engine's toolchain differs from stock javac/AGP). Same
root cause, same shape as fix #4: `buildFeatures { viewBinding true }` normally makes AGP inject
`androidx.databinding:viewbinding` onto the compile classpath on its own, and CodeAssist doesn't
replicate that build-feature-driven injection any more than it replicates a plugin's own
`afterEvaluate` logic. `generateViewBindingDebug` had already generated the `*Binding.java` files
correctly (that task passed) — they just couldn't compile because the interface they all
`implements`, `androidx.viewbinding.ViewBinding`, wasn't on the classpath. Fixed by adding
`implementation 'androidx.databinding:viewbinding:8.5.2'` explicitly to `app/build.gradle`,
version-matched to this project's AGP (8.5.2) — those two are released in lockstep upstream.
Harmless on a real Gradle+AGP build for the same reason as fix #4.

**6. Missing `INTERNET` permission.** `AndroidManifest.xml` only declared `ACCESS_NETWORK_STATE`.
Added `android.permission.INTERNET` — required at runtime for this phase's cloud execution and
AI calls to reach the network at all (unrelated to the build failures above, but this phase
would fail silently without it).

**A pattern worth naming:** fixes #4 and #5 are the same underlying gap — CodeAssist's build
engine (`dev.ide.build.engine.*`) reads a module's declared `dependencies {}` block but does not
execute the extra classpath wiring that a Gradle *plugin* (Chaquo) or an AGP *build feature*
(`viewBinding true`) would normally add on its own at configuration time. Any future dependency
that relies on that kind of implicit injection — rather than an explicit `implementation` line —
is a candidate for the same failure on this build engine specifically.

## Implemented

- **Real `CloudExecutionEngine`** (`execution/CloudExecutionEngine.kt`): replaces Phase 4's
  `notConfigured()` stub with a genuine HTTPS JSON client matching the plan's request/response
  shape (language, main file, project files, stdin → stdout, stderr, exit code, execution time,
  compiler errors, warnings). CodeArc ships no backend of its own; the endpoint is whatever the
  person sets in Settings > Execution. Until it's set, `execute()` returns a new
  `ExecutionStatus.CLOUD_NOT_CONFIGURED` result instead of silently doing nothing or faking
  output. `ExecutionManager`'s Automatic-mode decision logic is unchanged: offline runtime
  installed → run offline; else internet available → cloud; else report the runtime isn't
  installed. `Online` mode still fails honestly (`REQUIRES_INTERNET`) with no internet.
- **`net/NetworkStatus.kt`**: the connectivity check factored out of `ExecutionManager` so the
  new AI assistant uses the exact same "is there internet" logic instead of a second copy.
- **AI Assistant** (`ai/` — `AiAction`, `AiRequest`, `AiResult`/`AiStatus`, `AiClient` +
  `CloudAiClient`, `AiManager`): all eight actions from the plan — Explain Code, Explain Error,
  Fix Code, Generate Code, Refactor, Simplify, Add Comments, Ask AI. `AiManager` is
  `ExecutionManager`'s sibling: it checks internet, then whether a cloud endpoint is configured,
  and only then calls out — `REQUIRES_INTERNET` / `NOT_CONFIGURED` are real, distinct states, not
  a generic error. `CloudAiClient` sends only the action, language, the current selection (or
  whole file if nothing is selected) and, for Explain Error, the one relevant problem message —
  never the whole project, per the plan.
- **Editor AI** (`ui/EditorActivity.kt`): a new "✦ AI" toolbar button opens an action sheet
  scoped to the current selection when there is one. Each Problems-panel row now also has an
  inline "Explain with AI" action that sends just that problem's line/message as context. Results
  show in a dialog with the explanation and, when the AI returned one, a suggested-code block —
  **Apply only writes it into the file after the person taps Apply**; Copy and Dismiss never
  touch the file. Generate Code and Ask AI prompt for the free-text description/question the plan
  calls for.
- **Internet state, consistently**: both cloud Run and every AI action show a plain "Requires
  Internet" (no connection) or "isn't configured yet" (connected, but no endpoint set) message —
  never a spinner that hangs or a silent no-op — and offline functionality (Python Run, Learn,
  Practice) is completely unaffected either way.
- **Settings > Execution / AI** (`ui/PageFragment.kt`): both cards now open one real "Cloud & AI"
  dialog (endpoint URL + optional API key, persisted via the existing `Preferences` DataStore as
  a new `CloudSettings`) instead of Phase 5's static "Phase 6" placeholder text. Cloud Run and the
  AI assistant intentionally share one endpoint/key pair — CodeArc doesn't assume they're
  different services, but nothing stops a person from pointing both at the same backend that
  routes internally.

## Explicitly not done

- No AI model or execution sandbox ships with the app. Both features are HTTPS clients only —
  there is nothing to run offline, and nothing was faked to look otherwise.
- No streaming responses; a request blocks until the backend replies or times out.
- No AI conversation history is persisted (matches "no login required" — nothing to sync yet;
  Phase 7 lists optional AI history under Account).
- JavaScript/C/C++/Java/Kotlin/Lua still have no runtime, offline or cloud-routed — Automatic
  mode for them now has a real cloud path to fall into once a backend is configured, but CodeArc
  still reports `RUNTIME_NOT_INSTALLED` up front in the editor for languages with no runtime at
  all, exactly as Phase 4 did.

## Deliverable check (against the Phase 6 plan)

- Offline execution: unchanged, still real (Python via Chaquopy). ✅
- Online execution: real HTTPS client wired end-to-end; honestly reports not-configured/no
  backend rather than assumed working, since none is bundled. ✅
- Automatic execution selection: unchanged decision order, now resolving to a real cloud engine
  instead of a stub. ✅
- AI coding help: all eight actions implemented, editor-integrated, non-intrusive, Apply-gated. ✅
- Graceful offline behavior: verified for both cloud Run and every AI action. ✅

## Build

Unchanged from Phase 1: JDK 17, Android SDK Platform 34, Android Gradle Plugin 8.5.2, Gradle 8.7.
`./gradlew testDebugUnitTest assembleDebug`. No APK is included — this authoring environment has
no network access to Google's, Maven Central's or Chaquo's binary artifacts, so the fixes above
are source-verified (repository declarations, matched dependency versions, and the documented
cause of each error) but not build-verified here; `tools/validate_source.py` (XML/resource/
migration checks only) still passes. This drop was iterated against real on-device CodeAssist
build logs (not just reasoned about) and each fix targets the specific next failure that log
showed — but confirm a full green build, and an actual Run of a Python project, on-device before
trusting it completely; CodeAssist's build engine has already shown it doesn't behave identically
to stock Gradle/AGP in several ways (see fixes #2, #4 and #5).

Continue Phase 7 in this project. Preserve the offline/online/AI split; do not make either cloud
feature mandatory for basic app use.
