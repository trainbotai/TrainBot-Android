package com.luca.trainbot

import android.app.Application
import com.luca.trainbot.core.di.AppContainer

class TrainBotApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
