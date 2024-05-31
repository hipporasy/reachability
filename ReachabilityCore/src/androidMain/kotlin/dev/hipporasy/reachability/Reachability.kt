package dev.hipporasy.reachability

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
