package com.smsgateway

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p = getSharedPreferences("cfg", MODE_PRIVATE)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val title = TextView(this).apply { text = "SMS Gateway"; textSize = 24f }
        val server = EditText(this).apply {
            hint = "Server URL (https://yourdomain.com)"
            setText(p.getString("server", ""))
            setSingleLine()
        }
        val key = EditText(this).apply {
            hint = "API Key"
            setText(p.getString("key", ""))
            setSingleLine()
        }
        val start = Button(this).apply { text = "Start" }
        val stop = Button(this).apply { text = "Stop" }
        status = TextView(this).apply { textSize = 14f; setPadding(0, pad, 0, 0) }

        start.setOnClickListener {
            val s = server.text.toString().trim().trimEnd('/')
            val k = key.text.toString().trim()
            if (s.isEmpty() || k.isEmpty()) {
                Toast.makeText(this, "Server আর API Key দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            p.edit().putString("server", s).putString("key", k).putBoolean("enabled", true).apply()
            askPermissions()
            askBatteryExemption()
            val i = Intent(this, GatewayService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            Toast.makeText(this, "চালু হয়েছে", Toast.LENGTH_SHORT).show()
        }
        stop.setOnClickListener {
            p.edit().putBoolean("enabled", false).apply()
            stopService(Intent(this, GatewayService::class.java))
            Toast.makeText(this, "বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
        }

        for (v in listOf(title, server, key, start, stop, status)) root.addView(
            v, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val p = getSharedPreferences("cfg", MODE_PRIVATE)
        status.text = "সর্বশেষ: " + p.getString("last", "-")
    }

    private fun askPermissions() {
        val perms = mutableListOf(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.toTypedArray(), 1)
    }

    private fun askBatteryExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (Build.VERSION.SDK_INT >= 23 && !pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:$packageName"))
                )
            } catch (_: Exception) {}
        }
    }
}
