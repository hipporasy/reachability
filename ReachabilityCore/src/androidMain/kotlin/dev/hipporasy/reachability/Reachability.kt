package dev.hipporasy.reachability

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import kotlinx.coroutines.flow.StateFlow

actual class Reachability() {
    actual fun status(): StateFlow<ReachabilityStatus> {
        TODO("Not yet implemented")
    }

    actual fun startNotifier() {
    }

    actual fun stopNotifier() {
    }

}
