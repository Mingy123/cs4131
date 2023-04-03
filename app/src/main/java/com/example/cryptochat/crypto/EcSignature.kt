package com.example.cryptochat.crypto

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * A class to hold R and S values from a signature.
 *
 * @property r The r value of the signature
 * @property s The s value of the signature
 */
class EcSignature (val r : BigInteger, val s : BigInteger) {
    override fun toString(): String {
        return "$r,$s"
    }
}