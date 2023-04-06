package com.example.cryptochat.group

import com.example.cryptochat.chat.Message

data class GroupDetail(val uuid: String, val name: String, val owner: String,
                       val members: ArrayList<String>,
                       val recentMessages: ArrayList<Message>
)