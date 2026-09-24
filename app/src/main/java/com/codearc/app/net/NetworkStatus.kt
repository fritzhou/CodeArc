package com.codearc.app.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Single place that answers "is there internet right now" — used by ExecutionManager (cloud
 *  run) and AiManager (Phase 6) so both features agree on the same check instead of each
 *  re-implementing it. Reachability of a specific backend is a separate question, answered by
 *  the HTTP call itself. */
object NetworkStatus {
    fun hasInternet(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
