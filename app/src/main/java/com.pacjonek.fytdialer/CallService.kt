package com.pacjonek.fytdialer

import android.content.Context
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import android.widget.Toast
import com.pacjonek.fyttoys.BluetoothModule

/**
 * System apps can dial, skipping the dialer activity, just using this service
 */
class CallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val uri = call.details.handle
        val phoneNumber = uri.schemeSpecificPart

        // Prevents "real" call using SIM card in head unit
        call.disconnect()

        dialUsingBluetoothHfp(this, phoneNumber)

    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
    }

    companion object {
        private const val TAG = "FytCallService"

        fun dialUsingBluetoothHfp(context: Context, phoneNumber: String){
            val bluetoothModule = BluetoothModule.get(context)
            bluetoothModule.requestPhoneName { phoneName ->
                if(phoneName != null){
                    val phoneNameMessage = "📲 Calling with HFP using '$phoneName'"
                    Toast.makeText(context, phoneNameMessage, Toast.LENGTH_LONG).show()
                }
            }
            bluetoothModule.dialNumber(phoneNumber) { success ->
                val dialMessage = "Dial request ${if (success) "delivered" else "failed"}"
                Log.d(TAG, dialMessage)
                if(!success){
                    Toast.makeText(context, dialMessage, Toast.LENGTH_LONG).show()
                }
                bluetoothModule.close()
            }
        }

    }
}
