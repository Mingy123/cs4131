package com.example.cryptochat

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.example.cryptochat.group.User
import com.google.gson.Gson

fun usernameFromPubkey(context: Context, pubkey: String, queue: RequestQueue): String {
    val spf = context.getSharedPreferences("appcache", Context.MODE_PRIVATE)
    var username = spf.getString(pubkey, null)
    if (username == null) {
        queue.add(AuthorisedRequest(
            Request.Method.GET, "/user-info?pubkey=$pubkey",
            { response ->
                val gson = Gson()
                val user = gson.fromJson(response, User::class.java)!!
                val users = spf.getString("userlist", null)
                val edit = spf.edit()
                edit.putString(pubkey, user.username)
                if (users == null) edit.putString("userlist", pubkey)
                else edit.putString("userlist", "$users|$pubkey")
                edit.apply()
                username = user.username
            }, {
                Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            }
        ))
    }
    if (username == null) username = "network error"
    return username as String
}

fun usernameFromPubkey(context: Context, pubkey: String): String? {
    val spf = context.getSharedPreferences("appcache", Context.MODE_PRIVATE)
    return spf.getString(pubkey, null)
}

fun hashColor(str: String): Int {
    var hash = 0
    for (i in 0 until str.length) {
        hash = str[i].toInt() + ((hash shl 5) - hash)
        hash = hash and hash
    }
    val hue = hash % 360
    return Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 0.7f))
}