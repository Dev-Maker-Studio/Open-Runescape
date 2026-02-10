package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.staticmethods.StaticMethods
import jagtheora.ogg.OggPacket
import jagtheora.ogg.OggStreamState

class OggKateDecoder(oggstreamstate1: OggStreamState) : OggStreamDecoder(oggstreamstate1) {

    @JvmField var granuleRateNumerator: Int = 0
    @JvmField var granuleRateDenominator: Int = 0
    @JvmField var languageAbbreviation: String? = null
    @JvmField var category: String? = null
    @JvmField var startTime: Float = 0f
    @JvmField var endTime: Float = 0f
    @JvmField var subtitleText: String? = null

    override fun decode(oggpacket1: OggPacket) {
        if (packetCount <= 0 || "SUB" == category) {
            val rsbytebuffer3 = Packet(oggpacket1.getData())
            val i_4 = rsbytebuffer3.readUByte()
            if (packetCount <= 8) {
                if ((i_4 or 0x80) == 0) {
                    throw IllegalStateException()
                }

                if (packetCount == 0) {
                    rsbytebuffer3.currentPosition += 23
                    granuleRateNumerator = rsbytebuffer3.readUIntLittle()
                    granuleRateDenominator = rsbytebuffer3.readUIntLittle()
                    if (granuleRateNumerator == 0 || granuleRateDenominator == 0) {
                        throw IllegalStateException()
                    }

                    val rsbytebuffer12 = Packet(16)
                    rsbytebuffer3.readBytes(rsbytebuffer12.buffer, 0, 16)
                    languageAbbreviation = rsbytebuffer12.readString()
                    rsbytebuffer12.currentPosition = 0
                    rsbytebuffer3.readBytes(rsbytebuffer12.buffer, 0, 16)
                    category = rsbytebuffer12.readString()
                }
            } else {
                if (i_4 == 0) {
                    val long_5 = rsbytebuffer3.readULongLittle()
                    val long_7 = rsbytebuffer3.readULongLittle()
                    val long_9 = rsbytebuffer3.readULongLittle()
                    if (long_7 >= 0L && long_9 >= 0L && long_9 <= long_5) {
                        startTime = (granuleRateDenominator * long_5).toFloat() / granuleRateNumerator
                        endTime = (granuleRateDenominator * (long_7 + long_5)).toFloat() / granuleRateNumerator
                        val i_11 = rsbytebuffer3.readUIntLittle()
                        if (i_11 >= 0 && i_11 <= rsbytebuffer3.buffer.size - rsbytebuffer3.currentPosition) {
                            subtitleText = StaticMethods.decodeUtf8(rsbytebuffer3.buffer, rsbytebuffer3.currentPosition, i_11)
                        } else {
                            throw IllegalStateException()
                        }
                    } else {
                        throw IllegalStateException()
                    }
                }

                if ((i_4 or 0x80) != 0) {
                    // NOOP
                }
            }
        }
    }

    fun getStartTime(): Float {
        return startTime
    }

    fun getLanguageAbbreviation(): String? {
        return languageAbbreviation
    }

    fun getSubtitleText(): String? {
        return subtitleText
    }

    override fun reset() {
    }

    fun getEndTime(): Float {
        return endTime
    }
}
