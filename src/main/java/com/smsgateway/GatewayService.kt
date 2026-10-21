package com.smsgateway

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.SmsManager
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GatewayService : Service() {
    @Volatile private var running = false
    private var worker: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(1, notif)

        if (!running) {
            running = true
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "smsgateway:wl").apply { acquire() }
            worker = Thread { loop() }.also { it.start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        try { wakeLock?.release() } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val chId = "gateway"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(
                NotificationChannel(chId, "SMS Gateway", NotificationManager.IMPORTANCE_LOW)
            )
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, chId)
        else Notification.Builder(this)
        return b.setContentTitle("SMS Gateway চলছে")
            .setContentText("সার্ভার থেকে SMS অপেক্ষা করছে")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setOngoing(true)
            .build()
    }

    private fun log(msg: String) {
        getSharedPreferences("cfg", MODE_PRIVATE).edit().putString("last", msg).apply()
    }

    private fun loop() {
        val p = getSharedPreferences("cfg", MODE_PRIVATE)
        while (running) {
            try {
                val server = p.getString("server", "") ?: ""
                val key = p.getString("key", "") ?: ""
                val (code, body) = http("GET", "$server/api/device/pending", key, null)
                if (code == 401) {
                    log("API key ভুল")
                    Thread.sleep(30000)
                    continue
                }
                if (code == 200) {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        val id = m.getInt("id")
                        val to = m.getString("to")
                        val text = m.getString("message")
                        try {
                            sendSms(to, text)
                            report(server, key, id, "sent", "")
                            log("পাঠানো হয়েছে → $to")
                        } catch (e: Exception) {
                            report(server, key, id, "failed", e.message ?: "error")
                            log("ব্যর্থ → $to : ${e.message}")
                        }
                        Thread.sleep(2000) // সিম ব্লক এড়াতে বিরতি
                    }
                } else {
                    log("সার্ভার কোড: $code")
                }
            } catch (_: InterruptedException) {
                break
            } catch (e: Exception) {
                log("এরর: ${e.message}")
                try { Thread.sleep(10000) } catch (_: InterruptedException) { break }
            }
            try { Thread.sleep(5000) } catch (_: InterruptedException) { break }
        }
    }

    private fun sendSms(to: String, text: String) {
        val sms: SmsManager = if (Build.VERSION.SDK_INT >= 31)
            getSystemService(SmsManager::class.java)
        else @Suppress("DEPRECATION") SmsManager.getDefault()
        val parts = sms.divideMessage(text)
        sms.sendMultipartTextMessage(to, null, parts, null, null)
    }

    private fun report(server: String, key: String, id: Int, status: String, error: String) {
        val data = "id=$id&status=$status&error=" + URLEncoder.encode(error, "UTF-8")
        try { http("POST", "$server/api/device/status", key, data) } catch (_: Exception) {}
    }

    private fun http(method: String, url: String, key: String, data: String?): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("X-Api-Key", key)
        c.setRequestProperty("Accept", "application/json")
        if (data != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            c.outputStream.use { it.write(data.toByteArray()) }
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
        c.disconnect()
        return Pair(code, body)
    }
}
