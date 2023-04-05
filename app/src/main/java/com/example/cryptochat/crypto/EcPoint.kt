package com.example.cryptochat.crypto

import android.util.Log
import java.math.BigInteger

val two: BigInteger = BigInteger.valueOf(2)

private fun bigIntSqrt(n: BigInteger): BigInteger {
    if (n == BigInteger.ZERO || n == BigInteger.ONE) {
        return n
    }
    var left = BigInteger.ONE
    var right = n
    while (left <= right) {
        val mid = left + (right - left) / BigInteger("2")
        val midSquared = mid * mid
        if (midSquared == n) {
            return mid
        } else if (midSquared < n) {
            left = mid + BigInteger.ONE
        } else {
            right = mid - BigInteger.ONE
        }
    }
    return right
}

fun shanksTonelli(n: BigInteger, p: BigInteger): BigInteger? {
    val legendreSymbol = n.modPow((p - BigInteger.ONE) / two, p)
    if (legendreSymbol != BigInteger.ONE) {
        // If n is not a quadratic residue, return null
        return null
    }

    var q = p - BigInteger.ONE
    var s = 0
    while (q % two == BigInteger.ZERO) {
        q /= two
        s++
    }

    var z = two
    while (z.modPow((p - BigInteger.ONE) / two, p) == BigInteger.ONE) {
        z++
    }

    var c = z.modPow(q, p)
    var r = n.modPow((q + BigInteger.ONE) / two, p)
    var t = n.modPow(q, p)
    var m = s

    while (t != BigInteger.ONE) {
        var i = 0
        var ts = t
        while (ts != BigInteger.ONE) {
            ts = ts.modPow(two, p)
            i++
        }

        val b = c.modPow(two.pow(m - i - 1), p)
        r = r * b % p
        t = t * b.modPow(two, p) % p
        c = b.modPow(two, p)
        m = i
    }

    return r
}

fun decodeSec1(sec1: String, curve: EcCurve) : EcPoint? {
    val char = sec1[1]
    val y_is_even = (char == '2')
    val nx = BigInteger(sec1.substring(2), 16)
    val alpha = nx.modPow(BigInteger.valueOf(3), curve.p) + (curve.a.multiply(nx).add(curve.b)).mod(curve.p)
    Log.d("asd", "asd")
    Log.d("asd", shanksTonelli(BigInteger.valueOf(2), BigInteger.valueOf(113)).toString())
    Log.d("asd", "asd")
    Log.d("asd", "asd")
    Log.d("asd", "asd")
    val beta = shanksTonelli(alpha, curve.p) ?: return null
    Log.d("asd", "yay")
    val ny: BigInteger;
    if (y_is_even == (beta.mod(BigInteger.valueOf(2)) == BigInteger.ONE))
        ny = curve.p.subtract(beta)
    else ny = beta

    return EcPoint(nx, ny, curve)
}

/**
 * A point on an elliptical curve (x, y).
 *
 * @property x The x value of the point on the curve.
 * @property y The y value of the point on the curve.
 * @property curve The curve the point belongs to.
 */
class EcPoint (val x : BigInteger, val y : BigInteger, val curve: EcCurve) {
    /**
     * Adds a point to this point.
     *
     * @param other The point to add to this point.
     * @return The sum of the two points.
     */
    operator fun plus(other: EcPoint): EcPoint {
        return curve.add(this, other)
    }

    /**
     * Finds the product of this point and a number. (dotting the curve multiple times)
     *
     * @param n The number to multiply the point by.
     * @return The product of the point and the number.
     */
    operator fun times(n: BigInteger): EcPoint {
        return curve.multiply(this, n)
    }

    override fun equals(other: Any?): Boolean {
        if (other is EcPoint) {
            return (x == other.x && y == other.y)
        }

        return false
    }

    override fun hashCode(): Int {
        return x.hashCode() + y.hashCode()
    }

    override fun toString(): String {
        val prefix = "0" + if (y.mod(BigInteger("2", 10)) == BigInteger.ONE) '3' else '2'
        val pointBytes = x.toByteArray()
        val ans = pointBytes.joinToString("") {
            if (it.toInt() != 0) String.format("%02x", it) else ""
        }
        return prefix + ans
    }
}