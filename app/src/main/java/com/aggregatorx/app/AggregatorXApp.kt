package com.aggregatorx.app

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkLayerFactory
import dagger.hilt.android.HiltAndroidApp

/**
 * AggregatorX - Advanced Multi-Provider Web Scraping Aggregator
 */
@HiltAndroidApp
class AggregatorXApp : Application(), SingletonImageLoader.Factory {
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Manually enable MultiDex before the rest of the app loads
        MultiDex.install(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    /**
     * Coil 3 Singleton Configuration
     * This ensures thumbnails and previews can be fetched over the network.
     */
    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                // Adds OkHttp support for Coil 3
                add(OkHttpNetworkLayerFactory())
            }
            // Optional: Add a crossfade for smoother thumbnail loading
            .crossfade(true)
            .build()
    }
    
    companion object {
        lateinit var instance: AggregatorXApp
            private set
    }
}
