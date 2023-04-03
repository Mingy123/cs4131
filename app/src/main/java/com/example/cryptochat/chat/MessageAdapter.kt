package com.example.cryptochat.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptochat.R
import com.example.cryptochat.usernameFromPubkey
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MessageAdapter(val msgList: ArrayList<Message>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_card, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(msgList[position])
    }

    override fun getItemCount() = msgList.size
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var msgSender: TextView
        var msgContent: TextView
        var msgTimestamp: TextView
        lateinit var msg: Message

        init {
            msgSender = itemView.findViewById(R.id.messageSender)
            msgContent = itemView.findViewById(R.id.messageContent)
            msgTimestamp = itemView.findViewById(R.id.messageTimestamp)
            itemView.setOnLongClickListener { view ->
                Snackbar.make(view, msg.hash, Snackbar.LENGTH_LONG).show()
                true
            }
        }

        fun bindItems(msg: Message) {
            this.msg = msg
            val username = usernameFromPubkey(itemView.context, msg.sender)
            if (username == null) msgSender.text = msg.sender.substring(16) + "..."
            else msgSender.text = username
            msgContent.text = msg.content
            val date = Date()
            date.time = msg.timestamp
            msgTimestamp.text = SimpleDateFormat("dd/MM HH:mm").format(date)
        }
    }
}