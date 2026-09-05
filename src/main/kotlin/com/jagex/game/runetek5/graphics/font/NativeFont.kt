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
    toolkit: AbstractNativeToolkit,
    fontMetrics: FontMetrics,
    characterSprites: Array<SpriteType>,
    isPlain: Boolean
) : AbstractFontRenderer(toolkit, fontMetrics) {

    @JvmField val nativeToolkit: AbstractNativeToolkit
    @JvmField val glyphAtlasSampler: NativeTextureSampler
    @JvmField val glyphVertexStream: NativeStream

    init {
        nativeToolkit = toolkit
        var maxDimension = 0

        var atlasSize: Int
        for (charIndex in 0 until 256) {
            val charSprite = characterSprites[charIndex]
            if (charSprite.height > maxDimension) {
                maxDimension = charSprite.height
            }

            if (charSprite.width > maxDimension) {
                maxDimension = charSprite.width
            }
        }

        atlasSize = maxDimension * 16
        if (isPlain) {
            val pixels = ByteArray(atlasSize * atlasSize)

            for (charIndex in 0 until 256) {
                val charSprite = characterSprites[charIndex]
                val glyphHeight = charSprite.height
                val glyphWidth = charSprite.width
                val cellX = maxDimension * (charIndex % 16)
                val cellY = charIndex / 16 * maxDimension
                var atlasIndex = cellX + cellY * atlasSize
                var pixelIndex = 0
                val maskBytes: ByteArray
                if (charSprite.alpha == null) {
                    maskBytes = charSprite.pixels

                    for (y in 0 until glyphHeight) {
                        for (x in 0 until glyphWidth) {
                            pixels[atlasIndex++] = (if (maskBytes[pixelIndex++] == 0.toByte()) 0 else -1).toByte()
                        }

                        atlasIndex += atlasSize - glyphWidth
                    }
                } else {
                    maskBytes = charSprite.alpha!!

                    for (y in 0 until glyphHeight) {
                        for (x in 0 until glyphWidth) {
                            pixels[atlasIndex++] = maskBytes[pixelIndex++]
                        }

                        atlasIndex += atlasSize - glyphWidth
                    }
                }
            }

            if (!toolkit.supportsTextureFormat(GLFormat.ALPHA, GLActionFormat.PIXEL_FORMAT)) {
                val argbPixels = IntArray(pixels.size)

                for (i in pixels.indices) {
                    argbPixels[i] = pixels[i].toInt() shl 24
                }

                glyphAtlasSampler = toolkit.createTexture(atlasSize, atlasSize, false, argbPixels)
            } else {
                glyphAtlasSampler = toolkit.createTextureSampler(GLFormat.ALPHA, atlasSize, atlasSize, false, pixels)
            }
        } else {
            val pixels = IntArray(atlasSize * atlasSize)

            for (charIndex in 0 until 256) {
                val charSprite = characterSprites[charIndex]
                val palette = charSprite.pallete
                val alpha = charSprite.alpha
                val charPixels = charSprite.pixels
                val glyphHeight = charSprite.height
                val glyphWidth = charSprite.width
                val cellX = maxDimension * (charIndex % 16)
                val cellY = charIndex / 16 * maxDimension
                var atlasIndex = cellX + cellY * atlasSize
                var pixelIndex = 0
                if (alpha != null) {
                    for (y in 0 until glyphHeight) {
                        for (x in 0 until glyphWidth) {
                            pixels[atlasIndex++] = alpha[pixelIndex].toInt() shl 24 or palette[charPixels[pixelIndex].toInt() and 0xff]
                            ++pixelIndex
                        }

                        atlasIndex += atlasSize - glyphWidth
                    }
                } else {
                    for (y in 0 until glyphHeight) {
                        for (x in 0 until glyphWidth) {
                            val charPixel: Byte
                            charPixel = charPixels[pixelIndex++]
                            if (charPixel != 0.toByte()) {
                                pixels[atlasIndex++] = 0xffffff.inv() or palette[charPixel.toInt() and 0xff]
                            } else {
                                ++atlasIndex
                            }
                        }

                        atlasIndex += atlasSize - glyphWidth
                    }
                }
            }

            glyphAtlasSampler = toolkit.createTexture(atlasSize, atlasSize, false, pixels)
        }

        glyphAtlasSampler.setSamplerMode(SampleMode.NEAREST)
        glyphVertexStream = toolkit.createStream(false)
        glyphVertexStream.create(20480, 20)
        val vertexBuffer = nativeToolkit.directBuffer
        vertexBuffer.clear()
        val sScale = glyphAtlasSampler.toTextureCoordS(atlasSize.toFloat()) / atlasSize
        val tScale = glyphAtlasSampler.toTextureCoordT(atlasSize.toFloat()) / atlasSize

        for (charIndex in 0 until 256) {
            val charSprite = characterSprites[charIndex]
            val glyphHeight = charSprite.height
            val glyphWidth = charSprite.width
            val minY = charSprite.minY
            val minX = charSprite.minX
            val cellX = (maxDimension * (charIndex % 16)).toFloat()
            val cellY = (charIndex / 16 * maxDimension).toFloat()
            val s0 = cellX * sScale
            val t0 = cellY * tScale
            val s1 = (cellX + glyphWidth) * sScale
            val t1 = (cellY + glyphHeight) * tScale
            vertexBuffer.putFloat(minX.toFloat())
            vertexBuffer.putFloat(minY.toFloat())
            vertexBuffer.putFloat(0.0f)
            vertexBuffer.putFloat(s0)
            vertexBuffer.putFloat(t0)
            vertexBuffer.putFloat(minX.toFloat())
            vertexBuffer.putFloat((minY + glyphHeight).toFloat())
            vertexBuffer.putFloat(0.0f)
            vertexBuffer.putFloat(s0)
            vertexBuffer.putFloat(t1)
            vertexBuffer.putFloat((minX + glyphWidth).toFloat())
            vertexBuffer.putFloat((minY + glyphHeight).toFloat())
            vertexBuffer.putFloat(0.0f)
            vertexBuffer.putFloat(s1)
            vertexBuffer.putFloat(t1)
            vertexBuffer.putFloat((minX + glyphWidth).toFloat())
            vertexBuffer.putFloat(minY.toFloat())
            vertexBuffer.putFloat(0.0f)
            vertexBuffer.putFloat(s1)
            vertexBuffer.putFloat(t0)
        }

        glyphVertexStream.upload(0, vertexBuffer.position(), nativeToolkit.directBufferAddress)
        toolkit.getShaderByEffect(12)
    }

    override fun drawChar(charIndex: Int, translateX: Int, translateY: Int, colorArgb: Int, isShadow: Boolean) {
        nativeToolkit.setProjectionMatrixMode()
        nativeToolkit.toggleBlending(1)
        val spriteShader = nativeToolkit.spriteShader!!
        spriteShader.spriteSampler = glyphAtlasSampler
        spriteShader.setColours(3, colorArgb)
        val displayWidth = nativeToolkit.getDisplay()!!.getWidth().toFloat()
        val displayHeight = nativeToolkit.getDisplay()!!.getHeight().toFloat()
        spriteShader.worldViewProjectionMatrix.setDiagonal(2.0f / displayWidth, 2.0f / displayHeight, 1.0f, 1.0f)
        spriteShader.worldViewProjectionMatrix.elements[12] = (translateX + nativeToolkit.getPixelOffset()) * 2.0f / displayWidth - 1.0f
        spriteShader.worldViewProjectionMatrix.elements[13] = (translateY + nativeToolkit.getPixelOffset()) * 2.0f / displayHeight - 1.0f
        spriteShader.worldViewProjectionMatrix.elements[14] = -1.0f
        spriteShader.spriteTexCoordMatrix.identity()
        spriteShader.vertexStream = glyphVertexStream
        spriteShader.startVertex = charIndex * 4
        spriteShader.vertexDeclaration = nativeToolkit.positionTexCoordDeclaration
        spriteShader.loadNormal()
    }

    override fun drawChar(charIndex: Int, translateX: Int, translateY: Int, colorArgb: Int, isShadow: Boolean, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
    }

}
