package com.eliteonetube.glovebox

import android.app.Application

class GloveboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GloveboxApplication
            private set
    }
}
