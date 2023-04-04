package com.example.cryptochat.crypto

import java.math.BigInteger

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

fun decodeSec1(sec1: String, curve: EcCurve) : EcPoint {
    val char = sec1[1]
    val y_is_odd = (char == '2')
    val nx = BigInteger(sec1.substring(2), 16)
    val ny = bigIntSqrt(nx.modPow(BigInteger("3", 10), curve.p).add(curve.a.multiply(nx)).add(curve.b).mod(curve.p))

    val yCoordinate = if (y_is_odd) ny else curve.p.subtract(ny)
    return EcPoint(nx, yCoordinate, curve)
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