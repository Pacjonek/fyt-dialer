package com.pacjonek.fyttoys.internal.ipc

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.RemoteException

/**
 * Minimal replica of the FYT `com.syu.ipc.IRemoteToolkit` binder interface,
 * exposed by `com.syu.ms/.app.ToolkitService` (action `com.syu.ms.toolkit`).
 */
interface IRemoteToolkit : IInterface {

    @Throws(RemoteException::class)
    fun getRemoteModule(moduleId: Int): IRemoteModule?

    abstract class Stub : Binder(), IRemoteToolkit {

        init {
            attachInterface(this, DESCRIPTOR)
        }

        override fun asBinder(): IBinder = this

        @Throws(RemoteException::class)
        public override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                TRANSACTION_getRemoteModule -> {
                    data.enforceInterface(DESCRIPTOR)
                    val moduleCode = data.readInt()
                    reply!!.writeNoException()
                    reply.writeStrongBinder(getRemoteModule(moduleCode)?.asBinder())
                    true
                }
                TRANSACTION_getDescriptor -> {
                    reply!!.writeString(DESCRIPTOR)
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }

        private class Proxy internal constructor(private val mRemote: IBinder) : IRemoteToolkit {
            override fun asBinder(): IBinder = mRemote

            @Throws(RemoteException::class)
            override fun getRemoteModule(moduleId: Int): IRemoteModule? {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeInt(moduleId)
                    mRemote.transact(TRANSACTION_getRemoteModule, data, reply, 0)
                    reply.readException()
                    IRemoteModule.Stub.asInterface(reply.readStrongBinder())
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }
        }

        companion object {
            private const val DESCRIPTOR = "com.syu.ipc.IRemoteToolkit"
            const val TRANSACTION_getRemoteModule = 1
            const val TRANSACTION_getDescriptor = Binder.INTERFACE_TRANSACTION

            fun asInterface(obj: IBinder?): IRemoteToolkit? {
                if (obj == null) return null
                return obj.queryLocalInterface(DESCRIPTOR) as? IRemoteToolkit ?: Proxy(obj)
            }
        }
    }
}