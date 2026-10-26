package com.miaugrid.core

import com.miaugrid.core.ads.AdPlacement
import com.miaugrid.core.ads.AdProvider
import com.miaugrid.core.ads.AdService
import com.miaugrid.core.ads.InterstitialPolicy
import com.miaugrid.core.ads.InterstitialResult
import com.miaugrid.core.ads.NoAdsProvider
import com.miaugrid.core.ads.RewardedAdResult
import com.miaugrid.core.game.GameAction
import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameSession
import com.miaugrid.core.game.GameStatus
import com.miaugrid.core.recovery.RecoveryCoordinator
import com.miaugrid.core.recovery.RecoveryResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Provedor falso controlado pelos testes (substitui o SDK real). */
class FakeAdProvider(
    var rewardedReady: Boolean = true,
    var interstitialReady: Boolean = true,
    var rewardedResult: RewardedAdResult = RewardedAdResult.Rewarded(AdPlacement.RECOVERY),
    var throwOnShow: Boolean = false,
) : AdProvider {
    override val isEnabled = true
    var rewardedShows = 0
    var interstitialShows = 0
    var preloads = 0
    var gate: CompletableDeferred<Unit>? = null
    override fun preloadRewarded() { preloads++ }
    override fun preloadInterstitial() { preloads++ }
    override fun isRewardedReady() = rewardedReady
    override fun isInterstitialReady() = interstitialReady
    override suspend fun showRewarded(placement: AdPlacement): RewardedAdResult {
        rewardedShows++
        gate?.await()
        if (throwOnShow) throw IllegalStateException("SDK quebrou")
        return rewardedResult
    }
    override suspend fun showInterstitial(placement: AdPlacement): InterstitialResult {
        interstitialShows++
        if (throwOnShow) throw IllegalStateException("SDK quebrou")
        return InterstitialResult.Shown
    }
}

class AdsAndRecoveryTest {

    private val session = GameSession(Fixtures.puzzle5, GameModeConfig.CLASSIC)

    private fun lostState() = listOf(Fixtures.p(0, 0), Fixtures.p(0, 1), Fixtures.p(1, 0))
        .fold(session.newGame("ad")) { s, p -> session.reduce(s, GameAction.PlaceCat(p)).state }
        .also { assertEquals(GameStatus.LOST, it.status) }

    @Test fun `recuperacao concedida somente com recompensa confirmada`() = runTest {
        val provider = FakeAdProvider()
        val coordinator = RecoveryCoordinator(AdService(provider))
        val lost = lostState()
        val result = coordinator.request(lost.currentDefeatId!!)
        assertEquals(RecoveryResult.Granted(lost.currentDefeatId!!), result)
        val recovered = session.reduce(lost, GameAction.ApplyRecovery((result as RecoveryResult.Granted).defeatId)).state
        assertEquals(GameStatus.PLAYING, recovered.status)
    }

    @Test fun `fechar o anuncio sem recompensa nao concede nada`() = runTest {
        val provider = FakeAdProvider(rewardedResult = RewardedAdResult.DismissedWithoutReward)
        val result = RecoveryCoordinator(AdService(provider)).request("d1")
        assertEquals(RecoveryResult.NoReward, result)
    }

    @Test fun `recompensa duplicada para o mesmo evento e recusada`() = runTest {
        val coordinator = RecoveryCoordinator(AdService(FakeAdProvider()))
        assertTrue(coordinator.request("d1") is RecoveryResult.Granted)
        assertEquals(RecoveryResult.AlreadyGranted, coordinator.request("d1"))
    }

    @Test fun `cliques repetidos durante o anuncio nao abrem outro`() = runTest {
        val provider = FakeAdProvider().apply { gate = CompletableDeferred() }
        val coordinator = RecoveryCoordinator(AdService(provider))
        val first = async { coordinator.request("d1") }
        yield()
        assertEquals(RecoveryResult.Busy, coordinator.request("d1"))
        provider.gate!!.complete(Unit)
        assertTrue(first.await() is RecoveryResult.Granted)
        assertEquals(1, provider.rewardedShows)
    }

    @Test fun `anuncio nao carregado informa indisponibilidade e tenta carregar`() = runTest {
        val provider = FakeAdProvider(rewardedReady = false)
        val result = RecoveryCoordinator(AdService(provider)).request("d1")
        assertEquals(RecoveryResult.AdUnavailable, result)
        assertEquals(0, provider.rewardedShows)
        assertTrue(provider.preloads > 0)
    }

    @Test fun `falha do SDK vira resultado tratavel`() = runTest {
        val provider = FakeAdProvider(throwOnShow = true)
        val result = RecoveryCoordinator(AdService(provider)).request("d1")
        assertTrue(result is RecoveryResult.AdFailed)
        assertEquals(InterstitialResult.Skipped, AdService(provider).maybeShowInterstitial())
    }

    @Test fun `falha no carregamento informada pelo SDK`() = runTest {
        val provider = FakeAdProvider(rewardedResult = RewardedAdResult.Failed("no fill"))
        assertEquals(RecoveryResult.AdFailed("no fill"), RecoveryCoordinator(AdService(provider)).request("d1"))
    }

    @Test fun `sem publicidade o jogo continua`() = runTest {
        val service = AdService(NoAdsProvider)
        assertEquals(RecoveryResult.AdUnavailable, RecoveryCoordinator(service).request("d1"))
        service.onLevelCompleted(); service.onLevelCompleted(); service.onLevelCompleted()
        assertEquals(InterstitialResult.Skipped, service.maybeShowInterstitial())
    }

    @Test fun `frequencia de intersticiais`() = runTest {
        var now = 0L
        val provider = FakeAdProvider()
        val service = AdService(provider, InterstitialPolicy(levelsBetweenAds = 2, minIntervalMillis = 60_000), clock = { now })
        service.onLevelCompleted()
        assertEquals(InterstitialResult.Skipped, service.maybeShowInterstitial())
        service.onLevelCompleted()
        assertEquals(InterstitialResult.Shown, service.maybeShowInterstitial())
        service.onLevelCompleted(); service.onLevelCompleted()
        now = 30_000
        assertEquals("intervalo mínimo", InterstitialResult.Skipped, service.maybeShowInterstitial())
        now = 61_000
        assertEquals(InterstitialResult.Shown, service.maybeShowInterstitial())
        assertEquals(2, provider.interstitialShows)
    }

    @Test fun `intersticial falhando no carregamento nao bloqueia`() = runTest {
        val provider = FakeAdProvider(interstitialReady = false)
        val service = AdService(provider, InterstitialPolicy(levelsBetweenAds = 1, minIntervalMillis = 0))
        service.onLevelCompleted()
        assertEquals(InterstitialResult.NotAvailable, service.maybeShowInterstitial())
        assertFalse(service.isShowing)
    }
}
