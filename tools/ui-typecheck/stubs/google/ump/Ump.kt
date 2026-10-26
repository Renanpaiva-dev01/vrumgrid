@file:Suppress("unused", "UNUSED_PARAMETER")
package com.google.android.ump
import android.app.Activity
import android.content.Context
class FormError
fun interface OnConsentFormDismissedListener { fun onConsentFormDismissed(error: FormError?) }
class ConsentRequestParameters { class Builder { fun build() = ConsentRequestParameters() } }
interface ConsentInformation {
    fun canRequestAds(): Boolean
    val privacyOptionsRequirementStatus: PrivacyOptionsRequirementStatus
    fun requestConsentInfoUpdate(a: Activity, p: ConsentRequestParameters, s: OnConsentInfoUpdateSuccessListener, f: OnConsentInfoUpdateFailureListener)
    fun interface OnConsentInfoUpdateSuccessListener { fun onConsentInfoUpdateSuccess() }
    fun interface OnConsentInfoUpdateFailureListener { fun onConsentInfoUpdateFailure(error: FormError) }
    enum class PrivacyOptionsRequirementStatus { UNKNOWN, NOT_REQUIRED, REQUIRED }
}
object UserMessagingPlatform {
    @JvmStatic fun getConsentInformation(c: Context): ConsentInformation = TODO()
    @JvmStatic fun loadAndShowConsentFormIfRequired(a: Activity, l: OnConsentFormDismissedListener) {}
    @JvmStatic fun showPrivacyOptionsForm(a: Activity, l: OnConsentFormDismissedListener) {}
}
