package com.jagex.game.runetek5.sound.jagtheora

@Suppress("EXPOSED_PARAMETER_TYPE")
class MonoSterioAudioReference(
    @JvmField var data: MonoSterioData,
    @JvmField val key: Long
) : Comparable<Any> {

    @JvmField var refreshTime: Long = 0
    @JvmField var ticker: Long = 0

    override fun equals(other: Any?): Boolean {
        if (other is MonoSterioAudioReference) {
            return this.key == other.key
        }
        throw IllegalArgumentException()
    }

    override fun compareTo(other: Any): Int {
        return compare(other as MonoSterioAudioReference)
    }

    fun compare(toCompare: MonoSterioAudioReference): Int {
        return when {
            this.ticker < toCompare.ticker -> -1
            this.ticker > toCompare.ticker -> 1
            else -> 0
        }
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}
