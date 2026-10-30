package com.miaugrid.app

import android.app.Application
import androidx.test.runner.AndroidJUnitRunner

/** Executor dos testes instrumentados: desliga os anúncios antes de o aplicativo iniciar. */
class VrumGridTestRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application) {
        MiauGridApp.adsDisabledForTests = true
        super.callApplicationOnCreate(app)
    }
}
