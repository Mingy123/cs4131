package com.example.cryptochat

import android.content.Context
import android.graphics.Color

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