package com.jagex.game.runetek5.graphics.light

import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.texture.TextureTypeList
import com.jagex.game.runetek5.config.skybox.BackgroundMode
import com.jagex.game.runetek5.config.sun.Sun
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.ArrayUtils
import com.jagex.game.runetek5.utils.MathUtils

class Skybox(
    val currentMaterialId: Int,
    val initialSuns: Array<Sun?>?,
    defaultSunIndex: Int,
    val positionX: Int,
    val positionY: Int,
    val positionZ: Int,
    val backgroundMode: BackgroundMode,
    val skyboxArchiveId: Int
) {

    var srcBackgroundColor: Int = 0
    var dstBackgroundColor: Int = 0
    var backgroundSprite: NativeSprite? = null
    var skyboxAlpha: Int = 0
    var previousAlpha: Int = 0
    var copyAlpha: Boolean = false
    var skybox: Skybox? = null
    var lastComponentHeight: Int = -1
    var prepareSun: Boolean = true
    var skyModel: Model? = null
    var useSize: Int = 0
    var availableSunCount: Int = 0
    var faceAlpha: ByteArray? = null
    var lastAlpha: Int = 0

    val availableSuns: Array<Sun?>?
    val activeSun: Sun?

    init {
        if (initialSuns != null) {
            availableSuns = arrayOfNulls(initialSuns.size)
            activeSun = if (defaultSunIndex >= 0) initialSuns[defaultSunIndex] else null
        } else {
            availableSuns = null
            activeSun = null
        }
    }

    fun renderSkybox(
        toolkit: AbstractToolkit,
        skyboxAngle: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rotationY: Int,
        rotationX: Int,
        rotationZ: Int,
        backgroundColor: Int,
        clearBackground: Boolean,
        alpha: Int
    ) {
        val invertedAlpha = 255 - alpha

        if (skyModel == null) {
            toolkit.applyClearFlags(2, 0)
            val adjustedRotationX = rotationX + skyboxAngle and 0x3fff

            if (currentMaterialId != -1 && useSize != 0) {
                val material = materialProvider!!.getMaterial(currentMaterialId)
                if (backgroundSprite == null && materialProvider!!.isCached(currentMaterialId)) {
                    val materialPixels = if (material.effectCombiner == 2) {
                        materialProvider!!.getPixelsArgb(currentMaterialId, useSize, useSize)
                    } else {
                        materialProvider!!.getPixels(currentMaterialId, 0.7f, useSize, useSize, false)
                    }

                    srcBackgroundColor = materialPixels[0]
                    dstBackgroundColor = materialPixels[materialPixels.size - 1]
                    backgroundSprite = toolkit.createNativeSprite(materialPixels, useSize, useSize, useSize)
                }

                val combineMode = if (invertedAlpha == 255 && material.effectCombiner != 2) 0 else 1

                if (combineMode == 1 && clearBackground) {
                    toolkit.drawFilledRectangle(x, y, width, height, backgroundColor, 0)
                }

                if (backgroundSprite != null) {
                    var yOffset = rotationY * height / -4096
                    var xOffset = adjustedRotationX * height / 4096 + (width - height) / 2

                    xOffset %= height
                    yOffset %= height

                    if (backgroundMode != BackgroundMode.COLOR_TRANSITION) {
                        drawRepeatingBackground(toolkit, x, y, width, height, xOffset, yOffset, invertedAlpha, combineMode)
                    } else {
                        drawTransitioningBackground(toolkit, x, y, width, height, xOffset, yOffset, invertedAlpha, combineMode)
                    }
                }
            } else {
                toolkit.drawFilledRectangle(x, y, width, height, invertedAlpha shl 24 or backgroundColor, 1)
            }
        } else {
            if (clearBackground) {
                toolkit.applyClearFlags(3, backgroundColor)
            }
            applySunRotation(toolkit, rotationY, rotationX, rotationZ, alpha)
        }

        // Render additional sun projections
        for (index in availableSunCount - 1 downTo 0) {
            availableSuns!![index]!!.calculateProjection(toolkit, width, height, positionX, positionY, positionZ, invertedAlpha)
        }
        toolkit.applyClearFlags(2, 0)
    }

    private fun drawRepeatingBackground(
        toolkit: AbstractToolkit,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        xOffset: Int,
        yOffset: Int,
        invertedAlpha: Int,
        combineMode: Int
    ) {
        var i = xOffset - height
        while (i < width) {
            var j = yOffset - height
            while (j < height) {
                backgroundSprite!!.drawCircle(x + i, y + j, height, height, 0, invertedAlpha shl 24 or 0xffffff, combineMode)
                j += height
            }
            i += height
        }
    }

    private fun drawTransitioningBackground(
        toolkit: AbstractToolkit,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        xOffset: Int,
        yOffset: Int,
        invertedAlpha: Int,
        combineMode: Int
    ) {
        backgroundSprite!!.drawCircle(x + xOffset, y + yOffset, height, height, 0, invertedAlpha shl 24 or 0xffffff, combineMode)
        if (srcBackgroundColor and 0xffffff.inv() != 0) {
            toolkit.clearPixels(0, 0, width, yOffset + y + 1, srcBackgroundColor)
        }
        if (dstBackgroundColor and 0xffffff.inv() != 0) {
            toolkit.clearPixels(0, yOffset + y + height, width, height - (yOffset + y + height), dstBackgroundColor)
        }
    }

    fun interpolateAlpha(progress: Int, targetAlpha: Int) {
        skyboxAlpha = (targetAlpha - previousAlpha) * progress / 255 + previousAlpha
    }

    fun transitionToSkybox(targetSkybox: Skybox?) {
        if (copyAlpha) {
            previousAlpha = skyboxAlpha
        } else if (targetSkybox != null && targetSkybox.copyAlpha) {
            previousAlpha = 255 - targetSkybox.skyboxAlpha
        } else {
            previousAlpha = 0
        }
        copyAlpha = true
        skybox = targetSkybox
        skyboxAlpha = 0
    }

    fun resetTransition() {
        copyAlpha = false
        skybox = null
        skyboxAlpha = 0
    }

    fun isTransitioning(): Boolean {
        return copyAlpha
    }

    fun get(): Skybox? {
        return skybox
    }

    fun render(toolkit: AbstractToolkit, x: Int, y: Int, width: Int, height: Int, rotationX: Int, rotationY: Int) {
        render(toolkit, 0, x, y, width, height, rotationX, rotationY, 0, 0)
    }

    fun render(
        toolkit: AbstractToolkit,
        angle: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rotationX: Int,
        rotationY: Int,
        rotationZ: Int,
        fogColor: Int
    ) {
        var alpha = if (copyAlpha) skyboxAlpha else 0

        if (skybox != null) {
            var primarySkybox: Skybox = this
            var secondarySkybox: Skybox = skybox!!

            // Swap if necessary based on hashCode to ensure consistent rendering order
            if (hashCode() > secondarySkybox.hashCode()) {
                primarySkybox = skybox!!
                secondarySkybox = this
                alpha = 255 - alpha
            }

            // Render the primary and secondary skyboxes
            primarySkybox.renderSkybox(toolkit, angle, x, y, width, height, rotationX, rotationY, rotationZ, fogColor, true, alpha)
            secondarySkybox.renderSkybox(toolkit, angle, x, y, width, height, rotationX, rotationY, rotationZ, fogColor, false, 255 - alpha)
        } else {
            // Render the single skybox if no secondary is present
            renderSkybox(toolkit, angle, x, y, width, height, rotationX, rotationY, rotationZ, fogColor, true, alpha)
        }
    }

    fun updateForHeight(toolkit: AbstractToolkit, componentHeight: Int, skyboxPreference: Int): Boolean {
        if (componentHeight != lastComponentHeight) {
            lastComponentHeight = componentHeight
            var size = MathUtils.furtestBitValue(componentHeight)
            if (size > 512) {
                size = 512
            }
            if (size <= 0) {
                size = 1
            }
            if (size != useSize) {
                useSize = size
                backgroundSprite = null
            }
            if (initialSuns != null) {
                availableSunCount = 0
                val sunSizes = IntArray(initialSuns.size)
                for (i in initialSuns.indices) {
                    val sun = initialSuns[i]
                    if (sun != null && sun.move(positionX, positionY, positionZ, lastComponentHeight)) {
                        sunSizes[availableSunCount] = sun.sunSize2d
                        availableSuns!![availableSunCount++] = sun
                    }
                }
                ArrayUtils.quicksort(sunSizes, availableSuns!!, 0, availableSunCount - 1)
            }
            prepareSun = true
        }
        var ready = false
        if (prepareSun) {
            prepareSun = false
            for (index in availableSunCount - 1 downTo 0) {
                val sunReady = availableSuns!![index]!!.isSunGenerated(toolkit, activeSun)
                prepareSun = prepareSun or !sunReady
                ready = ready or sunReady
            }
        }
        if (skyboxPreference != 0 && toolkit.hasHardwareRendering()) {
            if (skyModel == null && skyboxArchiveId >= 0) {
                setupModel(toolkit)
            }
        } else {
            skyModel = null
        }
        if (skybox != null && this != skybox) {
            skybox!!.resetTransition()
            ready = ready or skybox!!.updateForHeight(toolkit, componentHeight, skyboxPreference)
        }
        return ready
    }

    /**
     * [J]
     */
    fun setupModel(toolkit: AbstractToolkit) {
        try {
            val archiveLoaded = TextureTypeList.modelResource!!.loadArchive(skyboxArchiveId)
            if (archiveLoaded) {
                toolkit.applyLighting(16777215, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
                val mesh = Mesh.fromResource(TextureTypeList.modelResource!!, skyboxArchiveId)
                skyModel = toolkit.createModel(mesh!!, 1099776, 0, 255, 1)
                val alpha = skyModel!!.getAlpha()
                if (alpha == null) {
                    faceAlpha = null
                } else {
                    faceAlpha = ByteArray(alpha.size)
                    System.arraycopy(alpha, 0, faceAlpha, 0, alpha.size)
                }
            }
        } catch (exception: Exception) {
            // NOOP
        }
    }

    fun applySunRotation(toolkit: AbstractToolkit, rotationY: Int, rotationX: Int, rotationZ: Int, alpha: Int) {
        // Backup the current sun position
        val originalSunPosition = toolkit.copyViewMatrix()
        // Create a new matrix for the sun position and apply the rotations
        val rotatedSunPosition = Matrix43()
        rotatedSunPosition.setViewPosition(0.0f, 0.0f, 0.0f)
        rotatedSunPosition.rotate(0.0f, -1.0f, 0.0f, MathUtils.getTheta(-rotationX and 0x3fff))
        rotatedSunPosition.rotate(-1.0f, 0.0f, 0.0f, MathUtils.getTheta(-rotationY and 0x3fff))
        rotatedSunPosition.rotate(0.0f, 0.0f, -1.0f, MathUtils.getTheta(-rotationZ and 0x3fff))
        // Update the sun position in the toolkit
        toolkit.setViewMatrix(rotatedSunPosition)
        // Update the sky model's alpha value if it has changed
        if (alpha != lastAlpha) {
            skyModel!!.applyEnvironmentAlpha(alpha.toByte(), faceAlpha!!)
            lastAlpha = alpha
        }
        // Draw the sky model using the rotated matrix
        val drawnMatrix = Matrix43()
        skyModel!!.drawMatrixThreaded(drawnMatrix, null, ModelFunctionMaskGL.EMPTY)
        // Restore the original sun position
        toolkit.setViewMatrix(originalSunPosition)
    }

    companion object {
        @JvmField
        var materialProvider: MaterialProvider? = null
    }
}
