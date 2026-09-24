# CodeArc — Phase 7 (0.7.0)

See [PHASE7.md](PHASE7.md) for current features, limitations and validation status
([PHASE6.md](PHASE6.md), [PHASE5.md](PHASE5.md), [PHASE4.md](PHASE4.md), [PHASE3.md](PHASE3.md)
and [PHASE2.md](PHASE2.md) for earlier phases).

Build: JDK 17 + Android SDK 34 + NDK, then `./gradlew testDebugUnitTest assembleDebug`.
No APK is included; this environment could not download Gradle or the Android/Chaquopy
dependencies (no network access to Google's or Maven Central's binary artifacts here).

## Troubleshooting a "style attribute '...' not found (aka com.codearc.app:attr/...)" build error

This means aapt2 couldn't find Material Components' own attributes (`colorPrimary`,
`colorSecondary`, `colorSurface`, etc.) when merging resources — it falls back to looking for
them as if they were your app's own attrs, hence the `com.codearc.app:attr/...` in the message.
`styles.xml` itself is correct (`Theme.CodeArc` extends `Theme.MaterialComponents.DayNight.NoActionBar`
and only sets standard Material attrs); this is a **dependency-resolution problem, not a source
bug**: the `com.google.android.material:material` (and/or `androidx.appcompat:appcompat`) AAR
didn't fully download/merge before the build ran.

On CodeAssist specifically:
1. Make sure the dependency sync/download shown in the build log (e.g. "Downloading
   lifecycle-livedata...") has **fully finished** before tapping Run/Build — starting a build
   mid-sync on a mobile connection is the most common cause of exactly this error.
2. If it still fails after a full sync, the local Gradle module cache likely has a corrupted or
   partial download. Clear CodeAssist's Gradle cache (or manually delete the
   `com.google.android.material` / `androidx.appcompat` folders under
   `.../codeassist/.gradle/caches/modules-2/`) and re-sync on a stable connection.
3. Do a Clean + Rebuild after the cache clear.
4. Confirm `compileSdk 34` has a matching platform installed in CodeAssist's SDK manager.

This is unrelated to the Phase 4 changes in this drop; it would show up on any phase's source
if the Material library never finished downloading.

## Troubleshooting an "error: Duplicate class ... — more than one library on the classpath defines it" build error

Fixed as of Phase 7 (see PHASE7.md, bug #1) by excluding the legacy `com.intellij:annotations`
artifact and forcing a single version of the modern `org.jetbrains:annotations` artifact in
`app/build.gradle`'s `configurations.all` block. If this resurfaces for a *different* class name
after adding a new dependency, the fix follows the same shape: identify which two artifacts both
ship that class (usually an old differently-named coordinate for the same library, or two
versions of one artifact resolved as separate jars) and add an `exclude`/`force` for that pair.

The following is the retained Phase 1 foundation record (historical):

# CodeArc — Phase 1 (0.1.0)

Learn. Code. Compile. Build.

## Scope
Kotlin + XML + Android Views + Material Components. No Compose. Dark palette from the brief; reusable XML card, button theme, Material dialogs and creation bottom sheet. Home has no greeting or user name. Four main destinations, emphasized center create action, short branded startup, four onboarding steps and no login. Onboarding completion is persisted with Preferences DataStore. Room database scaffolding includes app metadata only; real project metadata belongs to Phase 2.

Sample Home projects are explicitly labeled previews. No runtimes are installed or falsely advertised. Future actions explain the phase in which they become available. No compilation, project management, editor or real lessons are implemented.

## Build
Open this folder in Android Studio with JDK 17, Android SDK Platform 34 and Build Tools installed. Let Gradle sync, then run `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`). Gradle 8.7 and Android Gradle Plugin 8.5.2 are pinned. The first build needs internet for dependencies. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Groovy Gradle files are used; no version-catalog TOML is required. CodeAssist compatibility depends on its support for JDK 17, AGP 8.5.2 and SDK 34; this project has not been tested in CodeAssist.

## Validation status
XML parsing and local resource reference checks passed. Gradle build was attempted but failed before configuration: network unreachable when downloading the Gradle distribution. No Android SDK or emulator is installed in the authoring environment. Kotlin compilation, APK installation, visual layout, rotation and navigation are therefore NOT device-verified. No APK is included. Do not treat Phase 1 acceptance as fully passed until the build and device checks below succeed.

## Device acceptance checklist
- Clean install: brand → four onboarding steps → Start Coding → Home.
- Relaunch: onboarding stays hidden. Rotation on each onboarding step retains its index.
- Rotate during startup; ensure main content appears.
- Navigate Home / Projects / Learn / Settings; Back on a secondary screen returns Home.
- Center plus opens all five actions without switching destinations; dismiss returns to current page.
- Search finds all four screens; invalid input shows an inline error.
- Scroll Home on small screens and large font sizes; verify no overlap with system bars or navigation.
- Verify all cards/dialogs and screen-reader labels on device.
- Launch offline after install; all Phase 1 content remains available.
- Clear app data: onboarding appears again (backup intentionally disabled).

## Structure and continuation
`ui/MainActivity.kt`: startup and main destination routing.
`ui/OnboardingFragment.kt`: saved slide index and completion.
`ui/PageFragment.kt`: Home and three Phase 1 shells, composed from XML card/page resources.
`ui/CreationSheet.kt`: creation actions.
`data/Preferences.kt`: DataStore completion state.
`data/CodeArcDatabase.kt`: Room singleton and initial schema.

Continue Phase 2 in this project. Preserve palette, navigation and onboarding. Add project entities through an explicit Room migration; do not use destructive migration. Replace frontend samples with actual repository data. Later phases must not represent unavailable execution as working.
