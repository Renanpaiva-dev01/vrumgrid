@file:Suppress("unused", "UNUSED_PARAMETER")
package com.google.android.gms.ads
import android.content.Context
open class AdError(val code: Int, val message: String)
class LoadAdError(code: Int, message: String) : AdError(code, message)
class AdRequest { class Builder { fun build() = AdRequest() } }
abstract class FullScreenContentCallback {
    open fun onAdDismissedFullScreenContent() {}
    open fun onAdFailedToShowFullScreenContent(adError: AdError) {}
    open fun onAdShowedFullScreenContent() {}
}
fun interface OnUserEarnedRewardListener { fun onUserEarnedReward(item: com.google.android.gms.ads.rewarded.RewardItem) }
object MobileAds {
    fun interface InitListener { fun onInitializationComplete(status: Any) }
    @JvmStatic fun initialize(context: Context, listener: InitListener) {}
}
