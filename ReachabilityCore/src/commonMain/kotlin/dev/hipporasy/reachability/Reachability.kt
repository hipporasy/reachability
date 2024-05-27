package dev.hipporasy.reachability

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

expect class Reachability {

    fun status(): StateFlow<ReachabilityStatus>
    fun startNotifier()
    fun stopNotifier()

}

enum class ReachabilityStatus {
    Unknown,
    NotReachable,
    ReachableViaWiFi,
    Reachable
}

fun Reachability.isReachable(): Flow<Boolean> {
    return status().map { it != ReachabilityStatus.NotReachable || it != ReachabilityStatus.Unknown }
}

