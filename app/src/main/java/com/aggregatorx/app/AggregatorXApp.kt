package com.aggregatorx.app

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkLayerFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp

/**
 * AggregatorX - Advanced Multi-Provider Web Scraping Aggregator
 */
@HiltAndroidApp
class AggregatorXApp : Application(), SingletonImageLoader.Factory {
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkLayerFactory())
            }
            .crossfade(true)
            .build()
    }
    
    companion object {
        lateinit var instance: AggregatorXApp
            private set
    }
}
