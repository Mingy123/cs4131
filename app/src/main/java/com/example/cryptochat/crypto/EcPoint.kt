package com.example.cryptochat.crypto

import java.math.BigInteger


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
        val prefix = if (y.mod(BigInteger("2", 10)) == BigInteger.ONE) '3' else '2'
        val pointBytes = x.toByteArray()
        val ans = pointBytes.joinToString("") { String.format("%x", it) }
        val builder = java.lang.StringBuilder(ans)
        if (ans[0] != '0') { builder.insert(0, '0') }
        builder.insert(1, prefix)
        return builder.toString()
    }
}