package com.pacjonek.fytdialer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.pacjonek.fyttoys.BluetoothModule

/**
 * Headless handler for outgoing-call intents (ACTION_CALL / ACTION_DIAL with
 * a tel: URI). Extracts the number and hands it to [BluetoothModule] — this class
 * knows nothing about FYT binders, modules or command codes.
 *
 * Once the call request completes (delivered or failed), it cleans up and
 * closes itself.
 */
class CallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val phoneNumber = extractNumber(intent)
        if (phoneNumber.isNullOrBlank()) {
            val message = "No phone number in delivered intent"
            Log.w(TAG, message)
            Toast.makeText(this, "$message. App will be closed", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        CallService.dialUsingBluetoothHfp(this, phoneNumber)
        finish()
    }

    private fun extractNumber(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "tel") return null
        return Uri.decode(uri.schemeSpecificPart)?.trim()
    }

    companion object {
        private const val TAG = "CallActivity"
    }
}
