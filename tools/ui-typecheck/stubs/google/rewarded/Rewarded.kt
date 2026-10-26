@file:Suppress("unused", "UNUSED_PARAMETER")
package com.google.android.gms.ads.rewarded
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
interface RewardItem { val amount: Int; val type: String }
abstract class RewardedAdLoadCallback { open fun onAdLoaded(ad: RewardedAd) {}; open fun onAdFailedToLoad(error: LoadAdError) {} }
abstract class RewardedAd {
    var fullScreenContentCallback: FullScreenContentCallback? = null
    abstract fun show(activity: Activity, listener: OnUserEarnedRewardListener)
    companion object { @JvmStatic fun load(c: Context, id: String, r: AdRequest, cb: RewardedAdLoadCallback) {} }
}
