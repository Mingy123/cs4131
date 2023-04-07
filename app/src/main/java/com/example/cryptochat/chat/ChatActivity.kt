package com.example.cryptochat.chat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.example.cryptochat.AuthorisedRequest
import com.example.cryptochat.MainViewModel
import com.example.cryptochat.R
import com.example.cryptochat.crypto.*
import com.example.cryptochat.group.GroupDetailActivity
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
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class ChatActivity : AppCompatActivity() {
    val messageList = ArrayList<Message>()
    lateinit var messageListView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager
    lateinit var adapter: MessageAdapter
    private lateinit var editText: EditText
    private lateinit var uuid: String
    private lateinit var groupName: String
    private lateinit var queue: RequestQueue
    private lateinit var connection: HttpURLConnection

    private var active = false
    private lateinit var notificationManager: NotificationManager
    private lateinit var channelID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val spf = getSharedPreferences("metadata", Context.MODE_PRIVATE)
        val privkey = BigInteger(spf.getString("keypair", null)!!.split(',')[1], 16)
        val keyPair = EcKeyGenerator.newInstance(privkey, Secp256k1)
        val user = keyPair.publicKey.toString()
        channelID = getString(R.string.channelID)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(getString(R.string.channelID),
            getString(R.string.channelName), getString(R.string.channelDesc))

        // wallpaper wow
        val imageUri = spf.getString("wallpaper", null)
        val imageView = findViewById<ImageView>(R.id.chatWallpaper)
        if (imageUri == null) imageView.visibility = View.INVISIBLE
        else imageView.setImageURI(Uri.parse(imageUri))

        // init the chat
        messageListView = findViewById(R.id.messageList)
        layoutManager = LinearLayoutManager(this)
        messageListView.layoutManager = layoutManager
        adapter = MessageAdapter(messageList)
        messageListView.adapter = adapter
        uuid = intent.getStringExtra("uuid")!!
        groupName = intent.getStringExtra("name")!!
        supportActionBar?.title = groupName
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        var queue = viewModel.getRequestQueue()
        if (queue == null) {
            queue = Volley.newRequestQueue(this)
            viewModel.setRequestQueue(queue)
        }
        this.queue = queue
        queue.add(getMessagesBefore(Calendar.getInstance().timeInMillis + 1000000 // offset 1000 seconds cos why not
        ) { list ->
            val startIndex = messageList.size
            messageList.addAll(list)
            adapter.notifyItemRangeInserted(startIndex, list.size)
            messageListView.scrollToPosition(messageList.size - 1)
        })

        // sending messages
        editText = findViewById(R.id.messageInput)
        findViewById<ImageButton>(R.id.sendMessage).setOnClickListener { view ->
            val content = editText.text.toString()
            val signature = EcSign.signData(keyPair, content.toByteArray(), EcSha256).toString()
            val request = SendMessageRequest(Request.Method.POST, content, signature, uuid,
                { response ->
                    val now = Calendar.getInstance().timeInMillis
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

        // get more messages
        messageListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                // when its at the top it should be idle
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return super.onScrollStateChanged(recyclerView, newState)
                val thing = layoutManager.findFirstVisibleItemPosition()
                if (thing == 0) queue.add(getMessagesBefore(messageList[0].timestamp) { list ->
                    messageList.addAll(0, list)
                    adapter.notifyItemRangeInserted(0, list.size)
                })
            }
        })

        // eye candy
        if (spf.getBoolean("scroll", true))
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
                Log.d("mingy", "eventstream is listening...")
                Log.d("mingy", "Active: $active")
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
                        if (message.sender == user) continue
                        withContext(Dispatchers.Main) {
                            messageList.add(message)
                            adapter.notifyItemInserted(messageList.size - 1)
                            messageListView.scrollToPosition(messageList.size - 1)
                        }

                        if (!active) {
                            // send notification
                            val username = usernameFromPubkey(this@ChatActivity, message.sender) ?: message.sender
                            sendNotification(username, message.content)
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel(id: String, name: String, description: String){
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        channel.enableLights(true)
        channel.lightColor = Color.RED
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager.createNotificationChannel(channel)
    }

    var notifID = 80
    fun sendNotification (title: String, content: String){
        val notification = Notification.Builder(this, channelID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setChannelId(channelID).build()
        notificationManager.notify(notifID++, notification)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_chat_info -> {
                val intent = Intent(this, GroupDetailActivity::class.java)
                intent.putExtra("uuid", uuid)
                intent.putExtra("name", groupName)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun getMessagesBefore(timestamp: Long, todo: (ArrayList<Message>) -> Unit): AuthorisedRequest {
        return object : AuthorisedRequest(Method.POST, "/messages",
            { response ->
                val gson = Gson()
                val listType = object : TypeToken<ArrayList<Message>>(){}.type
                val list = gson.fromJson<ArrayList<Message>>(response, listType)

                val pubkeyList = HashSet<String>()
                for (msg in list) pubkeyList.add(msg.sender)
                for (pk in pubkeyList) usernameFromPubkey(applicationContext, pk, queue)

                list.reverse()
                todo(list)
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
                new["timestamp"] = timestamp.toString()
                new["uuid"] = uuid
                return new
            }
        }
    }

    override fun onResume() {
        active = true
        super.onResume()
    }

    override fun onPause() {
        active = false
        super.onPause()
    }
}

class SendMessageRequest(method: Int, val content: String, val signature: String, val uuid: String,
    listener: (response: String) -> Unit,
    errorListener: (error: VolleyError) -> Unit,
) : AuthorisedRequest(method, "/send", listener, errorListener)
{
    override fun getParams(): MutableMap<String, String> {
        val old = super.getParams()
        val new = HashMap<String, String>()
        if (old != null) { for ((key, value) in old) new[key] = value }

        new["uuid"] = uuid
        new["content"] = content
        new["signature"] = signature
        return new
    }
}