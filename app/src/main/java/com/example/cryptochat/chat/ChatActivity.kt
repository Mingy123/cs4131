package com.example.cryptochat.chat

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.example.cryptochat.AuthorisedRequest
import com.example.cryptochat.MainViewModel
import com.example.cryptochat.R
import com.example.cryptochat.crypto.*
import com.example.cryptochat.group.User
import com.example.cryptochat.usernameFromPubkey
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class ChatActivity : AppCompatActivity() {
    val messageList = ArrayList<Message>()
    lateinit var messageListView: RecyclerView
    lateinit var adapter: MessageAdapter
    private lateinit var editText: EditText
    private lateinit var uuid: String
    private lateinit var connection: HttpURLConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
        val privkey = BigInteger(spf.getString("keypair", null)!!.split(',')[1], 16)
        val keyPair = EcKeyGenerator.newInstance(privkey, Secp256k1)
        val user = keyPair.publicKey.toString()

        // init the chat
        messageListView = findViewById(R.id.messageList)
        messageListView.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(messageList)
        messageListView.adapter = adapter
        uuid = intent.getStringExtra("uuid")!!
        supportActionBar?.title = intent.getStringExtra("name")
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
                for (msg in list) { if (usernameFromPubkey(applicationContext ,msg.sender) == null) {
                    queue.add( AuthorisedRequest(Method.GET, "/user-info?pubkey="+msg.sender,
                        { resp ->
                            val user2 = gson.fromJson(resp, User::class.java)!!
                            val spf2 = getSharedPreferences("appcache", Context.MODE_PRIVATE)
                            val edit = spf2.edit()
                            edit.putString(msg.sender, user2.username)
                            edit.apply()
                        },
                        {}
                    ) )
                } }
                list.reverse()
                val startIndex = messageList.size
                messageList.addAll(list)
                adapter.notifyItemRangeInserted(startIndex, list.size)
                messageListView.scrollToPosition(messageList.size-1)
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
        editText = findViewById(R.id.messageInput)
        val now = Calendar.getInstance().timeInMillis
        findViewById<ImageButton>(R.id.sendMessage).setOnClickListener { view ->
            val content = editText.text.toString()
            val tmp = EcSign.signData(keyPair, content.toByteArray(), EcSha256)
            val signature = tmp.toString()
            val thing = signature.split(',')
            val sig = EcSignature(BigInteger(thing[0], 16), BigInteger(thing[1], 16))
            val pk = decodeSec1(AuthorisedRequest.PUBKEY, Secp256k1)!!
            val correct = EcSign.verifySignature(
                pk,
                content.toByteArray(), EcSha256,
                sig
            )
            if (!correct) {
                editText.setText("${keyPair.publicKey.x.toString(16)}, ${keyPair.publicKey.y.toString(16)}, ${pk.x.toString(16)}, ${pk.y.toString(16)}")
                return@setOnClickListener
            }
            val request = SendMessageRequest(Request.Method.POST, content, signature, uuid,
                { response ->
                    messageList.add(Message(
                        content, keyPair.publicKey.toString(), signature,
                        now, response
                    ))
                    adapter.notifyItemInserted(messageList.size-1)
                    messageListView.scrollToPosition(messageList.size-1)
                    editText.text.clear()
                },
                { Snackbar.make(view, getString(R.string.network_error), Snackbar.LENGTH_SHORT).show() }
            )
            queue.add(request)
        }

        // eye candy
        editText.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            messageListView.scrollToPosition(messageList.size-1)
        }

        // THE FUCKING EVENT STREAM THING
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val url = URL(AuthorisedRequest.HOST + "/subscribe")
            connection = withContext(Dispatchers.IO) {
                url.openConnection()
            } as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "text/event-stream")
            connection.setRequestProperty("X-Pubkey", AuthorisedRequest.PUBKEY)
            connection.setRequestProperty("X-Signature", AuthorisedRequest.SIGNATURE)

            val postData = "uuid=$uuid".toByteArray(Charsets.UTF_8)
            connection.setRequestProperty("Content-Length", postData.size.toString())
            connection.doOutput = true
            val outputStream = connection.outputStream
            withContext(Dispatchers.IO) {
                outputStream.write(postData)
            }
            withContext(Dispatchers.IO) {
                outputStream.flush()
            }

            val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)

            while (true) {
                val line = withContext(Dispatchers.IO) {
                    reader.readLine()
                }
                if (line == null) {
                    break
                } else if (line.isEmpty()) {
                    // Ignore empty lines
                } else {
                    // Parse the event and data fields from the line
                    val dataField = Regex("^data:(.*)$").find(line)?.groupValues?.get(1)

                    if (dataField != null) {
                        val message = Gson().fromJson(dataField, Message::class.java)
                        if (message.sender == user) return@launch
                        // Do something with the message
                        withContext(Dispatchers.Main) {
                            messageList.add(message)
                            adapter.notifyItemInserted(messageList.size - 1)
                            messageListView.scrollToPosition(messageList.size - 1)
                        }
                    }
                }
            }
        }
        //connection.disconnect()
    }
}

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