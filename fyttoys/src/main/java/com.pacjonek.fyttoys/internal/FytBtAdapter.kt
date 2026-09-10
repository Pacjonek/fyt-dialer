package com.pacjonek.fyttoys.internal

import android.os.RemoteException
import android.util.Log
import com.pacjonek.fyttoys.internal.ipc.IModuleCallback
import com.pacjonek.fyttoys.internal.ipc.IRemoteModule
import com.pacjonek.fyttoys.internal.ipc.IRemoteToolkit

/**
 * Adapter: translates high-level phone operations into FYT Bluetooth module
 * binder commands. This is the only class that knows FYT module/command codes.
 *
 *  Protocol reference (from `com.syu.bt/FinalBt`):
 *  - BT module code: 2 (MODULE_CODE_BT)
 *  - cmd 7  (C_DIAL):    dial number,        strs[0] = number
 *  - cmd 8  (C_REDIAL):  redial last number
 *  - cmd 9  (C_PICKUP):  answer incoming call
 *  - cmd 10 (C_HANG):    hang up current call
 *
 *  Update codes (`U_*` in `com.syu.bt`) live in their own namespace, separate
 *  from both cmd and get codes, so the same number can mean different things:
 *  - update 7 (U_PHONE_NAME): name of the connected phone
 *
 *  FYT never reads BT state via `get()` — the BT module server does not
 *  implement it. Clients register an [IModuleCallback] and receive pushed
 *  `update(U_PHONE_NAME, ...)` callbacks. Payload shapes seen in FYT code:
 *  - single-phone: strs[0] = name
 *  - dual-phone:   strs[0] = phone index ("0"/"1"), strs[1] = name
 *
 * Note: the BT module only dials while HFP is connected
 * (U_PHONE_STATE (update code 9) == 2 "connected" or 4 "ringing").
 * The module silently rejects the command otherwise.
 */
internal class FytBtAdapter(private val toolkit: IRemoteToolkit) {

    private fun getBtRemoteModule(): IRemoteModule? = try {
        toolkit.getRemoteModule(MODULE_ID)
    } catch (e: RemoteException) {
        Log.e(TAG, "Failed to get FYT BT remote module", e)
        null
    }

    /** @return true when the dial command was delivered to the BT module. */
    fun dial(number: String): Boolean {
        val module = getBtRemoteModule() ?: return false
        return try {
            module.cmd(CMD_DIAL, null, null, arrayOf(number))
            Log.d(TAG, "Dial cmd sent: $number")
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "Dial cmd failed", e)
            false
        }
    }

    fun hangUp(): Boolean = sendSimpleCmd(CMD_HANG)

    fun answer(): Boolean = sendSimpleCmd(CMD_PICKUP)

    fun redial(): Boolean = sendSimpleCmd(CMD_REDIAL)

    /**
     * Subscribes [callback] to connected-phone-name updates (U_PHONE_NAME).
     * The BT module pushes the current name to registered callbacks.
     * @return true when the registration was delivered to the BT module.
     */
    fun registerPhoneNameObserver(callback: IModuleCallback): Boolean {
        val module = getBtRemoteModule() ?: return false
        return try {
            module.register(callback, U_PHONE_NAME, REGISTER_UPDATE_PARAM)
            Log.d(TAG, "Registered for phone name updates")
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "Phone name registration failed", e)
            false
        }
    }

    fun unregisterPhoneNameObserver(callback: IModuleCallback): Boolean {
        val module = getBtRemoteModule() ?: return false
        return try {
            module.unregister(callback, U_PHONE_NAME)
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "Phone name unregistration failed", e)
            false
        }
    }

    private fun sendSimpleCmd(cmdCode: Int): Boolean {
        val module = getBtRemoteModule() ?: return false
        return try {
            module.cmd(cmdCode, null, null, null)
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "Cmd $cmdCode failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "FytBtAdapter"

        const val MODULE_ID = 2

        /**
         *  Dial number command (`C_DIAL`)
         */
        const val CMD_DIAL = 7
        const val CMD_REDIAL = 8
        const val CMD_PICKUP = 9
        const val CMD_HANG = 10

        /** Update code for the connected phone's name (`U_PHONE_NAME` in `com.syu.bt`). */
        const val U_PHONE_NAME = 7

        /** FYT always passes 1 as updateParam (notify with the current value). */
        const val REGISTER_UPDATE_PARAM = 1
    }
}
