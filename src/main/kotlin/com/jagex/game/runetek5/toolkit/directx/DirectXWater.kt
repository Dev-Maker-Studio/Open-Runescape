package com.jagex.game.runetek5.toolkit.directx

import jaclib.memory.Stream
import jaclib.memory.heap.NativeHeapBuffer
import java.nio.ByteBuffer
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.scene.ground.GroundDX
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.NativeStream

class DirectXWater(
    @JvmField val directxGround: GroundDX,
    @JvmField val textureId: Int,
    waterScale: Int,
    @JvmField val hdWaterTile: WaterTile
) : Node<Any?>() {

    @JvmField var anInt7508: Int = 0
    @JvmField var anInt7514: Int = 0
    @JvmField var anInt7519: Int = 0
    @JvmField var anInt7517: Int = 0
    @JvmField val nativeToolkit: AbstractNativeToolkit
    @JvmField val waterScale: Float
    @JvmField val anIntArray7515: IntArray
    @JvmField var directxWaterBuffer: NativeStream? = null
    @JvmField var nativeHeap: NativeHeapBuffer? = null
    @JvmField var buffer: Stream? = null

    init {
        nativeToolkit = directxGround.activeToolkit
        this.waterScale = waterScale.toFloat()
        anIntArray7515 = IntArray(directxGround.width * directxGround.length)
        var effect: Int = 10
        if (textureId >= 0) {
            val material = nativeToolkit.materialProvider!!.getMaterial(textureId)
            if (material.effectId.toInt() > 0) {
                effect = material.effectId.toInt()
            }
        }

        nativeToolkit.getShaderByEffect(effect)
        nativeToolkit.getShaderByEffect(3)
    }

    fun method12143(i_1: Int) {
        buffer!!.method2919(i_1 * 4 + 3)
        buffer!!.putByte(-1)
    }

    fun method12145(i_1: Int, colorRgb: Int, intensity: Int, transparency: Float) {
        var colorRgb = colorRgb
        if (textureId != -1) {
            val material = nativeToolkit.materialProvider!!.getMaterial(textureId)
            val alpha = material.alpha.toInt() and 0xff
            var baseColor: Int
            if (alpha != 0 && material.effectId.toInt() != 4) {
                if (intensity < 0) {
                    baseColor = 0
                } else if (intensity > 127) {
                    baseColor = 16777215
                } else {
                    baseColor = intensity * 131586
                }

                val i_9 = 256 - alpha
                colorRgb = ((baseColor and 0xff00ff) * alpha + i_9 * (colorRgb and 0xff00ff) and -16711936) + (i_9 * (colorRgb and 0xff00) + alpha * (baseColor and 0xff00) and 0xff0000) shr 8
            }

            val brightness = material.brightness.toInt() and 0xff
            if (brightness != 0) {
                var brightness_adj = brightness + 256
                var red = ((colorRgb and 0xff0000) shr 16) * brightness_adj
                if (red > 65535) {
                    red = 65535
                }

                var green = ((colorRgb and 0xff00) shr 8) * brightness_adj
                if (green > 65535) {
                    green = 65535
                }

                var blue = brightness_adj * (colorRgb and 0xff)
                if (blue > 65535) {
                    blue = 65535
                }

                colorRgb = (blue shr 8) + (green and 0xff00) + (red shl 8 and 0xff0000)
            }
        }

        if (transparency != 1.0f) {
            var red = colorRgb shr 16 and 0xff
            var green = colorRgb shr 8 and 0xff
            var blue = colorRgb and 0xff
            red = (red * transparency).toInt()
            if (red < 0) {
                red = 0
            } else if (red > 255) {
                red = 255
            }

            green = (green * transparency).toInt()
            if (green < 0) {
                green = 0
            } else if (green > 255) {
                green = 255
            }

            blue = (blue * transparency).toInt()
            if (blue < 0) {
                blue = 0
            } else if (blue > 255) {
                blue = 255
            }

            colorRgb = red shl 16 or (green shl 8) or blue
        }

        buffer!!.method2919(i_1 * 4)
        if (nativeToolkit.endiannessType == 0) {
            buffer!!.putByte(colorRgb)
            buffer!!.putByte(colorRgb shr 8)
            buffer!!.putByte(colorRgb shr 16)
        } else {
            buffer!!.putByte(colorRgb shr 16)
            buffer!!.putByte(colorRgb shr 8)
            buffer!!.putByte(colorRgb)
        }
    }

    fun method12146(i_1: Int) {
        buffer!!.method2925()
        directxWaterBuffer = nativeToolkit.createStream(false)
        directxWaterBuffer!!.createFromSource(i_1 * 4, 4, nativeHeap!!)
        nativeHeap = null
        buffer = null
    }

    fun method12147(ints_1: IntArray, i_2: Int) {
        anInt7514 = 0
        anInt7519 = 32767
        anInt7517 = -32768
        val bytebuffer3 = nativeToolkit.directBuffer

        label49@
        for (i_4 in 0 until i_2) {
            val i_5 = ints_1[i_4]
            val shorts_6 = directxGround.tileVertices[i_5]
            val i_7 = anIntArray7515[i_5]
            if (i_7 != 0 && shorts_6 != null) {
                var i_8 = 0
                var i_9 = 0

                while (true) {
                    while (true) {
                        if (i_9 >= shorts_6.size) {
                            continue@label49
                        }

                        if ((i_7 and (1 shl i_8++)) != 0) {
                            for (i_10 in 0 until 3) {
                                val i_11 = shorts_6[i_9++].toInt() and 0xffff
                                if (i_11 > anInt7517) {
                                    anInt7517 = i_11
                                }

                                if (i_11 < anInt7519) {
                                    anInt7519 = i_11
                                }

                                bytebuffer3.putShort(i_11.toShort())
                            }

                            anInt7514 += 3
                        } else {
                            i_9 += 3
                        }
                    }
                }
            }
        }
    }

    fun method12150(i_1: Int) {
        nativeHeap = nativeToolkit.generateNativeHeapBuffer(i_1 * 4, true)
        buffer = Stream(nativeHeap!!, 0, i_1 * 4)
    }

    fun method12152(i_1: Int, i_2: Int, i_3: Int) {
        anIntArray7515[i_2 * directxGround.width + i_1] = anIntArray7515[i_2 * directxGround.width + i_1] or (1 shl i_3)
        ++anInt7508
    }
}
