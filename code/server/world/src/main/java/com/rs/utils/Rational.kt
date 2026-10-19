package com.rs.utils

import org.darkan.core.Utils
import kotlin.math.abs
import kotlin.math.pow

class Rational(val num: Long, val denom: Long) {

    override fun toString(): String = asFraction(num, denom)

    companion object {
        @JvmStatic
        fun asFraction(a: Long, b: Long): String {
            val gcm = Utils.gcm(a, b)
            return "${a / gcm}/${b / gcm}"
        }

        @JvmStatic
        @JvmOverloads
        fun toRational(value: Double, largestRightOfDecimal: Int = 4): Rational {
            val sign = if (value < 0) -1L else 1L
            val number = abs(value)
            val secondMultiplierMax = 10.0.pow(largestRightOfDecimal - 1).toLong()
            val firstMultiplierMax = secondMultiplierMax * 10L
            val maxError = 10.0.pow(-largestRightOfDecimal - 1)
            var firstMultiplier = 1L
            var secondMultiplier = 1L
            var exact = false
            var truncated = number.toLong()
            var error = number - truncated
            while (error >= maxError && firstMultiplier <= firstMultiplierMax) {
                secondMultiplier = 1
                firstMultiplier *= 10
                while (secondMultiplier <= secondMultiplierMax && secondMultiplier < firstMultiplier) {
                    val difference = number * firstMultiplier - number * secondMultiplier
                    truncated = difference.toLong()
                    error = difference - truncated
                    if (error < maxError) {
                        exact = true
                        break
                    }
                    secondMultiplier *= 10
                }
            }
            if (exact) return Rational(sign * truncated, firstMultiplier - secondMultiplier)
            return Rational((sign * number * firstMultiplierMax).toLong(), firstMultiplierMax)
        }
    }
}
