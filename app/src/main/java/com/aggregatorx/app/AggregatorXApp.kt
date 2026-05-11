package com.aggregatorx.app

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import dagger.hilt.android.HiltAndroidApp

/**
 * AggregatorX - Advanced Multi-Provider Web Scraping Aggregator
 */
@HiltAndroidApp
class AggregatorXApp : Application() {
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Manually enable MultiDex before the rest of the app loads
        MultiDex.install(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: AggregatorXApp
            private set
    }
}
