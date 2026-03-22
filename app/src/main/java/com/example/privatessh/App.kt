package com.example.privatessh

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Private SSH application.
 *
 * Hilt manages all dependency injection through this annotation.
 */
@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Hilt will initialize all dependencies automatically
    }
}
