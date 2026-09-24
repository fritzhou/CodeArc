package com.codearc.app.execution

/** What happened when a run finished (or didn't). */
enum class ExecutionStatus { SUCCESS, RUNTIME_ERROR, COMPILE_ERROR, TIMEOUT, RUNTIME_NOT_INSTALLED, REQUIRES_INTERNET, CLOUD_NOT_CONFIGURED, CANCELLED }

data class ExecutionResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executionTimeMs: Long,
    val status: ExecutionStatus,
    val compilerErrors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun notInstalled() = ExecutionResult("", "Runtime not installed.", -1, 0, ExecutionStatus.RUNTIME_NOT_INSTALLED)
        fun requiresInternet() = ExecutionResult("", "This requires an internet connection.", -1, 0, ExecutionStatus.REQUIRES_INTERNET)
        fun cloudNotConfigured() = ExecutionResult("", "Cloud execution isn't configured yet. Add a cloud endpoint in Settings > Execution.", -1, 0, ExecutionStatus.CLOUD_NOT_CONFIGURED)
    }
}
