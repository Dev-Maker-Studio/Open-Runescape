package com.jagex.game.runetek5.toolkit

import java.awt.*
import java.util.Hashtable

import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.graphics.modern.debug.AutoLogin
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator
import com.jagex.game.runetek5.imageproducer.AbstractDisplay
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.js5.library.INativeLibraryLoader
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.rendering.RenderInfo
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.FrameRenderException
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.utils.ColorUtil

abstract class AbstractToolkit protected constructor(@JvmField val materialProvider: MaterialProvider?) : AutoCloseable {

    @JvmField
    var renderId: Int = 0

    @JvmField
    var display: AbstractDisplay? = null

    @JvmField
    protected var delegatingDisplay: DelegatingDisplay? = null

    @JvmField
    val canvasToProducerMap: Hashtable<Canvas, DelegatingDisplay> = Hashtable()

    @JvmField
    var directDisplayCursorPosition: Int = -1

    @JvmField
    val directDisplays: Array<DirectDisplay?> = arrayOfNulls(4)

    init {
        this.renderId = -1
        for (index in 0 until 8) {
            if (!ACTIVE_RENDER_UNITS[index]) {
                ACTIVE_RENDER_UNITS[index] = true
                this.renderId = index
                break
            }
        }
        if (this.renderId == -1) {
            throw IllegalStateException("")
        }
    }

    abstract fun getRenderInfo(): RenderInfo

    @Throws(FrameRenderException::class)
    open fun renderFrame() {
        AutoLogin.tick()
        render()
    }

    abstract fun awaitRenderCompletion()

    fun cleanupAndShutdown() {
        ACTIVE_RENDER_UNITS[this.renderId] = false
        val canvasProducerMapping = this.canvasToProducerMap.keys()
        while (canvasProducerMapping.hasMoreElements()) {
            val canvas = canvasProducerMapping.nextElement()
            val producer = this.canvasToProducerMap[canvas]
            producer?.cleanup()
        }
        shutdown()
    }

    abstract fun shutdown()

    abstract fun animateTextures(delta: Int)

    abstract fun getOffheap(): Int

    override fun close() {
        cleanupAndShutdown()
    }

    abstract fun utilisesNativeHeap(): Boolean

    abstract fun hasShadows(): Boolean

    abstract fun hasBloomSupport(): Boolean

    abstract fun hasHardwareRendering(): Boolean

    abstract fun hasAntiAliasing(): Boolean

    abstract fun hasTextures(): Boolean

    abstract fun drawFilledRectangle(x: Int, y: Int, width: Int, height: Int, color: Int, type: Int)

    abstract fun replaceCanvasOnDisplayUpdate(): Boolean

    open fun accumulateViewportDimensions(cacheInto: IntArray) {
        val delegating = this.delegatingDisplay
        if (delegating != null) {
            cacheInto[0] = delegating.getWidth()
            cacheInto[1] = delegating.getHeight()
        } else {
            cacheInto[1] = 0
            cacheInto[0] = 0
        }
    }

    fun removeCanvas(canvas: Canvas) {
        if (this.canvasToProducerMap.containsKey(canvas)) {
            val producer = this.canvasToProducerMap[canvas]
            producer?.cleanup()
            this.canvasToProducerMap.remove(canvas)
        }
    }

    fun addImageProducer(canvas: Canvas, producer: DelegatingDisplay?) {
        if (producer == null) {
            throw RuntimeException()
        }
        this.canvasToProducerMap[canvas] = producer
    }

    fun setActiveCanvas(canvas: Canvas) {
        val delegating = this.canvasToProducerMap[canvas]
            ?: throw RuntimeException()
        if (this.directDisplayCursorPosition > 0 || this.delegatingDisplay != this.display) {
            throw RuntimeException()
        }
        if (this.display != null) {
            this.display?.setDepthStencilSurface()
        }
        if (this.directDisplayCursorPosition < 0) {
            this.display = delegating
        }
        this.delegatingDisplay = delegating
        delegating.reload()
    }

    fun resizeGameCanvas(gameCanvas: Canvas, screenCanvasWidth: Int, screenCanvasHeight: Int) {
        val imageProducer = this.canvasToProducerMap[gameCanvas]
            ?: throw RuntimeException()
        imageProducer.resize(screenCanvasWidth, screenCanvasHeight)
    }

    open fun resizeDisplay(directDisplay: DirectDisplay) {
        if (this.directDisplayCursorPosition < 0 || directDisplay != this.directDisplays[this.directDisplayCursorPosition]) {
            throw RuntimeException()
        }
        this.directDisplays[--this.directDisplayCursorPosition + 1] = null
        directDisplay.setDepthStencilSurface()
        if (this.directDisplayCursorPosition >= 0) {
            this.display = this.directDisplays[this.directDisplayCursorPosition]
            this.directDisplays[this.directDisplayCursorPosition]?.reload()
        } else {
            this.display = this.delegatingDisplay
            this.delegatingDisplay?.reload()
        }
    }

    abstract fun createDelegatingDisplay(canvas: Canvas, width: Int, height: Int): DelegatingDisplay

    abstract fun getImageProducer(): DirectDisplay

    abstract fun getRenderBuffer(width: Int, height: Int): RenderBuffer

    abstract fun copyPixelRectangle(x: Int, y: Int, width: Int, height: Int): IntArray

    abstract fun renderLastFrame()

    abstract fun calculateViewportCenter()

    abstract fun resetDepthClippingMask()

    abstract fun resetClipping()

    abstract fun expandClipping(screenLeft: Int, screenTop: Int, screenRight: Int, screenBottom: Int)

    abstract fun getClipping(cacheInto: IntArray)

    abstract fun applyClearFlags(functionMask: Int, backgroundColorArgb: Int)

    abstract fun setModelMatrix(matrix: Matrix44)

    open fun clearPixels(x: Int, y: Int, width: Int, height: Int, color: Int) {
        drawFilledRectangle(x, y, width, height, color, 1)
    }

    fun drawOpaqueVerticalLine(x: Int, y: Int, height: Int, colorArgb: Int) {
        drawTransparentVerticalLine(x, y, height, colorArgb, 1)
    }

    fun drawOpaqueLine(x: Int, y: Int, width: Int, height: Int, colorArgb: Int) {
        drawTransparentLine(x, y, width, height, colorArgb, 1)
    }

    abstract fun drawRectangleOutline(x: Int, y: Int, width: Int, height: Int, color: Int, alpha: Int)

    abstract fun drawCircle(x: Int, y: Int, radius: Int, colorArgb: Int)

    abstract fun drawTransparentHorizontalLine(x: Int, y: Int, width: Int, colorArgb: Int, blendMode: Int)

    abstract fun drawTransparentVerticalLine(x: Int, y: Int, height: Int, colorArgb: Int, blendMode: Int)

    abstract fun drawTransparentLine(x: Int, y: Int, width: Int, height: Int, color: Int, alpha: Int)

    abstract fun drawLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, lineSpacing: Int, lineLength: Int, offset: Int)

    abstract fun isOnScreen(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): Int

    abstract fun getNativeMemory(capacity: Int): NativeMemoryNode?

    abstract fun setNativeHeap(nativeMemoryNode: NativeMemoryNode)

    fun generateEmptySprite(width: Int, height: Int): NativeSprite {
        return generateEmptySprite(width, height, true, false)
    }

    abstract fun createNativeSprite(pixels: IntArray, defaultBackgroundColor: Int, width: Int, useWidth: Int, useHeight: Int, hasAlpha: Boolean): NativeSprite

    abstract fun generateSprite(spriteType: SpriteType?, hasPalette: Boolean): NativeSprite

    abstract fun createFontRenderer(fontMetrics: FontMetrics?, spriteTypes: Array<SpriteType>?, isPlain: Boolean): AbstractFontRenderer

    abstract fun getScratchMatrix(): Matrix44

    abstract fun getRenderingMatrix(): Matrix43

    abstract fun createModel(mesh: Mesh, functionMask: Int, preferenceMask: Int, ambient: Int, contrast: Int): Model

    abstract fun mismatchingFunctionMask(functionMaskA: Int, functionMaskB: Int): Int

    abstract fun copyModelMatrix(): Matrix44

    abstract fun extendBuildArea(): Boolean

    open fun readyForRendering(): Boolean {
        return true
    }

    abstract fun renderParticles(particleWrapper: ParticleWrapper)

    abstract fun setViewMatrix(viewMatrix: Matrix43)

    abstract fun copyViewMatrix(): Matrix43

    abstract fun setDepthBuffering(enabled: Boolean)

    abstract fun initialize()

    protected open fun getDelegatingDisplay(): DelegatingDisplay? {
        return delegatingDisplay
    }

    abstract fun updateSun(brightness: Float)

    abstract fun applyLighting(color: Int, intensity: Float, antiIntensity: Float, positionX: Float, positionY: Float, positionZ: Float)

    abstract fun generateFlags()

    abstract fun getMaxLights(): Int

    abstract fun supportsFullscreen(): Boolean

    abstract fun createCubeTexture(materialAtPositiveX: Int, materialAtNegativeX: Int, materialAtPositiveY: Int, materialAtNegativeY: Int, materialAtPositiveZ: Int, materialAtNegativeZ: Int): AbstractCubeTextureGenerator?

    abstract fun drawQuad(sourceA: AbstractCubeTextureGenerator?, sourceB: AbstractCubeTextureGenerator?, alpha: Float, existing: AbstractCubeTextureGenerator?): AbstractCubeTextureGenerator?

    open fun drawMapCategory(x: Int, y: Int, radius: Int, colorArgb: Int) {
        drawCircle(x, y, radius, colorArgb)
    }

    abstract fun bloomSupported(): Boolean

    abstract fun bloomEnabled(): Boolean

    open fun updateBloomFilter(whitepoint: Float, bloom: Float, brightpass: Float) {
        setBloomFilter(whitepoint, bloom, brightpass)
    }

    abstract fun compositeBloomEffects()

    abstract fun addWater(waterTile: WaterTile)

    abstract fun addWaterTile(waterColor: Int, waterTile: WaterTile)

    abstract fun setSunCubeTexture(cubeTexture: AbstractCubeTextureGenerator?)

    abstract fun createGround(regionSizeX: Int, regionSizeY: Int, tileheightsCurrentLvl: Array<IntArray>, tileheightsNextLvl: Array<IntArray>, tileUnits: Int, floorFunctionMask: Int, waterFunctionMask: Int): Ground

    abstract fun calculateProjection(width: Float, height: Float, length: Float, matrix: FloatArray)

    open fun usesFixedFunctionLights(): Boolean = true

    open fun isColumnOnScreen(x: Int, z: Int, yLow: Int, yHigh: Int): Int =
        isOnScreen(x, yLow, z, x, yHigh, z)

    open fun isColumnRunOnScreen(
        x: Int, zs: IntArray, yLow: IntArray, yHigh: IntArray, out: IntArray, count: Int
    ) {
        for (i in 0 until count) out[i] = isColumnOnScreen(x, zs[i], yLow[i], yHigh[i])
    }

    abstract fun debugModeChanged()

    abstract fun resetBloom()

    abstract fun drawThickLine(leftX: Int, topY: Int, rightX: Int, bottomY: Int, colorArgb: Int, lineWidth: Int, blendMode: Int)

    abstract fun setClipping(left: Int, top: Int, right: Int, bottom: Int)

    abstract fun onTilesDrawn()

    abstract fun computeWorldToScreen(x: Float, y: Float, z: Float, screenCoords: FloatArray)

    abstract fun calculateFog(color: Int, density: Int, offset: Int)

    fun getDisplay(): AbstractDisplay? {
        return this.display
    }

    fun ensureImageproducerCreated(gameCanvas: Canvas, screenWidth: Int, screenHeight: Int) {
        if (!this.canvasToProducerMap.containsKey(gameCanvas)) {
            setIgnoreRepaint(gameCanvas)
            addImageProducer(gameCanvas, createDelegatingDisplay(gameCanvas, screenWidth, screenHeight))
        }
    }

    abstract fun beginBloomCapture(screenCanvasWidth: Int, screenCanvasHeight: Int)

    abstract fun clearRectangle(dimension: ComponentSurface, clipLeft: Int, clipTop: Int)

    abstract fun mergeFunctionMasks(functionMaskA: Int, functionMaskB: Int): Int

    abstract fun copyLights(lightCount: Int, lights: Array<PointLight>?)

    fun createNativeSprite(pixels: IntArray, width: Int, useWidth: Int, useHeight: Int): NativeSprite {
        return createNativeSprite(pixels, 0, width, useWidth, useHeight, true)
    }

    @Throws(FrameRenderException::class)
    abstract fun render()

    fun drawOpaqueRectangle(x: Int, y: Int, width: Int, height: Int, color: Int) {
        drawRectangleOutline(x, y, width, height, color, 1)
    }

    abstract fun drawTexturedLine(startX: Int, startY: Int, endX: Int, endY: Int, color: Int, surface: ComponentSurface, clipLeft: Int, clipTop: Int, lineSpacing: Int, lineLength: Int, offset: Int)

    abstract fun resetTextureState()

    abstract fun setBloomFilter(whitepoint: Float, bloom: Float, brightpass: Float)

    open fun createPointLight(x: Int, y: Int, z: Int, size2d: Int, color: Int): PointLight? {
        return PointLight(x, y, z, size2d, color)
    }

    abstract fun setDrawableArea(x: Int, y: Int, width: Int, height: Int)

    abstract fun createComponentSurface(sizeX: Int, sizeY: Int, rowStartX: IntArray, rowWidth: IntArray): ComponentSurface

    open fun setDisplay(activeDisplay: DirectDisplay) {
        if (this.directDisplayCursorPosition >= 3) {
            throw RuntimeException()
        }
        if (this.directDisplayCursorPosition >= 0) {
            this.directDisplays[this.directDisplayCursorPosition]?.setDepthStencilSurface()
        } else {
            this.delegatingDisplay?.setDepthStencilSurface()
        }
        ++this.directDisplayCursorPosition
        this.directDisplays[this.directDisplayCursorPosition] = activeDisplay
        this.display = activeDisplay
        activeDisplay.reload()
    }

    abstract fun generateEmptySprite(width: Int, height: Int, transparent: Boolean, mipmapped: Boolean): NativeSprite

    abstract fun benchmark()

    fun drawOpaqueHorizontalLine(x: Int, y: Int, width: Int, colorArgb: Int) {
        drawTransparentHorizontalLine(x, y, width, colorArgb, 1)
    }

    abstract fun forceFixedScreenmode(): Boolean

    abstract fun renderWorldmapTile(startX: Int, startY: Int, width: Int, height: Int, colorArgb: Int, textureEnvColor: Int, pixels: ByteArray, pixelOffset: Int, blendMode: Int)

    abstract fun createNativeSprite(width: Int, height: Int, textureWidth: Int, textureHeight: Int, hasAlpha: Boolean): NativeSprite

    abstract fun drawSurfaceLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, surface: ComponentSurface, clipLeft: Int, clipTop: Int)

    companion object {
        @JvmField
        val ACTIVE_RENDER_UNITS = BooleanArray(8)

        @JvmField
        var activeRenderMode = -1

        const val BOOTSTRAP_MODE = 0

        @JvmField
        var nativeLibraryLoader: INativeLibraryLoader? = null

        @JvmStatic
        fun create(renderMode: Int, canvasToDrawOn: Canvas?, materialProvider: MaterialProvider?, shaderResource: Js5ResourceProvider?, multiSamples: Int): AbstractToolkit {
            var screenWidth = 0
            var screenHeight = 0
            if (canvasToDrawOn != null) {
                val dimension = canvasToDrawOn.size
                screenWidth = dimension.width
                screenHeight = dimension.height
            }
            return toolkitForType(renderMode, canvasToDrawOn, materialProvider, shaderResource, multiSamples, screenWidth, screenHeight)
        }

        @JvmStatic
        @Synchronized
        fun toolkitForType(renderMode: Int, gameCanvas: Canvas?, materialProvider: MaterialProvider?, shaderIndex: Js5ResourceProvider?, multiSamples: Int, screenWidth: Int, screenHeight: Int): AbstractToolkit {
            if (renderMode == BOOTSTRAP_MODE) {
                return SafeModeToolkit.createToolkit(gameCanvas, materialProvider, screenWidth, screenHeight)
            }
            return ModernGLToolkit.createToolkit(gameCanvas, materialProvider, multiSamples)
        }

        @JvmStatic
        fun isJaclibLoaded(): Boolean {
            return getNativeLibraryLoader().loadIntoMemory("jaclib")
        }

        @JvmStatic
        fun setNativeLibraryLoader(nativeLibraryLoader: INativeLibraryLoader) {
            if (this.nativeLibraryLoader != null) {
                throw IllegalStateException("")
            }
            this.nativeLibraryLoader = nativeLibraryLoader
        }

        @JvmStatic
        fun getNativeLibraryLoader(): INativeLibraryLoader {
            if (nativeLibraryLoader == null) {
                throw IllegalStateException("")
            }
            return nativeLibraryLoader!!
        }

        @JvmStatic
        fun drawOutlinedFilledOpaqueRectangle(toolkit: AbstractToolkit, x: Int, y: Int, width: Int, height: Int, colorArgb: Int) {
            toolkit.clearPixels(x, y, width, height, colorArgb)
            toolkit.clearPixels(x + 1, y + 1, width - 2, 16, 0xffffff.inv())
            toolkit.drawOpaqueRectangle(x + 1, y + 18, width - 2, height - 19, 0xffffff.inv())
        }

        @JvmStatic
        fun setIgnoreRepaint(gameCanvas: Canvas) {
            try {
                val clazz = Class.forName("java.awt.Canvas")
                val setIgnoreRepaint = clazz.getMethod("setIgnoreRepaint", java.lang.Boolean.TYPE)
                setIgnoreRepaint.invoke(gameCanvas, java.lang.Boolean.TRUE)
            } catch (e: Exception) {
            }
        }

        @JvmStatic
        fun calculateGameArea(x: Int, y: Int, width: Int, height: Int, resetPixels: Boolean) {
            var x = x
            var y = y
            var w = width
            var h = height
            if (w < 1) {
                w = 1
            }
            if (h < 1) {
                h = 1
            }
            var heightScaleFactor = h - 334
            if (heightScaleFactor < 0) {
                heightScaleFactor = 0
            } else if (heightScaleFactor > 100) {
                heightScaleFactor = 100
            }
            var fovScale = (Client.viewportFovMax - Client.viewportFovMin) * heightScaleFactor / 100 + Client.viewportFovMin
            val aspectFov = h * fovScale * 512 / (w * 334)
            val adjustedDimension: Int
            val padding: Int
            val aspectFovLimit: Short
            if (aspectFov < Client.aspectFovMin) {
                aspectFovLimit = Client.aspectFovMin
                fovScale = aspectFovLimit.toInt() * w * 334 / (h * 512)
                if (fovScale > Client.fovScaleMax) {
                    fovScale = Client.fovScaleMax.toInt()
                    adjustedDimension = h * fovScale * 512 / (aspectFovLimit * 334)
                    padding = (w - adjustedDimension) / 2
                    if (resetPixels) {
                        Renderers.activeToolkit!!.resetClipping()
                        Renderers.activeToolkit!!.clearPixels(x, y, padding, h, 0xffffff.inv())
                        Renderers.activeToolkit!!.clearPixels(x + w - padding, y, padding, h, 0xffffff.inv())
                    }
                    x += padding
                    w -= padding * 2
                }
            } else if (aspectFov > Client.aspectFovMax) {
                aspectFovLimit = Client.aspectFovMax
                fovScale = aspectFovLimit.toInt() * w * 334 / (h * 512)
                if (fovScale < Client.fovScaleMin) {
                    fovScale = Client.fovScaleMin.toInt()
                    adjustedDimension = aspectFovLimit * w * 334 / (fovScale * 512)
                    padding = (h - adjustedDimension) / 2
                    if (resetPixels) {
                        Renderers.activeToolkit!!.resetClipping()
                        Renderers.activeToolkit!!.clearPixels(x, y, w, padding, 0xffffff.inv())
                        Renderers.activeToolkit!!.clearPixels(x, h + y - padding, w, padding, 0xffffff.inv())
                    }
                    y += padding
                    h -= padding * 2
                }
            }
            Client.viewportScaledFov = h * fovScale / 334
            Client.hintArrowOffsetX = x
            Client.hintArrowOffsetY = y
            Client.gameAreaWidth = w
            Client.gameAreaHeight = h
        }

        @JvmStatic
        fun drawFadingFilledRectangle(toolkit: AbstractToolkit, baseLight: Int, baseSaturation: Int, component: Component, hue: Int) {
            for (saturation in 7 downTo 0) {
                for (lightness in 127 downTo 0) {
                    val hsl = (hue and 0x3f shl 10) or (saturation and 0x7 shl 7) or (lightness and 0x7f)
                    ColorUtil.initializeTables(false, true)
                    val rgb = ColorUtil.hslToRgb!![hsl]
                    ColorUtil.resetTables(false, true)
                    toolkit.drawFilledRectangle((lightness * component.width shr 7) + baseLight, baseSaturation + ((7 - saturation) * component.height shr 3), (component.width shr 7) + 1, (component.height shr 3) + 1, rgb, 0)
                }
            }
        }

        @JvmStatic
        fun nativeLibraryLoaded(): Boolean {
            return nativeLibraryLoader != null
        }
    }
}
