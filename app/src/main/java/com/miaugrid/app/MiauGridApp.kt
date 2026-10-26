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
}
