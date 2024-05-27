package dev.hipporasy.reachability

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class Reachability(
    private val reachability: OSReachability = OSReachability.reachabilityForInternetConnection()
) {
    private var status: MutableStateFlow<ReachabilityStatus> =
        MutableStateFlow(ReachabilityStatus.Unknown)

    actual fun status(): StateFlow<ReachabilityStatus> = status

    actual fun startNotifier() {
        reachability.unreachableBlock = {
            dispatch_async(dispatch_get_main_queue()) {
                status.value = ReachabilityStatus.NotReachable
            }
        }
        reachability.reachableBlock = { reachability ->
            dispatch_async(dispatch_get_main_queue()) {
                status.value = when {
                    reachability.isReachableViaWiFi() -> ReachabilityStatus.ReachableViaWiFi
                    reachability.isReachable() -> ReachabilityStatus.Reachable
                    !reachability.isReachable() -> ReachabilityStatus.NotReachable
                    else -> ReachabilityStatus.Unknown
                }
            }
        }
        reachability.startNotifier()
    }

    actual fun stopNotifier() {
        reachability.stopNotifier()
    }

}


