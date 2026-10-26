package com.miaugrid.core.ads

import kotlinx.coroutines.sync.Mutex

/**
 * Coordena a publicidade sem conhecer as regras do puzzle:
 * - impede anúncios sobrepostos (um por vez);
 * - aplica a frequência dos intersticiais;
 * - converte exceções do provedor em falhas tratáveis, para que o jogo sempre continue.
 *
 * Deve ser chamado apenas em transições (fim de fase, tela de derrota, pedido de dica),
 * nunca no meio de uma jogada.
 */
class AdService(
    private val provider: AdProvider,
    private val interstitialPolicy: InterstitialPolicy = InterstitialPolicy(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val showing = Mutex()
    private var levelsSinceInterstitial = 0
    private var lastInterstitialAt: Long? = null

    val isEnabled: Boolean get() = provider.isEnabled
    val isShowing: Boolean get() = showing.isLocked

    fun preload() {
        if (!provider.isEnabled) return
        runCatching { provider.preloadRewarded() }
        runCatching { provider.preloadInterstitial() }
    }

    fun isRewardedReady(): Boolean = provider.isEnabled && runCatching { provider.isRewardedReady() }.getOrDefault(false)

    suspend fun showRewarded(placement: AdPlacement): RewardedAdResult {
        if (!provider.isEnabled) return RewardedAdResult.NotAvailable
        if (!showing.tryLock()) return RewardedAdResult.NotAvailable
        return try {
            if (!provider.isRewardedReady()) {
                provider.preloadRewarded()
                RewardedAdResult.NotAvailable
            } else {
                provider.showRewarded(placement)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            RewardedAdResult.Failed(e.message ?: e::class.simpleName.orEmpty())
        } finally {
            showing.unlock()
            runCatching { provider.preloadRewarded() }
        }
    }

    /** Registra uma fase concluída (alimenta a frequência dos intersticiais). */
    fun onLevelCompleted() {
        levelsSinceInterstitial++
    }

    fun shouldShowInterstitial(): Boolean {
        if (!provider.isEnabled) return false
        if (levelsSinceInterstitial < interstitialPolicy.levelsBetweenAds) return false
        val last = lastInterstitialAt ?: return true
        return clock() - last >= interstitialPolicy.minIntervalMillis
    }

    /** Exibe um intersticial se a política permitir. Nunca bloqueia a continuação do jogo. */
    suspend fun maybeShowInterstitial(placement: AdPlacement = AdPlacement.BETWEEN_LEVELS): InterstitialResult {
        if (!shouldShowInterstitial()) return InterstitialResult.Skipped
        if (!showing.tryLock()) return InterstitialResult.Skipped
        return try {
            if (!provider.isInterstitialReady()) {
                provider.preloadInterstitial()
                InterstitialResult.NotAvailable
            } else {
                val result = provider.showInterstitial(placement)
                if (result == InterstitialResult.Shown) {
                    levelsSinceInterstitial = 0
                    lastInterstitialAt = clock()
                }
                result
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            InterstitialResult.Failed(e.message ?: e::class.simpleName.orEmpty())
        } finally {
            showing.unlock()
            runCatching { provider.preloadInterstitial() }
        }
    }
}
