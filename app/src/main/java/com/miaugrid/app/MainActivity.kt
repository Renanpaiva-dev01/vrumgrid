package com.miaugrid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.miaugrid.app.ui.navigation.AppNavHost
import com.miaugrid.app.ui.theme.MiauGridTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MiauGridApp).container
        // Consentimento de privacidade antes de qualquer pedido de anúncio.
        if (savedInstanceState == null) {
            container.consentManager.gather(this) { canRequestAds -> container.onConsentResolved(canRequestAds) }
        } else if (container.consentManager.canRequestAds) {
            container.onConsentResolved(true)
        }
        setContent {
            MiauGridTheme {
                AppNavHost()
            }
        }
    }
}
