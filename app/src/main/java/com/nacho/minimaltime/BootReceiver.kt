package com.nacho.minimaltime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED && Prefs.blockerOn(c)) {
            try {
                c.startForegroundService(Intent(c, BlockerService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
