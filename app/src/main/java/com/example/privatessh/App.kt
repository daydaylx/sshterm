package com.example.privatessh

import android.app.Application
import com.example.privatessh.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for the Private SSH application.
 *
 * Hilt manages all dependency injection through this annotation.
 */
@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
