package com.dotpmm.stocker

import android.app.Application
import com.dotpmm.stocker.data.StockerStore
import com.dotpmm.stocker.settings.SettingsStore
import com.dotpmm.stocker.work.RefreshScheduler

/** Application shell: the single data store, the settings store, and the background refresh. */
class StockerApp : Application() {
    val store: StockerStore by lazy { StockerStore(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }

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
