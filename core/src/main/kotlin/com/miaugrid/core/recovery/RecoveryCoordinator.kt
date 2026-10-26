package com.miaugrid.core.recovery

import com.miaugrid.core.ads.AdPlacement
import com.miaugrid.core.ads.AdService
import com.miaugrid.core.ads.RewardedAdResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface RecoveryResult {
    /** Recompensa confirmada pelo SDK para o evento [defeatId]. */
    data class Granted(val defeatId: String) : RecoveryResult
    data object NoReward : RecoveryResult
    data object AdUnavailable : RecoveryResult
    data class AdFailed(val reason: String) : RecoveryResult
    data object AlreadyGranted : RecoveryResult
    data object Busy : RecoveryResult
}

/**
 * Faz a ponte entre uma derrota e o anúncio recompensado. Garante que cada evento de
 * derrota receba no máximo uma recompensa, mesmo com cliques repetidos ou callbacks
 * duplicados. O benefício em si é aplicado por [com.miaugrid.core.game.GameAction.ApplyRecovery],
 * que também é idempotente.
 */
class RecoveryCoordinator(private val adService: AdService) {

    private val lock = Mutex()
    private val granted = HashSet<String>()
    private var inFlight: String? = null

    suspend fun request(defeatId: String): RecoveryResult {
        lock.withLock {
            if (defeatId in granted) return RecoveryResult.AlreadyGranted
            if (inFlight != null) return RecoveryResult.Busy
            inFlight = defeatId
        }
        try {
            return when (val result = adService.showRewarded(AdPlacement.RECOVERY)) {
                is RewardedAdResult.Rewarded -> lock.withLock {
                    if (granted.add(defeatId)) RecoveryResult.Granted(defeatId) else RecoveryResult.AlreadyGranted
                }
                RewardedAdResult.DismissedWithoutReward -> RecoveryResult.NoReward
                RewardedAdResult.NotAvailable -> RecoveryResult.AdUnavailable
                is RewardedAdResult.Failed -> RecoveryResult.AdFailed(result.reason)
            }
        } finally {
            lock.withLock { inFlight = null }
        }
    }
}
