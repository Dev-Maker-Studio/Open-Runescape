package com.jagex.game.runetek5.toolkit.opengl

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer
import com.jagex.game.runetek5.scene.ground.GroundFunctionMask
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboArrayBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer

import jaclib.memory.Stream
import jaclib.memory.heap.NativeHeapBuffer
import jaggl.OpenGL

import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.buffer.ArrayBufferGL

class GroundMaterialGL(
    ground: GroundGL,
    materialId: Int,
    scaling: Int,
    waterTile: WaterTile
) : Node<Any?>() {
    @JvmField var arrayBufferPointer: OpenGlArrayDrawBuffer? = null
    @JvmField var nativeHeapBuffer: NativeHeapBuffer? = null
    @JvmField var nativeStream: Stream? = null
    @JvmField val ground: GroundGL
    @JvmField val toolkit: OpenGlToolkit
    @JvmField val materialId: Int
    @JvmField val textureScale: Float
    @JvmField val waterTile: WaterTile
    @JvmField val groundFunctionMask: IntArray
    @JvmField val texture2d: GlElementsArrayBuffer

    init {
        this.ground = ground
        toolkit = ground.activeToolkit
        this.materialId = materialId
        textureScale = scaling.toFloat()
        this.waterTile = waterTile
        groundFunctionMask = IntArray(this.ground.width * this.ground.length)
        texture2d = GlElementsArrayBuffer(toolkit, null, 1)
    }

    fun appendToNativeStream(i_1: Int) {
        nativeStream!!.method2919(i_1 * 4 + 3)
        nativeStream!!.putByte(-1)
    }

    fun generateFunctionMask(x: Int, y: Int, faceCount: Int) {
        groundFunctionMask[y * ground.width + x] = groundFunctionMask[y * ground.width + x] or (1 shl faceCount)
    }

    fun method12083(i_1: Int, color: Int, lightness: Int, colorMultiplier: Float) {
        var color = color
        if (materialId != -1) {
            val material = toolkit.materialProvider!!.getMaterial(materialId)
            val contrast = material.alpha.toInt() and 0xff
            if (contrast != 0 && material.effectId.toInt() != 4) {
                val i_7: Int
                if (lightness < 0) {
                    i_7 = 0
                } else if (lightness > 127) {
                    i_7 = 16777215
                } else {
                    i_7 = lightness * 131586
                }

                val blendContrast = 256 - contrast
                color = ((i_7 and 0xff00ff) * contrast + blendContrast * (color and 0xff00ff) and -16711936) + (blendContrast * (color and 0xff00) + contrast * (i_7 and 0xff00) and 0xff0000) shr 8
            }

            var brightness = material.brightness.toInt() and 0xff
            if (brightness != 0) {
                brightness += 256
                var red = ((color and 0xff0000) shr 16) * brightness
                if (red > 65535) {
                    red = 65535
                }
                var green = ((color and 0xff00) shr 8) * brightness
                if (green > 65535) {
                    green = 65535
                }
                var blue = brightness * (color and 0xff)
                if (blue > 65535) {
                    blue = 65535
                }
                color = (blue shr 8) + (green and 0xff00) + (red shl 8 and 0xff0000)
            }
        }

        if (colorMultiplier != 1.0f) {
            var red = color shr 16 and 0xff
            var green = color shr 8 and 0xff
            var blue = color and 0xff
            red = (red * colorMultiplier).toInt()
            if (red < 0) {
                red = 0
            } else if (red > 255) {
                red = 255
            }

            green = (green * colorMultiplier).toInt()
            if (green < 0) {
                green = 0
            } else if (green > 255) {
                green = 255
            }

            blue = (blue * colorMultiplier).toInt()
            if (blue < 0) {
                blue = 0
            } else if (blue > 255) {
                blue = 255
            }
            color = red shl 16 or (green shl 8) or blue
        }
        nativeStream!!.method2919(i_1 * 4)
        nativeStream!!.putByte(color shr 16)
        nativeStream!!.putByte(color shr 8)
        nativeStream!!.putByte(color)
    }

    fun method12085(ints_1: IntArray, i_2: Int) {
        var addedTextures = 0
        val textureBuffer = toolkit.textureBuffer
        textureBuffer.currentPosition = 0
        if (toolkit.bigEndianess) {
            for (i_5 in 0 until i_2) {
                val i_6 = ints_1[i_5]
                val materialsByTriangle = ground.materialIndices[i_6]
                val functionMask = groundFunctionMask[i_6]
                if (functionMask != 0 && materialsByTriangle != null) {
                    var loops = 0
                    var verticeCount = 0
                    while (verticeCount < materialsByTriangle.size) {
                        if ((functionMask and (1 shl loops++)) != 0) {
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                        } else {
                            verticeCount += 3
                        }
                    }
                }
            }
        } else {
            for (i_5 in 0 until i_2) {
                val i_6 = ints_1[i_5]
                val materialsByTriangle = ground.materialIndices[i_6]
                val functionMask = groundFunctionMask[i_6]
                if (functionMask != 0 && materialsByTriangle != null) {
                    var loops = 0
                    var verticeCount = 0

                    while (verticeCount < materialsByTriangle.size) {
                        if ((functionMask and (1 shl loops++)) != 0) {
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                        } else {
                            verticeCount += 3
                        }
                    }
                }
            }
        }
        if (addedTextures > 0) {
            texture2d.putElements(textureBuffer.buffer, textureBuffer.currentPosition)
            toolkit.toggleDrawingClientstates(ground.vertexBuffer, ground.normalsBuffer, arrayBufferPointer, ground.textureBuffer)
            toolkit.renderTexture(materialId, (ground.waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0, (ground.waterFunctionMask and 0x8) != 0)
            if (toolkit.stillWaterToggled) {
                toolkit.addWaterTile(Int.MAX_VALUE, waterTile)
            }

            OpenGL.glMatrixMode(GL_TEXTURE)
            OpenGL.glPushMatrix()
            OpenGL.glScalef(1.0f / textureScale, 1.0f / textureScale, 1.0f)
            OpenGL.glMatrixMode(GL_MODELVIEW)
            toolkit.toggleDrawingClientstates(ground.vertexBuffer, ground.normalsBuffer, arrayBufferPointer, ground.textureBuffer)
            toolkit.drawElements(texture2d, 0, addedTextures)
            OpenGL.glMatrixMode(GL_TEXTURE)
            OpenGL.glPopMatrix()
            OpenGL.glMatrixMode(GL_MODELVIEW)
        }

    }

    fun createDataArrayBuffer(totalTiles: Int) {
        nativeStream!!.method2925()
        val pointer = toolkit.createDataArrayBuffer(4, nativeHeapBuffer, totalTiles * 4)
        if (pointer is GlVboArrayBuffer) {
            nativeHeapBuffer!!.deallocateBuffer()
        }
        arrayBufferPointer = OpenGlArrayDrawBuffer(pointer, GL_UNSIGNED_BYTE, 4, 0)
        nativeHeapBuffer = null
        nativeStream = null
    }

    fun generateNativeBuffer(colorCount: Int) {
        nativeHeapBuffer = toolkit.nativeHeap.createBuffer(colorCount * 4, true)
        nativeStream = Stream(nativeHeapBuffer!!)
    }

}
