package com.sperance.exileforge.presentation.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

/**
 * Сеть устройства (3.95.2), пока игра на экране: появившаяся сеть - Wi-Fi после мобильной, связь после метро, сеть после
 * сна - сразу будит связь с сервером ([onAvailable]), не дожидаясь паузы повтора или таймаута первого запроса.
 */
class NetworkWatch(context: Context, private val onAvailable: () -> Unit) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        if (callback != null || manager == null) return
        val watching = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onAvailable()
        }
        runCatching { manager.registerDefaultNetworkCallback(watching) }.onSuccess { callback = watching }
    }

    fun stop() {
        callback?.let { runCatching { manager?.unregisterNetworkCallback(it) } }
        callback = null
    }
}
