package com.pacjonek.fyttoys.internal.ipc

/**
 * Minimal replica of the FYT `com.syu.ipc.ModuleObject` parcel layout (ints / flts / strs).
 * Payload container returned by [IRemoteModule.get].
 */
class ModuleObject {
    var ints: IntArray? = null
    var flts: FloatArray? = null
    var strs: Array<String?>? = null

//    companion object {
//        fun checkInts(obj: ModuleObject?, min: Int): Boolean =
//            (obj?.ints?.size ?: 0) >= min
//
//        operator fun get(obj: ModuleObject?, valueIfNotOk: Int): Int =
//            obj?.ints?.firstOrNull() ?: valueIfNotOk
//
//        operator fun get(obj: ModuleObject?, valueIfNotOk: String): String =
//            obj?.strs?.firstOrNull() ?: valueIfNotOk
//
//        operator fun get(proxy: RemoteModuleProxy, getCode: Int, valueIfNotOk: Int): Int =
//            proxy[getCode, null, null, null]?.ints?.firstOrNull() ?: valueIfNotOk
//    }
}