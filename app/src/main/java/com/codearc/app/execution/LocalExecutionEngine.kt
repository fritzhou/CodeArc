package com.codearc.app.execution

import android.content.Context

/** Routes a request to the right on-device runtime. Only Python has one in Phase 4; every
 *  other language reports RUNTIME_NOT_INSTALLED rather than pretending to run. */
class LocalExecutionEngine(private val context: Context) : ExecutionEngine {
    override suspend fun execute(request: ExecutionRequest): ExecutionResult {
        if (!RuntimeManager.isInstalled(request.languageId)) return ExecutionResult.notInstalled()
        val start = System.currentTimeMillis()
        return when (request.languageId) {
            "python" -> {
                val outcome = PythonRuntime.run(context, request.projectDir, request.mainFile, request.stdin)
                ExecutionResult(
                    stdout = outcome.stdout,
                    stderr = outcome.stderr,
                    exitCode = outcome.exitCode,
                    executionTimeMs = System.currentTimeMillis() - start,
                    status = if (outcome.exitCode == 0) ExecutionStatus.SUCCESS else ExecutionStatus.RUNTIME_ERROR
                )
            }
            else -> ExecutionResult.notInstalled()
        }
    }
}
