package dev.hipporasy.reachability

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build

actual class Reachability(private val context: Context) {



    actual fun connectionStatus(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val cap: NetworkCapabilities =
                    cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
                return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                val networks: Array<Network> = cm.allNetworks
                for (n in networks) {
                    val nInfo: NetworkInfo? = cm.getNetworkInfo(n)
                    if (nInfo?.isConnected == true) return true
                }
            }
            else -> {
                val networks: Array<NetworkInfo> = cm.allNetworkInfo
                for (nInfo in networks) {
                    if (nInfo.isConnected) return true
                }
            }
        }
        return false
    }

}
