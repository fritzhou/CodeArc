package com.codearc.app.ai

/** The eight actions from the Phase 6 plan's "AI Assistant" list. ASK_AI is the free-form one;
 *  every other action operates on the code passed in [AiRequest.code] (the current selection,
 *  or the whole file when nothing is selected). */
enum class AiAction(val label: String) {
    EXPLAIN_CODE("Explain Code"),
    EXPLAIN_ERROR("Explain Error"),
    FIX_CODE("Fix Code"),
    GENERATE_CODE("Generate Code"),
    REFACTOR("Refactor"),
    SIMPLIFY("Simplify"),
    ADD_COMMENTS("Add Comments"),
    ASK_AI("Ask AI")
}

/** [errorContext] carries only the relevant compiler/linter message and its line — per the
 *  plan's instruction not to send entire projects unnecessarily, this (and [code]) is the only
 *  source CodeArc ever sends off-device. */
data class AiRequest(
    val action: AiAction,
    val languageId: String,
    val code: String,
    val errorContext: String? = null,
    val question: String? = null
)

enum class AiStatus { SUCCESS, REQUIRES_INTERNET, NOT_CONFIGURED, ERROR }

/** [suggestedCode], when present, is never applied to the editor automatically — EditorActivity
 *  always routes it through an explicit Apply button, per the plan ("Never modify source code
 *  automatically. Require Apply confirmation."). */
data class AiResult(
    val status: AiStatus,
    val explanation: String,
    val suggestedCode: String? = null
) {
    companion object {
        fun requiresInternet() = AiResult(AiStatus.REQUIRES_INTERNET, "Requires Internet.")
        fun notConfigured() = AiResult(AiStatus.NOT_CONFIGURED, "AI assistance isn't configured yet. Add a cloud endpoint in Settings > AI.")
        fun error(message: String) = AiResult(AiStatus.ERROR, message)
    }
}
