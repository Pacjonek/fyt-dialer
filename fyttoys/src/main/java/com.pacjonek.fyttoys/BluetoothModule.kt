package com.pacjonek.fyttoys

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pacjonek.fyttoys.internal.FytBtAdapter
import com.pacjonek.fyttoys.internal.ToolkitConnection
import com.pacjonek.fyttoys.internal.ipc.IModuleCallback
import com.pacjonek.fyttoys.internal.ipc.IRemoteToolkit

/**
 * FYT internal Bluetooth module facade library
 * Current status: provides interface for making the phone calls using HFP
 * and for reading connected-phone info (e.g. the phone name).
 *
 * Clients only see [dialNumber], [requestPhoneName] and [close] — they never
 * touch binders, module codes, or FYT command codes. All of that is handled
 * internally by [ToolkitConnection] (service plumbing) and [FytBtAdapter]
 * (protocol).
 *
 * Typical usage from an Android Activity:
 * ```
 * val module = BluetoothModule.get(context)
 * module.dialNumber("+48123456789") { success ->
 *     Log.d(TAG,"Connection successful:  $success")
 *     module.disconnect()
 * }
 * ```
 *
 * Threading: [dialNumber] and [requestPhoneName] may be invoked from any thread;
 * listeners are always invoked on the main thread.
 */
class BluetoothModule private constructor(appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun interface DialObserver {
        /** @param success true when the call request was delivered to the FYT BT module. */
        fun onDialResult(success: Boolean)
    }

    fun interface PhoneNameObserver {
        /** @param name name of the connected phone, or null when unavailable. */
        fun onPhoneNameUpdate(name: String?)
    }

    private class PendingDial(val number: String, val observer: DialObserver?)

    /**
     * Pending phone name request.
     * @param observer - Callback to invoke with the phone name.
     * @param once - Unregister after first update.
     */
    private class PendingPhoneName(val observer: PhoneNameObserver?, val once: Boolean = false) {
        var toolkit: IRemoteToolkit? = null
        var callback: IModuleCallback? = null
        var timeout: Runnable? = null
    }

    private val connection = ToolkitConnection(appContext).apply {
        observer = object : ToolkitConnection.Observer {
            override fun onToolkitReady(toolkit: IRemoteToolkit) {

                /* Flush all previous awaiting calls if exist */
                flushPendingDial(toolkit)
                flushPendingPhoneNameRequest(toolkit)
            }

            override fun onToolkitUnavailable() {
                /* Flush all previous awaiting calls if exist and mark them as failed */
                failPendingDial()
                failPendingPhoneNameRequest()
            }
        }
    }

    @Volatile
    private var pendingDial: PendingDial? = null
    @Volatile
    private var pendingPhoneName: PendingPhoneName? = null


    /**
     * Initiates an outgoing call to [number] via the phone connected over
     * Bluetooth HFP. Binds to the FYT main server on demand; the observer is
     * invoked once the request has been delivered (or has failed).
     */
    fun dialNumber(number: String, observer: DialObserver? = null) {
        mainHandler.post {
            if (number.isBlank()) {
                Log.w(TAG, "dialNumber() with empty number, ignoring")
                observer?.onDialResult(false)
                return@post
            }
            val toolkit = connection.toolkitOrNull()
            if (toolkit != null) {
                deliverDialResult(FytBtAdapter(toolkit).dial(number), observer)
            } else {
                pendingDial = PendingDial(number, observer)
                connection.connect()
            }
        }
    }

    /**
     * Request the name of the phone currently connected over Bluetooth HFP once.
     * Binds to the FYT main server on demand; the observer is invoked with the
     * name, or with null when the name is unavailable (no phone connected,
     * module unreachable or request failed).
     *
     * Hint: The FYT BT module does not answer `get()` requests — it pushes state to
     * registered callbacks. This registers for `U_PHONE_NAME`, waits for the
     * first update (with a timeout), then unregisters.
     */
    fun requestPhoneName(onResult: PhoneNameObserver) {
        mainHandler.post {
            cancelPhoneNameRequest()
            val request = PendingPhoneName(onResult, true)
            pendingPhoneName = request
            val toolkit = connection.toolkitOrNull()
            if (toolkit != null) {
                addPhoneNameUpdateObserver(request, toolkit)
            } else {
                connection.connect()
            }
        }
    }

    private fun addPhoneNameUpdateObserver(request: PendingPhoneName, toolkit: IRemoteToolkit) {
        /**
         * Single-phone: strs[0] = name;
         * "Dual-phone" (whatever that means): strs[0] = phone index, strs[1] = name.
         */
        fun parsePhoneName(strs: Array<String?>?): String? {
            if (strs.isNullOrEmpty()) return null
            val phoneName = if (strs.size >= 2 && strs[0]?.toIntOrNull() in 0..1) strs[1]
            else strs[0]
            return phoneName
        }
        val callback = object : IModuleCallback.Stub() {
            override fun update(updatedCode: Int, ints: IntArray?, flts: FloatArray?, strs: Array<String?>?) {
                if (updatedCode != FytBtAdapter.U_PHONE_NAME) return
                mainHandler.post { completePhoneNameRequest(request, parsePhoneName(strs)) }
            }
        }
        request.toolkit = toolkit
        request.callback = callback
        if (!FytBtAdapter(toolkit).registerPhoneNameObserver(callback)) {
            Log.w(TAG, "Failed to register phone name observer")
            completePhoneNameRequest(request, null)
            return
        }
        request.timeout = Runnable { completePhoneNameRequest(request, null) }
            .also { mainHandler.postDelayed(it, 1000) }
    }

    private fun completePhoneNameRequest(request: PendingPhoneName, name: String?) {
        if (pendingPhoneName !== request) return
        pendingPhoneName = null
        request.timeout?.let { mainHandler.removeCallbacks(it) }
        if(request.once){
            removePhoneNameUpdateObserver(request)
        }
        val observer = request.observer
        if(observer != null){
            deliverPhoneNameValue(name, observer)
        }
    }

    private fun cancelPhoneNameRequest() {
        val request = pendingPhoneName ?: return
        pendingPhoneName = null
        request.timeout?.let { mainHandler.removeCallbacks(it) }
        removePhoneNameUpdateObserver(request)
    }

    private fun removePhoneNameUpdateObserver(request: PendingPhoneName) {
        val toolkit = request.toolkit ?: return
        val callback = request.callback ?: return
        request.toolkit = null
        request.callback = null
        FytBtAdapter(toolkit).unregisterPhoneNameObserver(callback)
    }

    private fun deliverDialResult(success: Boolean, observer: DialObserver?) {
        mainHandler.post { observer?.onDialResult(success) }
    }

    private fun deliverPhoneNameValue(phoneName: String?, observer: PhoneNameObserver) {
        mainHandler.post { observer.onPhoneNameUpdate(phoneName) }
    }

    private fun flushPendingDial(toolkit: IRemoteToolkit) {
        val pending = pendingDial ?: return
        pendingDial = null
        deliverDialResult(FytBtAdapter(toolkit).dial(pending.number), pending.observer)
    }

    private fun flushPendingPhoneNameRequest(toolkit: IRemoteToolkit) {
        val request = pendingPhoneName ?: return
        if (request.callback != null) return
        addPhoneNameUpdateObserver(request, toolkit)
    }

    private fun failPendingDial() {
        val pending = pendingDial ?: return
        pendingDial = null
        deliverDialResult(false, pending.observer)
    }

    private fun failPendingPhoneNameRequest() {
        val request = pendingPhoneName ?: return
        completePhoneNameRequest(request, null)
    }

    /** Disconnects from the FYT main server. Safe to call multiple times. */
    fun close() {
        mainHandler.post {
            cancelPhoneNameRequest()
            failPendingDial()
            connection.release()
        }
    }

    companion object {
        private const val TAG = "FytToysBluetoothModule"
        // private const val REQUEST_TIMEOUT_MS = 3000L

        @Volatile
        private var instance: BluetoothModule? = null

        /** Shared instance; safe to call repeatedly (cheap after first call). */
        fun get(context: Context): BluetoothModule =
            instance ?: synchronized(this) {
                instance ?: BluetoothModule(context.applicationContext).also { instance = it }
            }
    }
}
