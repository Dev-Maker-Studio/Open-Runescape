package com.jagex.game.runetek5.config.sun

import com.jagex.game.runetek5.config.material.texture.TextureTypeList
import com.jagex.game.runetek5.graphics.light.Skybox
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

class Sun(
    val sunActionType: Int,
    val sunMaterialId: Int,
    val sunPositionX: Int,
    val sunPositionY: Int,
    val sunPositionZ: Int,
    val positionOffsetStep: Int,
    val sunColor: Int,
    val invertPosition: Boolean,
    val rotationX: Int,
    val rotationY: Int,
    val rotationZ: Int
) {

    @JvmField
    var sunSize2d: Int = 0
    var positionOffset: Int = 0
    var sunSprite: NativeSprite? = null
    var sunUseDimension: Int = 0
    var sunRotationX: Int = 0
    var sunRotationY: Int = 0

    fun isSunGenerated(toolkit: AbstractToolkit, sun: Sun?): Boolean {
        return sunSprite != null || performAction(toolkit, sun)
    }

    fun move(deltaX: Int, deltaY: Int, deltaZ: Int, offsetMultiplier: Int): Boolean {
        val x: Int
        val y: Int
        val z: Int
        if (invertPosition) {
            sunSize2d = 1073741823
            x = sunPositionX
            y = sunPositionY
            z = sunPositionZ
        } else {
            x = sunPositionX - deltaX
            y = sunPositionY - deltaY
            z = sunPositionZ - deltaZ
            sunSize2d = sqrt((x * x + y * y + z * z).toDouble()).toInt()
            if (sunSize2d == 0) {
                sunSize2d = 1
            }
            val scaledX = (x shl 8) / sunSize2d
            val scaledY = (y shl 8) / sunSize2d
            val scaledZ = (z shl 8) / sunSize2d
            val sunScaling = (sqrt((scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ).toDouble()) * 256.0).toInt()
            if (sunScaling > 128) {
                val normalizedX = (scaledX shl 16) / sunScaling
                val normalizedY = (scaledY shl 16) / sunScaling
                val normalizedZ = (scaledZ shl 16) / sunScaling
                positionOffset = offsetMultiplier * positionOffsetStep / (if (invertPosition) 1024 else sunSize2d)
            } else {
                positionOffset = 0
            }
            if (positionOffset < 8) {
                sunSprite = null
                return false
            }
            var dimension = MathUtils.nextPowerOfTwo(positionOffset)
            if (dimension > offsetMultiplier) {
                dimension = MathUtils.furtestBitValue(offsetMultiplier)
            }
            if (dimension > 512) {
                dimension = 512
            }
            if (dimension != sunUseDimension) {
                sunUseDimension = dimension
            }
            sunRotationX = (asin((scaledY / 256.0f).toDouble()) * 2607.5945876176133).toInt() and 0x3fff
            sunRotationY = (atan2(scaledX.toDouble(), -scaledZ.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
            sunSprite = null
            return true
        }
        return false
    }

    fun drawColoredSun(toolkit: AbstractToolkit, sun: Sun?) {
        val mesh = Mesh.fromResource(TextureTypeList.modelResource!!, sunMaterialId)
        if (mesh != null) {
            toolkit.getClipping(SUN_CLIPPING)
            toolkit.setClipping(0, 0, sunUseDimension, sunUseDimension)
            toolkit.applyClearFlags(2, 0)
            toolkit.drawFilledRectangle(0, 0, sunUseDimension, sunUseDimension, 0, 0)
            var positionX = 0
            var positionY = 0
            var positionZ = 256
            if (sun != null) {
                if (sun.invertPosition) {
                    positionX = -sun.sunPositionX
                    positionY = -sun.sunPositionY
                    positionZ = -sun.sunPositionZ
                } else {
                    positionX = sunPositionX - sun.sunPositionX
                    positionY = sunPositionY - sun.sunPositionY
                    positionZ = sunPositionZ - sun.sunPositionZ
                }
            }
            if (sunRotationX != 0) {
                val rotation = -sunRotationX and 0x3fff
                val xan = MathUtils.SINE[rotation]
                val zan = MathUtils.COSINE[rotation]
                val tmpY = positionY * zan - positionZ * xan shr 14
                positionZ = positionY * xan + positionZ * zan shr 14
                positionY = tmpY
            }
            if (sunRotationY != 0) {
                val rotation = -sunRotationY and 0x3fff
                val xan = MathUtils.SINE[rotation]
                val zan = MathUtils.COSINE[rotation]
                val tmpX = positionZ * xan + positionX * zan shr 14
                positionZ = positionZ * zan - positionX * xan shr 14
                positionX = tmpX
            }
            toolkit.updateSun(1.0f)
            toolkit.applyLighting(sunColor, 1.0f, 1.0f, positionX.toFloat(), positionY.toFloat(), positionZ.toFloat())
            mesh.rotateWithoutNormals(rotationX and 0x3fff, rotationY and 0x3fff, rotationZ and 0x3fff)
            val coloredSunModel = toolkit.createModel(mesh, 2048, 0, 64, 768)
            val sizeX = coloredSunModel.getMaxX() - coloredSunModel.getMinX()
            val sizeY = coloredSunModel.getMaxY() - coloredSunModel.getMinY()
            val highestDimension = max(sizeX, sizeY)
            val size = sunUseDimension * 1024 / highestDimension
            val savedModelMatrix = toolkit.copyModelMatrix()
            val projectionMatrix = toolkit.getScratchMatrix()
            projectionMatrix.setPerspectiveFromViewport(
                (sunUseDimension / 2).toFloat(),
                (sunUseDimension / 2).toFloat(),
                size.toFloat(),
                size.toFloat(),
                50.0f,
                50000.0f,
                toolkit.getDisplay()!!!!.getWidth().toFloat(),
                toolkit.getDisplay()!!!!.getHeight().toFloat(),
                1024.0f
            )
            toolkit.setModelMatrix(projectionMatrix)
            val sunPosition = Matrix43()
            toolkit.setViewMatrix(sunPosition)
            val renderingMatrix = toolkit.getRenderingMatrix()
            renderingMatrix.setViewPosition(0.0f, 0.0f, (50 - coloredSunModel.getMinZ()).toFloat())
            coloredSunModel.drawMatrixThreaded(renderingMatrix, null, ModelFunctionMaskGL.SMOOTH_EDGES)
            sunSprite = toolkit.createNativeSprite(0, 0, sunUseDimension, sunUseDimension, true)
            sunSprite!!.pixelTransfer()
            toolkit.setModelMatrix(savedModelMatrix)
            toolkit.setClipping(SUN_CLIPPING[0], SUN_CLIPPING[1], SUN_CLIPPING[2], SUN_CLIPPING[3])
        }
    }

    fun performAction(toolkit: AbstractToolkit, sun: Sun?): Boolean {
        if (sunSprite == null) {
            when (sunActionType) {
                0 -> {
                    if (Skybox.materialProvider!!.isCached(sunMaterialId)) {
                        val pixels = Skybox.materialProvider!!.getPixelsArgb(sunMaterialId, sunUseDimension, sunUseDimension)
                        sunSprite = toolkit.createNativeSprite(pixels, sunUseDimension, sunUseDimension, sunUseDimension)
                    }
                }
                2 -> drawColoredSun(toolkit, sun)
                1 -> drawTexturedSun(toolkit, sun)
            }
        }
        return sunSprite != null
    }

    fun drawTexturedSun(toolkit: AbstractToolkit, sun: Sun?) {
        generateModel(toolkit)
        generateSunSprites(toolkit)
        toolkit.getClipping(SUN_CLIPPING)
        toolkit.setClipping(0, 0, sunUseDimension, sunUseDimension)
        toolkit.applyClearFlags(2, 0)
        toolkit.drawFilledRectangle(0, 0, sunUseDimension, sunUseDimension, 0xff000000.toInt() or sunColor, 0)
        var positionX = 0
        var positionY = 0
        var positionZ = 256
        if (sun != null) {
            if (sun.invertPosition) {
                positionX = -sun.sunPositionX
                positionY = -sun.sunPositionY
                positionZ = -sun.sunPositionZ
            } else {
                positionX = sun.sunPositionX - sunPositionX
                positionY = sun.sunPositionY - sunPositionY
                positionZ = sun.sunPositionZ - sunPositionZ
            }
        }
        if (sunRotationX != 0) {
            val xan = MathUtils.SINE[sunRotationX]
            val zan = MathUtils.COSINE[sunRotationX]
            val tmpY = positionY * zan - positionZ * xan shr 14
            positionZ = positionY * xan + positionZ * zan shr 14
            positionY = tmpY
        }
        if (sunRotationY != 0) {
            val xan = MathUtils.SINE[sunRotationY]
            val zan = MathUtils.COSINE[sunRotationY]
            val tmpX = positionZ * xan + positionX * zan shr 14
            positionZ = positionZ * zan - positionX * xan shr 14
            positionX = tmpX
        }
        val copiedSunModel = sunModel!!.copy(0.toByte(), ModelFunctionMask.HAS_TEXTURE_COORDS or ModelFunctionMask.RECOLOR or ModelFunctionMask.RETEXTURE, true)
        copiedSunModel!!.rematerial(0.toShort(), sunMaterialId.toShort())
        toolkit.updateSun(1.0f)
        toolkit.applyLighting(16777215, 1.0f, 1.0f, positionX.toFloat(), positionY.toFloat(), positionZ.toFloat())
        var size = sunUseDimension * 1024 / (copiedSunModel!!.getMaxX() - copiedSunModel!!.getMinX())
        if (sunColor != 0) {
            size = size * 13 / 16
        }
        val savedModelMatrix = toolkit.copyModelMatrix()
        val projectionMatrix = toolkit.getScratchMatrix()
        projectionMatrix.setPerspectiveFromViewport(
            (sunUseDimension / 2).toFloat(),
            (sunUseDimension / 2).toFloat(),
            size.toFloat(),
            size.toFloat(),
            50.0f,
            50000.0f,
            toolkit.getDisplay()!!.getWidth().toFloat(),
            toolkit.getDisplay()!!.getHeight().toFloat(),
            1024.0f
        )
        toolkit.setModelMatrix(projectionMatrix)
        toolkit.setViewMatrix(Matrix43())
        val renderingMatrix = Matrix43()
        renderingMatrix.setViewPosition(0.0f, 0.0f, (50 - copiedSunModel!!.getMinZ()).toFloat())
        copiedSunModel!!.drawMatrixThreaded(renderingMatrix, null, ModelFunctionMaskGL.SMOOTH_EDGES)
        val useDimension = sunUseDimension * 13 / 16
        val position = (sunUseDimension - useDimension) / 2
        activeAntiSun!!.drawCircle(position, position, useDimension, useDimension, 0, 0xff000000.toInt() or sunColor, 1)
        sunSprite = toolkit.createNativeSprite(0, 0, sunUseDimension, sunUseDimension, true)
        toolkit.applyClearFlags(2, 0)
        toolkit.drawFilledRectangle(0, 0, sunUseDimension, sunUseDimension, 0, 0)
        activeSun!!.drawCircle(0, 0, sunUseDimension, sunUseDimension, 1, 0, 0)
        sunSprite!!.pixelTransfer()
        toolkit.setModelMatrix(savedModelMatrix)
        toolkit.setClipping(SUN_CLIPPING[0], SUN_CLIPPING[1], SUN_CLIPPING[2], SUN_CLIPPING[3])
    }

    fun calculateProjection(
        toolkit: AbstractToolkit,
        width: Int,
        height: Int,
        sunDeltaX: Int,
        sunDeltaY: Int,
        sunDeltaZ: Int,
        alpha: Int
    ) {
        if (sunSprite != null) {
            val sunProjection = FloatArray(3)
            var sunX = (-(sunPositionX - sunDeltaX shl 16)).toFloat()
            var sunY = (sunPositionY - sunDeltaY shl 15).toFloat()
            var sunZ = (-(sunPositionZ - sunDeltaZ shl 16)).toFloat()
            val sunPosition = FloatArray(3)
            toolkit.copyViewMatrix().cacheInto(sunPosition)
            sunX += sunPosition[0]
            sunY += sunPosition[1]
            sunZ += sunPosition[2]
            toolkit.calculateProjection(sunX, sunY, sunZ, sunProjection)
            if (sunProjection[2] >= 0.0f) {
                val x = (sunProjection[0] - (positionOffset / 2)).toInt()
                val y = (sunProjection[1] - (positionOffset / 2)).toInt()
                if (y < height && y + positionOffset > 0 && x < width && x + positionOffset > 0) {
                    sunSprite!!.drawCircle(x, y, positionOffset, positionOffset, 0, alpha shl 24 or 0xffffff, 1)
                }
            }
        }
    }

    companion object {
        @JvmField
        val SUN_CLIPPING = IntArray(4)

        @JvmField
        var sunModel: Model? = null

        @JvmField
        var activeSun: NativeSprite? = null

        @JvmField
        var activeAntiSun: NativeSprite? = null

        @JvmStatic
        fun resetMedia() {
            sunModel = null
            activeSun = null
        }

        @JvmStatic
        fun generateModel(toolkit: AbstractToolkit) {
            if (sunModel == null) {
                val sunMesh = Mesh(580, 1104, 1)
                sunMesh.addSunTexture()
                sunMesh.addVertex(0, 128, 0)
                sunMesh.addVertex(0, -128, 0)
                for (index in 0..24) {
                    val rot = index * 8192 / 24
                    val rotateZ = MathUtils.SINE[rot]
                    val rotateX = MathUtils.COSINE[rot]
                    for (subIndex in 1 until 24) {
                        val rotation = subIndex * 8192 / 24
                        val y = MathUtils.COSINE[rotation] shr 7
                        val z = MathUtils.SINE[rotation] * rotateZ shr 21
                        val x = MathUtils.SINE[rotation] * rotateX shr 21
                        sunMesh.addVertex(x, y, -z)
                    }
                    if (index > 0) {
                        val faceA = index * 23 + 2
                        val faceB = faceA - 23
                        sunMesh.addFace(0, faceB, faceA, 0.toByte(), 0.toByte(), 127.toShort(), 0.toByte(), 0.toShort())
                        for (faceIndex in 1 until 23) {
                            val faceC = faceB + 1
                            val faceD = faceA + 1
                            sunMesh.addFace(faceB, faceC, faceA, 0.toByte(), 0.toByte(), 127.toShort(), 0.toByte(), 0.toShort())
                            sunMesh.addFace(faceC, faceD, faceA, 0.toByte(), 0.toByte(), 127.toShort(), 0.toByte(), 0.toShort())
                            // faceB and faceA updated in next iteration
                        }
                        sunMesh.addFace(faceA, faceB, 1, 0.toByte(), 0.toByte(), 127.toShort(), 0.toByte(), 0.toShort())
                    }
                }
                sunMesh.maxDepth = sunMesh.numVertices
                sunMesh.faceGroups = null
                sunMesh.vertexGroups = null
                sunMesh.facesLayer = null
                sunModel = toolkit.createModel(sunMesh, ModelFunctionMask.HAS_TEXTURE_COORDS or ModelFunctionMask.RETEXTURE or ModelFunctionMask.RECOLOR, ModelFunctionMask.ALTERS_VERTICES or ModelFunctionMask.DISPLACE_X, 64, 768)
            }
        }

        @JvmStatic
        fun generateSunSprites(toolkit: AbstractToolkit) {
            if (activeSun == null) {
                val antiSunPixels = IntArray(16384)
                val sunPixels = IntArray(16384)
                for (y in 0 until 64) {
                    var dySq = 64 - y
                    dySq *= dySq
                    val mirrorY = 128 - y - 1
                    val rowOffset = y * 128
                    val mirrorRowOffset = mirrorY * 128
                    for (x in 0 until 64) {
                        var dxSq = 64 - x
                        dxSq *= dxSq
                        val mirrorX = 128 - x - 1
                        var alpha = 256 - (dxSq + dySq shl 8) / 4096
                        alpha = alpha * 3072 / 1536
                        if (alpha < 0) {
                            alpha = 0
                        } else if (alpha > 255) {
                            alpha = 255
                        }
                        val halvedAlpha = alpha / 2
                        sunPixels[rowOffset + x] = 0xff000000.toInt() or (alpha shl 16)
                        sunPixels[rowOffset + mirrorX] = sunPixels[rowOffset + x]
                        sunPixels[mirrorRowOffset + x] = sunPixels[rowOffset + x]
                        sunPixels[mirrorRowOffset + mirrorX] = sunPixels[rowOffset + x]
                        antiSunPixels[rowOffset + x] = (127 - halvedAlpha shl 24) or 0xffffff
                        antiSunPixels[rowOffset + mirrorX] = antiSunPixels[rowOffset + x]
                        antiSunPixels[mirrorRowOffset + x] = antiSunPixels[rowOffset + x]
                        antiSunPixels[mirrorRowOffset + mirrorX] = antiSunPixels[rowOffset + x]
                    }
                }
                activeSun = toolkit.createNativeSprite(sunPixels, 128, 128, 128)
                activeAntiSun = toolkit.createNativeSprite(antiSunPixels, 128, 128, 128)
            }
        }

        @JvmStatic
        fun reset() {
            resetMedia()
        }
    }
}
