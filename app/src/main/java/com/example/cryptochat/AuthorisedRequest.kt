package com.example.cryptochat

import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest

var PUBKEY = "03feba6c9fe8fa1be3a54755df3e32aae6baa507c569c602ac3bc94e180da4af27"
var SIGNATURE = "3045022100c2b273bef5daeea9e19e14079390fb76c5d7e4c18cf1a420ad613472cc1a7b730220480294b5fb8abfd18749b98607fdbcd239d8bd012c173ba662b0c84a1ba562e3"
var HOST = "http://13.229.240.184:8000"

open class AuthorisedRequest(
    method: Int, url: String,
    listener: (response: String) -> Unit,
    errorListener: (error: VolleyError) -> Unit,
) : StringRequest(method, HOST+url, listener, errorListener)
{
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