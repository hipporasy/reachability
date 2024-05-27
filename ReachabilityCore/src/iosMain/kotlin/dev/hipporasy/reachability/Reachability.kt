package dev.hipporasy.reachability

import cnames.structs.__SCNetworkReachability
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.SystemConfiguration.SCNetworkReachabilityContext
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithAddress
import platform.SystemConfiguration.SCNetworkReachabilityFlags
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.SCNetworkReachabilityRef
import platform.SystemConfiguration.SCNetworkReachabilitySetCallback
import platform.SystemConfiguration.SCNetworkReachabilitySetDispatchQueue
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionOnDemand
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionOnTraffic
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsInterventionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsIsDirect
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsIsLocalAddress
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsIsWWAN
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsTransientConnection
import platform.darwin.dispatch_queue_t
import platform.darwin.inet_aton
import platform.posix.AF_INET
import platform.posix.IN_LINKLOCALNETNUM
import platform.posix.in_addr
import platform.posix.sockaddr_in

@OptIn(ExperimentalForeignApi::class)
actual class Reachability {

    var reachabilitySerialQueue: dispatch_queue_t = null
    var reachabilityObject: Reachability? = null

    private var _reachabilityRef: SCNetworkReachabilityRef? = null

    fun finalize() {
        if (_reachabilityRef != null) {
            CFRelease(_reachabilityRef)
        }
    }

    val reachableBlock: NetworkReachable? = null
    val unreachableBlock: NetworkUnreachable? = null

    actual fun connectionStatus(): Boolean {
        val flags = getFlags() ?: return false

        if ((flags and kSCNetworkReachabilityFlagsReachable) == 0u) {
            return false
        }

        if ((flags and kSCNetworkReachabilityFlagsConnectionRequired) == 0u) {
            return true
        }

        if ((((flags and kSCNetworkReachabilityFlagsConnectionOnDemand) != 0u) ||
                    (flags and kSCNetworkReachabilityFlagsConnectionOnTraffic) != 0u)
        ) {
            if ((flags and kSCNetworkReachabilityFlagsInterventionRequired) == 0u) {
                return true
            }
        }

        if ((flags and kSCNetworkReachabilityFlagsIsWWAN) == kSCNetworkReachabilityFlagsIsWWAN) {
            return true
        }
        return false
    }

    private fun getFlags(): UInt? {
        return memScoped {
            val flag = alloc<UIntVar>()
            if (!SCNetworkReachabilityGetFlags(_reachabilityRef, flag.ptr)) {
                return null
            }
            flag.value
        }
    }

    companion object {
        fun reachabilityFlags(flags: UInt): String {
            val wan = if ((flags and kSCNetworkReachabilityFlagsIsWWAN) != 0u) 'W' else '-'
            val r = if ((flags and kSCNetworkReachabilityFlagsReachable) != 0u) 'R' else '-'
            val c =
                if ((flags and kSCNetworkReachabilityFlagsConnectionRequired) != 0u) 'c' else '-'
            val t =
                if ((flags and kSCNetworkReachabilityFlagsTransientConnection) != 0u) 't' else '-'
            val i =
                if ((flags and kSCNetworkReachabilityFlagsInterventionRequired) != 0u) 'i' else '-'
            val cC =
                if ((flags and kSCNetworkReachabilityFlagsConnectionOnTraffic) != 0u) 'C' else '-'
            val dD =
                if ((flags and kSCNetworkReachabilityFlagsConnectionOnDemand) != 0u) 'D' else '-'
            val l = if ((flags and kSCNetworkReachabilityFlagsIsLocalAddress) != 0u) 'l' else '-'
            val d = if ((flags and kSCNetworkReachabilityFlagsIsDirect) != 0u) 'd' else '-'

            return "$wan$r $c$t$i$cC$dD$l$d"
        }

        fun reachabilityForLocalWiFi(): Reachability {
            val localWifiAddress = cValue<sockaddr_in> {
                sin_len = sizeOf<sockaddr_in>().convert()
                sin_family = AF_INET.convert()
                sin_addr.s_addr = IN_LINKLOCALNETNUM.convert()
            }
            val reachability = memScoped {
                SCNetworkReachabilityCreateWithAddress(
                    kCFAllocatorDefault,
                    localWifiAddress.ptr.reinterpret()
                )
            }
            var returnedValue: Reachability? = null

            if (reachability != null) {
                returnedValue = Reachability()
                returnedValue._reachabilityRef = reachability
            } else {
                CFRelease(reachability)
            }
            return returnedValue!!
        }

        fun reachabilityForInternetConnection(): Reachability {
            val zeroAddress = cValue<sockaddr_in> {
                sin_len = sizeOf<sockaddr_in>().convert()
                sin_family = AF_INET.convert()
            }
            val reachability = memScoped {
                SCNetworkReachabilityCreateWithAddress(
                    kCFAllocatorDefault,
                    zeroAddress.ptr.reinterpret()
                )
            }
            var returnedValue: Reachability? = null

            if (reachability != null) {
                returnedValue = Reachability()
                returnedValue._reachabilityRef = reachability
            } else {
                CFRelease(reachability)
            }
            return returnedValue!!
        }

        fun tmReachabilityCallback(
            target: CPointer<__SCNetworkReachability>?,
            flags: UInt,
            info: CPointer<out CPointed>?
        ) {
            val reachability = info?.asStableRef<Reachability>()!!.get()
            reachability.reachabilityChanged(flags)
        }
    }


    fun isIpAddress(host: String): Boolean {
        memScoped {
            val pin = alloc<in_addr>()
            return 1 == inet_aton(host, pin.ptr)
        }
    }


    fun startNotifier(): Boolean {
        // allow start notifier to be called multiple times
        reachabilityObject?.let {
            if (it == this) {
                return true
            }
        }
        memScoped {
            val context = alloc<SCNetworkReachabilityContext> {
                version = 0
                info = null
                retain = null
                release = null
            }
            val result = SCNetworkReachabilitySetCallback(
                target = _reachabilityRef,
                callout = staticCFunction { target, flags, info ->
                    tmReachabilityCallback(target, flags, info)
                },
                context = context.ptr
            )
            if (result) {
                if (SCNetworkReachabilitySetDispatchQueue(
                        target = _reachabilityRef,
                        queue = reachabilitySerialQueue
                    )
                ) {
                    reachabilityObject = this@Reachability
                    return true
                } else {
                    SCNetworkReachabilitySetCallback(_reachabilityRef, null, null)
                }
            } else {
                println("SCNetworkReachabilitySetCallback() failed")
            }
            // if we get here we fail at the internet
            reachabilityObject = null
            return false
        }
    }

    fun stopNotifier() {
        SCNetworkReachabilitySetCallback(_reachabilityRef, null, null)
        SCNetworkReachabilitySetDispatchQueue(_reachabilityRef, null)
        reachabilityObject = null
    }

    fun reachabilityChanged(flags: SCNetworkReachabilityFlags) {
        if (isReachableWithFlags(flags)) {
            reachableBlock?.let { it(this) }
        } else {
            unreachableBlock?.let { it(this) }
        }

    }


    private fun isReachableWithFlags(flags: SCNetworkReachabilityFlags): Boolean {
        var connectionUP = true

        if ((flags and kSCNetworkReachabilityFlagsReachable) == 0u) {
            connectionUP = false
        }

        if ((flags and kSCNetworkReachabilityFlagsIsWWAN) == 0u) {
            // We're on 3G.
            if (!this.reachableOnWWAN) {
                // We don't want to connect when on 3G.
                connectionUP = false
            }
        }

        return connectionUP
    }

    fun isReachable(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and kSCNetworkReachabilityFlagsReachable) != 0u
    }

    fun isConnectionRequired(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and kSCNetworkReachabilityFlagsConnectionRequired) != 0u
    }

    fun isConnectionOnDemand(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and kSCNetworkReachabilityFlagsConnectionOnDemand) != 0u
    }

    fun isInterventionRequired(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and kSCNetworkReachabilityFlagsInterventionRequired) != 0u
    }

    fun isConnectionOnTraffic(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and kSCNetworkReachabilityFlagsConnectionOnTraffic) != 0u
    }

    fun isConnectionOnDemandOrTraffic(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and (kSCNetworkReachabilityFlagsConnectionOnDemand or kSCNetworkReachabilityFlagsConnectionOnTraffic)) != 0u
    }

    fun isTransientConnection(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and kSCNetworkReachabilityFlagsTransientConnection) != 0u
    }

    fun isDirect(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and kSCNetworkReachabilityFlagsIsDirect) != 0u
    }

    fun isWWAN(): Boolean {
        val flags = getFlags() ?: return false
        return (flags and kSCNetworkReachabilityFlagsIsWWAN) != 0u
    }

    private var reachableOnWWAN: Boolean = false

}

typealias NetworkUnreachable = (Reachability) -> Unit

typealias NetworkReachable = (Reachability) -> Unit

