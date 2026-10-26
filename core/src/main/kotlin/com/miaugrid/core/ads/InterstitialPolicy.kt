package com.miaugrid.core.ads

/**
 * Frequência de anúncios intersticiais entre partidas (configurável).
 * Exibe no máximo um a cada [levelsBetweenAds] fases concluídas e nunca antes de
 * [minIntervalMillis] desde o último.
 */
data class InterstitialPolicy(
    val levelsBetweenAds: Int = 3,
    val minIntervalMillis: Long = 3 * 60_000L,
) {
    init {
        require(levelsBetweenAds >= 1)
        require(minIntervalMillis >= 0)
    }
}
