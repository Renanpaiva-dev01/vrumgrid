package com.miaugrid.core.telemetry

/** Eventos de uso. Não contêm dados pessoais. */
sealed interface AnalyticsEvent {
    data class LevelStarted(val levelId: String) : AnalyticsEvent
    data class LevelWon(val levelId: String, val timeMillis: Long, val mistakes: Int) : AnalyticsEvent
    data class LevelLost(val levelId: String) : AnalyticsEvent
    data class HintUsed(val levelId: String, val kind: String) : AnalyticsEvent
    data class AdResult(val placement: String, val result: String) : AnalyticsEvent
    data class StorageError(val key: String, val message: String) : AnalyticsEvent
}

/**
 * Telemetria. A implementação atual é apenas local (log em depuração); nada é enviado
 * para servidores. Um backend real pode ser conectado implementando esta interface.
 */
fun interface Analytics {
    fun track(event: AnalyticsEvent)

    companion object {
        val NONE = Analytics { }
    }
}
