package com.example.cryptochat.chat

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.size
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Request.Method
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.example.cryptochat.*
import com.example.cryptochat.AuthorisedRequest
import com.example.cryptochat.crypto.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.math.BigInteger
import java.util.Calendar

class ChatActivity : AppCompatActivity() {
    val messageList = ArrayList<Message>()
    val eventStream = MessageEventStream()
    lateinit var messageListView: RecyclerView
    lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
        val privkey = BigInteger(spf.getString("keypair", null)!!.split(',')[1], 16)
        val keyPair = EcKeyGenerator.newInstance(privkey, Secp256k1)
        val user = keyPair.publicKey.toString()
        Toast.makeText(applicationContext, user, Toast.LENGTH_LONG).show()

        // init the chat
        messageListView = findViewById(R.id.messageList)
        messageListView.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(messageList)
        messageListView.adapter = adapter
        val uuid = intent.getStringExtra("uuid")!!
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        var queue = viewModel.getRequestQueue()
        if (queue == null) {
            queue = Volley.newRequestQueue(this)
            viewModel.setRequestQueue(queue)
        }
        queue.add(object : AuthorisedRequest(Method.POST, "/messages",
            { response ->
                val gson = Gson()
                val listType = object : TypeToken<ArrayList<Message>>(){}.type
                val list = gson.fromJson<ArrayList<Message>>(response, listType)
                for (msg in list) { if (usernameFromPubkey(this ,msg.sender) == null) {
                    queue.add( AuthorisedRequest(Method.GET, "/user-info?pubkey="+msg.sender,
                        { response ->
                            val user = gson.fromJson(response, User::class.java)!!
                            val spf = getSharedPreferences("appcache", Context.MODE_PRIVATE)
                            val edit = spf.edit()
                            edit.putString(msg.sender, user.username)
                            edit.apply()
                        },
                        {}
                    ) )
                } }
                list.reverse()
                val startIndex = messageList.size
                messageList.addAll(list)
                adapter.notifyItemRangeInserted(startIndex, list.size)
            },
            {}) {
            override fun getParams(): MutableMap<String, String> {
                val old = super.getParams()
                val new = HashMap<String, String>()
                if (old != null) {
                    for ((key, value) in old) {
                        new[key] = value
                    }
                }
                // init timestamp, 10 sec offset cos why not
                val now = Calendar.getInstance().timeInMillis.toString() + 10000
                new["timestamp"] = now
                new["uuid"] = uuid
                return new
            }
        })


        // sending messages
        val editText = findViewById<EditText>(R.id.messageInput)
        val now = Calendar.getInstance().timeInMillis
        findViewById<ImageButton>(R.id.sendMessage).setOnClickListener { view ->
            val content = editText.text.toString()
            val signature = EcSign.signData(keyPair, content.toByteArray(), EcSha256).toString()
            val request = SendMessageRequest(Request.Method.POST, content, signature, uuid,
                { response ->
                    messageList.add(Message(
                        content, keyPair.publicKey.toString(), signature,
                        now, response
                    ))
                    adapter.notifyItemInserted(messageListView.size - 1)
                },
                { Snackbar.make(view, getString(R.string.network_error), Snackbar.LENGTH_SHORT).show() }
            )
            queue.add(request)
        }

        // the fucking eventstream thing
        val esRequest = AuthorisedRequest(Method.POST, "/subscribe",
            { response ->
                val gson = Gson()
                val msg = gson.fromJson(response, Message::class.java)!!
                if (msg.sender != user) eventStream.emit(msg)
            },
            { error -> eventStream.error(error) }
        )
        queue.add(esRequest)

        val disposable = eventStream.observe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { data ->
                // add to recyclerview
            }
        disposable.dispose()
    }
}

data class User(val pubkey: String, val username: String, val nonce: String)

class SendMessageRequest(method: Int, val content: String, val signature: String, val uuid: String,
    listener: (response: String) -> Unit,
    errorListener: (error: VolleyError) -> Unit,
) : AuthorisedRequest(method, "/send", listener, errorListener)
{
    override fun getParams(): MutableMap<String, String>? {
        val old = super.getParams()
        val new = HashMap<String, String>()
        if (old != null) { for ((key, value) in old) new[key] = value }

        new["uuid"] = uuid
        new["content"] = content
        new["signature"] = signature
        return new
    }
}