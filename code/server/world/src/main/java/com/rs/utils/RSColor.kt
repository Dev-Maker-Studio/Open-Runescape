package com.rs.utils

import org.darkan.core.Utils

class RSColor private constructor(private var hue: Int, private var saturation: Int, private var luminance: Int) {

    val value: Int
        get() = packHSL(hue, saturation, luminance)

    fun adjustHue(amount: Int) = apply { hue = Utils.clampI(hue + amount, 0, HUE_MAX) }

    fun adjustSaturation(amount: Int) = apply { saturation = Utils.clampI(saturation + amount, 0, SATURATION_MAX) }

    fun adjustLuminance(amount: Int) = apply { luminance = Utils.clampI(luminance + amount, 0, LUMINANCE_MAX) }

    companion object {
        const val HUE_MAX = 63
        const val SATURATION_MAX = 7
        const val LUMINANCE_MAX = 127

        @JvmStatic
        fun fromHSL(hsl: Int) = RSColor(hsl shr 10 and HUE_MAX, hsl shr 7 and SATURATION_MAX, hsl and LUMINANCE_MAX)

        @JvmStatic
        fun packHSL(hue: Int, saturation: Int, luminance: Int): Int =
            ((hue and HUE_MAX) shl 10 or ((saturation and SATURATION_MAX) shl 7) or (luminance and LUMINANCE_MAX)).toShort().toInt()
    }
}
