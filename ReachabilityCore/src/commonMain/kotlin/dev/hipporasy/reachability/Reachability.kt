package dev.hipporasy.reachability

expect class Reachability {
    fun connectionStatus(): Boolean
}