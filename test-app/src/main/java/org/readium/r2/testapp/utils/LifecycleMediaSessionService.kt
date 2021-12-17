package org.readium.r2.testapp.utils

import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.media2.session.MediaSessionService
import androidx.lifecycle.Lifecycle

import androidx.annotation.CallSuper

import android.content.Intent

import android.os.IBinder
import androidx.lifecycle.LifecycleOwner


abstract class LifecycleMediaSessionService : MediaSessionService(), LifecycleOwner {

    @Suppress("LeakingThis")
    private val lifecycleDispatcher = ServiceLifecycleDispatcher(this)

    @CallSuper
    override fun onCreate() {
        lifecycleDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    @CallSuper
    override fun onBind(intent: Intent): IBinder? {
        lifecycleDispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    @CallSuper
    override fun onStart(intent: Intent?, startId: Int) {
        lifecycleDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    // this method is added only to annotate it with @CallSuper.
    // In usual service super.onStartCommand is no-op, but in LifecycleService
    // it results in mDispatcher.onServicePreSuperOnStart() call, because
    // super.onStartCommand calls onStart().
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onDestroy() {
        lifecycleDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleDispatcher.lifecycle
    }
}