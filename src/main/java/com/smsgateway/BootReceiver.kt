package com.smsgateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = context.getSharedPreferences("cfg", Context.MODE_PRIVATE)
        if (p.getBoolean("enabled", false)) {
            try {
                val i = Intent(context, GatewayService::class.java)
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
                else context.startService(i)
            } catch (_: Exception) {}
        }
    }
}
