package com.miaugrid.app.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.miaugrid.core.ads.AdPlacement
import com.miaugrid.core.ads.AdProvider
import com.miaugrid.core.ads.InterstitialResult
import com.miaugrid.core.ads.RewardedAdResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Adaptador do Google Mobile Ads SDK (AdMob) para a interface [AdProvider] do núcleo.
 *
 * - Usa os IDs de teste oficiais em builds de depuração (ver app/build.gradle.kts).
 * - Só inicializa o SDK depois da verificação de consentimento ([ConsentManager]).
 * - A recompensa só é devolvida se o callback `OnUserEarnedRewardListener` do SDK disparar.
 * - Cada exibição completa exatamente uma vez, mesmo que o SDK chame vários callbacks.
 */
class AdMobProvider(
    private val context: Context,
    private val activityTracker: ActivityTracker,
    private val scope: CoroutineScope,
    private val rewardedUnitId: String,
    private val interstitialUnitId: String,
    configuredEnabled: Boolean,
) : AdProvider {

    private val enabledByConfig = configuredEnabled && rewardedUnitId.isNotBlank() && interstitialUnitId.isNotBlank()
    private val initialized = AtomicBoolean(false)
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var loadingRewarded = false
    private var loadingInterstitial = false

    override val isEnabled: Boolean get() = enabledByConfig && initialized.get()

    /** Inicializa o SDK (fora da thread principal) e pré-carrega os anúncios. */
    fun initialize() {
        if (!enabledByConfig || !initialized.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {
                scope.launch(Dispatchers.Main) {
                    preloadRewarded()
                    preloadInterstitial()
                }
            }
        }
    }

    override fun preloadRewarded() {
        if (!isEnabled || rewardedAd != null || loadingRewarded) return
        loadingRewarded = true
        scope.launch(Dispatchers.Main) {
            RewardedAd.load(context, rewardedUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    loadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Recompensado não carregou: ${error.code} ${error.message}")
                    rewardedAd = null
                    loadingRewarded = false
                }
            })
        }
    }

    override fun preloadInterstitial() {
        if (!isEnabled || interstitialAd != null || loadingInterstitial) return
        loadingInterstitial = true
        scope.launch(Dispatchers.Main) {
            InterstitialAd.load(context, interstitialUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Intersticial não carregou: ${error.code} ${error.message}")
                    interstitialAd = null
                    loadingInterstitial = false
                }
            })
        }
    }

    override fun isRewardedReady(): Boolean = rewardedAd != null

    override fun isInterstitialReady(): Boolean = interstitialAd != null

    override suspend fun showRewarded(placement: AdPlacement): RewardedAdResult = withContext<RewardedAdResult>(Dispatchers.Main) {
        val ad = rewardedAd ?: return@withContext RewardedAdResult.NotAvailable
        val activity = activityTracker.current ?: return@withContext RewardedAdResult.NotAvailable
        rewardedAd = null // um anúncio carregado só pode ser exibido uma vez
        suspendCancellableCoroutine { continuation ->
            val finished = AtomicBoolean(false)
            val earned = AtomicBoolean(false)
            fun finish(result: RewardedAdResult) {
                if (finished.compareAndSet(false, true) && continuation.isActive) continuation.resume(result)
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    finish(if (earned.get()) RewardedAdResult.Rewarded(placement) else RewardedAdResult.DismissedWithoutReward)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    finish(RewardedAdResult.Failed(adError.message))
                }
            }
            ad.show(activity) { _ -> earned.set(true) }
        }
    }

    override suspend fun showInterstitial(placement: AdPlacement): InterstitialResult = withContext<InterstitialResult>(Dispatchers.Main) {
        val ad = interstitialAd ?: return@withContext InterstitialResult.NotAvailable
        val activity = activityTracker.current ?: return@withContext InterstitialResult.NotAvailable
        interstitialAd = null
        suspendCancellableCoroutine { continuation ->
            val finished = AtomicBoolean(false)
            fun finish(result: InterstitialResult) {
                if (finished.compareAndSet(false, true) && continuation.isActive) continuation.resume(result)
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() = finish(InterstitialResult.Shown)
                override fun onAdFailedToShowFullScreenContent(adError: AdError) =
                    finish(InterstitialResult.Failed(adError.message))
            }
            ad.show(activity)
        }
    }

    private companion object {
        const val TAG = "MiauGridAds"
    }
}
