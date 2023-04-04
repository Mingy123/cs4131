package com.example.cryptochat.onboarding

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.cryptochat.AuthorisedRequest
import com.example.cryptochat.MainViewModel
import com.example.cryptochat.R
import com.example.cryptochat.crypto.*
import com.example.cryptochat.group.User
import com.google.gson.Gson
import java.math.BigInteger

class Onboarding : AppCompatActivity() {
    private lateinit var server: EditText
    private lateinit var privkey: EditText
    private lateinit var requestQueue: RequestQueue
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        supportActionBar?.hide()
        server = findViewById(R.id.serverInput)
        privkey = findViewById(R.id.privateKeyInput)
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        var queue = viewModel.getRequestQueue()
        if (queue == null) {
            queue = Volley.newRequestQueue(this)
            viewModel.setRequestQueue(queue)
        }
        requestQueue = queue
        val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
        server.setText(spf.getString("server", ""))
        val keyPair = spf.getString("keypair", null)
        if (keyPair != null) privkey.setText(keyPair.split(',')[1])
    }

    fun genRandomKey(view: View) {
        val keyPair = EcKeyGenerator.newInstance(Secp256k1)
        privkey.setText(keyPair.privateKey.toString(16))
    }

    // TODO: literally everything
    fun submit(view: View) {
        val privateKey = BigInteger(privkey.text.toString(), 16)
        val keyPair = EcKeyGenerator.newInstance(privateKey, Secp256k1)

        val host = server.text.toString().removeSuffix("/")
        val pubkey = keyPair.publicKey.toString()
        // check that the user is in the server using "/user-info"
        val request = StringRequest(Method.GET, "$host/user-info?pubkey=$pubkey",
            { response ->
                // indicate success somehow? ig start the new activity here
                val gson = Gson()
                val user = gson.fromJson(response, User::class.java)!!
                // set AuthorisedRequest stuff
                val signature = EcSign.signData(keyPair, user.nonce.toByteArray(), EcSha256).toString()
                AuthorisedRequest.HOST = host
                AuthorisedRequest.PUBKEY = pubkey
                AuthorisedRequest.SIGNATURE = signature
                // add to shared preferences
                val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
                val edit = spf.edit()
                edit.putString("server", host)
                edit.putString("keypair", pubkey + "," + privateKey.toString(16))
                edit.putString("username", user.username)
                edit.putString("signature", signature)
                edit.apply()

                // go back to the main activity or smt
                finish()
            },
            {
                // dialog alerting that either the server is wrong or the pubkey does not exist.
                // implicit intent to open the host in the browser
                Toast.makeText(applicationContext, "server is wrong or the pubkey does not exist",
                    Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(request)
    }

    override fun onBackPressed() {}
}