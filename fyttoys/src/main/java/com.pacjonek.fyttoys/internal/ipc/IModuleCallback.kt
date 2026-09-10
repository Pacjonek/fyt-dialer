package com.pacjonek.fyttoys.internal.ipc

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.RemoteException

/**
 * Minimal replica of the FYT `com.syu.ipc.IModuleCallback` binder interface.
 * Implemented by clients that want module state updates pushed back to them.
 */
interface IModuleCallback : IInterface {

    @Throws(RemoteException::class)
    fun update(updatedCode: Int, ints: IntArray?, flts: FloatArray?, strs: Array<String?>?)

    abstract class Stub : Binder(), IModuleCallback {

        init {
            attachInterface(this, DESCRIPTOR)
        }

        override fun asBinder(): IBinder = this

        @Throws(RemoteException::class)
        public override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                TRANSACTION_update -> {
                    data.enforceInterface(DESCRIPTOR)
                    update(data.readInt(), data.createIntArray(), data.createFloatArray(), data.createStringArray())
                    true
                }
                TRANSACTION_getDescriptor -> {
                    reply!!.writeString(DESCRIPTOR)
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }

        private class Proxy(private val mRemote: IBinder) : IModuleCallback {
            override fun asBinder(): IBinder = mRemote

            @Throws(RemoteException::class)
            override fun update(updatedCode: Int, ints: IntArray?, flts: FloatArray?, strs: Array<String?>?) {
                val data = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeInt(updatedCode)
                    data.writeIntArray(ints)
                    data.writeFloatArray(flts)
                    data.writeStringArray(strs)
                    mRemote.transact(TRANSACTION_update, data, null, FLAG_ONEWAY)
                } finally {
                    data.recycle()
                }
            }
        }

        companion object {
            private const val DESCRIPTOR = "com.syu.ipc.IModuleCallback"
            const val TRANSACTION_update = 1
            const val TRANSACTION_getDescriptor = Binder.INTERFACE_TRANSACTION

            fun asInterface(obj: IBinder?): IModuleCallback? {
                if (obj == null) return null
                return obj.queryLocalInterface(DESCRIPTOR) as? IModuleCallback ?: Proxy(obj)
            }
        }
    }
}
