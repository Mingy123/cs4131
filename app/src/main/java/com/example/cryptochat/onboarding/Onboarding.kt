package com.example.cryptochat.onboarding

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.example.cryptochat.R
import com.example.cryptochat.crypto.EcKeyGenerator
import com.example.cryptochat.crypto.Secp256k1
import java.math.BigInteger

class Onboarding : AppCompatActivity() {
    private lateinit var server: EditText
    private lateinit var privkey: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        server = findViewById(R.id.serverInput)
        privkey = findViewById(R.id.privateKeyInput)
    }

    fun genRandomKey(view: View) {
        val keyPair = EcKeyGenerator.newInstance(Secp256k1)
        privkey.setText(keyPair.privateKey.toString(16))
    }

    fun submit(view: View) {
        val privateKey = BigInteger(privkey.text.toString(), 16)
        val keyPair = EcKeyGenerator.newInstance(privateKey, Secp256k1)
        val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
        val edit = spf.edit()
        edit.putString("server", server.text.toString())
        edit.putString("keypair", keyPair.publicKey.toString() + "," + privateKey.toString(16))
    }
}