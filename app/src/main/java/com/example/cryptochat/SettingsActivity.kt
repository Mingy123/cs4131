package com.example.cryptochat

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.cryptochat.crypto.EcKeyGenerator
import com.example.cryptochat.crypto.EcSha256
import com.example.cryptochat.crypto.EcSign
import com.example.cryptochat.crypto.Secp256k1
import com.google.android.material.snackbar.Snackbar
import java.math.BigInteger
import java.util.HashMap
import android.Manifest
import android.content.Intent
import android.provider.MediaStore
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate

const val REQUEST_GALLERY_CODE = 300
class SettingsActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var queue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.title = "Settings"
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        var queue = viewModel.getRequestQueue()
        if (queue == null) {
            queue = Volley.newRequestQueue(this)
            viewModel.setRequestQueue(queue)
        }
        this.queue = queue

        val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
        val edit = spf.edit()

        val switch = findViewById<Switch>(R.id.jumpNewestSwitch)
        switch.isChecked = spf.getBoolean("scroll", true)
        switch.setOnCheckedChangeListener { _, isChecked ->
            edit.putBoolean("scroll", isChecked)
            edit.apply()
        }

        val nightModeOn: Boolean = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        val darkSwitch = findViewById<Switch>(R.id.darkModeSwitch)
        darkSwitch.isChecked = nightModeOn
        darkSwitch.setOnCheckedChangeListener { _, isChecked ->
            val thing: Boolean = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            if (thing) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            darkSwitch.isChecked = !thing
        }

        val changeWallpaper = findViewById<Button>(R.id.settingsChangeWallpaper)
        changeWallpaper.setOnClickListener {
            if (checkAndRequestPermissions(this)) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                val request = REQUEST_GALLERY_CODE
                if (intent.resolveActivity(packageManager) != null)
                    startActivityForResult(intent, request)
            }
        }
        changeWallpaper.setOnLongClickListener { view ->
            edit.remove("wallpaper")
            edit.apply()
            Snackbar.make(view, "Removed wallpaper!", Snackbar.LENGTH_LONG).show()
            true
        }

        val privkey = BigInteger(spf.getString("keypair", null)!!.split(',')[1], 16)
        val keyPair = EcKeyGenerator.newInstance(privkey, Secp256k1)
        findViewById<Button>(R.id.settingsResetNonce).setOnClickListener { view ->
            val path = "${AuthorisedRequest.HOST}/reset-nonce"
            queue.add(StringRequest(Method.GET, "$path?pubkey=${keyPair.publicKey}",
                { response ->
                    val signature = EcSign.signData(keyPair, response.toByteArray(), EcSha256).toString()
                    queue.add(object : StringRequest(Method.POST, path,
                        { resp ->
                            Snackbar.make(view, resp, Snackbar.LENGTH_SHORT).show()
                            AuthorisedRequest.SIGNATURE = signature
                            edit.putString("signature", signature)
                            edit.apply()
                        }, {}
                    ) {
                        override fun getParams(): MutableMap<String, String> {
                            val old = super.getParams()
                            val new = HashMap<String, String>()
                            if (old != null) { for ((key, value) in old) new[key] = value }
                            new["pubkey"] = keyPair.publicKey.toString()
                            new["signature"] = signature
                            return new
                        }
                    })
                }, { Toast.makeText(applicationContext, getString(R.string.network_error), Toast.LENGTH_SHORT).show() }
            ))
        }


        findViewById<TextView>(R.id.settingServer).text = "Server: " + AuthorisedRequest.HOST
        findViewById<TextView>(R.id.settingUsername).text = "Username: " + spf.getString("username", "")
        findViewById<TextView>(R.id.settingPubkey).text = "Public key: " + keyPair.publicKey.toString()
        findViewById<TextView>(R.id.settingPrivkey).text = "Private key:" + keyPair.privateKey.toString(16)
    }

    private fun checkAndRequestPermissions(context: Activity?): Boolean {
        val permission = ContextCompat.checkSelfPermission(
            context!!, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                69
            )
            return false
        }
        return true
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(applicationContext, "App Requires Access to Your Storage.",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_GALLERY_CODE && data != null) {
            val selectedImage = data.data
            val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
            val edit = spf.edit()
            edit.putString("wallpaper", selectedImage.toString())
            edit.apply()
        }
    }
}