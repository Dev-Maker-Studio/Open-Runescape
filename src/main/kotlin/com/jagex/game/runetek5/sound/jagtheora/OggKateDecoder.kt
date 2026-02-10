package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.staticmethods.StaticMethods
import jagtheora.ogg.OggPacket
import jagtheora.ogg.OggStreamState

class OggKateDecoder(oggstreamstate1: OggStreamState) : OggStreamDecoder(oggstreamstate1) {

    @JvmField var anInt9464: Int = 0
    @JvmField var anInt9467: Int = 0
    @JvmField var languageAbbreviation: String? = null
    @JvmField var aString9469: String? = null
    @JvmField var aFloat9462: Float = 0f
    @JvmField var aFloat9465: Float = 0f
    @JvmField var aString9470: String? = null

    override fun decode(oggpacket1: OggPacket) {
        if (anInt8031 <= 0 || "SUB" == aString9469) {
            val rsbytebuffer3 = Packet(oggpacket1.getData())
            val i_4 = rsbytebuffer3.readUByte()
            if (anInt8031 <= 8) {
                if ((i_4 or 0x80) == 0) {
                    throw IllegalStateException()
                }

                if (anInt8031 == 0) {
                    rsbytebuffer3.currentPosition += 23
                    anInt9464 = rsbytebuffer3.readUIntLittle()
                    anInt9467 = rsbytebuffer3.readUIntLittle()
                    if (anInt9464 == 0 || anInt9467 == 0) {
                        throw IllegalStateException()
                    }

                    val rsbytebuffer12 = Packet(16)
                    rsbytebuffer3.readBytes(rsbytebuffer12.buffer, 0, 16)
                    languageAbbreviation = rsbytebuffer12.readString()
                    rsbytebuffer12.currentPosition = 0
                    rsbytebuffer3.readBytes(rsbytebuffer12.buffer, 0, 16)
                    aString9469 = rsbytebuffer12.readString()
                }
            } else {
                if (i_4 == 0) {
                    val long_5 = rsbytebuffer3.readULongLittle()
                    val long_7 = rsbytebuffer3.readULongLittle()
                    val long_9 = rsbytebuffer3.readULongLittle()
                    if (long_7 >= 0L && long_9 >= 0L && long_9 <= long_5) {
                        aFloat9462 = (anInt9467 * long_5).toFloat() / anInt9464
                        aFloat9465 = (anInt9467 * (long_7 + long_5)).toFloat() / anInt9464
                        val i_11 = rsbytebuffer3.readUIntLittle()
                        if (i_11 >= 0 && i_11 <= rsbytebuffer3.buffer.size - rsbytebuffer3.currentPosition) {
                            aString9470 = StaticMethods.method4824(rsbytebuffer3.buffer, rsbytebuffer3.currentPosition, i_11)
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

    fun method14702(): Float {
        return aFloat9462
    }

    fun getLanguageAbbreviation(): String? {
        return languageAbbreviation
    }

    fun method14704(): String? {
        return aString9470
    }

    override fun reset() {
    }

    fun method14705(): Float {
        return aFloat9465
    }
}
