package com.dotpmm.stocker

import android.app.Application
import com.dotpmm.stocker.data.StockerStore
import com.dotpmm.stocker.work.RefreshScheduler

class StockerApp : Application() {
    val store: StockerStore by lazy { StockerStore(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        RefreshScheduler.schedule(this)
    }

    companion object {
        lateinit var instance: StockerApp
            private set
    }
}
