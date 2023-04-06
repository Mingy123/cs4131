package com.example.cryptochat.group

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.cryptochat.AuthorisedRequest
import com.example.cryptochat.MainViewModel
import com.example.cryptochat.R
import com.example.cryptochat.contact.UserAdapter
import com.example.cryptochat.usernameFromPubkey
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.util.HashMap

class GroupDetailActivity : AppCompatActivity() {
    private lateinit var details: GroupDetail
    private lateinit var queue: RequestQueue
    private lateinit var userListView: RecyclerView
    private lateinit var userList: ArrayList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)
        val uuid = intent.getStringExtra("uuid")!!

        val gson = Gson()
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        var queue = viewModel.getRequestQueue()
        if (queue == null) {
            queue = Volley.newRequestQueue(this)
            viewModel.setRequestQueue(queue)
        }
        this.queue = queue
        queue.add(AuthorisedRequest(Method.GET, "/group-info?uuid=$uuid",
            { response ->
                details = gson.fromJson(response, GroupDetail::class.java)!!
                supportActionBar?.title = details.name

                findViewById<TextView>(R.id.groupDetailUuid).text = uuid
                findViewById<TextView>(R.id.groupDetailOwnerPubkey).text = details.owner
                val ownerUsername = usernameFromPubkey(applicationContext, details.owner, queue)
                findViewById<TextView>(R.id.groupDetailOwner).text = ownerUsername

                userListView = findViewById(R.id.groupDetailUserList)
                userListView.layoutManager = LinearLayoutManager(this)
                userList = ArrayList(details.members.map { pubkey ->
                    val username = usernameFromPubkey(this, pubkey, queue)
                    User(pubkey, username, "")
                })
                userListView.adapter = UserAdapter(userList)
            }, {
                Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                onBackPressed()
            }
        ))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_chat_edit -> {
                val dialog = AlertDialog.Builder(this)
                val inflater = LayoutInflater.from(this)
                val popup = inflater.inflate(R.layout.rename_group, null)
                val editText = popup.findViewById<EditText>(R.id.editGroupName)
                dialog.setView(popup)
                dialog.setPositiveButton("Confirm") { _, _ ->
                    queue.add(object : AuthorisedRequest(Method.POST, "/rename-group",
                        { response ->
                            Snackbar.make(userListView, response, Snackbar.LENGTH_SHORT).show()
                            if (response == "success") supportActionBar?.title = editText.text.toString()
                        }, { Toast.makeText(applicationContext, getString(R.string.network_error), Toast.LENGTH_SHORT).show() }
                    ) {
                        override fun getParams(): MutableMap<String, String>? {
                            val old = super.getParams()
                            val new = HashMap<String, String>()
                            if (old != null) { for ((key, value) in old) new[key] = value }
                            new["uuid"] = details.uuid
                            new["name"] = editText.text.toString()
                            return new
                        }
                    })
                }
                dialog.setNeutralButton("Cancel") {_, _ ->}
                dialog.show()
                true
            }
            R.id.menu_chat_delete -> {
                val dialog = AlertDialog.Builder(this)
                dialog.setTitle("Confirm?")
                dialog.setMessage("You will be deleting the group ${details.name}\n\n(${details.uuid})")
                dialog.setPositiveButton("Confirm") { _, _ ->
                    queue.add(object : AuthorisedRequest(Method.POST, "/delete-group",
                        { response ->
                            Snackbar.make(userListView, response, Snackbar.LENGTH_SHORT).show()
                        }, {
                            Toast.makeText(applicationContext, getString(R.string.network_error),
                                Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        override fun getParams(): MutableMap<String, String> {
                            val old = super.getParams()
                            val new = HashMap<String, String>()
                            if (old != null) {
                                for ((key, value) in old) new[key] = value
                            }
                            new["uuid"] = details.uuid
                            return new
                        }
                    })
                }
                dialog.setNeutralButton("Cancel") { _, _ -> }
                dialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}