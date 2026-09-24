package com.codearc.app.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
private val Context.store by preferencesDataStore("codearc_preferences")

/** Phase 6: where CloudExecutionEngine and the AI assistant send their HTTPS requests. Both are
 *  optional — CodeArc never ships a hardcoded backend, so until someone fills these in, cloud
 *  run and AI honestly report "not configured" instead of silently doing nothing. */
data class CloudSettings(
    val endpoint: String = "",
    val apiKey: String = "",
    val aiEnabled: Boolean = true
) {
    val configured get() = endpoint.isNotBlank()
}

data class EditorSettings(
    val fontSize: Int = 14,
    val tabSize: Int = 4,
    val wordWrap: Boolean = false,
    val lineNumbers: Boolean = true,
    val autoIndent: Boolean = true,
    val autoBrackets: Boolean = true,
    val autoQuotes: Boolean = true,
    val syntaxHighlighting: Boolean = true
)

class Preferences(private val context: Context) {
 private val completed = booleanPreferencesKey("onboarding_completed")
 val onboardingCompleted = context.store.data.map { it[completed] ?: false }
 suspend fun completeOnboarding() { context.store.edit { it[completed] = true } }

 private val kFont = intPreferencesKey("editor_font_size")
 private val kTab = intPreferencesKey("editor_tab_size")
 private val kWrap = booleanPreferencesKey("editor_word_wrap")
 private val kLines = booleanPreferencesKey("editor_line_numbers")
 private val kIndent = booleanPreferencesKey("editor_auto_indent")
 private val kBrackets = booleanPreferencesKey("editor_auto_brackets")
 private val kQuotes = booleanPreferencesKey("editor_auto_quotes")
 private val kSyntax = booleanPreferencesKey("editor_syntax_highlighting")
 val editorSettings = context.store.data.map {
  EditorSettings(
   it[kFont] ?: 14, it[kTab] ?: 4, it[kWrap] ?: false, it[kLines] ?: true,
   it[kIndent] ?: true, it[kBrackets] ?: true, it[kQuotes] ?: true, it[kSyntax] ?: true
  )
 }
 suspend fun saveEditorSettings(s: EditorSettings) {
  context.store.edit {
   it[kFont]=s.fontSize; it[kTab]=s.tabSize; it[kWrap]=s.wordWrap; it[kLines]=s.lineNumbers
   it[kIndent]=s.autoIndent; it[kBrackets]=s.autoBrackets; it[kQuotes]=s.autoQuotes; it[kSyntax]=s.syntaxHighlighting
  }
 }

 private val kCloudEndpoint = stringPreferencesKey("cloud_endpoint")
 private val kCloudApiKey = stringPreferencesKey("cloud_api_key")
 private val kAiEnabled = booleanPreferencesKey("ai_enabled")
 val cloudSettings = context.store.data.map {
  CloudSettings(it[kCloudEndpoint] ?: "", it[kCloudApiKey] ?: "", it[kAiEnabled] ?: true)
 }
 suspend fun saveCloudSettings(s: CloudSettings) {
  context.store.edit {
   it[kCloudEndpoint] = s.endpoint.trim(); it[kCloudApiKey] = s.apiKey.trim(); it[kAiEnabled] = s.aiEnabled
  }
 }
}
