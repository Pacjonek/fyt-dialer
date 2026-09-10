package com.pacjonek.fyttoys.internal.ipc

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.RemoteException

/**
 * Minimal replica of the FYT `com.syu.ipc.IRemoteModule` binder interface.
 * One instance per FYT module (module ids: MAIN = 0, BT = 2, CANBUS = 7, ...)
 */
interface IRemoteModule : IInterface {

    @Throws(RemoteException::class)
    fun cmd(cmdCode: Int, ints: IntArray?, flts: FloatArray?, strs: Array<String?>?)

    @Throws(RemoteException::class)
    fun register(moduleCallback: IModuleCallback?, updateCode: Int, notifyFlag: Int)

    @Throws(RemoteException::class)
    fun unregister(moduleCallback: IModuleCallback?, updateCode: Int)

    @Throws(RemoteException::class)
    fun get(getCode: Int, ints: IntArray?, flts: FloatArray?, strs: Array<String?>?): ModuleObject?

    abstract class Stub : Binder(), IRemoteModule {

        init {
            attachInterface(this, DESCRIPTOR)
        }

        override fun asBinder(): IBinder = this

        @Throws(RemoteException::class)
        public override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                TRANSACTION_cmd -> {
                    data.enforceInterface(DESCRIPTOR)
                    cmd(data.readInt(), data.createIntArray(), data.createFloatArray(), data.createStringArray())
                    true
                }
                TRANSACTION_get -> {
                    data.enforceInterface(DESCRIPTOR)
                    val result = get(data.readInt(), data.createIntArray(), data.createFloatArray(), data.createStringArray())
                    reply!!.writeNoException()
                    if (result != null) {
                        reply.writeInt(1)
                        reply.writeIntArray(result.ints)
                        reply.writeFloatArray(result.flts)
                        reply.writeStringArray(result.strs)
                    } else {
                        reply.writeInt(0)
                    }
                    true
                }
                TRANSACTION_register -> {
                    data.enforceInterface(DESCRIPTOR)
                    register(IModuleCallback.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt())
                    true
                }
                TRANSACTION_unregister -> {
                    data.enforceInterface(DESCRIPTOR)
                    unregister(IModuleCallback.Stub.asInterface(data.readStrongBinder()), data.readInt())
                    true
                }
                TRANSACTION_getDescriptor -> {
                    reply!!.writeString(DESCRIPTOR)
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }

        private class Proxy constructor(private val mRemote: IBinder) : IRemoteModule {
            override fun asBinder(): IBinder = mRemote

            @Throws(RemoteException::class)
            override fun cmd(cmdCode: Int, ints: IntArray?, flts: FloatArray?, strs: Array<String?>?) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeInt(cmdCode)
                    data.writeIntArray(ints)
                    data.writeFloatArray(flts)
                    data.writeStringArray(strs)
                    mRemote.transact(TRANSACTION_cmd, data, reply, FLAG_ONEWAY)
                    reply.readException()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }

            @Throws(RemoteException::class)
            override fun get(getCode: Int, ints: IntArray?, flts: FloatArray?, strs: Array<String?>?): ModuleObject? {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeInt(getCode)
                    data.writeIntArray(ints)
                    data.writeFloatArray(flts)
                    data.writeStringArray(strs)
                    mRemote.transact(TRANSACTION_get, data, reply, 0)
                    reply.readException()
                    if (reply.readInt() != 0) {
                        ModuleObject().apply {
                            this.ints = reply.createIntArray()
                            this.flts = reply.createFloatArray()
                            this.strs = reply.createStringArray()
                        }
                    } else {
                        null
                    }
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }

            @Throws(RemoteException::class)
            override fun register(moduleCallback: IModuleCallback?, updateCode: Int, notifyFlag: Int) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(moduleCallback?.asBinder())
                    data.writeInt(updateCode)
                    data.writeInt(notifyFlag)
                    mRemote.transact(TRANSACTION_register, data, reply, FLAG_ONEWAY)
                    reply.readException()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }

            @Throws(RemoteException::class)
            override fun unregister(moduleCallback: IModuleCallback?, updateCode: Int) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStrongBinder(moduleCallback?.asBinder())
                    data.writeInt(updateCode)
                    mRemote.transact(TRANSACTION_unregister, data, reply, FLAG_ONEWAY)
                    reply.readException()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }
        }

        companion object {
            private const val DESCRIPTOR = "com.syu.ipc.IRemoteModule"
            const val TRANSACTION_cmd = 1
            const val TRANSACTION_get = 2
            const val TRANSACTION_register = 3
            const val TRANSACTION_unregister = 4
            const val TRANSACTION_getDescriptor = Binder.INTERFACE_TRANSACTION

            // const val REGISTER_NOTIFY_FLAG = 1

            fun asInterface(obj: IBinder?): IRemoteModule? {
                if (obj == null) return null
                return obj.queryLocalInterface(DESCRIPTOR) as? IRemoteModule ?: Proxy(obj)
            }
        }
    }
}
