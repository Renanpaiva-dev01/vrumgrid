package com.miaugrid.core.ads

/** Onde um anúncio é exibido; usado para relatórios e para separar recompensas. */
enum class AdPlacement { RECOVERY, EXTRA_HINT, BETWEEN_LEVELS }

sealed interface RewardedAdResult {
    /** O SDK confirmou o evento de recompensa (o usuário assistiu o necessário). */
    data class Rewarded(val placement: AdPlacement) : RewardedAdResult
    /** O anúncio foi fechado antes de a recompensa ser confirmada. */
    data object DismissedWithoutReward : RewardedAdResult
    data object NotAvailable : RewardedAdResult
    data class Failed(val reason: String) : RewardedAdResult
}

sealed interface InterstitialResult {
    data object Shown : InterstitialResult
    /** A política de frequência decidiu não exibir agora. */
    data object Skipped : InterstitialResult
    data object NotAvailable : InterstitialResult
    data class Failed(val reason: String) : InterstitialResult
}

/**
 * Fronteira com um SDK de publicidade concreto. Implementações devem:
 * - completar cada `show*` exatamente uma vez;
 * - devolver [RewardedAdResult.Rewarded] somente após o callback de recompensa do SDK;
 * - nunca lançar exceção para falhas previsíveis (usar `Failed`/`NotAvailable`).
 */
interface AdProvider {
    val isEnabled: Boolean
    fun preloadRewarded()
    fun preloadInterstitial()
    fun isRewardedReady(): Boolean
    fun isInterstitialReady(): Boolean
    suspend fun showRewarded(placement: AdPlacement): RewardedAdResult
    suspend fun showInterstitial(placement: AdPlacement): InterstitialResult
}

/** Provedor usado quando a publicidade está desativada ou indisponível. */
object NoAdsProvider : AdProvider {
    override val isEnabled = false
    override fun preloadRewarded() = Unit
    override fun preloadInterstitial() = Unit
    override fun isRewardedReady() = false
    override fun isInterstitialReady() = false
    override suspend fun showRewarded(placement: AdPlacement) = RewardedAdResult.NotAvailable
    override suspend fun showInterstitial(placement: AdPlacement) = InterstitialResult.NotAvailable
}
