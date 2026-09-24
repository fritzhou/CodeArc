package com.codearc.app.editor

enum class Severity { ERROR, WARNING }
data class Problem(val file: String, val line: Int, val column: Int, val severity: Severity, val message: String)
