package com.dotpmm.stocker

import android.app.Application

/**
 * Test application that skips the real [StockerApp] onCreate, which schedules WorkManager.
 * Compose rendering tests do not need a background worker.
 */
class TestApp : Application()
