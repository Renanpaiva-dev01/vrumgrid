package com.miaugrid.app

import android.app.Application
import com.miaugrid.app.di.AppContainer

class MiauGridApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        /**
         * Ligado só pelo executor dos testes instrumentados, antes do `onCreate`: o SDK de
         * anúncios carregando em segundo plano deixa a thread principal ocupada e torna os
         * testes de interface instáveis.
         */
        @Volatile
        var adsDisabledForTests: Boolean = false
    }
}
