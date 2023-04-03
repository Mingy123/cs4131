package com.example.cryptochat.group

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptochat.R
import com.example.cryptochat.chat.ChatActivity

class GroupAdapter(val groupList: ArrayList<Group>) :
    RecyclerView.Adapter<GroupAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.group_card, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(groupList[position])
    }

    override fun getItemCount() = groupList.size
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var groupName: TextView
        var groupUuid: TextView

        init {
            groupName = itemView.findViewById(R.id.groupName)
            groupUuid = itemView.findViewById(R.id.groupUuid)
            itemView.setOnClickListener { view ->
                val intent = Intent(itemView.context, ChatActivity::class.java)
                intent.putExtra("uuid", groupUuid.text)
                itemView.context.startActivity(intent)
            }
        }

        fun bindItems(group: Group) {
            groupName.text = group.name
            groupUuid.text = group.uuid
        }
    }
}