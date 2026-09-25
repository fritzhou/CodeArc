package com.codearc.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.codearc.app.data.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Applies the person's saved Light/Dark/System choice before any Activity is created, so the
 *  very first frame (including the splash screen) already renders in the right theme instead of
 *  flashing one theme then switching. The read is a single fast local DataStore lookup, so a
 *  brief blocking read here (same pattern most DayNight apps use) is preferable to a visible
 *  theme flash. */
class CodeArcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val mode = runBlocking { Preferences(this@CodeArcApplication).themeMode.first() }
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
