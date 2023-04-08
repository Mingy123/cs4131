package com.example.cryptochat.group

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptochat.R
import com.example.cryptochat.chat.ChatActivity
import com.google.android.material.snackbar.Snackbar

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
        var intent: Intent

        init {
            groupName = itemView.findViewById(R.id.groupName)
            groupUuid = itemView.findViewById(R.id.groupUuid)
            intent = Intent(itemView.context, ChatActivity::class.java)
            val pulse = ScaleAnimation(
                1f, 0.9f,
                1f, 0.9f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 100
                repeatCount = 1
                repeatMode = Animation.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            pulse.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { }
                override fun onAnimationRepeat(animation: Animation?) { }
                override fun onAnimationEnd(animation: Animation?) {
                    itemView.context.startActivity(intent)
                }
            })
            itemView.setOnClickListener { itemView.startAnimation(pulse) }
            itemView.setOnLongClickListener { view ->
                val clipboardManager: ClipboardManager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("pubkey", groupUuid.text)
                clipboardManager.setPrimaryClip(clipData)
                Snackbar.make(view, view.context.getString(R.string.clipboard_success), Snackbar.LENGTH_SHORT).show()
                true
            }
        }

        fun bindItems(group: Group) {
            groupName.text = group.name
            groupUuid.text = group.uuid
            intent.putExtra("name", group.name)
            intent.putExtra("uuid", group.uuid)
        }
    }
}