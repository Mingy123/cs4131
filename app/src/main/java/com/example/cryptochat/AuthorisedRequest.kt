package com.example.cryptochat

import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest

open class AuthorisedRequest(
    method: Int, url: String,
    listener: (response: String) -> Unit,
    errorListener: (error: VolleyError) -> Unit,
) : StringRequest(method, HOST+url, listener, errorListener)
{
    companion object {
        var PUBKEY = ""
        var SIGNATURE = ""
        var HOST = ""
    }

    override fun getHeaders(): MutableMap<String, String> {
        val old = super.getHeaders()
        val new = HashMap<String, String>()
        for ((key, value) in old) {
            new[key] = value
        }
        new["X-Pubkey"] = PUBKEY
        new["X-Signature"] = SIGNATURE
        return new
    }
}