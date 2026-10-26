package com.miaugrid.app.telemetry

import android.util.Log
import com.miaugrid.app.BuildConfig
import com.miaugrid.core.telemetry.Analytics
import com.miaugrid.core.telemetry.AnalyticsEvent

/** Telemetria somente local: registra no Logcat em builds de depuração. Nada sai do aparelho. */
class LogcatAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) {
        if (BuildConfig.DEBUG) Log.d("MiauGrid", event.toString())
    }
}
