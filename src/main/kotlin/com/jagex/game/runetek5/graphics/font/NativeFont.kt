package com.jagex.game.runetek5.graphics.font

import java.nio.ByteBuffer

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.SampleMode
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

class NativeFont(
    class505Sub21: AbstractNativeToolkit,
    fontmetrics2: FontMetrics,
    arr_3: Array<SpriteType>,
    bool4: Boolean
) : AbstractFontRenderer(class505Sub21, fontmetrics2) {

    @JvmField val aGraphicalRendererSub29321: AbstractNativeToolkit
    @JvmField val anInterface69322: NativeTextureSampler
    @JvmField val anInterface49320: NativeStream

    init {
        aGraphicalRendererSub29321 = class505Sub21
        var i_5 = 0

        var i_6: Int
        for (i_6 in 0 until 256) {
            val class917 = arr_3[i_6]
            if (class917.height > i_5) {
                i_5 = class917.height
            }

            if (class917.width > i_5) {
                i_5 = class917.width
            }
        }

        i_6 = i_5 * 16
        var i_8: Int
        var i_10: Int
        var i_12: Int
        var i_13: Int
        var i_14: Int
        var i_15: Int
        var i_17: Int
        var i_18: Int
        var class9123: SpriteType
        if (bool4) {
            val bytes_28 = ByteArray(i_6 * i_6)

            for (i_8 in 0 until 256) {
                class9123 = arr_3[i_8]
                i_10 = class9123.height
                val i_11 = class9123.width
                i_12 = i_5 * (i_8 % 16)
                i_13 = i_8 / 16 * i_5
                i_14 = i_12 + i_13 * i_6
                i_15 = 0
                val bytes_16: ByteArray
                if (class9123.alpha == null) {
                    bytes_16 = class9123.pixels

                    for (i_17 in 0 until i_10) {
                        for (i_18 in 0 until i_11) {
                            bytes_28[i_14++] = (if (bytes_16[i_15++] == 0.toByte()) 0 else -1).toByte()
                        }

                        i_14 += i_6 - i_11
                    }
                } else {
                    bytes_16 = class9123.alpha!!

                    for (i_17 in 0 until i_10) {
                        for (i_18 in 0 until i_11) {
                            bytes_28[i_14++] = bytes_16[i_15++]
                        }

                        i_14 += i_6 - i_11
                    }
                }
            }

            if (!class505Sub21.supportsTextureFormat(GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT)) {
                val ints_22 = IntArray(bytes_28.size)

                for (i_9 in bytes_28.indices) {
                    ints_22[i_9] = bytes_28[i_9].toInt() shl 24
                }

                anInterface69322 = class505Sub21.createTexture(i_6, i_6, false, ints_22)
            } else {
                anInterface69322 = class505Sub21.createTextureSampler(GLFormat.RGBA, i_6, i_6, false, bytes_28)
            }
        } else {
            val ints_29 = IntArray(i_6 * i_6)

            for (i_8 in 0 until 256) {
                class9123 = arr_3[i_8]
                val ints_24 = class9123.pallete
                val bytes_25 = class9123.alpha
                val bytes_26 = class9123.pixels
                i_13 = class9123.height
                i_14 = class9123.width
                i_15 = i_5 * (i_8 % 16)
                val i_27 = i_8 / 16 * i_5
                i_17 = i_15 + i_27 * i_6
                i_18 = 0
                var i_19: Int
                var i_20: Int
                if (bytes_25 != null) {
                    for (i_19 in 0 until i_13) {
                        for (i_20 in 0 until i_14) {
                            ints_29[i_17++] = bytes_25[i_18].toInt() shl 24 or ints_24[bytes_26[i_18].toInt() and 0xff]
                            ++i_18
                        }

                        i_17 += i_6 - i_14
                    }
                } else {
                    for (i_19 in 0 until i_13) {
                        for (i_20 in 0 until i_14) {
                            val b_21: Byte
                            b_21 = bytes_26[i_18++]
                            if (b_21 != 0.toByte()) {
                                ints_29[i_17++] = 0xffffff.inv() or ints_24[b_21.toInt() and 0xff]
                            } else {
                                ++i_17
                            }
                        }

                        i_17 += i_6 - i_14
                    }
                }
            }

            anInterface69322 = class505Sub21.createTexture(i_6, i_6, false, ints_29)
        }

        anInterface69322.setSamplerMode(SampleMode.NEAREST)
        anInterface49320 = class505Sub21.createStream(false)
        anInterface49320.create(20480, 20)
        val bytebuffer30 = aGraphicalRendererSub29321.directBuffer
        bytebuffer30.clear()
        val f_31 = anInterface69322.method47(i_6.toFloat()) / i_6
        val f_32 = anInterface69322.method62(i_6.toFloat()) / i_6

        for (i_10 in 0 until 256) {
            val class9138 = arr_3[i_10]
            i_12 = class9138.height
            i_13 = class9138.width
            i_14 = class9138.minY
            i_15 = class9138.minX
            val f_39 = (i_5 * (i_10 % 16)).toFloat()
            val f_33 = (i_10 / 16 * i_5).toFloat()
            val f_34 = f_39 * f_31
            val f_35 = f_33 * f_32
            val f_36 = (f_39 + i_13) * f_31
            val f_37 = (f_33 + i_12) * f_32
            bytebuffer30.putFloat(i_15.toFloat())
            bytebuffer30.putFloat(i_14.toFloat())
            bytebuffer30.putFloat(0.0f)
            bytebuffer30.putFloat(f_34)
            bytebuffer30.putFloat(f_35)
            bytebuffer30.putFloat(i_15.toFloat())
            bytebuffer30.putFloat((i_14 + i_12).toFloat())
            bytebuffer30.putFloat(0.0f)
            bytebuffer30.putFloat(f_34)
            bytebuffer30.putFloat(f_37)
            bytebuffer30.putFloat((i_15 + i_13).toFloat())
            bytebuffer30.putFloat((i_14 + i_12).toFloat())
            bytebuffer30.putFloat(0.0f)
            bytebuffer30.putFloat(f_36)
            bytebuffer30.putFloat(f_37)
            bytebuffer30.putFloat((i_15 + i_13).toFloat())
            bytebuffer30.putFloat(i_14.toFloat())
            bytebuffer30.putFloat(0.0f)
            bytebuffer30.putFloat(f_36)
            bytebuffer30.putFloat(f_35)
        }

        anInterface49320.upload(0, bytebuffer30.position(), aGraphicalRendererSub29321.directBufferAddress)
        class505Sub21.getShaderByEffect(12)
    }

    override fun drawChar(var_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean) {
        aGraphicalRendererSub29321.setProjectionMatrixMode()
        aGraphicalRendererSub29321.toggleBlending(1)
        val class476 = aGraphicalRendererSub29321.spriteShader!!
        class476.spriteSampler = anInterface69322
        class476.setColours(3, i_4)
        val f_7 = aGraphicalRendererSub29321.getDisplay()!!.getWidth().toFloat()
        val f_8 = aGraphicalRendererSub29321.getDisplay()!!.getHeight().toFloat()
        class476.worldViewProjectionMatrix.method6525(2.0f / f_7, 2.0f / f_8, 1.0f, 1.0f)
        class476.worldViewProjectionMatrix.faceList[12] = (i_2 + aGraphicalRendererSub29321.getPixelOffset()) * 2.0f / f_7 - 1.0f
        class476.worldViewProjectionMatrix.faceList[13] = (i_3 + aGraphicalRendererSub29321.getPixelOffset()) * 2.0f / f_8 - 1.0f
        class476.worldViewProjectionMatrix.faceList[14] = -1.0f
        class476.spriteTexCoordMatrix.identity()
        class476.vertexStream = anInterface49320
        class476.startVertex = var_1 * 4
        class476.vertexDeclaration = aGraphicalRendererSub29321.positionTexCoordDeclaration
        class476.loadNormal()
    }

    override fun drawChar(var_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean, class4556: ComponentSurface, i_7: Int, i_8: Int) {
    }

}
