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
            val packetBuffer = Packet(oggpacket1.getData())
            val packetType = packetBuffer.readUByte()
            if (packetCount <= 8) {
                if ((packetType or 0x80) == 0) {
                    throw IllegalStateException()
                }

                if (packetCount == 0) {
                    packetBuffer.currentPosition += 23
                    granuleRateNumerator = packetBuffer.readUIntLittle()
                    granuleRateDenominator = packetBuffer.readUIntLittle()
                    if (granuleRateNumerator == 0 || granuleRateDenominator == 0) {
                        throw IllegalStateException()
                    }

                    val stringBuffer = Packet(16)
                    packetBuffer.readBytes(stringBuffer.buffer, 0, 16)
                    languageAbbreviation = stringBuffer.readString()
                    stringBuffer.currentPosition = 0
                    packetBuffer.readBytes(stringBuffer.buffer, 0, 16)
                    category = stringBuffer.readString()
                }
            } else {
                if (packetType == 0) {
                    val startGranulePos = packetBuffer.readULongLittle()
                    val duration = packetBuffer.readULongLittle()
                    val backlink = packetBuffer.readULongLittle()
                    if (duration >= 0L && backlink >= 0L && backlink <= startGranulePos) {
                        startTime = (granuleRateDenominator * startGranulePos).toFloat() / granuleRateNumerator
                        endTime = (granuleRateDenominator * (duration + startGranulePos)).toFloat() / granuleRateNumerator
                        val textLength = packetBuffer.readUIntLittle()
                        if (textLength >= 0 && textLength <= packetBuffer.buffer.size - packetBuffer.currentPosition) {
                            subtitleText = StaticMethods.decodeUtf8(packetBuffer.buffer, packetBuffer.currentPosition, textLength)
                        } else {
                            throw IllegalStateException()
                        }
                    } else {
                        throw IllegalStateException()
                    }
                }

                if ((packetType or 0x80) != 0) {
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
