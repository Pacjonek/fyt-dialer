package com.pacjonek.fytdialer

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/**
 * Launcher entry point. Shows no UI: asks the system to make this app the
 * default dialer (if it isn't already), then closes.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultDialer = isDefaultDialer()
        if (defaultDialer) {
            val message = "Already set as the default Android dialer, nothing to do"
            Log.d(TAG, message)
            Toast.makeText(this, "✅ $message. You might hide this app icon", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if(isSyuMsAvailable(this)){
            Toast.makeText(this, "SyuMs service detected so it's probably FYT head unit", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "⛔ I don't see SyuMs service. It looks like not FYT head unit. Exiting...", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if(!requestDefaultDialerRole()) {
            val message = "Cannot request the default dialer role"
            Log.w(TAG, message)
            Toast.makeText(applicationContext, "⛔ Error: $message. Closing...", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    fun isSyuMsAvailable(context: Context): Boolean {
        return try {
            context.packageManager.getApplicationInfo("com.syu.ms", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package `com.syu.ms` not found", e)
            false
        }
    }

    private fun isDefaultDialer(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    private fun requestDefaultDialerRole(): Boolean {
        return try {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                startActivityForResult(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER),
                    REQUEST_ROLE
                )
                return true
            }
            false
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No system UI to request the dialer role", e)
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Role request finished, default dialer now: ${isDefaultDialer()}")
        if(isDefaultDialer()){
            Toast.makeText(applicationContext, "✅ The role obtained successfully. You can hide this launcher shortcut", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(applicationContext, "⛔ Error. This app require default dialer role. Set in manually in Android settings", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    companion object {
        private const val TAG = "FytDialerMainActivity"
        private const val REQUEST_ROLE = 1
    }
}
