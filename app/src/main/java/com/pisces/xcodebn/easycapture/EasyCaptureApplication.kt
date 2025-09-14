package com.pisces.xcodebn.easycapture

import android.app.Application
import com.pisces.xcodebn.easycapture.di.AppContainer

class EasyCaptureApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

}