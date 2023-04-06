package com.example.cryptochat.contact

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptochat.R
import com.example.cryptochat.group.User
import com.example.cryptochat.hashColor
import com.google.android.material.snackbar.Snackbar

class UserAdapter(val userList: ArrayList<User>) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.group_card, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(userList[position])
    }

    override fun getItemCount() = userList.size
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var username: TextView
        var pubkeyView: TextView

        init {
            username = itemView.findViewById(R.id.groupName)
            pubkeyView = itemView.findViewById(R.id.groupUuid)
            itemView.setOnLongClickListener { view ->
                val clipboardManager: ClipboardManager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("pubkey", pubkeyView.text)
                clipboardManager.setPrimaryClip(clipData)
                Snackbar.make(view, view.context.getString(R.string.clipboard_success), Snackbar.LENGTH_SHORT).show()
                true
            }
        }

        fun bindItems(user: User) {
            username.text = user.username
            username.setTextColor(hashColor(user.pubkey))
            pubkeyView.text = user.pubkey
        }
    }
}