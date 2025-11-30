package com.jagex.game.runetek5.graphics.shadow

import java.nio.ByteBuffer

import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.scene.ground.GroundDX
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

internal class ShadowTileDX(
    @JvmField val toolkit: AbstractNativeToolkit,
    @JvmField val aClass74711: ShadowMapperDX,
    ground: GroundDX,
    i_4: Int,
    i_5: Int,
    i_6: Int,
    @JvmField val anInt712: Int,
    @JvmField val anInt718: Int
) {

    @JvmField var needsUpdate: Boolean = true
    @JvmField var anInt715: Int = -1
    @JvmField var textureSampler: NativeTextureSampler? = null
    @JvmField val anInt716: Int
    @JvmField var anInt709: Int = 0
    @JvmField var anInt719: Int = 0
    @JvmField var anInterface32717: DirectXBuffer? = null

    init {
        val i_9 = 1 shl i_6
        var i_10 = 0
        val i_11 = i_4 shl i_6
        val i_12 = i_5 shl i_6

        var i_14: Int
        var i_15: Int
        for (i_13 in 0 until i_9) {
            i_14 = ground.width * (i_12 + i_13) + i_11

            i_15 = 0
            while (i_15 < i_9) {
                val shorts_21 = ground.tileVertices[i_14++]
                if (shorts_21 != null) {
                    i_10 += shorts_21.size
                }
                i_15++
            }
        }

        if (i_10 > 0) {
            anInt709 = Int.MIN_VALUE
            anInt719 = Int.MAX_VALUE
            anInterface32717 = toolkit.method13993(false)
            anInterface32717!!.create(i_10)
            val buffer = toolkit.directBuffer
            buffer.clear()

            i_14 = 0
            while (i_14 < i_9) {
                i_15 = ground.width * (i_12 + i_14) + i_11

                var i_16 = 0
                while (i_16 < i_9) {
                    val shorts_17 = ground.tileVertices[i_15++]
                    if (shorts_17 != null) {
                        for (i_18 in shorts_17.indices) {
                            val i_19 = shorts_17[i_18].toInt() and 0xffff
                            if (i_19 < anInt719) {
                                anInt719 = i_19
                            }

                            if (i_19 > anInt709) {
                                anInt709 = i_19
                            }

                            buffer.putShort(i_19.toShort())
                        }
                    }
                    i_16++
                }
                i_14++
            }

            anInterface32717!!.upload(0, buffer.position(), toolkit.directBufferAddress)
            anInt716 = i_10 / 3
        } else {
            anInt716 = 0
            textureSampler = null
        }
    }

    fun method1288(class481: AbstractEnvironmentShader) {
        method1290(class481, anInterface32717!!, 0, anInt716)
    }

    fun method1290(environmentShader: AbstractEnvironmentShader, buffer: DirectXBuffer, i_3: Int, i_4: Int) {
        if (i_4 > 0) {
            method1293()
            toolkit.method13997(buffer)
            environmentShader.textureSampler = textureSampler
            environmentShader.anInt467 = anInt719
            environmentShader.anInt468 = anInt709 - anInt719 + 1
            environmentShader.anInt469 = i_3
            environmentShader.anInt470 = i_4
            environmentShader.loadStandartPointlight(0)
        }
    }

    fun method1293() {
        if (needsUpdate) {
            needsUpdate = false
            val shadowedPixel = aClass74711.shadowMapperData
            var i_2 = 0
            val i_3 = aClass74711.width
            var i_4 = anInt712 + aClass74711.width * anInt718

            var x = -128
            while (x < 0) {
                i_2 = (i_2 shl 8) - i_2

                var i_6 = -128
                while (i_6 < 0) {
                    if (shadowedPixel[i_4++].toInt() != 0) {
                        ++i_2
                    }
                    i_6++
                }

                i_4 += i_3 - 128
                x++
            }

            if (textureSampler != null && i_2 == anInt715) {
                // No update needed
            } else {
                anInt715 = i_2
                var i_5 = 0
                i_4 = i_3 * anInt718 + anInt712
                var x2: Int
                var y: Int
                var i_9: Int
                if (toolkit.method13954(GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT)) {
                    if (aByteArray722 == null) {
                        aByteArray722 = ByteArray(16384)
                    }

                    val bytes_10 = aByteArray722!!

                    x2 = -128
                    while (x2 < 0) {
                        y = -128
                        while (y < 0) {
                            if (shadowedPixel[i_4].toInt() != 0) {
                                bytes_10[i_5++] = 68
                            } else {
                                i_9 = 0
                                if (shadowedPixel[i_4 - 1].toInt() != 0) {
                                    ++i_9
                                }

                                if (shadowedPixel[i_4 + 1].toInt() != 0) {
                                    ++i_9
                                }

                                if (shadowedPixel[i_4 - i_3].toInt() != 0) {
                                    ++i_9
                                }

                                if (shadowedPixel[i_4 + i_3].toInt() != 0) {
                                    ++i_9
                                }

                                bytes_10[i_5++] = (i_9 * 17).toByte()
                            }

                            ++i_4
                            y++
                        }

                        i_4 += aClass74711.width - 128
                        x2++
                    }

                    if (textureSampler == null) {
                        textureSampler = toolkit.createTextureSampler(GLFormat.RGBA, 128, 128, false, aByteArray722!!)
                        textureSampler!!.setRepeat(false, false)
                    } else {
                        textureSampler!!.method52(0, 0, 128, 128, aByteArray722!!, GLFormat.RGBA, 0, 128)
                    }
                } else {
                    if (pixels == null) {
                        pixels = IntArray(16384)
                    }

                    val ints_11 = pixels!!

                    x2 = -128
                    while (x2 < 0) {
                        y = -128
                        while (y < 0) {
                            if (shadowedPixel[i_4].toInt() != 0) {
                                ints_11[i_5++] = 1140850688
                            } else {
                                i_9 = 0
                                if (shadowedPixel[i_4 - 1].toInt() != 0) {
                                    ++i_9
                                }

                                if (shadowedPixel[i_4 + 1].toInt() != 0) {
                                    ++i_9
                                }

                                if (shadowedPixel[i_4 - i_3].toInt() != 0) {
                                    ++i_9
                                }

                                if (shadowedPixel[i_4 + i_3].toInt() != 0) {
                                    ++i_9
                                }

                                ints_11[i_5++] = i_9 * 17 shl 24
                            }

                            ++i_4
                            y++
                        }

                        i_4 += aClass74711.width - 128
                        x2++
                    }

                    if (textureSampler == null) {
                        textureSampler = toolkit.createTexture(128, 128, false, pixels!!)
                        textureSampler!!.setRepeat(false, false)
                    } else {
                        textureSampler!!.method48(0, 0, 128, 128, pixels!!, 0, 128)
                    }
                }
            }
        }
    }

    companion object {
        @JvmField var pixels: IntArray? = null
        @JvmField var aByteArray722: ByteArray? = null
    }
}
