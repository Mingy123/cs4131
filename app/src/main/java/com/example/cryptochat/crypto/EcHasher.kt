package com.example.cryptochat.crypto

interface EcHasher {
    fun hash (data : ByteArray) : ByteArray
}
