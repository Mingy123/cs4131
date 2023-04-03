package com.example.cryptochat

import android.content.Context

fun usernameFromPubkey(context: Context, pubkey: String): String? {
    val spf = context.getSharedPreferences("appcache", Context.MODE_PRIVATE)
    return spf.getString(pubkey, null)
}