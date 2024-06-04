@file:OptIn(ExperimentalForeignApi::class)

package dev.hipporasy.reachability

import cnames.structs.__SCNetworkReachability
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
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
import platform.Foundation.NSNotificationCenter
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
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsIsWWAN
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsTransientConnection
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.darwin.inet_aton
import platform.posix.AF_INET
import platform.posix.IN_LINKLOCALNETNUM
import platform.posix.in_addr
import platform.posix.sockaddr_in


const val kReachabilityChangedNotification = "ReachabilityChangedNotification"

class NativeReachability private constructor(private val reachableOnWWAN: Boolean = true) {

    private var reachabilitySerialQueue: dispatch_queue_t = dispatch_queue_t()
    private var reachabilityObject: NativeReachability? = null
    private var _reachabilityRef: SCNetworkReachabilityRef? = null

    fun finalize() {
        if (_reachabilityRef != null) {
            CFRelease(_reachabilityRef)
        }
    }

    init {
        reachabilitySerialQueue = dispatch_queue_create("dev.hipporasy.reachability", null)
    }

    var reachableBlock: NetworkReachable? = null
    var unreachableBlock: NetworkUnreachable? = null

    fun connectionStatus(): Boolean {
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

        fun reachabilityForLocalWiFi(): NativeReachability {
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
            var returnedValue: NativeReachability? = null

            if (reachability != null) {
                returnedValue = NativeReachability()
                returnedValue._reachabilityRef = reachability
            } else {
                CFRelease(reachability)
            }
            return returnedValue!!
        }

        fun reachabilityForInternetConnection(): NativeReachability {
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
            var returnedValue: NativeReachability? = null

            if (reachability != null) {
                returnedValue = NativeReachability(reachableOnWWAN = true)
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
            val reachability = info?.asStableRef<NativeReachability>()?.get()
            reachability?.reachabilityChanged(flags)
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
            context.info = StableRef.create(this@NativeReachability).asCPointer()
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
                    reachabilityObject = this@NativeReachability
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
        dispatch_async(dispatch_get_main_queue()) {
            NSNotificationCenter.defaultCenter()
                .postNotificationName(kReachabilityChangedNotification, this)
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

    fun isReachableViaWiFi(): Boolean {
        if (isReachable()) {
            val flags = getFlags() ?: return false
            return (flags and kSCNetworkReachabilityFlagsIsWWAN) == 0u
        }
        return false
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

}

typealias NetworkUnreachable = (NativeReachability) -> Unit

typealias NetworkReachable = (NativeReachability) -> Unit
