package com.example.cryptochat.contact

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.cryptochat.AuthorisedRequest
import com.example.cryptochat.MainViewModel
import com.example.cryptochat.R
import com.example.cryptochat.group.User
import com.example.cryptochat.usernameFromPubkey
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson

class ContactsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var queue: RequestQueue
    private lateinit var editText: EditText
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        supportActionBar?.title = "Saved Users"
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        var queue = viewModel.getRequestQueue()
        if (queue == null) {
            queue = Volley.newRequestQueue(this)
            viewModel.setRequestQueue(queue)
        }
        this.queue = queue
        editText = findViewById(R.id.addUserPubkey)

        // look through the user list
        val spf = getSharedPreferences("appcache", Context.MODE_PRIVATE)
        val users = spf.getString("userlist", "")!!
        val userList = ArrayList<User>()
        for (pk in users.split('|'))
            userList.add(User(pk, spf.getString(pk, "")!!, ""))

        recyclerView = findViewById(R.id.contactList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UserAdapter(userList)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.addUserPubkeyButton).setOnClickListener { view ->
            val pubkey = editText.text.toString()
            // Cache::usernameFromPubkey, but i display the username in snackbar
            if (usernameFromPubkey(applicationContext, pubkey) == null)
                queue.add(AuthorisedRequest(
                    Request.Method.GET, "/user-info?pubkey=$pubkey",
                    { response ->
                        val gson = Gson()
                        val user = gson.fromJson(response, User::class.java)!!
                        val prevUsers = spf.getString("userlist", null)
                        val edit = spf.edit()
                        edit.putString(pubkey, user.username)
                        if (prevUsers == null) edit.putString("userlist", pubkey)
                        else edit.putString("userlist", "$users|$pubkey")
                        edit.apply()

                        userList.add(user)
                        adapter.notifyItemInserted(userList.size-1)
                        recyclerView.scrollToPosition(userList.size-1)
                        Snackbar.make(view, "Added user: ${user.username}", Snackbar.LENGTH_LONG).show()
                    }, {
                        Toast.makeText(applicationContext, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                    }
                ))
            else Snackbar.make(recyclerView, "User already added", Snackbar.LENGTH_SHORT).show()
        }
    }
}