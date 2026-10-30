package com.miaugrid.app.di

import android.app.Application
import com.miaugrid.app.BuildConfig
import com.miaugrid.app.MiauGridApp
import com.miaugrid.app.ads.ActivityTracker
import com.miaugrid.app.ads.AdMobProvider
import com.miaugrid.app.ads.ConsentManager
import com.miaugrid.app.audio.SoundPlayer
import com.miaugrid.app.data.DataStoreKeyValueStore
import com.miaugrid.app.telemetry.LogcatAnalytics
import com.miaugrid.core.ads.AdService
import com.miaugrid.core.ads.InterstitialPolicy
import com.miaugrid.core.daily.DailyChallenges
import com.miaugrid.core.levels.LevelRepository
import com.miaugrid.core.persistence.PlayerDataRepository
import com.miaugrid.core.persistence.VersionedStorage
import com.miaugrid.core.recovery.RecoveryCoordinator
import com.miaugrid.core.telemetry.AnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Injeção de dependências manual: um único lugar cria os serviços do aplicativo.
 * A lógica de jogo vem do módulo `core`; aqui ficam só as implementações Android.
 */
class AppContainer(application: Application) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val analytics = LogcatAnalytics()

    /** Carregado fora da thread principal; as telas aguardam com `await()`. */
    val levels = applicationScope.async(Dispatchers.Default) { LevelRepository.loadCampaign() }

    val playerData = PlayerDataRepository(
        VersionedStorage(DataStoreKeyValueStore(application)) { key, message ->
            analytics.track(AnalyticsEvent.StorageError(key, message))
        },
    )

    val dailyChallenges = DailyChallenges()

    val activityTracker = ActivityTracker().also { application.registerActivityLifecycleCallbacks(it) }

    val consentManager = ConsentManager(application)

    val adMob = AdMobProvider(
        context = application,
        activityTracker = activityTracker,
        scope = applicationScope,
        rewardedUnitId = BuildConfig.REWARDED_AD_UNIT,
        interstitialUnitId = BuildConfig.INTERSTITIAL_AD_UNIT,
        configuredEnabled = BuildConfig.ADS_ENABLED && !MiauGridApp.adsDisabledForTests,
    )

    val adService = AdService(adMob, InterstitialPolicy(levelsBetweenAds = 3, minIntervalMillis = 3 * 60_000L))

    val recoveryCoordinator = RecoveryCoordinator(adService)

    val soundPlayer = SoundPlayer()

    init {
        applicationScope.launch { playerData.load() }
        playerData.settings
            .map { it.soundEnabled }
            .distinctUntilChanged()
            .onEach { soundPlayer.enabled = it }
            .launchIn(applicationScope)
    }

    /** Chamado pela Activity depois da verificação de consentimento. */
    fun onConsentResolved(canRequestAds: Boolean) {
        if (canRequestAds) adMob.initialize()
    }
}
