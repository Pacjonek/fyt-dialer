package com.pacjonek.fyttoys.internal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.pacjonek.fyttoys.internal.ipc.IRemoteToolkit

/**
 * Pure binder plumbing: binds to the FYT main server ToolkitService and hands
 * out the [IRemoteToolkit]
 */
internal class ToolkitConnection(private val appContext: Context) {

    internal interface Observer {
        fun onToolkitReady(toolkit: IRemoteToolkit)
        fun onToolkitUnavailable()
    }

    var observer: Observer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var toolkit: IRemoteToolkit? = null
    private var bound = false

    private val timeout = Runnable {
        Log.w(TAG, "Timed out waiting for $TOOLKIT_URI ($CONNECT_TIMEOUT_MS ms)")
        observer?.onToolkitUnavailable()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            handler.removeCallbacks(timeout)
            toolkit = IRemoteToolkit.Stub.asInterface(service)
            val tool = toolkit
            if (tool != null) {
                Log.d(TAG, "Connected successfully to $TOOLKIT_URI")
                observer?.onToolkitReady(tool)
            } else {
                observer?.onToolkitUnavailable()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "Disconnected from $TOOLKIT_URI")
            toolkit = null
            bound = false
            observer?.onToolkitUnavailable()
        }
    }

    /** Current toolkit, or null when not connected. */
    fun toolkitOrNull(): IRemoteToolkit? = toolkit

    fun connect() {
        if (toolkit != null || bound) return
        val intent = Intent(TOOLKIT_ACTION).setComponent(
            ComponentName(TOOLKIT_PACKAGE, TOOLKIT_SERVICE_CLASS)
        )
        bound = try {
            appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind to $TOOLKIT_URI using $TOOLKIT_ACTION action", e)
            false
        }
        if (bound) {
            handler.postDelayed(timeout, CONNECT_TIMEOUT_MS)
        } else {
            observer?.onToolkitUnavailable()
        }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        if (bound) {
            try {
                appContext.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unbind bounded service connection", e)
            }
            bound = false
        }
        toolkit = null
    }

    companion object {
        private const val TAG = "FytToysToolkitConnection"
        private const val TOOLKIT_SERVICE_CLASS = "app.ToolkitService"
        private const val TOOLKIT_PACKAGE = "com.syu.ms"
        private const val TOOLKIT_ACTION = "$TOOLKIT_PACKAGE.toolkit"
        private const val TOOLKIT_URI = "$TOOLKIT_PACKAGE/.$TOOLKIT_SERVICE_CLASS"
        private const val CONNECT_TIMEOUT_MS = 5000L
    }
}
