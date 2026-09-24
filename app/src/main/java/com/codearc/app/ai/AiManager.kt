package com.codearc.app.ai

import android.content.Context
import com.codearc.app.data.Preferences
import com.codearc.app.net.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** What EditorActivity calls for every AI action. Mirrors ExecutionManager's shape on purpose:
 *  check internet, check configuration, then hand off — so the Internet State behavior from the
 *  plan ("When offline: AI should show Requires Internet") is consistent with cloud Run. */
class AiManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = Preferences(appContext)

    suspend fun ask(request: AiRequest): AiResult = withContext(Dispatchers.IO) {
        if (!NetworkStatus.hasInternet(appContext)) return@withContext AiResult.requiresInternet()
        val settings = prefs.cloudSettings.first()
        if (!settings.aiEnabled) return@withContext AiResult.notConfigured()
        CloudAiClient(settings).ask(request)
    }
}
