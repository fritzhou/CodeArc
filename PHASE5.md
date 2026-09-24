# Phase 5 — CodeArc 0.5.0

Continues Phase 4 unchanged in behavior; adds the Learn system: lessons, practice, quizzes,
guided projects and persisted progress, exactly wired to the same `ExecutionManager` the editor
uses.

## Implemented

- **Learn content model** (`learn/LearnContent.kt`): plain, offline data — `Lesson`,
  `QuizQuestion`, `PracticeChallenge`, `GuidedProject`, `LanguageCourse`, `CourseRegistry`.
  Nothing here is fetched; it's all bundled in the app.
- **Python Beginner — real and complete.** All 12 lessons from the plan (Introduction to Python
  through File Handling), each with an objective, explanation, a runnable code example, notes,
  a common mistake, a practice challenge (starter code, hint, solution) and a quiz question. Quiz
  types are mixed across the course (multiple choice, true/false, predict-output,
  identify-error), per the plan.
- **Interactive Examples and Practice run for real.** `ExecutionManager.runSnippet()` is a new
  method on the *same* `ExecutionManager` class the IDE's Run button calls (Phase 4) — it writes
  the snippet to a private scratch folder under the app's cache dir and executes it through the
  same `LocalExecutionEngine` / Chaquopy path. No separate fake compiler was written for
  learning, per the plan's explicit instruction.
- **Course screen** (`ui/CourseActivity.kt`): Learn / Practice / Projects / Reference tabs, with
  a Beginner / Intermediate / Advanced switch inside Learn. Lesson rows show duration or a ✓ once
  completed.
- **Lesson screen** (`ui/LessonActivity.kt`): objective, explanation, an editable + runnable code
  example, notes, common mistakes, an inline practice challenge (Run Code / Reset / Hint /
  Solution — solution never shown automatically), a quiz with Submit, Previous/Next, and a
  bookmark toggle. Answering a quiz correctly marks the lesson complete.
- **Standalone Practice screen** (`ui/PracticeActivity.kt`): reached from a Course's Practice
  tab for jumping straight into an exercise without reading the lesson first.
- **Guided Projects**: Python Beginner's five projects (Calculator, Number Guessing Game, Quiz,
  Grade Calculator, To-Do CLI) each open Phase 2's real New Project flow, pre-filled with a
  suggested name, language and the closest matching template — building the project itself uses
  the existing, unmodified project system.
- **Progress persists.** New Room tables (`lesson_progress`, `quiz_results`,
  `practice_progress`), added via an explicit `MIGRATION_2_3` (version 2 → 3, non-destructive,
  matching the precedent set by `MIGRATION_1_2` in Phase 2). Completed lessons, bookmarks and
  quiz attempts all survive app restarts.
- **Home and Learn now show real "Continue Learning"** instead of Phase 4's static placeholder
  card — it reflects the most recently touched lesson, or invites the person to start if there
  isn't one yet.

## Intentional scope and limits

- **Only Python Beginner has full lesson content.** This mirrors Phase 4's own scoping principle
  (“only Python is real”): Learn never claims a language works when `RuntimeManager` says its
  runtime isn't installed, and it never claims a lesson is finished when it isn't.
- **Python Intermediate ships as a real, titled syllabus with locked lessons**, not full content.
  The 12 titles from the plan (Advanced Functions through Intermediate Project) are shown in the
  Course screen so the shape of the path is honest and visible, each opening a short "coming in
  a later update" notice instead of silently doing nothing.
- **Advanced is "Coming soon" for every language**, exactly as the plan allows.
- **JavaScript, C, C++, Java, Kotlin and Lua have no lesson content.** Their Course screens
  explain plainly that they don't have a real offline runtime yet and link to Languages — no
  invented lessons that couldn't actually run, consistent with `LanguageRegistry`'s existing
  "Not yet implemented" runtime descriptions from Phase 4.
- **Guided Projects intermediate list** (Expense Tracker, Contact Manager, JSON Notes, API Data
  Viewer, File Organizer) is titled but not yet buildable — a tap explains it's coming later
  rather than opening a half-built flow.
- **Practice challenges reuse each lesson's one practice exercise** rather than a separate,
  larger practice bank; the Course "Practice" tab is a shortcut into the same content the lesson
  already has, not new material.
- **Reference tab** is a compact, static Python cheatsheet (print/input/types/conditionals/
  loops/functions/list/dict/file), not a full API reference. Only Python has one.
- **The scratch folder used by `runSnippet()`** (`cacheDir/learn_scratch/<language>/`) is wiped
  and rewritten on every run, and is separate from any real project directory — lesson code
  can't accidentally read or write a person's actual projects.

## Validation actually performed

Same constraint as every previous phase: this sandbox has no Android SDK, no Gradle network
access, and no device — **none of this has been compiled**. New/changed Kotlin files were
manually checked for balanced braces/parentheses and for consistency with Phase 1–4 APIs
(`ExecutionRequest`, `LanguageRegistry`, `Templates`, `ProjectRepository`, the `item_card`/
`fragment_page` view-building helpers) but not run through a compiler. Treat `LessonActivity.kt`
and the `ExecutionManager.runSnippet()` addition as the highest-risk files this phase — the
render-rebuild pattern used for Hint/Solution/Bookmark toggling was specifically checked for
state loss across re-renders (moved to instance fields rather than locals inside `render()`),
but a compiler and a device are the only real confirmation.

## Required device checks

1. `./gradlew testDebugUnitTest assembleDebug` with JDK 17, SDK 34, and the Chaquopy
   dependencies available (same requirement as Phase 4).
2. Fresh install (or upgrade from a Phase 4 build with the same signing key): confirm the
   `MIGRATION_2_3` runs without wiping existing projects, favorites or execution settings.
3. Open Learn → Python → Beginner. Open "Introduction to Python", tap ▶ Run Example — confirm
   real stdout, matching the IDE's own output format.
4. Edit the code example's text before tapping Run — confirm your edit actually runs, not the
   original.
5. Work through a lesson's Practice Challenge: Run Code, Reset, Hint, Solution — confirm
   Solution is never visible until tapped, and stays visible after Reset only if you re-tap it.
6. Answer a quiz incorrectly, then correctly — confirm the lesson gets a ✓ in the lesson list
   only after a correct answer, and that a second wrong-then-right attempt still records
   correctly (`attempts` increments, `correct` flips to true).
7. Bookmark a lesson, back out, come back in — confirm the ★ persisted.
8. Force-stop the app after completing a couple of lessons; reopen — confirm Home and Learn's
   "Continue Learning" card reflects the most recent one, and the Course screen still shows the
   right lessons as completed.
9. Tap a Beginner Guided Project (e.g. Calculator) — confirm it opens New Project with Python
   and "Basic Calculator" pre-selected, and that creating it works exactly like Phase 2's New
   Project flow (nothing about project creation itself was changed).
10. Open Learn for a non-Python language (e.g. JavaScript) — confirm every tab explains the
    missing runtime instead of showing empty or broken lists, and the link to Languages works.
11. Turn off Wi-Fi/data — confirm lesson examples and practice still run (they're fully local,
    same as the IDE's own offline Python execution).
12. Rotate the Course, Lesson and Practice screens; check that in-progress code you typed isn't
    silently discarded on rotation (note: this phase does not add `onSaveInstanceState` handling
    for in-progress lesson/practice code edits — rotating mid-edit will reset the text field to
    the lesson's original example or your last-loaded starter code; flag this if it's worth
    fixing before Phase 6 rather than treating it as new Phase 5 scope).

STOP at Phase 5. Next phase (6) should add online compiler + AI assistant, per the plan —
Learn's Interactive Examples and Practice should keep routing through `ExecutionManager`
unchanged when cloud execution arrives, exactly like the editor's Run button will.
