package com.example.cryptochat.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request.Method
import com.android.volley.toolbox.Volley
import com.example.cryptochat.AuthorisedRequest
import com.example.cryptochat.R
import com.example.cryptochat.crypto.*
import com.example.cryptochat.hashColor
import com.example.cryptochat.usernameFromPubkey
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.coroutineContext

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
                val context = view.context
                val queue = Volley.newRequestQueue(view.context)
                val request = AuthorisedRequest(Method.GET, "/message-info?hash=${msg.hash}",
                    { response ->
                        // ignoring the message's group
                        val part = response.substringAfter(',').dropLast(1)
                        // TODO: create dialog
                        val dialog = AlertDialog.Builder(context)
                        val inflater = LayoutInflater.from(context)
                        val popup = inflater.inflate(R.layout.message_details, null)
                        popup.findViewById<TextView>(R.id.messageContentShort).text = msgContent.text
                        popup.findViewById<TextView>(R.id.senderUsername).text = msgSender.text
                        popup.findViewById<TextView>(R.id.senderPubkey).text = msg.sender
                        popup.findViewById<TextView>(R.id.senderPubkey).text = msg.sender

                        // CRYPTO
                        val thing = msg.signature.split(',')
                        val signature = EcSignature(BigInteger(thing[0], 16), BigInteger(thing[1], 16))
                        val publicKey = decodeSec1(msg.sender, Secp256k1)
                        val correct = if (publicKey == null) false
                        else {
                            EcSign.verifySignature(publicKey,
                                msg.content.toByteArray(), EcSha256,
                                signature
                            )
                        }
                        if (correct) {
                            popup.findViewById<ImageView>(R.id.signatureStatusIcon)
                                .setImageResource(R.drawable.baseline_thumb_up_24)
                            popup.findViewById<TextView>(R.id.signatureStatus).text =
                                context.getString(R.string.signature_correct)
                        } else {
                            popup.findViewById<ImageView>(R.id.signatureStatusIcon)
                                .setImageResource(R.drawable.baseline_warning_24)
                            popup.findViewById<TextView>(R.id.signatureStatus).text =
                                context.getString(R.string.signature_wrong)
                        }

                        dialog.setView(popup)
                        dialog.setTitle(context.getString(R.string.message_details_title))
                        dialog.show()
                    },
                    {
                        Snackbar.make(view, view.context.getString(R.string.network_error),
                            Snackbar.LENGTH_SHORT).show()
                    }
                )
                queue.add(request)
                true
            }
        }

        fun bindItems(msg: Message) {
            this.msg = msg
            val username = usernameFromPubkey(itemView.context, msg.sender)
            if (username == null) msgSender.text = msg.sender.substring(16) + "..."
            else msgSender.text = username
            msgSender.setTextColor(hashColor(msg.sender))
            msgContent.text = msg.content
            val date = Date()
            date.time = msg.timestamp
            msgTimestamp.text = SimpleDateFormat("dd/MM HH:mm").format(date)
        }
    }
}