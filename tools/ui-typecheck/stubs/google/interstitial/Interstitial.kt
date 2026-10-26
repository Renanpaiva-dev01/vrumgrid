@file:Suppress("unused", "UNUSED_PARAMETER")
package com.google.android.gms.ads.interstitial
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
abstract class InterstitialAdLoadCallback { open fun onAdLoaded(ad: InterstitialAd) {}; open fun onAdFailedToLoad(error: LoadAdError) {} }
abstract class InterstitialAd {
    var fullScreenContentCallback: FullScreenContentCallback? = null
    abstract fun show(activity: Activity)
    companion object { @JvmStatic fun load(c: Context, id: String, r: AdRequest, cb: InterstitialAdLoadCallback) {} }
}
