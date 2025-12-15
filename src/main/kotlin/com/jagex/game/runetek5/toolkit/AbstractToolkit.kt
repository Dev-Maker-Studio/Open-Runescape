package com.jagex.game.runetek5.toolkit

import java.awt.*
import java.util.Hashtable

import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.client.client
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

    abstract fun method8404(): Boolean

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

    fun resizeDisplay(directDisplay: DirectDisplay) {
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

    abstract fun createDelegatingDisplay(var1: Canvas, var2: Int, var3: Int): DelegatingDisplay

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

    abstract fun setModelMatrix(var1: Matrix44)

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

    abstract fun isOnScreen(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int): Int

    abstract fun getNativeMemory(var1: Int): NativeMemoryNode

    abstract fun setNativeHeap(nativeMemoryNode: NativeMemoryNode)

    fun generateEmptySprite(width: Int, height: Int): NativeSprite {
        return generateEmptySprite(width, height, true, false)
    }

    abstract fun createNativeSprite(pixels: IntArray, defaultBackgroundColor: Int, width: Int, useWidth: Int, useHeight: Int, var6: Boolean): NativeSprite

    abstract fun generateSprite(spriteType: SpriteType?, hasPalette: Boolean): NativeSprite

    abstract fun createFontRenderer(var1: FontMetrics?, var2: Array<SpriteType>?, var3: Boolean): AbstractFontRenderer

    abstract fun method8449(): Matrix44

    abstract fun getRenderingMatrix(): Matrix43

    /**
     * Creates a model and applies the function mask to set the correct attributes
     */
    abstract fun createModel(mesh: Mesh, functionMask: Int, preferenceMask: Int, ambient: Int, contrast: Int): Model

    /**
     * Checks if function_mask_a has all the functions of function_mask_b if so returns 0 otherwise returns 1
     */
    abstract fun mismatchingFunctionMask(functionMaskA: Int, functionMaskB: Int): Int

    abstract fun copyModelMatrix(): Matrix44

    abstract fun extendBuildArea(): Boolean

    open fun readyForRendering(): Boolean {
        return true
    }

    abstract fun renderParticles(var1: ParticleWrapper)

    abstract fun setSunPosition(var1: Matrix43)

    abstract fun copySunPosition(): Matrix43

    abstract fun setDepthBuffering(var1: Boolean)

    abstract fun initialize()

    protected open fun getDelegatingDisplay(): DelegatingDisplay? {
        return delegatingDisplay
    }

    /**
     * Sets the general rendering brightness
     * @param brightness 0.0 - 1.0
     */
    abstract fun updateSun(brightness: Float)

    abstract fun applyLighting(color: Int, intensity: Float, f_3: Float, positionX: Float, positionY: Float, positionZ: Float)

    abstract fun generateFlags()

    abstract fun getMaxLights(): Int

    abstract fun supportsFullscreen(): Boolean

    abstract fun createCubeTexture(materialAtPositiveX: Int, materialAtNegativeX: Int, materialAtPositiveY: Int, materialAtNegativeY: Int, materialAtPositiveZ: Int, materialAtNegativeZ: Int): AbstractCubeTextureGenerator

    abstract fun drawQuad(var1: AbstractCubeTextureGenerator?, var2: AbstractCubeTextureGenerator?, var3: Float, var4: AbstractCubeTextureGenerator?): AbstractCubeTextureGenerator?

    open fun drawMapCategory(x: Int, y: Int, radius: Int, colorArgb: Int) {
        drawCircle(x, y, radius, colorArgb)
    }

    abstract fun bloomSupported(): Boolean

    abstract fun bloomEnabled(): Boolean

    open fun updateBloomFilter(whitepoint: Float, bloom: Float, brightpass: Float) {
        setBloomFilter(whitepoint, bloom, brightpass)
    }

    abstract fun method8474()

    abstract fun addWater(waterTile: WaterTile)

    abstract fun addWaterTile(waterColor: Int, waterTile: WaterTile)

    abstract fun setSunCubeTexture(var1: AbstractCubeTextureGenerator?)

    abstract fun createGround(regionSizeX: Int, regionSizeY: Int, tileheightsCurrentLvl: Array<IntArray>, tileheightsNextLvl: Array<IntArray>, floorFunctionMask: Int, waterFunctionMask: Int): Ground

    abstract fun calculateProjection(width: Float, height: Float, length: Float, matrix: FloatArray)

    abstract fun debugModeChanged()

    abstract fun resetBloom()

    abstract fun drawThickLine(leftX: Int, topY: Int, rightX: Int, bottomY: Int, colorArgb: Int, lineWidth: Int, blendMode: Int)

    abstract fun setClipping(left: Int, top: Int, right: Int, bottom: Int)

    abstract fun onTilesDrawn()

    abstract fun computeWorldToScreen(x: Float, y: Float, z: Float, screenCoords: FloatArray)

    abstract fun calculateFog(color: Int, density: Int, var3: Int)

    fun getDisplay(): AbstractDisplay? {
        return this.display
    }

    fun ensureImageproducerCreated(gameCanvas: Canvas, screenWidth: Int, screenHeight: Int) {
        if (!this.canvasToProducerMap.containsKey(gameCanvas)) {
            setIgnoreRepaint(gameCanvas)
            addImageProducer(gameCanvas, createDelegatingDisplay(gameCanvas, screenWidth, screenHeight))
        }
    }

    abstract fun method8525(screenCanvasWidth: Int, screenCanvasHeight: Int)

    abstract fun clearRectangle(dimension: ComponentSurface, clipLeft: Int, clipTop: Int)

    abstract fun mergeFunctionMasks(functionMaskA: Int, functionMaskB: Int): Int

    abstract fun copyLights(lightCount: Int, lights: Array<PointLight>)

    fun createNativeSprite(pixels: IntArray, width: Int, useWidth: Int, useHeight: Int): NativeSprite {
        return createNativeSprite(pixels, 0, width, useWidth, useHeight, true)
    }

    @Throws(FrameRenderException::class)
    abstract fun render()

    fun drawOpaqueRectangle(x: Int, y: Int, width: Int, height: Int, color: Int) {
        drawRectangleOutline(x, y, width, height, color, 1)
    }

    abstract fun drawTexturedLine(var1: Int, var2: Int, var3: Int, var4: Int, color: Int, dimension: ComponentSurface, clipLeft: Int, clipTop: Int, var10: Int, var11: Int, var12: Int)

    abstract fun resetTextureState()

    abstract fun setBloomFilter(whitepoint: Float, bloom: Float, brightpass: Float)

    open fun createPointLight(x: Int, y: Int, z: Int, size2d: Int, color: Int): PointLight {
        return PointLight(x, y, z, size2d, color)
    }

    abstract fun setDrawableArea(x: Int, y: Int, width: Int, height: Int)

    abstract fun createComponentSurface(sizeX: Int, sizeY: Int, rowStartX: IntArray, rowWidth: IntArray): ComponentSurface

    fun setDisplay(activeDisplay: DirectDisplay) {
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

    abstract fun generateEmptySprite(width: Int, height: Int, transparent: Boolean, var4: Boolean): NativeSprite

    /**
     * Benchmarks the performance of given toolkit in order
     * to determine which display mode is optimal for your pc
     */
    abstract fun benchmark()

    fun drawOpaqueHorizontalLine(x: Int, y: Int, width: Int, colorArgb: Int) {
        drawTransparentHorizontalLine(x, y, width, colorArgb, 1)
    }

    abstract fun forceFixedScreenmode(): Boolean

    /**
     * Give this a better name (?), currently only used for worldmap tiles
     */
    abstract fun renderWorldmapTile(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: ByteArray, var8: Int, var9: Int)

    abstract fun createNativeSprite(var1: Int, var2: Int, var3: Int, var4: Int, var5: Boolean): NativeSprite

    abstract fun method8669(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var7: ComponentSurface, clipLeft: Int, clipTop: Int)

    companion object {
        @JvmField
        val ACTIVE_RENDER_UNITS = BooleanArray(8)

        @JvmField
        var activeRenderMode = -1

        @JvmField
        var nativeLibraryLoader: INativeLibraryLoader? = null

        /**
         * @param renderMode Identifier for the display mode we're using
         * @param canvasToDrawOn EG: game canvas / inventory item canvas
         * @param materialProvider The wrapper around material management
         * @param shaderResource The resource containing all shaders
         * @param multiSamples Multi samples preference
         */
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

        /**
         * Currently software mode does not work due to renamed native methods & potentially other issues
         */
        @JvmStatic
        @Synchronized
        fun toolkitForType(renderMode: Int, gameCanvas: Canvas?, materialProvider: MaterialProvider?, shaderIndex: Js5ResourceProvider?, multiSamples: Int, screenWidth: Int, screenHeight: Int): AbstractToolkit {
            val mode = if (renderMode == 1 || renderMode == 5) "GL" else if (renderMode == 0) "SM" else "DX"
            println("[AbstractToolkit] Entering render mode: $renderMode = $mode")
            return when (renderMode) {
                0 -> SafeModeToolkit.createToolkit(gameCanvas, materialProvider, screenWidth, screenHeight)
                2 -> SoftwareToolkit.createToolkit(gameCanvas, materialProvider, screenWidth, screenHeight)
                1 -> OpenGlToolkit.createToolkitLd(gameCanvas, materialProvider, multiSamples)
                5 -> OpenGlToolkit.createToolkitHd(gameCanvas, materialProvider, shaderIndex, multiSamples)
                3 -> AbstractNativeToolkit.createToolkit(gameCanvas, materialProvider, shaderIndex, multiSamples)
                else -> throw IllegalArgumentException("")
            }
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
                // NOOP
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
            var i_6 = h - 334
            if (i_6 < 0) {
                i_6 = 0
            } else if (i_6 > 100) {
                i_6 = 100
            }
            var i_7 = (client.aShort7437 - client.aShort7436) * i_6 / 100 + client.aShort7436
            val i_8 = h * i_7 * 512 / (w * 334)
            val i_9: Int
            val i_10: Int
            val s_11: Short
            if (i_8 < client.aShort7214) {
                s_11 = client.aShort7214
                i_7 = s_11.toInt() * w * 334 / (h * 512)
                if (i_7 > client.aShort7441) {
                    i_7 = client.aShort7441.toInt()
                    i_9 = h * i_7 * 512 / (s_11 * 334)
                    i_10 = (w - i_9) / 2
                    if (resetPixels) {
                        Renderers.activeToolkit!!.resetClipping()
                        Renderers.activeToolkit!!.clearPixels(x, y, i_10, h, 0xffffff.inv())
                        Renderers.activeToolkit!!.clearPixels(x + w - i_10, y, i_10, h, 0xffffff.inv())
                    }
                    x += i_10
                    w -= i_10 * 2
                }
            } else if (i_8 > client.aShort7474) {
                s_11 = client.aShort7474
                i_7 = s_11.toInt() * w * 334 / (h * 512)
                if (i_7 < client.aShort7276) {
                    i_7 = client.aShort7276.toInt()
                    i_9 = s_11 * w * 334 / (i_7 * 512)
                    i_10 = (h - i_9) / 2
                    if (resetPixels) {
                        Renderers.activeToolkit!!.resetClipping()
                        Renderers.activeToolkit!!.clearPixels(x, y, w, i_10, 0xffffff.inv())
                        Renderers.activeToolkit!!.clearPixels(x, h + y - i_10, w, i_10, 0xffffff.inv())
                    }
                    y += i_10
                    h -= i_10 * 2
                }
            }
            client.anInt7451 = h * i_7 / 334
            client.hintArrowOffsetX = x
            client.hintArrowOffsetY = y
            client.gameAreaWidth = w
            client.gameAreaHeight = h
        }

        @JvmStatic
        fun drawFadingFilledRectangle(toolkit: AbstractToolkit, baseLight: Int, baseSaturation: Int, component: Component, hue: Int) {
            for (saturation in 7 downTo 0) {
                for (lightness in 127 downTo 0) {
                    val hsl = (hue and 0x3f shl 10) or (saturation and 0x7 shl 7) or (lightness and 0x7f)
                    ColorUtil.initializeTables(false, true)
                    val rgb = ColorUtil.unsignedHslToRgb!![hsl]
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
