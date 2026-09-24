package com.codearc.app.execution

import android.content.Context
import com.codearc.app.data.Preferences
import com.codearc.app.net.NetworkStatus
import com.codearc.app.projects.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** Single entry point the editor (and, from Phase 5, Learn) calls. Implements the
 *  Automatic-mode decision logic from the Phase 4 plan: offline runtime installed -> run
 *  offline; else internet available -> cloud path (real HTTPS client as of Phase 6, honestly
 *  reporting CLOUD_NOT_CONFIGURED until a backend is set in Settings); else tell the user to
 *  install a language pack. Offline/Online modes skip the decision and fail honestly instead of
 *  silently falling back. */
class ExecutionManager(context: Context) {
    private val appContext = context.applicationContext
    private val local = LocalExecutionEngine(appContext)
    private val prefs = Preferences(appContext)

    private fun hasInternet(): Boolean = NetworkStatus.hasInternet(appContext)

    suspend fun run(project: Project, projectDir: File, stdin: String = ""): ExecutionResult = withContext(Dispatchers.IO) {
        val lang = LanguageRegistry.forDisplayName(project.language) ?: return@withContext ExecutionResult.notInstalled()
        val offlineReady = RuntimeManager.isInstalled(lang.id)
        val internet = hasInternet()
        val cloud = CloudExecutionEngine(prefs.cloudSettings.first())

        val engine: ExecutionEngine? = when (project.executionMode) {
            "Offline" -> if (offlineReady) local else null
            "Online" -> if (internet) cloud else null
            else -> if (offlineReady) local else if (internet) cloud else null
        }

        if (engine == null) {
            return@withContext when {
                project.executionMode == "Online" -> ExecutionResult.requiresInternet()
                !offlineReady && !internet -> ExecutionResult.notInstalled()
                else -> ExecutionResult.notInstalled()
            }
        }

        val start = System.currentTimeMillis()
        val request = ExecutionRequest(lang.id, projectDir, project.mainFile, stdin)
        val result = withTimeoutOrNull(request.timeoutMs + 2_000) { engine.execute(request) }
        result ?: ExecutionResult("", "Execution timed out.", -1, System.currentTimeMillis() - start, ExecutionStatus.TIMEOUT)
    }

    /** Phase 5 addition for Learn: runs a standalone snippet (a lesson example or a practice
     *  challenge) through the SAME offline engine as project Run — never a separate fake
     *  compiler, per the plan. There's no Project for a lesson, so this writes the snippet to a
     *  private scratch folder under the app's cache dir and executes it exactly like a
     *  one-file project. Always local/offline: lesson content only exists for languages with a
     *  real runtime (see LearnContent.kt), so there's no cloud/automatic decision to make here. */
    suspend fun runSnippet(languageId: String, code: String, stdin: String = ""): ExecutionResult = withContext(Dispatchers.IO) {
        val lang = LanguageRegistry.forId(languageId) ?: return@withContext ExecutionResult.notInstalled()
        if (!RuntimeManager.isInstalled(languageId)) return@withContext ExecutionResult.notInstalled()
        val scratch = File(appContext.cacheDir, "learn_scratch/$languageId").apply { deleteRecursively(); mkdirs() }
        val mainFile = "main.${lang.extensions.first()}"
        File(scratch, mainFile).writeText(code)
        val start = System.currentTimeMillis()
        val request = ExecutionRequest(languageId, scratch, mainFile, stdin)
        val result = withTimeoutOrNull(request.timeoutMs + 2_000) { local.execute(request) }
        result ?: ExecutionResult("", "Execution timed out.", -1, System.currentTimeMillis() - start, ExecutionStatus.TIMEOUT)
    }
}
