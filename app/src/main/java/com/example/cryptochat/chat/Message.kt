package com.example.cryptochat.chat

data class Message(val content: String, val sender: String, val signature: String, val timestamp: Long, val hash: String)
