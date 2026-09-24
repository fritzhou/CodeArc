# Phase 2 — CodeArc 0.2.0

Continues the original Phase 1 source. Kotlin, XML, Android Views and Material Components remain unchanged as the UI stack. No Compose, compiler, editor, account or network execution was added.

## Implemented
- Real app-private project directories, src/, assets/, main source and atomic project.json writes.
- Room Project entity/DAO, observable lists and explicit non-destructive database migration 1 → 2.
- New Project: name, seven languages, five templates, Automatic/Offline/Online execution configuration and an explicit app-private location summary.
- Search, language filter, recent/name/oldest sorting, favorites, language monograms and modification timestamps.
- Open, rename, duplicate, export and confirmed delete for projects.
- File explorer: create file/folder, open folders, read-only text preview, rename, move, duplicate, import file, export file/folder, confirmed deletion.
- Export/import CodeArc project ZIPs using Android's document picker. Imports need project.json at the ZIP root. No broad storage permissions.
- Main-file, execution-mode and runtime-preference configuration. Moving/renaming a main file or its containing folder updates its configuration.
- Home now shows actual recent projects instead of Phase 1 mock data.

## Intentional scope and limits
- Project location is app-private storage, not an arbitrary external directory. Export copies to your chosen document provider. Uninstall/clear app data removes local data; keep ZIP backups.
- Read-only text previews are capped at 256 KB. Editing arrives in Phase 3.
- JavaScript templates target a future Node-compatible runtime. No language is executable yet.
- ZIP import limits: 50 MB expanded content and 5,000 entries. Individual file imports are capped at 50 MB. Paths outside project storage and overwrites are rejected.
- Delete moves files/projects to private trash. Trash is excluded from ZIP exports. There is no trash restore/cleanup screen yet; retained copies still consume local storage.
- Filesystem and SQLite are separate persistence systems. Ordinary failures are handled and file operations serialized, but abrupt OS/process termination during a mutation is not comprehensively journaled or tested. Export backups before relying on this as primary storage.

## Validation actually performed
`python3 tools/validate_source.py` passed XML/resource checks and executed the migration SQL against SQLite, preserving a Phase 1 metadata record and inserting a project row. This is not Room's generated migration validation.

`./gradlew assembleDebug --no-daemon` was attempted but failed while downloading Gradle: network unreachable. This host has no Android SDK or Kotlin compiler. No APK, Kotlin compilation, JUnit execution, emulator run, visual verification or device acceptance claim is made.

JUnit tests are included for path traversal, malicious ZIP paths, overwrite protection, recursive copy protection, ZIP round trips/trash exclusion and template coverage. Run `./gradlew testDebugUnitTest assembleDebug` with JDK 17, SDK 34 and internet access.

## Required device checks
1. Install Phase 1, finish onboarding, upgrade to 0.2.0 using the same signing key; verify onboarding stays completed and migration succeeds.
2. Create projects across all seven languages and five templates. Verify actual source files in previews.
3. Force-stop and reopen; verify projects, names, favorites and execution settings persist.
4. Create nested folders/files, rename and move them, including the main-file folder; verify configuration follows the move.
5. Try duplicate names, outside-root paths, deleting the main file/folder, and copying a folder into itself. Check that existing files remain intact.
6. Export a project ZIP, import it, and compare file contents. Cancel document pickers and try malformed/oversized ZIPs.
7. Confirm deletion, relaunch and verify removed entries remain absent from active lists.
8. Rotate New Project and the explorer, use Back and every bottom destination, and check small screens/large fonts.

Stop at Phase 2. Next phase should extend this repository with the real editor and preserve local storage.
