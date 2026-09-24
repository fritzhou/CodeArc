package com.codearc.app.ai

import com.codearc.app.data.CloudSettings
import com.codearc.app.execution.CloudExecutionEngine
import org.json.JSONObject

/** Contract for one call to an AI backend. Kept separate from [CloudExecutionEngine] because
 *  the two hit different endpoints and can fail independently (a project's execution backend
 *  and its AI assistant don't have to be the same service), even though they share the plan's
 *  "optional internet-powered feature" shape and the same JSON-over-HTTPS transport. */
interface AiClient {
    suspend fun ask(request: AiRequest): AiResult
}

/** Sends [request] to `${endpoint}/v1/ai`. CodeArc bundles no AI model of its own — this only
 *  talks to whatever endpoint the person configures — and never sends more than the action,
 *  language, the relevant code/selection and (for Explain Error) the specific error message,
 *  matching the plan's "only send relevant context" instruction. */
class CloudAiClient(private val settings: CloudSettings) : AiClient {
    override suspend fun ask(request: AiRequest): AiResult {
        if (!settings.configured) return AiResult.notConfigured()
        return try {
            val body = JSONObject().apply {
                put("action", request.action.name)
                put("language", request.languageId)
                put("code", request.code)
                request.errorContext?.let { put("error", it) }
                request.question?.let { put("question", it) }
            }
            val raw = CloudExecutionEngine.post("${settings.endpoint.trimEnd('/')}/v1/ai", settings.apiKey, body)
            val json = JSONObject(raw)
            AiResult(
                status = AiStatus.SUCCESS,
                explanation = json.optString("explanation", "").ifBlank { "No explanation returned." },
                suggestedCode = json.optString("suggested_code", "").ifBlank { null }
            )
        } catch (e: Exception) {
            AiResult.error(e.message ?: "The AI request failed. Please try again.")
        }
    }
}
