package com.vsca.vsnapvoicecollege.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide view of whether the device has a validated internet connection, used for
 * offline banners and to pause uploads / online-only previews.
 */
object NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    @Volatile
    private var started = false

    /** Starts listening once; safe to call from every screen that needs it. */
    fun start(context: Context) {
        if (started) return
        synchronized(this) {
            if (started) return
            val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
            _isOnline.value = manager.activeNetwork
                ?.let(manager::getNetworkCapabilities)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
            started = true
        }
    }
}
