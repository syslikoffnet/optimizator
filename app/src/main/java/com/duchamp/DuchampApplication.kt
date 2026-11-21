package com.duchamp

import android.app.Application
import com.duchamp.root.RootConfig
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Configures libsu once (flags + timeout) before any root command runs,
 * and hosts the Hilt graph.
 */
@HiltAndroidApp
class DuchampApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RootConfig.configure(debug = BuildConfig.DEBUG)
    }
}
