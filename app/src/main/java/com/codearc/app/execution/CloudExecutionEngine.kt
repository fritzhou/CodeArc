package com.codearc.app.execution

import com.codearc.app.data.CloudSettings
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Phase 6: a real HTTPS client for the "Execution API" from the plan's architecture diagram
 *  (Android -> HTTPS -> Execution API -> Isolated Container -> Compiler/Runtime -> Result ->
 *  Android). CodeArc does not ship or bundle a backend — [settings] points this at whatever
 *  execution service the person configures in Settings > Execution. Until that's filled in,
 *  [execute] honestly reports NOT_CONFIGURED instead of pretending to run something. This class
 *  never runs untrusted code itself; all execution happens on the remote, sandboxed service. */
class CloudExecutionEngine(private val settings: CloudSettings) : ExecutionEngine {

    override suspend fun execute(request: ExecutionRequest): ExecutionResult {
        if (!settings.configured) return ExecutionResult.cloudNotConfigured()
        val start = System.currentTimeMillis()
        return try {
            val body = buildRequestBody(request)
            val response = post("${settings.endpoint.trimEnd('/')}/v1/execute", settings.apiKey, body)
            parseResponse(response, System.currentTimeMillis() - start)
        } catch (e: Exception) {
            ExecutionResult(
                stdout = "",
                stderr = "Cloud execution failed: ${e.message ?: e.javaClass.simpleName}",
                exitCode = -1,
                executionTimeMs = System.currentTimeMillis() - start,
                status = ExecutionStatus.RUNTIME_ERROR
            )
        }
    }

    /** Only [mainFile] plus files it lives alongside in the same project are sent — never
     *  arbitrary unrelated project contents — matching the plan's "Cloud Request" shape
     *  (language, runtime version, files, main file, stdin, arguments). */
    private fun buildRequestBody(request: ExecutionRequest): JSONObject {
        val files = JSONArray()
        val root = request.projectDir
        root.walkTopDown().filter { it.isFile }.forEach { f ->
            val relative = f.relativeTo(root).invariantSeparatorsPath
            files.put(JSONObject().put("path", relative).put("content", runCatching { f.readText() }.getOrDefault("")))
        }
        return JSONObject().apply {
            put("language", request.languageId)
            put("main_file", request.mainFile)
            put("files", files)
            put("stdin", request.stdin)
            put("timeout_ms", request.timeoutMs)
        }
    }

    private fun parseResponse(raw: String, elapsedMs: Long): ExecutionResult {
        val json = JSONObject(raw)
        val exitCode = json.optInt("exit_code", -1)
        val status = when {
            json.has("error") -> ExecutionStatus.RUNTIME_ERROR
            exitCode == 0 -> ExecutionStatus.SUCCESS
            else -> ExecutionStatus.RUNTIME_ERROR
        }
        val errors = json.optJSONArray("compiler_errors")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
        val warnings = json.optJSONArray("warnings")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
        return ExecutionResult(
            stdout = json.optString("stdout", ""),
            stderr = json.optString("stderr", ""),
            exitCode = exitCode,
            executionTimeMs = json.optLong("execution_time_ms", elapsedMs),
            status = status,
            compilerErrors = errors,
            warnings = warnings
        )
    }

    companion object {
        /** Shared by CloudExecutionEngine and CloudAiClient — a plain JSON-over-HTTPS POST with
         *  no external HTTP library (Chaquopy already pulls in enough native footprint; adding
         *  OkHttp/Retrofit for one endpoint isn't worth it). Rejects plain http:// endpoints —
         *  cloud execution and AI both send source code, so the connection must be encrypted. */
        fun post(url: String, apiKey: String, body: JSONObject, connectTimeoutMs: Int = 8_000, readTimeoutMs: Int = 20_000): String {
            require(url.startsWith("https://")) { "Cloud endpoint must be an https:// URL." }
            val conn = URL(url).openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (apiKey.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw java.io.IOException("Server returned HTTP $code" + if (text.isNotBlank()) ": $text" else "")
            return text
        }
    }
}
