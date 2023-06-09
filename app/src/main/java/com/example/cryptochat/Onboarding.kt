package com.example.cryptochat

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.cryptochat.crypto.*
import com.example.cryptochat.group.User
import com.google.gson.Gson
import java.math.BigInteger

class Onboarding : AppCompatActivity() {
    private lateinit var server: EditText
    private lateinit var privkey: EditText
    private lateinit var requestQueue: RequestQueue
    private lateinit var lockedImage: ImageView
    private lateinit var unlockImage: ImageView
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

        lockedImage = findViewById(R.id.onboardingLock)
        unlockImage = findViewById(R.id.onboardingUnlock)
    }

    fun genRandomKey(view: View) {
        val keyPair = EcKeyGenerator.newInstance(Secp256k1)
        privkey.setText(keyPair.privateKey.toString(16).padStart(64, '0'))
    }

    fun submit(view: View) {
        val privateKey = BigInteger(privkey.text.toString(), 16)
        val keyPair = EcKeyGenerator.newInstance(privateKey, Secp256k1)

        var host = server.text.toString().removeSuffix("/")
        if (!host.contains("://")) {
            // WARNING: change to https when my backend supports it
            host = "http://$host"
        }
        val pubkey = keyPair.publicKey.toString()
        // check that the user is in the server using
        val request = StringRequest(Method.GET, "$host/user-info?pubkey=$pubkey",
            { response ->
                // fucking animations
                val fade = ValueAnimator.ofFloat(1f, 0f)
                fade.apply {
                    duration = 300
                    addUpdateListener { animator ->
                        val value = animator.animatedValue as Float
                        lockedImage.alpha = value
                        unlockImage.alpha = 1f - value
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // go back to the main activity hopefully
                            finish()
                        }
                    })

                }
                fade.start()
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
                edit.putString("keypair", pubkey + "," + privateKey.toString(16).padStart(16, '0'))
                edit.putString("username", user.username)
                edit.putString("signature", signature)
                edit.apply()
            }
        ) {
            // dialog alerting that either the server is wrong or the pubkey does not exist.
            // implicit intent to open the host in the browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(host))
            val dialog = AlertDialog.Builder(view.context)
            dialog.setTitle(view.context.getString(R.string.network_error))
            dialog.setMessage("Either the server is not responding, or your key is invalid.\n" +
                    "Try checking the host in browser?")
            dialog.setPositiveButton("OK") { _, _ -> startActivity(intent) }
            dialog.setNeutralButton("Cancel") { _, _ -> }
            dialog.show()
        }
        requestQueue.add(request)
    }

    override fun onBackPressed() {}
}