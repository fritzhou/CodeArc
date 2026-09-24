package com.codearc.app.execution

import java.io.File

/** One request to run a project's entry point. [projectDir] is the project's real root on
 *  local storage (from ProjectRepository.root); [mainFile] is project-relative, e.g. "src/main.py". */
data class ExecutionRequest(
    val languageId: String,
    val projectDir: File,
    val mainFile: String,
    val stdin: String = "",
    val timeoutMs: Long = 15_000
)

/** Editor -> ExecutionManager -> ExecutionEngine -> ExecutionResult. Implemented by
 *  LocalExecutionEngine (offline) and CloudExecutionEngine (online, Phase 6). */
interface ExecutionEngine {
    suspend fun execute(request: ExecutionRequest): ExecutionResult
}
