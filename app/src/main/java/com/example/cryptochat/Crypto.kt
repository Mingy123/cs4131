package com.example.cryptochat

import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

private const val EC_ALGORITHM = "SHA256withECDSA"
val keyFactory: KeyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)

fun privkeyFromString(privatePEMKey: String): PrivateKey {
    val privateKey = privatePEMKey
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("/n", "")
        .trim()
    val encodedPrivateKey = Base64.decode(privateKey, Base64.DEFAULT)
    val encodedKeySpec = PKCS8EncodedKeySpec(encodedPrivateKey)
    return keyFactory.generatePrivate(encodedKeySpec) as ECPrivateKey
}

fun pubkeyFromString(publicPEMKey: String): PublicKey {
    val publicKey = publicPEMKey
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("/n", "")
        .trim()
    val decodedPublicKey = Base64.decode(publicKey, Base64.DEFAULT)
    val encodedKeySpec = X509EncodedKeySpec(decodedPublicKey)
    return keyFactory.generatePublic(encodedKeySpec)
}

fun makeSignature(privateKey: PrivateKey, data: ByteArray): ByteArray =
    Signature.getInstance(EC_ALGORITHM).run {
        initSign(privateKey)
        update(data)
        sign()
    }

fun verifySignature(publicKey: PublicKey, data: ByteArray, realSignature: ByteArray) =
    Signature.getInstance(EC_ALGORITHM).run {
        initVerify(publicKey)
        update(data)
        verify(realSignature)
    }