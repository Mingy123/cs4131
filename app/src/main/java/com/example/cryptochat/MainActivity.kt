package com.example.cryptochat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request.Method
import com.android.volley.toolbox.Volley
import com.example.cryptochat.databinding.ActivityMainBinding
import com.example.cryptochat.group.Group
import com.example.cryptochat.group.GroupAdapter
import com.example.cryptochat.onboarding.Onboarding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
        if (spf.getString("server", null) == null) {
            val intent = Intent(applicationContext, Onboarding::class.java)
            startActivity(intent)
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        val queue = Volley.newRequestQueue(this)
        viewModel.setRequestQueue(queue)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val list = ArrayList<Group>()
        list.add(Group("uuid", "name", "owner"))
        recyclerView.adapter = GroupAdapter(list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // [{"uuid":"4680a265-5515-497a-9410-c78dd9796596","name":"blohai","owner":"pubkey"}]
        val request = AuthorisedRequest(Method.GET, "/my-groups",
            { response ->
                val gson = Gson()
                val listType = object : TypeToken<ArrayList<Group>>() {}.type
                val list = gson.fromJson<ArrayList<Group>>(response, listType)
                recyclerView.adapter = GroupAdapter(list)
            },
            { Snackbar.make(binding.root, getString(R.string.network_error), Snackbar.LENGTH_LONG).show() }
        )
        queue.add(request)

        setSupportActionBar(binding.toolbar)
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}


/*
// check whether the user has entered credentials: privkey
// generates a random key pair on the secp256k1 curve
val randomKeys = EcKeyGenerator.newInstance(Secp256k1)

// crypto test
//val privateKey = BigInteger("ec946950ff0f723e983caeee46241fa57b2e823f1975283808371ee5f9e47de5", 16)
val privateKey = BigInteger("ec946950ff0f723e983caeee46241fa57b2e823f1975283808371ee5f9e12345", 16)
val fromPrivateKey = EcKeyGenerator.newInstance(privateKey, Secp256k1)
val data = "among".toByteArray()
val signature = EcSign.signData(fromPrivateKey, data, EcSha256)
val correct = EcSign.verifySignature(fromPrivateKey.publicKey, "among".toByteArray(), EcSha256, signature)
Snackbar.make(binding.root, "signature correct: $correct", Snackbar.LENGTH_LONG).show()


val textView = findViewById<TextView>(R.id.httpResponse)
val request = AuthorisedRequest(Method.GET, "/my-groups",
    { response -> textView.text = "Response: $response" },
    { textView.text = "error" }
)
queue.add(request)
*/
