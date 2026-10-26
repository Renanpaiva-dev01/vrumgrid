package com.miaugrid.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Consentimento de privacidade (Google User Messaging Platform). O formulário só aparece
 * onde a lei exige (ex.: Espaço Econômico Europeu, Reino Unido). Os anúncios só são
 * solicitados quando [ConsentInformation.canRequestAds] permite.
 */
class ConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Atualiza o consentimento e mostra o formulário, se necessário. [onDone] sempre é chamado. */
    fun gather(activity: Activity, onDone: (canRequestAds: Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    onDone(consentInformation.canRequestAds())
                }
            },
            { _ -> onDone(consentInformation.canRequestAds()) },
        )
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> onDone() }
    }
}
