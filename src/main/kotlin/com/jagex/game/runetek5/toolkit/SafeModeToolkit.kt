package com.jagex.game.runetek5.toolkit

import java.awt.*
import kotlin.math.floor
import kotlin.math.sqrt

import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.framebuffer.SafeModeRenderBuffer
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.font.FontRendererSM
import com.jagex.game.runetek5.graphics.font.OpaqueFontRendererSM
import com.jagex.game.runetek5.graphics.font.TransparentFontRendererSM
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelSM
import com.jagex.game.runetek5.graphics.model.particle.ParticleNode
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.model.particle.PointEntity
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.OpaqueSpriteSM
import com.jagex.game.runetek5.graphics.sprite.PaletteSpriteSM
import com.jagex.game.runetek5.graphics.sprite.SpriteSM
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.sprite.TransparentSpriteSM
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator
import com.jagex.game.runetek5.graphics.texture.SafeModeTexture2D
import com.jagex.game.runetek5.imageproducer.AbstractImageProducerSM
import com.jagex.game.runetek5.imageproducer.BufferedImageProducerSM
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.imageproducer.ImageProducerSM1
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.rendering.RenderInfo
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.ground.GroundSM
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.safemode.RasterizerSM
import com.jagex.game.runetek5.toolkit.safemode.ThreadResourceSM
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.Utils

open class SafeModeToolkit : AbstractToolkit {

    @JvmField
    var currentCycle: Int = 0

    @JvmField
    var drawableX: Int = 0

    @JvmField
    var drawableY: Int = 0

    @JvmField
    var drawableWidth: Int = 0

    @JvmField
    var drawableHeight: Int = 0

    @JvmField
    var initialized: Boolean = false

    @JvmField
    var shutdown: Boolean = false

    @JvmField
    var viewportHalfWidth: Float = 0f

    @JvmField
    var viewportHalfHeight: Float = 0f

    @JvmField
    var viewportOriginX: Float = 0f

    @JvmField
    var viewportOriginY: Float = 0f

    @JvmField
    var clipLeft: Int = 0

    @JvmField
    var clipTop: Int = 0

    @JvmField
    var clipRight: Int = 0

    @JvmField
    var clipBottom: Int = 0

    @JvmField
    var depthRangeFar: Float = 0f

    @JvmField
    var depthRangeNear: Float = 0f

    @JvmField
    var lowestContrast: Int = 0

    @JvmField
    var highestContrast: Int = 0

    @JvmField
    var lightX: Int = 0

    @JvmField
    var lightY: Int = 0

    @JvmField
    var lightZ: Int = 0

    @JvmField
    var executingThreadCount: Int = 0

    @JvmField
    var renderingThreads: Array<ThreadResourceSM>? = null

    @JvmField
    var renderedMaterialId: Int = 0

    @JvmField
    var renderedNativeSprite: NativeSprite? = null

    @JvmField
    var currentSunPosition: Matrix43? = null

    @JvmField
    var lightIntensity: Int = 0

    @JvmField
    val frustumPlanes: Array<FloatArray>

    @JvmField
    val materialSize: Int

    @JvmField
    var forceSmallTexture: Boolean = false

    @JvmField
    val animatedSpriteMaterials: LRUCache

    @JvmField
    var animatedModelMaterials: LRUCache? = null

    @JvmField
    var viewMatrix44: Matrix44? = null

    @JvmField
    var modelMatrix: Matrix44? = null

    @JvmField
    var floorMatrix: Matrix44? = null

    @JvmField
    var viewportWidth: Int = 0

    @JvmField
    var viewportHeight: Int = 0

    @JvmField
    var colourBuffer: IntArray? = null

    @JvmField
    var depthBuffer: FloatArray? = null

    constructor(interface221: MaterialProvider?) : super(interface221) {
        this.shutdown = false
        this.initialized = false
        this.clipLeft = 0
        this.clipRight = 0
        this.clipTop = 0
        this.clipBottom = 0
        this.lowestContrast = 45823
        this.highestContrast = 78642
        this.lightIntensity = 75518
        this.frustumPlanes = Array(6) { FloatArray(4) }
        this.depthRangeFar = 1.0f
        this.depthRangeNear = 0.0f
        this.materialSize = 128
        this.forceSmallTexture = false
        this.animatedSpriteMaterials = LRUCache(16)
        this.renderedMaterialId = -1
        try {
            this.animatedModelMaterials = LRUCache(256)
            this.currentSunPosition = Matrix43()
            this.viewMatrix44 = Matrix44()
            this.modelMatrix = Matrix44()
            this.floorMatrix = Matrix44()
            initializeThreads(1)
            mainThreadInitialize()
            ColorUtil.initializeTables(true, true)
            this.initialized = true
            this.currentCycle = Utils.time().toInt()
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            cleanupAndShutdown()
            throw RuntimeException("")
        }
    }

    constructor(gameCanvas: Canvas?, materialProvider: MaterialProvider?, screenWidth: Int, screenHeight: Int) : this(materialProvider) {
        try {
            ensureImageproducerCreated(gameCanvas!!, screenWidth, screenHeight)
            setActiveCanvas(gameCanvas!!)
        } catch (throwable6: Throwable) {
            throwable6.printStackTrace()
            cleanupAndShutdown()
            throw RuntimeException("")
        }
    }

    override fun getClipping(clipping: IntArray) {
        clipping[0] = this.clipLeft
        clipping[1] = this.clipTop
        clipping[2] = this.clipRight
        clipping[3] = this.clipBottom
    }

    fun bind(width: Int, height: Int, pixels: IntArray?, depth: FloatArray?) {
        this.viewportWidth = width
        this.viewportHeight = height
        this.colourBuffer = pixels
        this.depthBuffer = depth
        for (threadIndex in 0 until this.executingThreadCount) {
            this.renderingThreads!![threadIndex].initializeRasterizer()
        }
        resetClipping()
        calculateViewportCenter()
    }

    override fun resetClipping() {
        this.clipLeft = 0
        this.clipTop = 0
        this.clipRight = this.viewportWidth
        this.clipBottom = this.viewportHeight
        viewportUpdate()
    }

    /**************************************************************
     *					(TODO: textures)
     *************************************************************/

    override fun animateTextures(clientCycle: Int) {
        val cycle = clientCycle - this.currentCycle // ~30 avg ish
        //System.out.println("CYCLE: "+cycle);
        //cycle = 30;
        var materialNode = this.animatedModelMaterials!!.getFirst() as SafeModeTexture2D?
        while (materialNode != null) {
            if (materialNode.isAnimated) {
                materialNode.cycle += cycle
                val materialCycle = materialNode.cycle / 50
                if (materialCycle > 0) {
                    val material = materialProvider!!.getMaterial(materialNode.materialId)
                    val size = if (material.isSmall) 64.0f else 128.0f
                    materialNode.animate(
                        (size * (material.textureSpeedU * (cycle.toFloat() / 1000.0f) / 64.0f)).toInt(),
                        (material.textureSpeedV * (cycle.toFloat() / 1000.0f) / 64.0f * size).toInt()
                    )
                    materialNode.cycle -= materialCycle * 50
                }
                materialNode.isAnimated = false
            }
            materialNode = this.animatedModelMaterials!!.getNext() as SafeModeTexture2D?
        }
        this.currentCycle = clientCycle
        this.animatedSpriteMaterials.clean(5)
        this.animatedModelMaterials!!.clean(5)
    }

    fun getAnimatedMaterialArgb(materialId: Int): IntArray? {
        val animatedMaterial: SafeModeTexture2D?
        synchronized(this.animatedModelMaterials!!) {
            var mat = this.animatedModelMaterials!!.get(materialId.toLong() or Long.MIN_VALUE) as SafeModeTexture2D?
            if (mat == null) {
                if (!materialProvider!!.isCached(materialId)) {
                    return null
                }
                val material = materialProvider!!.getMaterial(materialId)
                val forcedTextureSize = if (material.isSmall || this.forceSmallTexture) 64 else this.materialSize
                mat = SafeModeTexture2D(materialId, forcedTextureSize, materialProvider!!.getPixelsArgb(materialId, forcedTextureSize, forcedTextureSize), material.effectCombiner != 1)
                this.animatedModelMaterials!!.put(mat, materialId.toLong() or Long.MIN_VALUE)
            }
            animatedMaterial = mat
        }
        animatedMaterial!!.isAnimated = true
        return animatedMaterial.getArgbColor()
    }

    fun isMaterialCached(materialId: Int): Boolean {
        return materialProvider!!.isCached(materialId)
    }

    fun getMaterialEffectCombiner(materialId: Int): Int {
        return materialProvider!!.getMaterial(materialId).effectCombiner
    }

    fun getMaterialColor(materialId: Int): Int {
        return materialProvider!!.getMaterial(materialId).colorHsl.toInt() and 0xffff
    }

    fun clearDepth() {
        if (this.depthBuffer != null) {
            if (this.clipLeft == 0 && this.clipRight == this.viewportWidth && this.clipTop == 0 && this.clipBottom == this.viewportHeight) {
                val depthBufferLength = this.depthBuffer!!.size
                val depth = depthBufferLength - (depthBufferLength and 0x7)
                var pixelIndex = 0
                while (pixelIndex < depth) {
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                }
                while (pixelIndex < depthBufferLength) {
                    this.depthBuffer!![pixelIndex++] = 2.14748365E9f
                }
            } else {
                var width = this.clipRight - this.clipLeft
                val height = this.clipBottom - this.clipTop
                val scanOffset = this.viewportWidth - width
                val i_4 = this.viewportWidth * this.clipTop + this.clipLeft
                val i_5 = width shr 3
                val i_6 = width and 0x7
                width = i_4 - 1
                for (invertedHeight in -height until 0) {
                    var i_8: Int
                    if (i_5 > 0) {
                        i_8 = i_5
                        do {
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            --i_8
                        } while (i_8 > 0)
                    }

                    if (i_6 > 0) {
                        i_8 = i_6
                        do {
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            --i_8
                        } while (i_8 > 0)
                    }
                    width += scanOffset
                }
            }
        }
    }


    override fun accumulateViewportDimensions(viewportDimensions: IntArray) {
        viewportDimensions[0] = this.viewportWidth
        viewportDimensions[1] = this.viewportHeight
    }

    override fun calculateViewportCenter() {
        this.drawableX = 0
        this.drawableY = 0
        this.drawableWidth = this.viewportWidth
        this.drawableHeight = this.viewportHeight
        viewportUpdate()
    }

    override fun setDrawableArea(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        this.drawableX = i_1
        this.drawableY = i_2
        this.drawableWidth = i_3
        this.drawableHeight = i_4
        viewportUpdate()
    }

    override fun shutdown() {
        if (this.initialized) {
            ColorUtil.resetTables(true, false)
            this.initialized = false
        }
        this.shutdown = true
    }

    fun viewportUpdate() {
        val clipWidth = this.clipRight - this.clipLeft
        val clipHeight = this.clipBottom - this.clipTop
        val f_3 = this.drawableWidth / 2.0f
        this.viewportHalfWidth = f_3
        val f_4 = this.drawableHeight / 2.0f
        this.viewportHalfHeight = f_4
        this.viewportOriginX = this.drawableX + f_3
        this.viewportOriginY = this.drawableY + f_4
        for (threadIndex in 0 until this.executingThreadCount) {
            val renderingThread = this.renderingThreads!![threadIndex]
            val rasterizer = renderingThread.rasterizer
            rasterizer!!.offsetX = f_3
            rasterizer.offsetY = f_4
            rasterizer.centerX = this.viewportOriginX - this.clipLeft
            rasterizer.centerY = this.viewportOriginY - this.clipTop
            rasterizer.clipWidth = clipWidth
            rasterizer.clipHeight = clipHeight
        }
        var scanlineOffset = this.viewportWidth * this.clipTop + this.clipLeft
        for (y in this.clipTop until this.clipBottom) {
            for (threadIndex in 0 until this.executingThreadCount) {
                this.renderingThreads!![threadIndex].rasterizer!!.scanlineOffsets!![y - this.clipTop] = scanlineOffset
            }
            scanlineOffset += this.viewportWidth
        }
    }


    override fun addWaterTile(averageHeight: Int, waterTile: WaterTile) {
        val viewport = getThreadResource(Thread.currentThread())
        viewport!!.waterAverageHeight = averageHeight
        viewport.fogColor = waterTile.color
        viewport.waterScale = waterTile.scale
    }

    override fun onTilesDrawn() {
        for (index in this.renderingThreads!!.indices) {
            this.renderingThreads!![index].fogColor = this.renderingThreads!![index].lastWaterColor
            this.renderingThreads!![index].hasWaterUpdate = false
        }
    }

    override fun utilisesNativeHeap(): Boolean {
        return false
    }

    override fun getRenderInfo(): RenderInfo {
        return RenderInfo(0, "Pure Java", 1, "CPU", 0L)
    }

    override fun awaitRenderCompletion() {
    }


    override fun setBloomFilter(f_1: Float, f_2: Float, f_3: Float) {
    }


    fun renderParticles(bool1: Boolean, bool2: Boolean, particleWrapper: ParticleWrapper) {
        val threadResource = getThreadResource(Thread.currentThread())
        val mainNode = particleWrapper.particleBinaryTree.current

        var particleNode: ParticleNode? = mainNode!!.child
        while (particleNode != mainNode) {
            val pointEntry = particleNode as PointEntity
            val positionX = pointEntry.x shr 12
            val positionY = pointEntry.y shr 12
            val positionZ = pointEntry.z shr 12
            val f_12 = this.floorMatrix!!.elements[14] + this.floorMatrix!!.elements[10] * positionZ + positionX * this.floorMatrix!!.elements[2] + positionY * this.floorMatrix!!.elements[6]
            val f_13 = this.floorMatrix!!.elements[15] + this.floorMatrix!!.elements[7] * positionY + this.floorMatrix!!.elements[3] * positionX + this.floorMatrix!!.elements[11] * positionZ
            if (f_12 >= -f_13) {
                val f_14 = this.depthRangeNear + f_12 * this.depthRangeFar / f_13
                if (f_12 <= threadResource!!.aFloat2305) {
                    val f_15 = this.floorMatrix!!.elements[0] * positionX + this.floorMatrix!!.elements[4] * positionY + positionZ * this.floorMatrix!!.elements[8] + this.floorMatrix!!.elements[12]
                    val f_16 = positionZ * this.floorMatrix!!.elements[9] + this.floorMatrix!!.elements[1] * positionX + this.floorMatrix!!.elements[5] * positionY + this.floorMatrix!!.elements[13]
                    if (f_15 >= -f_13 && f_15 <= f_13 && f_16 >= -f_13 && f_16 <= f_13) {
                        val f_17 = pointEntry.size / 4096.0f
                        val f_18 = f_17 * this.modelMatrix!!.elements[0] + f_15
                        val f_19 = f_13 + this.modelMatrix!!.elements[3] * f_17
                        val f_20 = this.viewportOriginX + f_15 * this.viewportHalfWidth / f_13
                        val f_21 = this.viewportHalfHeight * f_16 / f_13 + this.viewportOriginY
                        val f_22 = this.viewportOriginX + f_18 * this.viewportHalfWidth / f_19
                        renderMaterializedSprite(bool1, bool2, pointEntry, f_20.toInt(), f_21.toInt(), f_14, (if (f_22 < f_20) f_20 - f_22 else f_22 - f_20).toInt())
                    }
                }
            }
            particleNode = particleNode.child
        }

    }

    override fun supportsFullscreen(): Boolean {
        return false
    }

    override fun forceFixedScreenmode(): Boolean {
        return true
    }

    override fun hasBloomSupport(): Boolean {
        return false
    }

    override fun hasHardwareRendering(): Boolean { // unknown
        return false
    }

    override fun hasShadows(): Boolean {
        return false
    }

    override fun hasAntiAliasing(): Boolean {
        return false
    }

    override fun hasTextures(): Boolean {
        return false
    }

    override fun extendBuildArea(): Boolean {
        return false
    }

    override fun replaceCanvasOnDisplayUpdate(): Boolean {
        return true
    }

    override fun createDelegatingDisplay(canvas1: Canvas, i_2: Int, i_3: Int): DelegatingDisplay {
        return createImageProducer(this, canvas1, i_2, i_3)
    }

    override fun setDepthBuffering(useDepthBuffer: Boolean) {
        val threadResource = getThreadResource(Thread.currentThread())
        threadResource!!.zbuffering = useDepthBuffer
    }

    override fun copyPixelRectangle(rotation: Int, offY: Int, width: Int, height: Int): IntArray {
        if (this.colourBuffer == null) {
            throw IllegalStateException("")
        }
        val pixels = IntArray(width * height)
        var pixelIndex = 0
        for (y in 0 until height) {
            val off = (y + offY) * this.viewportWidth + rotation
            for (x in 0 until width) {
                pixels[pixelIndex++] = this.colourBuffer!![off + x]
            }
        }
        return pixels
    }

    override fun renderLastFrame() {
    }

    override fun createNativeSprite(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean): NativeSprite {
        var bool7 = false
        var i_8 = i_2

        for (i_9 in 0 until i_5) {
            for (i_10 in 0 until i_4) {
                val i_11 = ints_1[i_8++] ushr 24
                if (i_11 != 0 && i_11 != 255) {
                    bool7 = true
                    return TransparentSpriteSM(this, ints_1, i_2, i_3, i_4, i_5, bool6)
                }
            }
        }

        return OpaqueSpriteSM(this, ints_1, i_2, i_3, i_4, i_5, bool6)
    }

    override fun createFontRenderer(fontmetrics: FontMetrics?, spriteTypes: Array<SpriteType>?, monochome: Boolean): AbstractFontRenderer {
        val spriteWidths = IntArray(spriteTypes!!.size)
        val spriteHeights = IntArray(spriteTypes.size)
        var hasAlpha = false
        for (spriteIndex in spriteTypes.indices) {
            spriteWidths[spriteIndex] = spriteTypes[spriteIndex].width
            spriteHeights[spriteIndex] = spriteTypes[spriteIndex].height
            if (spriteTypes[spriteIndex].alpha != null) {
                hasAlpha = true
            }
        }
        if (monochome) {
            return if (hasAlpha) {
                TransparentFontRendererSM(this, fontmetrics!!, spriteTypes, spriteWidths, spriteHeights)
            } else {
                OpaqueFontRendererSM(this, fontmetrics!!, spriteTypes, spriteWidths, spriteHeights)
            }
        } else if (hasAlpha) {
            throw IllegalArgumentException("")
        } else {
            return FontRendererSM(this, fontmetrics!!, spriteTypes, spriteWidths, spriteHeights)
        }
    }

    override fun expandClipping(left: Int, top: Int, right: Int, bottom: Int) {
        if (this.clipLeft < left) {
            this.clipLeft = left
        }
        if (this.clipTop < top) {
            this.clipTop = top
        }
        if (this.clipRight > right) {
            this.clipRight = right
        }
        if (this.clipBottom > bottom) {
            this.clipBottom = bottom
        }
        viewportUpdate()
    }

    override fun drawFilledRectangle(x: Int, y: Int, width: Int, height: Int, color: Int, type: Int) {
        var x = x
        var y = y
        var width = width
        var height = height
        var color = color
        if (this.colourBuffer != null) {
            if (x < this.clipLeft) {
                width -= this.clipLeft - x
                x = this.clipLeft
            }
            if (y < this.clipTop) {
                height -= this.clipTop - y
                y = this.clipTop
            }
            if (width + x > this.clipRight) {
                width = this.clipRight - x
            }
            if (y + height > this.clipBottom) {
                height = this.clipBottom - y
            }
            if (width > 0 && height > 0 && x <= this.clipRight && y <= this.clipBottom) {
                val i_7 = this.viewportWidth - width
                var i_8 = y * this.viewportWidth + x
                val alpha = color ushr 24
                var transparency: Int
                var i_11: Int
                var i_12: Int
                if (type != 0 && (type != 1 || alpha != 255)) {
                    var lastColor: Int
                    if (type == 1) {
                        color = (alpha * ((color and -16711936) ushr 8) and -16711936) + (alpha * (color and 0xff00ff) shr 8 and 0xff00ff)
                        transparency = 256 - alpha
                        i_11 = 0
                        while (i_11 < height) {
                            i_12 = -width
                            while (i_12 < 0) {
                                lastColor = this.colourBuffer!![i_8]
                                lastColor = (((lastColor and -16711936) ushr 8) * transparency and -16711936) + ((lastColor and 0xff00ff) * transparency shr 8 and 0xff00ff)
                                this.colourBuffer!![i_8++] = color + lastColor
                                i_12++
                            }
                            i_8 += i_7
                            i_11++
                        }
                    } else {
                        if (type != 2) {
                            throw IllegalArgumentException()
                        }
                        transparency = 0
                        while (transparency < height) {
                            i_11 = -width
                            while (i_11 < 0) {
                                i_12 = this.colourBuffer!![i_8]
                                lastColor = color + i_12
                                val i_14 = (i_12 and 0xff00ff) + (color and 0xff00ff)
                                i_12 = (i_14 and 0x1000100) + (lastColor - i_14 and 0x10000)
                                this.colourBuffer!![i_8++] = lastColor - i_12 or (i_12 - (i_12 ushr 8))
                                i_11++
                            }
                            i_8 += i_7
                            transparency++
                        }
                    }
                } else {
                    transparency = width shr 3
                    i_11 = width and 0x7
                    width = i_8 - 1
                    i_12 = -height
                    while (i_12 < 0) {
                        if (transparency > 0) {
                            var xx = transparency
                            do {
                                ++width
                                this.colourBuffer!![width] = color
                                ++width
                                this.colourBuffer!![width] = color
                                ++width
                                this.colourBuffer!![width] = color
                                ++width
                                this.colourBuffer!![width] = color
                                ++width
                                this.colourBuffer!![width] = color
                                ++width
                                this.colourBuffer!![width] = color
                                ++width
                                this.colourBuffer!![width] = color
                                ++width
                                this.colourBuffer!![width] = color
                                --xx
                            } while (xx > 0)
                        }
                        if (i_11 > 0) {
                            var xx = i_11
                            do {
                                ++width
                                this.colourBuffer!![width] = color
                                --xx
                            } while (xx > 0)
                        }
                        width += i_7
                        i_12++
                    }
                }
            }
        }
    }

    override fun renderWorldmapTile(x: Int, y: Int, width: Int, height: Int, color2: Int, color: Int, bytes_7: ByteArray, i_8: Int, i_9: Int) {
        var x = x
        var width = width
        var height = height
        if (this.colourBuffer != null && width > 0 && height > 0) {
            var i_10 = 0
            var i_11 = 0
            val i_12 = (i_8 shl 16) / width
            val i_13 = (bytes_7.size / i_8 shl 16) / height
            var i_14 = y * this.viewportWidth + x
            var i_15 = this.viewportWidth - width
            if (y + height > this.clipBottom) {
                height -= y + height - this.clipBottom
            }
            var i_16: Int
            if (y < this.clipTop) {
                i_16 = this.clipTop - y
                height -= i_16
                i_14 += i_16 * this.viewportWidth
                i_11 += i_16 * i_13
            }
            if (width + x > this.clipRight) {
                i_16 = width + x - this.clipRight
                width -= i_16
                i_15 += i_16
            }
            if (x < this.clipLeft) {
                i_16 = this.clipLeft - x
                width -= i_16
                i_14 += i_16
                i_10 += i_16 * i_12
                i_15 += i_16
            }
            i_16 = color2 ushr 24
            val i_17 = color ushr 24
            var i_18: Int
            var i_19: Int
            var i_20: Int
            var i_21: Int
            if (i_9 == 0 || i_9 == 1 && i_16 == 255 && i_17 == 255) {
                i_18 = i_10
                i_19 = -height
                while (i_19 < 0) {
                    i_20 = i_8 * (i_11 shr 16)
                    i_21 = -width
                    while (i_21 < 0) {
                        if (bytes_7[i_20 + (i_10 shr 16)].toInt() != 0) {
                            this.colourBuffer!![i_14++] = color
                        } else {
                            this.colourBuffer!![i_14++] = color2
                        }
                        i_10 += i_12
                        i_21++
                    }
                    i_11 += i_13
                    i_10 = i_18
                    i_14 += i_15
                    i_19++
                }
            } else {
                var i_22: Int
                var i_23: Int
                var i_24: Int
                var i_25: Int
                if (i_9 == 1) {
                    i_18 = i_10
                    i_19 = -height
                    while (i_19 < 0) {
                        i_20 = i_8 * (i_11 shr 16)
                        i_21 = -width
                        while (i_21 < 0) {
                            i_22 = color2
                            if (bytes_7[i_20 + (i_10 shr 16)].toInt() != 0) {
                                i_22 = color
                            }
                            i_23 = i_22 ushr 24
                            i_24 = 255 - i_23
                            i_25 = this.colourBuffer!![i_14]
                            this.colourBuffer!![i_14++] = (i_24 * (i_25 and 0xff00) + i_23 * (i_22 and 0xff00) and 0xff0000) + ((i_25 and 0xff00ff) * i_24 + (i_22 and 0xff00ff) * i_23 and -16711936) shr 8
                            i_10 += i_12
                            i_21++
                        }
                        i_11 += i_13
                        i_10 = i_18
                        i_14 += i_15
                        i_19++
                    }
                } else {
                    if (i_9 != 2) {
                        throw IllegalArgumentException()
                    }
                    i_18 = i_10
                    i_19 = -height
                    while (i_19 < 0) {
                        i_20 = i_8 * (i_11 shr 16)
                        i_21 = -width
                        while (i_21 < 0) {
                            i_22 = color2
                            if (bytes_7[i_20 + (i_10 shr 16)].toInt() != 0) {
                                i_22 = color
                            }
                            if (i_22 != 0) {
                                i_23 = this.colourBuffer!![i_14]
                                i_24 = i_22 + i_23
                                i_25 = (i_23 and 0xff00ff) + (i_22 and 0xff00ff)
                                i_23 = (i_25 and 0x1000100) + (i_24 - i_25 and 0x10000)
                                this.colourBuffer!![i_14++] = i_24 - i_23 or (i_23 - (i_23 ushr 8))
                            } else {
                                ++i_14
                            }
                            i_10 += i_12
                            i_21++
                        }
                        i_11 += i_13
                        i_10 = i_18
                        i_14 += i_15
                        i_19++
                    }
                }
            }
        }
    }

    override fun drawCircle(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        var i_3 = i_3
        var i_4 = i_4
        var i_22 = i_2
        if (this.colourBuffer != null) {
            if (i_3 < 0) {
                i_3 = -i_3
            }

            var i_6 = i_22 - i_3
            if (i_6 < this.clipTop) {
                i_6 = this.clipTop
            }

            var i_7 = i_3 + i_22 + 1
            if (i_7 > this.clipBottom) {
                i_7 = this.clipBottom
            }

            var i_8 = i_6
            val i_9 = i_3 * i_3
            var i_10 = 0
            var i_11 = i_22 - i_6
            var i_12 = i_11 * i_11
            var i_13 = i_12 - i_11
            if (i_22 > i_7) {
                i_22 = i_7
            }

            val i_14 = i_4 ushr 24
            var i_15: Int
            var i_16: Int
            var i_17: Int
            var i_18: Int
            if ((i_14 != 255)) {
                var i_19: Int
                var i_20: Int
                i_4 = (i_14 * (i_4 and 0xff00ff) shr 8 and 0xff00ff) + (i_14 * (i_4 and 0xff00) shr 8 and 0xff00) + (i_14 shl 24)

                i_15 = 256 - i_14
                while (i_8 < i_22) {
                    while (i_13 <= i_9 || i_12 <= i_9) {
                        i_12 += i_10 + i_10
                        i_13 += i_10++ + i_10
                    }

                    i_16 = i_1 - i_10 + 1
                    if (i_16 < this.clipLeft) {
                        i_16 = this.clipLeft
                    }

                    i_17 = i_10 + i_1
                    if (i_17 > this.clipRight) {
                        i_17 = this.clipRight
                    }

                    i_18 = i_16 + i_8 * this.viewportWidth

                    i_19 = i_16
                    while (i_19 < i_17) {
                        i_20 = this.colourBuffer!![i_18]
                        i_20 = ((i_20 and 0xff00ff) * i_15 shr 8 and 0xff00ff) + (i_15 * (i_20 and 0xff00) shr 8 and 0xff00)
                        this.colourBuffer!![i_18++] = i_20 + i_4
                        i_19++
                    }

                    ++i_8
                    i_12 -= i_11-- + i_11
                    i_13 -= i_11 + i_11
                }

                i_10 = i_3
                i_11 = -i_11
                i_13 = i_9 + i_11 * i_11
                i_12 = i_13 - i_3

                i_13 -= i_11
                while (i_8 < i_7) {
                    while (i_13 > i_9 && i_12 > i_9) {
                        i_13 -= i_10-- + i_10
                        i_12 -= i_10 + i_10
                    }

                    i_16 = i_1 - i_10
                    if (i_16 < this.clipLeft) {
                        i_16 = this.clipLeft
                    }

                    i_17 = i_10 + i_1
                    if (i_17 > this.clipRight - 1) {
                        i_17 = this.clipRight - 1
                    }

                    i_18 = i_16 + i_8 * this.viewportWidth

                    i_19 = i_16
                    while (i_19 <= i_17) {
                        i_20 = this.colourBuffer!![i_18]
                        i_20 = (i_15 * (i_20 and 0xff00) shr 8 and 0xff00) + ((i_20 and 0xff00ff) * i_15 shr 8 and 0xff00ff)
                        this.colourBuffer!![i_18++] = i_20 + i_4
                        i_19++
                    }

                    ++i_8
                    i_13 += i_11 + i_11
                    i_12 += i_11++ + i_11
                }
            } else {
                while (i_8 < i_22) {
                    while (i_13 <= i_9 || i_12 <= i_9) {
                        i_12 += i_10 + i_10
                        i_13 += i_10++ + i_10
                    }

                    i_15 = i_1 - i_10 + 1
                    if (i_15 < this.clipLeft) {
                        i_15 = this.clipLeft
                    }

                    i_16 = i_10 + i_1
                    if (i_16 > this.clipRight) {
                        i_16 = this.clipRight
                    }

                    i_17 = i_15 + i_8 * this.viewportWidth

                    i_18 = i_15
                    while (i_18 < i_16) {
                        this.colourBuffer!![i_17++] = i_4
                        i_18++
                    }

                    ++i_8
                    i_12 -= i_11-- + i_11
                    i_13 -= i_11 + i_11
                }

                i_10 = i_3
                i_11 = i_8 - i_22
                i_13 = i_9 + i_11 * i_11
                i_12 = i_13 - i_3

                i_13 -= i_11
                while (i_8 < i_7) {
                    while (i_13 > i_9 && i_12 > i_9) {
                        i_13 -= i_10-- + i_10
                        i_12 -= i_10 + i_10
                    }

                    i_15 = i_1 - i_10
                    if (i_15 < this.clipLeft) {
                        i_15 = this.clipLeft
                    }

                    i_16 = i_10 + i_1
                    if (i_16 > this.clipRight - 1) {
                        i_16 = this.clipRight - 1
                    }

                    i_17 = i_15 + i_8 * this.viewportWidth

                    i_18 = i_15
                    while (i_18 <= i_16) {
                        this.colourBuffer!![i_17++] = i_4
                        i_18++
                    }

                    ++i_8
                    i_13 += i_11 + i_11
                    i_12 += i_11++ + i_11
                }

            }
        }

    }

    override fun drawTransparentHorizontalLine(x: Int, y: Int, width: Int, color: Int, alpha: Int) {
        var x = x
        var width = width
        var color = color
        if (this.colourBuffer != null && y >= this.clipTop && y < this.clipBottom) {
            if (x < this.clipLeft) {
                width -= this.clipLeft - x
                x = this.clipLeft
            }

            if (width + x > this.clipRight) {
                width = this.clipRight - x
            }

            val i_6 = y * this.viewportWidth + x
            val colorAlpha = color ushr 24
            var i_8: Int
            if (alpha == 0 || alpha == 1 && colorAlpha == 255) {
                for (i_8 in 0 until width) {
                    this.colourBuffer!![i_6 + i_8] = color
                }
            } else {
                var i_9: Int
                var i_10: Int
                if (alpha == 1) {
                    color = (colorAlpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha shl 24) + (colorAlpha * (color and 0xff00) shr 8 and 0xff00)
                    i_8 = 256 - colorAlpha

                    for (i_9 in 0 until width) {
                        i_10 = this.colourBuffer!![i_9 + i_6]
                        i_10 = ((i_10 and 0xff00ff) * i_8 shr 8 and 0xff00ff) + (i_8 * (i_10 and 0xff00) shr 8 and 0xff00)
                        this.colourBuffer!![i_9 + i_6] = i_10 + color
                    }
                } else {
                    if (alpha != 2) {
                        throw IllegalArgumentException()
                    }

                    for (i_8 in 0 until width) {
                        i_9 = this.colourBuffer!![i_6 + i_8]
                        i_10 = i_9 + color
                        val i_11 = (i_9 and 0xff00ff) + (color and 0xff00ff)
                        i_9 = (i_11 and 0x1000100) + (i_10 - i_11 and 0x10000)
                        this.colourBuffer!![i_8 + i_6] = i_10 - i_9 or (i_9 - (i_9 ushr 8))
                    }
                }
            }
        }

    }

    fun drawDashedHorizontalLine(x: Int, y: Int, width: Int, color: Int, maxY: Int, i_7: Int, i_8: Int) {
        var color = color
        if (this.colourBuffer != null && y >= this.clipTop && y < this.clipBottom) {
            val offset = y * this.viewportWidth + x
            val alphaComponent = color ushr 24
            val i_11 = i_7 + maxY
            var deltaY = i_8 % i_11
            if (alphaComponent == 255) {
                var deltaX = 0
                while (deltaX < width) {
                    if (deltaX + x >= this.clipLeft && deltaX + x < this.clipRight && deltaY < maxY) {
                        this.colourBuffer!![deltaX + offset] = color
                    }

                    ++deltaX
                    ++deltaY
                    deltaY %= i_11
                }
            } else {
                var deltaX: Int
                var currentColor: Int
                color = (alphaComponent * (color and 0xff00) shr 8 and 0xff00) + (alphaComponent * (color and 0xff00ff) shr 8 and 0xff00ff) + (alphaComponent shl 24)
                val transparency = 256 - alphaComponent

                deltaX = 0
                while (deltaX < width) {
                    if (deltaX + x >= this.clipLeft && deltaX + x < this.clipRight && deltaY < maxY) {
                        currentColor = this.colourBuffer!![deltaX + offset]
                        currentColor = ((currentColor and 0xff00ff) * transparency shr 8 and 0xff00ff) + (transparency * (currentColor and 0xff00) shr 8 and 0xff00)
                        this.colourBuffer!![deltaX + offset] = currentColor + color
                    }

                    ++deltaX
                    ++deltaY
                    deltaY %= i_11
                }
            }
        }

    }

    override fun drawTransparentLine(x: Int, y: Int, width: Int, height: Int, color: Int, alpha: Int) {
        var x = x
        var y = y
        var width = width
        var height = height
        var color = color
        if (this.colourBuffer != null) {
            width -= x
            height -= y
            if (height == 0) {
                if (width >= 0) {
                    drawTransparentHorizontalLine(x, y, width + 1, color, alpha)
                } else {
                    drawTransparentHorizontalLine(width + x, y, -width + 1, color, alpha)
                }
            } else if (width == 0) {
                if (height >= 0) {
                    drawTransparentVerticalLine(x, y, height + 1, color, alpha)
                } else {
                    drawTransparentVerticalLine(x, y + height, -height + 1, color, alpha)
                }
            } else {
                if (width + height < 0) {
                    x += width
                    width = -width
                    y += height
                    height = -height
                }

                var i_7: Int
                var i_8: Int
                var i_9: Int
                var i_10: Int
                var i_11: Int
                var i_12: Int
                var i_13: Int
                if (width > height) {
                    y = y shl 16
                    y += 32768
                    height = height shl 16
                    i_7 = floor(0.5 + height.toDouble() / width).toInt()
                    width += x
                    if (x < this.clipLeft) {
                        y += i_7 * (this.clipLeft - x)
                        x = this.clipLeft
                    }

                    if (width >= this.clipRight) {
                        width = this.clipRight - 1
                    }

                    i_8 = color ushr 24
                    if (alpha != 0 && (alpha != 1 || i_8 != 255)) {
                        if (alpha == 1) {
                            color = (i_8 * (color and 0xff00ff) shr 8 and 0xff00ff) + (i_8 * (color and 0xff00) shr 8 and 0xff00) + (i_8 shl 24)

                            i_9 = 256 - i_8
                            while (x <= width) {
                                i_10 = y shr 16
                                if (i_10 >= this.clipTop && i_10 < this.clipBottom) {
                                    i_11 = i_10 * this.viewportWidth + x
                                    i_12 = this.colourBuffer!![i_11]
                                    i_12 = (i_9 * (i_12 and 0xff00) shr 8 and 0xff00) + ((i_12 and 0xff00ff) * i_9 shr 8 and 0xff00ff)
                                    this.colourBuffer!![i_11] = color + i_12
                                }

                                y += i_7
                                x++
                            }
                        } else {
                            if (alpha != 2) {
                                throw IllegalArgumentException()
                            }

                            while (x <= width) {
                                i_9 = y shr 16
                                if (i_9 >= this.clipTop && i_9 < this.clipBottom) {
                                    i_10 = i_9 * this.viewportWidth + x
                                    i_11 = this.colourBuffer!![i_10]
                                    i_12 = color + i_11
                                    i_13 = (i_11 and 0xff00ff) + (color and 0xff00ff)
                                    i_11 = (i_13 and 0x1000100) + (i_12 - i_13 and 0x10000)
                                    this.colourBuffer!![i_10] = i_12 - i_11 or (i_11 - (i_11 ushr 8))
                                }

                                y += i_7
                                ++x
                            }
                        }
                    } else {
                        while (x <= width) {
                            i_9 = y shr 16
                            if (i_9 >= this.clipTop && i_9 < this.clipBottom) {
                                this.colourBuffer!![i_9 * this.viewportWidth + x] = color
                            }

                            y += i_7
                            ++x
                        }
                    }
                } else {
                    x = x shl 16
                    x += 32768
                    width = width shl 16
                    i_7 = floor(width.toDouble() / height + 0.5).toInt()
                    height += y
                    if (y < this.clipTop) {
                        x += i_7 * (this.clipTop - y)
                        y = this.clipTop
                    }

                    if (height >= this.clipBottom) {
                        height = this.clipBottom - 1
                    }

                    i_8 = color ushr 24
                    if (alpha == 0 || alpha == 1 && i_8 == 255) {
                        while (y <= height) {
                            i_9 = x shr 16
                            if (i_9 >= this.clipLeft && i_9 < this.clipRight) {
                                this.colourBuffer!![y * this.viewportWidth + i_9] = color
                            }

                            x += i_7
                            ++y
                        }
                    } else if (alpha == 1) {
                        color = (i_8 * (color and 0xff00ff) shr 8 and 0xff00ff) + (i_8 * (color and 0xff00) shr 8 and 0xff00) + (i_8 shl 24)

                        i_9 = 256 - i_8
                        while (y <= height) {
                            i_10 = x shr 16
                            if (i_10 >= this.clipLeft && i_10 < this.clipRight) {
                                i_11 = y * this.viewportWidth + i_10
                                i_12 = this.colourBuffer!![i_11]
                                i_12 = ((i_12 and 0xff00ff) * i_9 shr 8 and 0xff00ff) + (i_9 * (i_12 and 0xff00) shr 8 and 0xff00)
                                this.colourBuffer!![y * this.viewportWidth + i_10] = color + i_12
                            }

                            x += i_7
                            y++
                        }
                    } else {
                        if (alpha != 2) {
                            throw IllegalArgumentException()
                        }

                        while (y <= height) {
                            i_9 = x shr 16
                            if (i_9 >= this.clipLeft && i_9 < this.clipRight) {
                                i_10 = y * this.viewportWidth + i_9
                                i_11 = this.colourBuffer!![i_10]
                                i_12 = color + i_11
                                i_13 = (i_11 and 0xff00ff) + (color and 0xff00ff)
                                i_11 = (i_13 and 0x1000100) + (i_12 - i_13 and 0x10000)
                                this.colourBuffer!![i_10] = i_12 - i_11 or (i_11 - (i_11 ushr 8))
                            }

                            x += i_7
                            ++y
                        }
                    }
                }
            }
        }
    }

    override fun getScratchMatrix(): Matrix44 {
        val class1851 = getThreadResource(Thread.currentThread())
        return class1851!!.aClass3842346!!
    }

    override fun drawSurfaceLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, class4557: ComponentSurface, i_8: Int, i_9: Int) {
        var i_11 = i_1
        var i_31 = i_3
        var i_24 = i_2
        var i_41 = i_4
        var i_5 = i_5
        if (this.colourBuffer != null) {
            val packetsdecoder10 = class4557 as ComponentDimensionSM
            val ints_11 = packetsdecoder10.anIntArray9077
            val ints_12 = packetsdecoder10.anIntArray9078
            val i_13 = maxOf(this.clipTop, i_9)
            val i_14 = minOf(this.clipBottom, ints_11!!.size + i_9)
            i_31 -= i_11
            i_41 -= i_24
            if (i_31 + i_41 < 0) {
                i_11 += i_31
                i_31 = -i_31
                i_24 += i_41
                i_41 = -i_41
            }

            var i_15: Int
            var i_16: Int
            var i_17: Int
            var i_18: Int
            var i_19: Int
            var i_20: Int
            var i_21: Int
            var i_22: Int
            var i_23: Int
            if (i_31 > i_41) {
                i_24 = i_24 shl 16
                i_24 += 32768
                i_41 = i_41 shl 16
                i_15 = floor(i_41.toDouble() / i_31 + 0.5).toInt()
                i_31 += i_11
                if (i_11 < this.clipLeft) {
                    i_24 += i_15 * (this.clipLeft - i_11)
                    i_11 = this.clipLeft
                }

                if (i_31 >= this.clipRight) {
                    i_31 = this.clipRight - 1
                }

                i_16 = i_5 ushr 24
                if (i_16 == 255) {
                    while (i_11 <= i_31) {
                        i_17 = i_24 shr 16
                        i_18 = i_17 - i_9
                        if (i_17 >= i_13 && i_17 < i_14) {
                            i_19 = i_8 + ints_11[i_18]
                            if (i_11 >= i_19 && i_11 < i_19 + ints_12!![i_18]) {
                                this.colourBuffer!![i_17 * this.viewportWidth + i_11] = i_5
                            }
                        }

                        i_24 += i_15
                        ++i_11
                    }
                } else {
                    i_5 = (i_16 * (i_5 and 0xff00ff) shr 8 and 0xff00ff) + (i_16 * (i_5 and 0xff00) shr 8 and 0xff00) + (i_16 shl 24)

                    i_17 = 256 - i_16
                    while (i_11 <= i_31) {
                        i_18 = i_24 shr 16
                        i_19 = i_18 - i_9
                        if (i_18 >= i_13 && i_18 < i_14) {
                            i_20 = i_8 + ints_11[i_19]
                            if (i_11 >= i_20 && i_11 < i_20 + ints_12!![i_19]) {
                                i_21 = i_18 * this.viewportWidth + i_11
                                i_22 = this.colourBuffer!![i_21]
                                i_22 = (i_17 * (i_22 and 0xff00) shr 8 and 0xff00) + ((i_22 and 0xff00ff) * i_17 shr 8 and 0xff00ff)
                                this.colourBuffer!![i_21] = i_5 + i_22
                            }
                        }

                        i_24 += i_15
                        i_11++
                    }
                }
            } else {
                i_11 = i_11 shl 16
                i_11 += 32768
                i_31 = i_31 shl 16
                i_15 = floor(0.5 + i_31.toDouble() / i_41).toInt()
                i_41 += i_24
                if (i_24 < i_13) {
                    i_11 += (i_13 - i_24) * i_15
                    i_24 = i_13
                }

                if (i_41 >= i_14) {
                    i_41 = i_14 - 1
                }

                i_16 = i_5 ushr 24
                if ((i_16 != 255)) {
                    i_5 = (i_16 * (i_5 and 0xff00) shr 8 and 0xff00) + (i_16 * (i_5 and 0xff00ff) shr 8 and 0xff00ff) + (i_16 shl 24)

                    i_17 = 256 - i_16
                    while (i_24 <= i_41) {
                        i_18 = i_11 shr 16
                        i_19 = i_24 - i_9
                        i_20 = i_8 + ints_11[i_19]
                        if (i_18 >= this.clipLeft && i_18 < this.clipRight && i_18 >= i_20 && i_18 < i_20 + ints_12!![i_19]) {
                            i_21 = i_24 * this.viewportWidth + i_18
                            i_22 = this.colourBuffer!![i_21]
                            i_22 = (i_17 * (i_22 and 0xff00) shr 8 and 0xff00) + ((i_22 and 0xff00ff) * i_17 shr 8 and 0xff00ff)
                            this.colourBuffer!![i_24 * this.viewportWidth + i_18] = i_5 + i_22
                        }

                        i_11 += i_15
                        i_24++
                    }
                } else {
                    while (i_24 <= i_41) {
                        i_17 = i_11 shr 16
                        i_18 = i_24 - i_9
                        i_19 = i_8 + ints_11[i_18]
                        if (i_17 >= this.clipLeft && i_17 < this.clipRight && i_17 >= i_19 && i_17 < i_19 + ints_12!![i_18]) {
                            this.colourBuffer!![i_24 * this.viewportWidth + i_17] = i_5
                        }

                        i_11 += i_15
                        ++i_24
                    }
                }
            }
        }

    }

    override fun drawTexturedLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, class4557: ComponentSurface, i_8: Int, i_9: Int, i_10: Int, i_11: Int, i_12: Int) {
        var i_1 = i_1
        var i_2 = i_2
        var i_3 = i_3
        var i_4 = i_4
        var i_5 = i_5
        var i_10 = i_10
        var i_11 = i_11
        var i_12 = i_12
        if (this.colourBuffer != null) {
            val packetsdecoder13 = class4557 as ComponentDimensionSM
            val ints_14 = packetsdecoder13.anIntArray9077
            val ints_15 = packetsdecoder13.anIntArray9078
            val i_16 = maxOf(this.clipTop, i_9)
            val i_17 = minOf(this.clipBottom, ints_14!!.size + i_9)
            i_12 = i_12 shl 8
            i_10 = i_10 shl 8
            i_11 = i_11 shl 8
            val i_18 = i_11 + i_10
            i_12 %= i_18
            i_3 -= i_1
            i_4 -= i_2
            var i_19: Int
            var i_20: Int
            if (i_3 + i_4 < 0) {
                i_19 = (sqrt((i_3 * i_3 + i_4 * i_4).toDouble()) * 256.0).toInt()
                i_20 = i_19 % i_18
                i_12 = i_10 + i_18 - i_12 - i_20
                i_12 %= i_18
                if (i_12 < 0) {
                    i_12 += i_18
                }

                i_1 += i_3
                i_3 = -i_3
                i_2 += i_4
                i_4 = -i_4
            }

            var i_21: Int
            var i_22: Int
            var i_23: Int
            var i_24: Int
            var i_25: Int
            var i_26: Int
            var i_27: Int
            if (i_3 > i_4) {
                i_2 = i_2 shl 16
                i_2 += 32768
                i_4 = i_4 shl 16
                i_19 = floor(i_4.toDouble() / i_3 + 0.5).toInt()
                i_3 += i_1
                i_20 = i_5 ushr 24
                i_21 = sqrt(((i_19 shr 8) * (i_19 shr 8) + 65536).toDouble()).toInt()
                if (i_20 == 255) {
                    while (i_1 <= i_3) {
                        i_22 = i_2 shr 16
                        i_23 = i_22 - i_9
                        if (i_1 >= this.clipLeft && i_1 < this.clipRight && i_22 >= i_16 && i_22 < i_17 && i_12 < i_10) {
                            i_24 = i_8 + ints_14[i_23]
                            if (i_1 >= i_24 && i_1 < i_24 + ints_15!![i_23]) {
                                this.colourBuffer!![i_22 * this.viewportWidth + i_1] = i_5
                            }
                        }

                        i_2 += i_19
                        ++i_1
                        i_12 += i_21
                        i_12 %= i_18
                    }
                } else {
                    i_5 = (i_20 * (i_5 and 0xff00ff) shr 8 and 0xff00ff) + (i_20 * (i_5 and 0xff00) shr 8 and 0xff00) + (i_20 shl 24)

                    i_22 = 256 - i_20
                    while (i_1 <= i_3) {
                        i_23 = i_2 shr 16
                        i_24 = i_23 - i_9
                        if (i_1 >= this.clipLeft && i_1 < this.clipRight && i_23 >= i_16 && i_23 < i_17 && i_12 < i_10) {
                            i_25 = i_8 + ints_14[i_24]
                            if (i_1 >= i_25 && i_1 < i_25 + ints_15!![i_24]) {
                                i_26 = i_23 * this.viewportWidth + i_1
                                i_27 = this.colourBuffer!![i_26]
                                i_27 = (i_22 * (i_27 and 0xff00) shr 8 and 0xff00) + ((i_27 and 0xff00ff) * i_22 shr 8 and 0xff00ff)
                                this.colourBuffer!![i_26] = i_5 + i_27
                            }
                        }

                        i_2 += i_19
                        ++i_1
                        i_12 += i_21
                        i_12 %= i_18
                    }
                }
            } else {
                i_1 = i_1 shl 16
                i_1 += 32768
                i_3 = i_3 shl 16
                i_19 = floor(0.5 + i_3.toDouble() / i_4).toInt()
                i_20 = sqrt(((i_19 shr 8) * (i_19 shr 8) + 65536).toDouble()).toInt()
                i_4 += i_2
                i_21 = i_5 ushr 24
                if (i_21 == 255) {
                    while (i_2 <= i_4) {
                        i_22 = i_1 shr 16
                        i_23 = i_2 - i_9
                        if (i_2 >= i_16 && i_2 < i_17 && i_22 >= this.clipLeft && i_22 < this.clipRight && i_12 < i_10 && i_22 >= i_8 + ints_14[i_23] && i_22 < i_8 + ints_14[i_23] + ints_15!![i_23]) {
                            this.colourBuffer!![i_2 * this.viewportWidth + i_22] = i_5
                        }

                        i_1 += i_19
                        ++i_2
                        i_12 += i_20
                        i_12 %= i_18
                    }
                } else {
                    i_5 = (i_21 * (i_5 and 0xff00ff) shr 8 and 0xff00ff) + (i_21 * (i_5 and 0xff00) shr 8 and 0xff00) + (i_21 shl 24)

                    i_22 = 256 - i_21
                    while (i_2 <= i_4) {
                        i_23 = i_1 shr 16
                        i_24 = i_2 - i_9
                        if (i_2 >= i_16 && i_2 < i_17 && i_23 >= this.clipLeft && i_23 < this.clipRight && i_12 < i_10 && i_23 >= i_8 + ints_14[i_24] && i_23 < i_8 + ints_14[i_24] + ints_15!![i_24]) {
                            i_25 = i_2 * this.viewportWidth + i_23
                            i_26 = this.colourBuffer!![i_25]
                            i_26 = (i_22 * (i_26 and 0xff00) shr 8 and 0xff00) + ((i_26 and 0xff00ff) * i_22 shr 8 and 0xff00ff)
                            this.colourBuffer!![i_2 * this.viewportWidth + i_23] = i_5 + i_26
                        }

                        i_1 += i_19
                        ++i_2
                        i_12 += i_20
                        i_12 %= i_18
                    }
                }
            }
        }

    }

    override fun isOnScreen(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int): Int {
        var flags = 0
        val f_8 = i_3 * this.floorMatrix!!.elements[10] + i_2 * this.floorMatrix!!.elements[6] + this.floorMatrix!!.elements[14] + i_1 * this.floorMatrix!!.elements[2]
        val f_9 = this.floorMatrix!!.elements[6] * i_5 + this.floorMatrix!!.elements[14] + this.floorMatrix!!.elements[2] * i_4 + this.floorMatrix!!.elements[10] * i_6
        val f_10 = this.floorMatrix!!.elements[15] + i_1 * this.floorMatrix!!.elements[3] + i_2 * this.floorMatrix!!.elements[7] + i_3 * this.floorMatrix!!.elements[11]
        val f_11 = i_5 * this.floorMatrix!!.elements[7] + this.floorMatrix!!.elements[15] + this.floorMatrix!!.elements[3] * i_4 + i_6 * this.floorMatrix!!.elements[11]
        if (f_8 < -f_10 && f_9 < -f_11) {
            flags = flags or 0x10
        } else if (f_8 > f_10 && f_9 > f_11) {
            flags = flags or 0x20
        }

        val f_12 = this.floorMatrix!!.elements[4] * i_2 + this.floorMatrix!!.elements[12] + i_1 * this.floorMatrix!!.elements[0] + i_3 * this.floorMatrix!!.elements[8]
        val f_13 = i_6 * this.floorMatrix!!.elements[8] + i_4 * this.floorMatrix!!.elements[0] + this.floorMatrix!!.elements[12] + this.floorMatrix!!.elements[4] * i_5
        if (f_12 < -f_10 && f_13 < -f_11) {
            flags = flags or 0x1
        }

        if (f_12 > f_10 && f_13 > f_11) {
            flags = flags or 0x2
        }

        val f_14 = this.floorMatrix!!.elements[5] * i_2 + i_1 * this.floorMatrix!!.elements[1] + this.floorMatrix!!.elements[13] + this.floorMatrix!!.elements[9] * i_3
        val f_15 = this.floorMatrix!!.elements[13] + this.floorMatrix!!.elements[1] * i_4 + this.floorMatrix!!.elements[5] * i_5 + i_6 * this.floorMatrix!!.elements[9]
        if (f_14 < -f_10 && f_15 < -f_11) {
            flags = flags or 0x4
        }

        if (f_14 > f_10 && f_15 > f_11) {
            flags = flags or 0x8
        }

        return flags
    }

    override fun setNativeHeap(class282Sub11: NativeMemoryNode) {
    }

    override fun generateEmptySprite(i_1: Int, i_2: Int, bool3: Boolean, bool4: Boolean): NativeSprite {
        return if (bool3) TransparentSpriteSM(this, i_1, i_2) else OpaqueSpriteSM(this, i_1, i_2)
    }

    override fun calculateProjection(width: Float, height: Float, length: Float, matrix: FloatArray) {
        val z = length * this.floorMatrix!!.elements[11] + this.floorMatrix!!.elements[7] * height + width * this.floorMatrix!!.elements[3] + this.floorMatrix!!.elements[15]
        val x = height * this.floorMatrix!!.elements[4] + width * this.floorMatrix!!.elements[0] + this.floorMatrix!!.elements[12] + this.floorMatrix!!.elements[8] * length
        val y = height * this.floorMatrix!!.elements[5] + width * this.floorMatrix!!.elements[1] + this.floorMatrix!!.elements[13] + length * this.floorMatrix!!.elements[9]
        val floorOffset = length * this.viewMatrix44!!.elements[10] + this.viewMatrix44!!.elements[6] * height + width * this.viewMatrix44!!.elements[2] + this.viewMatrix44!!.elements[14]
        matrix[0] = this.viewportOriginX + x * this.viewportHalfWidth / z
        matrix[1] = this.viewportOriginY + y * this.viewportHalfHeight / z
        matrix[2] = floorOffset
    }

    override fun createNativeSprite(i_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean): NativeSprite {
        if (this.colourBuffer == null) {
            throw IllegalStateException("")
        } else {
            val ints_6 = IntArray(i_3 * i_4)
            var i_7 = i_2 * this.viewportWidth + i_1
            val i_8 = this.viewportWidth - i_3

            for (i_9 in 0 until i_4) {
                val i_10 = i_3 * i_9

                for (i_11 in 0 until i_3) {
                    ints_6[i_10 + i_11] = this.colourBuffer!![i_7++]
                }

                i_7 += i_8
            }

            return if (bool5) {
                TransparentSpriteSM(this, ints_6, i_3, i_4)
            } else {
                OpaqueSpriteSM(this, ints_6, i_3, i_4)
            }
        }
    }

    override fun createComponentSurface(i_1: Int, i_2: Int, ints_3: IntArray, ints_4: IntArray): ComponentSurface {
        return ComponentDimensionSM(ints_3, ints_4)
    }

    override fun resetDepthClippingMask() {
        this.depthRangeFar = 1.0f
        this.depthRangeNear = 0.0f

        for (i_3 in 0 until this.executingThreadCount) {
            val class1854 = this.renderingThreads!![i_3]
            val choppyitemfixclass5 = class1854.rasterizer
            choppyitemfixclass5!!.aFloat1683 = this.depthRangeFar
            choppyitemfixclass5.aFloat1682 = this.depthRangeNear
        }

    }

    override fun createModel(mesh: Mesh, modelFlags: Int, preferenceFlags: Int, ambient: Int, contrast: Int): Model {
        return ModelSM(this, mesh, modelFlags, ambient, contrast, preferenceFlags)
    }

    override fun mergeFunctionMasks(base: Int, mask: Int): Int {
        return base or mask
    }

    override fun createGround(sizeX: Int, sizeY: Int, heightsNext: Array<IntArray>, heightsCurrent: Array<IntArray>, unused: Int, tileFunctionMask: Int): Ground {
        return GroundSM(this, tileFunctionMask, sizeX, sizeY, heightsNext, heightsCurrent)
    }


    override fun getRenderingMatrix(): Matrix43 {
        val class1851 = getThreadResource(Thread.currentThread())
        return class1851!!.aClass2942314!!
    }

    override fun bloomEnabled(): Boolean {
        return false
    }

    override fun copyLights(i_1: Int, arr_2: Array<PointLight>?) {
    }

    fun initializeThreads(threadCount: Int) {
        this.executingThreadCount = threadCount
        this.renderingThreads = Array(this.executingThreadCount) { ThreadResourceSM(this) }
    }

    fun getThreadResource(runnable: Runnable): ThreadResourceSM? {
        for (threadIndex in 0 until this.executingThreadCount) {
            if (runnable == this.renderingThreads!![threadIndex].runnable) {
                return this.renderingThreads!![threadIndex]
            }
        }
        return null
    }

    override fun renderParticles(particles: ParticleWrapper) {
        renderParticles(this.colourBuffer != null, this.depthBuffer != null, particles)
    }


    fun renderMaterializedSprite(bool1: Boolean, bool2: Boolean, pointEntity: PointEntity, i_5: Int, i_6: Int, f_7: Float, i_8: Int) {
        var i_8 = i_8
        val materialId = pointEntity.materialId
        val i_10 = i_8
        i_8 = i_8 shl 1
        if (materialId == -1) {
            drawParticleShape(bool2, i_5, i_6, f_7, i_10, pointEntity.colorArgb, 1)
        } else {
            if (materialId != this.renderedMaterialId) {
                var textureSprite = this.animatedSpriteMaterials.get(materialId.toLong()) as NativeSprite?
                if (textureSprite == null) {
                    val pizels = getAnimatedMaterialArgb(materialId)
                    if (pizels == null) {
                        return
                    }
                    val spriteSize = if (isSmallTexture(materialId)) 64 else this.materialSize
                    textureSprite = createNativeSprite(pizels, spriteSize, spriteSize, spriteSize)
                    this.animatedSpriteMaterials.put(textureSprite, materialId.toLong())
                }
                this.renderedMaterialId = materialId
                this.renderedNativeSprite = textureSprite
            }
            ++i_8
            (this.renderedNativeSprite as SpriteSM).drawParticle(bool1, bool2, false, i_5 - i_10, i_6 - i_10, f_7, i_8, i_8, 0, pointEntity.colorArgb, 1, false)
        }

    }

    fun drawBillboard(colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean, x: Int, y: Int, z: Float, size2d: Int, size3d: Int, materialId: Int, color: Int, blendType: Int, shape: Int) {
        if (size2d != 0 && size3d != 0) {
            if (materialId != 0xffff) {
                val material = materialProvider!!.getMaterial(materialId)
                if (!material.detailsOnly) {
                    if (materialId != this.renderedMaterialId) {
                        var nativeSprite = this.animatedSpriteMaterials.get(materialId.toLong()) as NativeSprite?
                        if (nativeSprite == null) {
                            val texturePixels = getAnimatedMaterialArgb(materialId)
                            if (texturePixels == null) {
                                return
                            }
                            val size = if (isSmallTexture(materialId)) 64 else this.materialSize
                            nativeSprite = createNativeSprite(texturePixels, size, size, size)
                            this.animatedSpriteMaterials.put(nativeSprite, materialId.toLong())
                        }
                        this.renderedMaterialId = materialId
                        this.renderedNativeSprite = nativeSprite
                    }
                    (this.renderedNativeSprite as SpriteSM).drawParticle(colorWrite, depthWrite, transparent, x - size2d, y - size3d, z, size2d shl 1, size3d shl 1, blendType, color, shape, material.effectCombiner != 2)
                    return
                }
            }
            drawParticleShape(depthWrite, x, y, z, size2d, color, shape)
        }

    }

    fun drawParticleShape(writeDepth: Boolean, x: Int, y: Int, z: Float, deltaY: Int, color: Int, shape: Int) {
        var y = y
        var deltaY = deltaY
        var color = color
        if (this.colourBuffer != null) {
            if (deltaY < 0) {
                deltaY = -deltaY
            }

            var highestY = y - deltaY
            if (highestY < this.clipTop) {
                highestY = this.clipTop
            }

            var lowestY = y + deltaY + 1
            if (lowestY > this.clipBottom) {
                lowestY = this.clipBottom
            }

            var i_10 = highestY
            val i_11 = deltaY * deltaY
            var deltaX = 0
            var i_13 = y - highestY
            var i_14 = i_13 * i_13
            var i_15 = i_14 - i_13
            if (y > lowestY) {
                y = lowestY
            }

            val alpha = color ushr 24
            var i_17: Int
            var i_18: Int
            var i_19: Int
            var i_20: Int
            if (shape != 0 && (shape != 1 || alpha != 255)) {
                var i_21: Int
                var i_22: Int
                if (shape == 1) {
                    color = ((color and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha shl 24) + (alpha * (color and 0xff00) shr 8 and 0xff00)

                    i_17 = 256 - alpha
                    while (i_10 < y) {
                        while (i_15 <= i_11 || i_14 <= i_11) {
                            i_14 += deltaX + deltaX
                            i_15 += deltaX++ + deltaX
                        }

                        i_18 = x - deltaX + 1
                        if (i_18 < this.clipLeft) {
                            i_18 = this.clipLeft
                        }

                        i_19 = deltaX + x
                        if (i_19 > this.clipRight) {
                            i_19 = this.clipRight
                        }

                        i_20 = i_18 + i_10 * this.viewportWidth

                        i_21 = i_18
                        while (i_21 < i_19) {
                            if (!writeDepth || z < this.depthBuffer!![i_20]) {
                                i_22 = this.colourBuffer!![i_20]
                                i_22 = ((i_22 and 0xff00ff) * i_17 shr 8 and 0xff00ff) + (i_17 * (i_22 and 0xff00) shr 8 and 0xff00)
                                this.colourBuffer!![i_20] = i_22 + color
                            }

                            ++i_20
                            i_21++
                        }

                        ++i_10
                        i_14 -= i_13-- + i_13
                        i_15 -= i_13 + i_13
                    }

                    deltaX = deltaY
                    i_13 = -i_13
                    i_15 = i_11 + i_13 * i_13
                    i_14 = i_15 - deltaY

                    i_15 -= i_13
                    while (i_10 < lowestY) {
                        while (i_15 > i_11 && i_14 > i_11) {
                            i_15 -= deltaX-- + deltaX
                            i_14 -= deltaX + deltaX
                        }

                        i_18 = x - deltaX
                        if (i_18 < this.clipLeft) {
                            i_18 = this.clipLeft
                        }

                        i_19 = deltaX + x
                        if (i_19 > this.clipRight - 1) {
                            i_19 = this.clipRight - 1
                        }

                        i_20 = i_18 + i_10 * this.viewportWidth

                        i_21 = i_18
                        while (i_21 <= i_19) {
                            if (!writeDepth || z < this.depthBuffer!![i_20]) {
                                i_22 = this.colourBuffer!![i_20]
                                i_22 = ((i_22 and 0xff00ff) * i_17 shr 8 and 0xff00ff) + (i_17 * (i_22 and 0xff00) shr 8 and 0xff00)
                                this.colourBuffer!![i_20] = i_22 + color
                            }

                            ++i_20
                            i_21++
                        }

                        ++i_10
                        i_15 += i_13 + i_13
                        i_14 += i_13++ + i_13
                    }
                } else {
                    if (shape != 2) {
                        throw IllegalArgumentException()
                    }

                    var i_23: Int
                    while (i_10 < y) {
                        while (i_15 <= i_11 || i_14 <= i_11) {
                            i_14 += deltaX + deltaX
                            i_15 += deltaX++ + deltaX
                        }

                        i_17 = x - deltaX + 1
                        if (i_17 < this.clipLeft) {
                            i_17 = this.clipLeft
                        }

                        i_18 = deltaX + x
                        if (i_18 > this.clipRight) {
                            i_18 = this.clipRight
                        }

                        i_19 = i_17 + i_10 * this.viewportWidth

                        i_20 = i_17
                        while (i_20 < i_18) {
                            if (!writeDepth || z < this.depthBuffer!![i_19]) {
                                i_21 = this.colourBuffer!![i_19]
                                i_22 = i_21 + color
                                i_23 = (i_21 and 0xff00ff) + (color and 0xff00ff)
                                i_21 = (i_23 and 0x1000100) + (i_22 - i_23 and 0x10000)
                                this.colourBuffer!![i_19] = i_22 - i_21 or (i_21 - (i_21 ushr 8))
                            }

                            ++i_19
                            i_20++
                        }

                        ++i_10
                        i_14 -= i_13-- + i_13
                        i_15 -= i_13 + i_13
                    }

                    deltaX = deltaY
                    i_13 = -i_13
                    i_15 = i_11 + i_13 * i_13
                    i_14 = i_15 - deltaY

                    i_15 -= i_13
                    while (i_10 < lowestY) {
                        while (i_15 > i_11 && i_14 > i_11) {
                            i_15 -= deltaX-- + deltaX
                            i_14 -= deltaX + deltaX
                        }

                        i_17 = x - deltaX
                        if (i_17 < this.clipLeft) {
                            i_17 = this.clipLeft
                        }

                        i_18 = deltaX + x
                        if (i_18 > this.clipRight - 1) {
                            i_18 = this.clipRight - 1
                        }

                        i_19 = i_17 + i_10 * this.viewportWidth

                        i_20 = i_17
                        while (i_20 <= i_18) {
                            if (!writeDepth || z < this.depthBuffer!![i_19]) {
                                i_21 = this.colourBuffer!![i_19]
                                i_22 = i_21 + color
                                i_23 = (i_21 and 0xff00ff) + (color and 0xff00ff)
                                i_21 = (i_23 and 0x1000100) + (i_22 - i_23 and 0x10000)
                                this.colourBuffer!![i_19] = i_22 - i_21 or (i_21 - (i_21 ushr 8))
                            }

                            ++i_19
                            i_20++
                        }

                        ++i_10
                        i_15 += i_13 + i_13
                        i_14 += i_13++ + i_13
                    }
                }
            } else {
                while (i_10 < y) {
                    while (i_15 <= i_11 || i_14 <= i_11) {
                        i_14 += deltaX + deltaX
                        i_15 += deltaX++ + deltaX
                    }

                    i_17 = x - deltaX + 1
                    if (i_17 < this.clipLeft) {
                        i_17 = this.clipLeft
                    }

                    i_18 = deltaX + x
                    if (i_18 > this.clipRight) {
                        i_18 = this.clipRight
                    }

                    i_19 = i_17 + i_10 * this.viewportWidth

                    i_20 = i_17
                    while (i_20 < i_18) {
                        if (!writeDepth || z < this.depthBuffer!![i_19]) {
                            this.colourBuffer!![i_19] = color
                        }

                        ++i_19
                        i_20++
                    }

                    ++i_10
                    i_14 -= i_13-- + i_13
                    i_15 -= i_13 + i_13
                }

                deltaX = deltaY
                i_13 = i_10 - y
                i_15 = i_11 + i_13 * i_13
                i_14 = i_15 - deltaY

                i_15 -= i_13
                while (i_10 < lowestY) {
                    while (i_15 > i_11 && i_14 > i_11) {
                        i_15 -= deltaX-- + deltaX
                        i_14 -= deltaX + deltaX
                    }

                    i_17 = x - deltaX
                    if (i_17 < this.clipLeft) {
                        i_17 = this.clipLeft
                    }

                    i_18 = deltaX + x
                    if (i_18 > this.clipRight - 1) {
                        i_18 = this.clipRight - 1
                    }

                    i_19 = i_17 + i_10 * this.viewportWidth

                    i_20 = i_17
                    while (i_20 <= i_18) {
                        if (!writeDepth || z < this.depthBuffer!![i_19]) {
                            this.colourBuffer!![i_19] = color
                        }

                        ++i_19
                        i_20++
                    }

                    ++i_10
                    i_15 += i_13 + i_13
                    i_14 += i_13++ + i_13
                }

            }
        }

    }

    override fun setSunPosition(sunPosition: Matrix43) {
        this.currentSunPosition = sunPosition
        updateFrustumPlanes()
    }

    override fun copySunPosition(): Matrix43 {
        return Matrix43(this.currentSunPosition!!)
    }

    override fun copyModelMatrix(): Matrix44 {
        return Matrix44(this.modelMatrix!!)
    }

    override fun applyLighting(i_1: Int, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float) {
        this.lowestContrast = (65535.0f * f_2).toInt()
        this.highestContrast = (65535.0f * f_3).toInt()
        val f_7 = sqrt((f_6 * f_6 + f_5 * f_5 + f_4 * f_4).toDouble()).toFloat()
        this.lightX = (f_4 * 65535.0f / f_7).toInt()
        this.lightY = (65535.0f * f_5 / f_7).toInt()
        this.lightZ = (f_6 * 65535.0f / f_7).toInt()
    }

    override fun generateFlags() {
    }

    override fun calculateFog(baseColor: Int, count: Int, dummy: Int) {
        for (index in this.renderingThreads!!.indices) {
            val resource = this.renderingThreads!![index]
            resource.fogColor = baseColor and 0xffffff
            var r = resource.fogColor ushr 16 and 0xff
            if (r < 2) {
                r = 2
            }
            var g = resource.fogColor shr 8 and 0xff
            if (g < 2) {
                g = 2
            }
            var b = resource.fogColor and 0xff
            if (b < 2) {
                b = 2
            }
            resource.fogColor = r shl 16 or (g shl 8) or b
            resource.aBool2304 = count >= 0
        }
    }

    override fun resetTextureState() {
        this.forceSmallTexture = false
        this.animatedModelMaterials!!.reset()
    }

    fun updateFrustumPlanes() {
        this.viewMatrix44!!.fromMatrix(this.currentSunPosition!!)
        this.floorMatrix!!.copyFrom(this.viewMatrix44!!)
        this.floorMatrix!!.multiplyBy(this.modelMatrix!!)
        this.floorMatrix!!.extractNearPlane(this.frustumPlanes[0])
        this.floorMatrix!!.extractFarPlane(this.frustumPlanes[1])
        this.floorMatrix!!.extractLeftPlane(this.frustumPlanes[2])
        this.floorMatrix!!.extractRightPlane(this.frustumPlanes[3])
        this.floorMatrix!!.extractBottomPlane(this.frustumPlanes[4])
        this.floorMatrix!!.extractTopPlane(this.frustumPlanes[5])
        val f_1 = this.modelMatrix!!.getDepthPerspectiveCD()
        val f_2 = this.modelMatrix!!.elements[10] * (f_1 - 255.0f) + this.modelMatrix!!.elements[14]
        val f_3 = this.modelMatrix!!.elements[10] * f_1 + this.modelMatrix!!.elements[14]
        val f_4 = f_3 - f_2

        for (i_5 in 0 until this.executingThreadCount) {
            val class1856 = this.renderingThreads!![i_5]
            class1856.aFloat2305 = f_2
            class1856.aFloat2303 = f_4
        }

    }

    override fun createCubeTexture(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int): AbstractCubeTextureGenerator? {
        return null
    }

    override fun resetBloom() {
    }

    override fun beginBloomCapture(i_3: Int, i_4: Int) {
    }


    override fun benchmark() {
        val hasColor = this.colourBuffer != null
        val hasDepth = this.depthBuffer != null
        if (hasColor || hasDepth) {
            val dependencies = getThreadResource(Thread.currentThread())
            val rasterizer = dependencies!!.rasterizer
            rasterizer!!.smoothEdges = false
            rasterizer.clipEdges = rasterizer.clipWidth < 5 || rasterizer.clipWidth < 75
            rasterizer.blendingAlpha = 0
            rasterizer.blending = false
            rasterizer.drawTriangle(hasColor, hasDepth, false, 10f, 50f, 90f, 5f, 75f, 15f, 100.0f, 100.0f, 100.0f, -65536, -65536, -65536)
            rasterizer.smoothEdges = true
        }
    }

    override fun initialize() {
        ModelSM.maxModelVertices = 10000
        ModelSM.maxModelTriangles = 10000
        if (this.executingThreadCount > 1) {
            throw IllegalStateException()
        } else {
            initializeThreads(this.executingThreadCount)
            mainThreadInitialize()
        }
    }

    override fun getRenderBuffer(width: Int, height: Int): RenderBuffer {
        return SafeModeRenderBuffer(width, height)
    }

    override fun computeWorldToScreen(f_1: Float, f_2: Float, f_3: Float, floats_4: FloatArray) {
        val f_5 = this.floorMatrix!!.elements[10] * f_3 + f_1 * this.floorMatrix!!.elements[2] + this.floorMatrix!!.elements[14] + f_2 * this.floorMatrix!!.elements[6]
        val f_6 = this.floorMatrix!!.elements[3] * f_1 + this.floorMatrix!!.elements[15] + this.floorMatrix!!.elements[7] * f_2 + f_3 * this.floorMatrix!!.elements[11]
        if (f_5 >= -f_6 && f_5 <= f_6) {
            val f_7 = f_2 * this.floorMatrix!!.elements[4] + this.floorMatrix!!.elements[12] + f_1 * this.floorMatrix!!.elements[0] + this.floorMatrix!!.elements[8] * f_3
            if (f_7 >= -f_6 && f_7 <= f_6) {
                val f_8 = f_3 * this.floorMatrix!!.elements[9] + this.floorMatrix!!.elements[13] + this.floorMatrix!!.elements[1] * f_1 + this.floorMatrix!!.elements[5] * f_2
                if (f_8 >= -f_6 && f_8 <= f_6) {
                    val f_9 = this.viewMatrix44!!.elements[14] + this.viewMatrix44!!.elements[2] * f_1 + this.viewMatrix44!!.elements[6] * f_2 + this.viewMatrix44!!.elements[10] * f_3
                    floats_4[0] = this.viewportOriginX + f_7 * this.viewportHalfWidth / f_6
                    floats_4[1] = f_8 * this.viewportHalfHeight / f_6 + this.viewportOriginY
                    floats_4[2] = f_9
                } else {
                    floats_4[2] = Float.NaN
                    floats_4[1] = Float.NaN
                    floats_4[0] = Float.NaN
                }
            } else {
                floats_4[2] = Float.NaN
                floats_4[1] = Float.NaN
                floats_4[0] = Float.NaN
            }
        } else {
            floats_4[2] = Float.NaN
            floats_4[1] = Float.NaN
            floats_4[0] = Float.NaN
        }

    }

    override fun clearRectangle(class4552: ComponentSurface, i_3: Int, i_4: Int) {
        var i_41 = i_4
        if (this.colourBuffer != null) {
            val packetsdecoder5 = class4552 as ComponentDimensionSM
            val ints_6 = packetsdecoder5.anIntArray9077
            val ints_7 = packetsdecoder5.anIntArray9078
            var i_8: Int
            if (this.clipBottom < ints_6!!.size + i_41) {
                i_8 = this.clipBottom - i_41
            } else {
                i_8 = ints_6.size
            }

            var i_9: Int
            if (this.clipTop > i_41) {
                i_9 = this.clipTop - i_41
                i_41 = this.clipTop
            } else {
                i_9 = 0
            }

            if (i_8 - i_9 > 0) {
                var i_10 = i_41 * this.viewportWidth

                for (i_11 in i_9 until i_8) {
                    var i_12 = i_3 + ints_6[i_11]
                    var i_13 = ints_7!![i_11]
                    if (this.clipLeft > i_12) {
                        i_13 -= this.clipLeft - i_12
                        i_12 = this.clipLeft
                    }

                    if (this.clipRight < i_13 + i_12) {
                        i_13 = this.clipRight - i_12
                    }

                    i_12 += i_10

                    for (i_14 in -i_13 until 0) {
                        this.colourBuffer!![i_12++] = -16777216.toInt()
                    }

                    i_10 += this.viewportWidth
                }
            }
        }

    }

    fun isSmallTexture(materialId: Int): Boolean {
        return this.forceSmallTexture || materialProvider!!.getMaterial(materialId).isSmall
    }


    override fun setSunCubeTexture(class1521: AbstractCubeTextureGenerator?) {
    }


    override fun getImageProducer(): DirectDisplay {
        return ImageProducerSM1(this)
    }


    override fun render() {
        delegatingDisplay!!.draw(0, 0)
        if (materialProvider != null) {
            materialProvider!!.cycle()
        }
    }


    override fun addWater(waterTile: WaterTile) {
        for (threadIndex in this.renderingThreads!!.indices) {
            this.renderingThreads!![threadIndex].lastWaterColor = this.renderingThreads!![threadIndex].fogColor
            this.renderingThreads!![threadIndex].waterAverageHeight = -1
            this.renderingThreads!![threadIndex].fogColor = waterTile.color
            this.renderingThreads!![threadIndex].waterScale = waterTile.scale
            this.renderingThreads!![threadIndex].hasWaterUpdate = true
        }
    }


    override fun setModelMatrix(modelMatrix: Matrix44) {
        this.modelMatrix!!.copyFrom(modelMatrix)
        updateFrustumPlanes()
    }

    override fun generateSprite(spriteType: SpriteType?, paletteSprite: Boolean): NativeSprite {
        val spritePalette = spriteType!!.pallete
        val spritePixels = spriteType.pixels
        val spriteWidth = spriteType.width
        val spriteHeight = spriteType.height
        val safeModeSprite: SpriteSM
        if (paletteSprite && spriteType.alpha == null) {
            val palette = IntArray(spritePalette!!.size)
            val pixels = ByteArray(spriteWidth * spriteHeight)
            for (h in 0 until spriteHeight) {
                val currentPos = h * spriteWidth
                if (spriteWidth >= 0)
                    System.arraycopy(spritePixels, 0 + currentPos, pixels, 0 + currentPos, spriteWidth)
            }
            System.arraycopy(spritePalette, 0, palette, 0, spritePalette.size)
            safeModeSprite = PaletteSpriteSM(this, pixels, palette, spriteWidth, spriteHeight)
        } else {
            val spriteTransparentPixels = IntArray(spriteHeight * spriteWidth)
            val spriteAlpha = spriteType.alpha
            if (spriteAlpha != null) {
                for (h in 0 until spriteHeight) {
                    val currentPos = spriteWidth * h
                    for (w in 0 until spriteWidth) {
                        spriteTransparentPixels[w + currentPos] = spritePalette!![spritePixels!![w + currentPos].toInt() and 0xff] or (spriteAlpha[w + currentPos].toInt() shl 24)
                    }
                }
                safeModeSprite = TransparentSpriteSM(this, spriteTransparentPixels, spriteWidth, spriteHeight)
            } else {
                for (h in 0 until spriteHeight) {
                    val currentPos = h * spriteWidth
                    for (w in 0 until spriteWidth) {
                        val i_13 = spritePalette!![spritePixels!![w + currentPos].toInt() and 0xff]
                        spriteTransparentPixels[currentPos + w] = if (i_13 != 0) -16777216.toInt() or i_13 else 0
                    }
                }
                safeModeSprite = OpaqueSpriteSM(this, spriteTransparentPixels, spriteWidth, spriteHeight)
            }
        }
        safeModeSprite.setSize(spriteType.minX, spriteType.minY, spriteType.borderX, spriteType.borderY)
        return safeModeSprite
    }


    override fun drawThickLine(startX: Int, startY: Int, x: Int, y: Int, color: Int, thickness: Int, blendType: Int) {
        if (this.colourBuffer != null) {
            val dependencies = getThreadResource(Thread.currentThread())
            val rasterizer = dependencies!!.rasterizer
            val width = x - startX
            val height = y - startY
            val i_12 = if (width >= 0) width else -width
            val i_13 = if (height >= 0) height else -height
            var dimension = i_12
            if (i_12 < i_13) {
                dimension = i_13
            }
            if (dimension != 0) {
                val lineX = (width shl 16) / dimension
                val lineY = (height shl 16) / dimension
                val widthAdj = width + (lineX shr 16)
                val heightAdj = height + (lineY shr 16)
                val lineXFinal: Int
                val lineYFinal: Int
                if (lineY <= lineX) {
                    lineXFinal = -lineX
                    lineYFinal = lineY
                } else {
                    lineXFinal = lineX
                    lineYFinal = -lineY
                }
                val lineStartY = lineYFinal * thickness shr 17
                val lineEndY = lineYFinal * thickness + 1 shr 17
                val lineStartX = lineXFinal * thickness shr 17
                val lineEndX = lineXFinal * thickness + 1 shr 17
                val startXAdj = startX - rasterizer!!.currentColumn
                val startYAdj = startY - rasterizer.currentRow
                val xA = lineStartY + startXAdj
                val xB = startXAdj - lineEndY
                val xC = widthAdj + startXAdj - lineEndY
                val xC2 = widthAdj + lineStartY + startXAdj
                val yA = lineStartX + startYAdj
                val yB = startYAdj - lineEndX
                val yC = heightAdj + startYAdj - lineEndX
                val yC2 = heightAdj + lineStartX + startYAdj
                if (blendType == 0) {
                    rasterizer.blendingAlpha = 0
                } else {
                    if (blendType != 1) {
                        throw IllegalArgumentException()
                    }
                    rasterizer.blendingAlpha = 255 - (color ushr 24)
                }
                setDepthBuffering(false)
                rasterizer.clipEdges = xA < 0 || xA > rasterizer.clipWidth || xB < 0 || xB > rasterizer.clipWidth || xC < 0 || xC > rasterizer.clipWidth
                rasterizer.drawFlatTriangle(true, false, false, yA.toFloat(), yB.toFloat(), yC.toFloat(), xA.toFloat(), xB.toFloat(), xC.toFloat(), 100.0f, 100.0f, 100.0f, color)
                rasterizer.clipEdges = xA < 0 || xA > rasterizer.clipWidth || xC < 0 || xC > rasterizer.clipWidth || xC2 < 0 || xC2 > rasterizer.clipWidth
                rasterizer.drawFlatTriangle(true, false, false, yA.toFloat(), yC.toFloat(), yC2.toFloat(), xA.toFloat(), xC.toFloat(), xC2.toFloat(), 100.0f, 100.0f, 100.0f, color)
                setDepthBuffering(true)
            }
        }

    }


    override fun debugModeChanged() {
    }

    override fun getNativeMemory(i_1: Int): NativeMemoryNode? {
        return null
    }


    override fun getMaxLights(): Int {
        return 0
    }

    fun drawDashedVerticalLine(x: Int, y: Int, i_3: Int, color: Int, i_6: Int, i_7: Int, i_8: Int) {
        var color = color
        if (this.colourBuffer != null && x >= this.clipLeft && x < this.clipRight) {
            val currentPos = y * this.viewportWidth + x
            val alpha = color ushr 24
            val i_11 = i_7 + i_6
            var i_12 = i_8 % i_11
            var i_13: Int
            if (alpha == 255) {
                i_13 = 0
                while (i_13 < i_3) {
                    if (i_13 + y >= this.clipTop && i_13 + y < this.clipBottom && i_12 < i_6) {
                        this.colourBuffer!![currentPos + i_13 * this.viewportWidth] = color
                    }

                    ++i_13
                    ++i_12
                    i_12 %= i_11
                }
            } else {
                var i_14: Int
                var i_15: Int
                var i_16: Int
                color = (alpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (alpha * (color and 0xff00) shr 8 and 0xff00) + (alpha shl 24)
                i_13 = 256 - alpha
                i_14 = 0
                while (i_14 < i_3) {
                    if (i_14 + y >= this.clipTop && i_14 + y < this.clipBottom && i_12 < i_6) {
                        i_15 = currentPos + i_14 * this.viewportWidth
                        i_16 = this.colourBuffer!![i_15]
                        i_16 = (i_13 * (i_16 and 0xff00) shr 8 and 0xff00) + ((i_16 and 0xff00ff) * i_13 shr 8 and 0xff00ff)
                        this.colourBuffer!![i_15] = i_16 + color
                    }
                    ++i_14
                    ++i_12
                    i_12 %= i_11
                }
            }
        }
    }


    override fun applyClearFlags(flags: Int, backgroundColor: Int) {
        if ((flags and 0x1) != 0) {
            drawFilledRectangle(0, 0, this.viewportWidth, this.viewportHeight, backgroundColor, 0)
        }
        if ((flags and 0x2) != 0) {
            clearDepth()
        }
    }


    override fun drawTransparentVerticalLine(x: Int, y: Int, height: Int, color: Int, alpha: Int) {
        var y = y
        var height = height
        var color = color
        if (this.colourBuffer != null && x >= this.clipLeft && x < this.clipRight) {
            if (y < this.clipTop) {
                height -= this.clipTop - y
                y = this.clipTop
            }

            if (height + y > this.clipBottom) {
                height = this.clipBottom - y
            }

            val i_6 = y * this.viewportWidth + x
            val colorAlpha = color ushr 24
            if (alpha == 0 || alpha == 1 && colorAlpha == 255) {
                for (h in 0 until height) {
                    this.colourBuffer!![i_6 + h * this.viewportWidth] = color
                }
            } else {
                if (alpha == 1) {
                    color = (colorAlpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha shl 24) + (colorAlpha * (color and 0xff00) shr 8 and 0xff00)
                    val i_8 = 256 - colorAlpha
                    for (i_9 in 0 until height) {
                        val i_10 = i_6 + i_9 * this.viewportWidth
                        var i_11 = this.colourBuffer!![i_10]
                        i_11 = ((i_11 and 0xff00ff) * i_8 shr 8 and 0xff00ff) + (i_8 * (i_11 and 0xff00) shr 8 and 0xff00)
                        this.colourBuffer!![i_10] = i_11 + color
                    }
                } else {
                    if (alpha != 2) {
                        throw IllegalArgumentException()
                    }
                    for (i_8 in 0 until height) {
                        val colorIndex = i_6 + i_8 * this.viewportWidth
                        var originalColor = this.colourBuffer!![colorIndex]
                        val colorSum = originalColor + color
                        val blendColor = (originalColor and 0xff00ff) + (color and 0xff00ff)
                        originalColor = (blendColor and 0x1000100) + (colorSum - blendColor and 0x10000)
                        this.colourBuffer!![colorIndex] = colorSum - originalColor or (originalColor - (originalColor ushr 8))
                    }
                }
            }
        }

    }

    fun hasRepeat(materialId: Int): Boolean {
        return materialProvider!!.getMaterial(materialId).repeatS || materialProvider!!.getMaterial(materialId).repeatT
    }

    override fun drawRectangleOutline(x: Int, y: Int, width: Int, height: Int, color: Int, alpha: Int) {
        drawTransparentHorizontalLine(x, y, width, color, alpha)
        drawTransparentHorizontalLine(x, y + height - 1, width, color, alpha)
        drawTransparentVerticalLine(x, y + 1, height - 2, color, alpha)
        drawTransparentVerticalLine(width + x - 1, y + 1, height - 2, color, alpha)
    }

    override fun compositeBloomEffects() {
    }

    override fun updateSun(multiplier: Float) {
        this.lightIntensity = (multiplier * 65535.0f).toInt()
    }


    override fun setClipping(left: Int, top: Int, right: Int, bottom: Int) {
        var left = left
        var top = top
        var right = right
        var bottom = bottom
        if (left < 0) {
            left = 0
        }
        if (top < 0) {
            top = 0
        }
        if (right > this.viewportWidth) {
            right = this.viewportWidth
        }
        if (bottom > this.viewportHeight) {
            bottom = this.viewportHeight
        }
        this.clipLeft = left
        this.clipRight = right
        this.clipTop = top
        this.clipBottom = bottom
        viewportUpdate()
    }

    override fun drawQuad(class1521: AbstractCubeTextureGenerator?, class1522: AbstractCubeTextureGenerator?, f_3: Float, class1524: AbstractCubeTextureGenerator?): AbstractCubeTextureGenerator? {
        return null
    }


    fun hasBeenShutdown(): Boolean {
        return this.shutdown
    }


    override fun drawLine(x: Int, y: Int, width: Int, length: Int, color: Int, i_7: Int, i_8: Int, i_9: Int) {
        var x = x
        var y = y
        var width = width
        var length = length
        var color = color
        var i_7 = i_7
        var i_8 = i_8
        var i_9 = i_9
        if (this.colourBuffer != null) {
            width -= x
            length -= y
            var i_10: Int
            if (length == 0) {
                if (width >= 0) {
                    drawDashedHorizontalLine(x, y, width + 1, color, i_7, i_8, i_9)
                } else {
                    i_10 = i_8 + i_7
                    i_9 %= i_10
                    i_9 = i_10 + i_7 - i_9 - (-width + 1) % i_10
                    i_9 %= i_10
                    if (i_9 < 0) {
                        i_9 += i_10
                    }

                    drawDashedHorizontalLine(width + x, y, -width + 1, color, i_7, i_8, i_9)
                }
            } else if (width == 0) {
                if (length >= 0) {
                    drawDashedVerticalLine(x, y, length + 1, color, i_7, i_8, i_9)
                } else {
                    i_10 = i_8 + i_7
                    i_9 %= i_10
                    i_9 = i_10 + i_7 - i_9 - (-length + 1) % i_10
                    i_9 %= i_10
                    if (i_9 < 0) {
                        i_9 += i_10
                    }

                    drawDashedVerticalLine(x, y + length, -length + 1, color, i_7, i_8, i_9)
                }
            } else {
                i_9 = i_9 shl 8
                i_7 = i_7 shl 8
                i_8 = i_8 shl 8
                i_10 = i_8 + i_7
                i_9 %= i_10
                var i_11: Int
                var i_12: Int
                if (width + length < 0) {
                    i_11 = (sqrt((width * width + length * length).toDouble()) * 256.0).toInt()
                    i_12 = i_11 % i_10
                    i_9 = i_10 + i_7 - i_9 - i_12
                    i_9 %= i_10
                    if (i_9 < 0) {
                        i_9 += i_10
                    }

                    x += width
                    width = -width
                    y += length
                    length = -length
                }

                var i_13: Int
                var i_14: Int
                var i_15: Int
                var i_16: Int
                var i_17: Int
                var i_18: Int
                if (width > length) {
                    y = y shl 16
                    y += 32768
                    length = length shl 16
                    i_11 = floor(length.toDouble() / width + 0.5).toInt()
                    width += x
                    i_12 = color ushr 24
                    i_13 = sqrt(((i_11 shr 8) * (i_11 shr 8) + 65536).toDouble()).toInt()
                    if (i_12 == 255) {
                        while (x <= width) {
                            i_14 = y shr 16
                            if (x >= this.clipLeft && x < this.clipRight && i_14 >= this.clipTop && i_14 < this.clipBottom && i_9 < i_7) {
                                this.colourBuffer!![i_14 * this.viewportWidth + x] = color
                            }

                            y += i_11
                            ++x
                            i_9 += i_13
                            i_9 %= i_10
                        }
                    } else {
                        color = (i_12 * (color and 0xff00ff) shr 8 and 0xff00ff) + (i_12 * (color and 0xff00) shr 8 and 0xff00) + (i_12 shl 24)

                        i_14 = 256 - i_12
                        while (x <= width) {
                            i_15 = y shr 16
                            if (x >= this.clipLeft && x < this.clipRight && i_15 >= this.clipTop && i_15 < this.clipBottom && i_9 < i_7) {
                                i_16 = i_15 * this.viewportWidth + x
                                i_17 = this.colourBuffer!![i_16]
                                i_17 = ((i_17 and 0xff00ff) * i_14 shr 8 and 0xff00ff) + (i_14 * (i_17 and 0xff00) shr 8 and 0xff00)
                                this.colourBuffer!![i_16] = color + i_17
                            }

                            y += i_11
                            ++x
                            i_9 += i_13
                            i_9 %= i_10
                        }
                    }
                } else {
                    x = x shl 16
                    x += 32768
                    width = width shl 16
                    i_11 = floor(width.toDouble() / length + 0.5).toInt()
                    length += y
                    i_12 = color ushr 24
                    i_13 = sqrt(((i_11 shr 8) * (i_11 shr 8) + 65536).toDouble()).toInt()
                    if ((i_12 != 255)) {
                        color = (i_12 * (color and 0xff00ff) shr 8 and 0xff00ff) + (i_12 * (color and 0xff00) shr 8 and 0xff00) + (i_12 shl 24)

                        i_14 = 256 - i_12
                        while (y <= length) {
                            i_15 = x shr 16
                            if (y >= this.clipTop && y < this.clipBottom && i_15 >= this.clipLeft && i_15 < this.clipRight && i_9 < i_7) {
                                i_16 = y * this.viewportWidth + i_15
                                i_17 = this.colourBuffer!![i_16]
                                i_17 = (i_14 * (i_17 and 0xff00) shr 8 and 0xff00) + ((i_17 and 0xff00ff) * i_14 shr 8 and 0xff00ff)
                                this.colourBuffer!![y * this.viewportWidth + i_15] = color + i_17
                            }

                            x += i_11
                            ++y
                            i_9 += i_13
                            i_9 %= i_10
                        }
                    } else {
                        while (y <= length) {
                            i_14 = x shr 16
                            if (y >= this.clipTop && y < this.clipBottom && i_14 >= this.clipLeft && i_14 < this.clipRight && i_9 < i_7) {
                                this.colourBuffer!![y * this.viewportWidth + i_14] = color
                            }

                            x += i_11
                            ++y
                            i_9 += i_13
                            i_9 %= i_10
                        }
                    }
                }
            }
        }

    }



    override fun createPointLight(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int): PointLight? {
        return null
    }


    override fun bloomSupported(): Boolean {
        return false
    }


    override fun mismatchingFunctionMask(toFlag: Int, xor: Int): Int {
        var toFlag = toFlag
        toFlag = toFlag or 0x20800
        return toFlag and xor xor xor
    }


    override fun getOffheap(): Int {
        return 0
    }


    fun mainThreadInitialize() {
        this.renderingThreads!![0].setRunnable(Thread.currentThread())
    }

    companion object {
        @JvmStatic
        fun createToolkit(gameCanvas: Canvas?, materialProvider: MaterialProvider?, screenWidth: Int, screenHeight: Int): AbstractToolkit {
            return SafeModeToolkit(gameCanvas, materialProvider, screenWidth, screenHeight)
        }

        @JvmStatic
        fun createImageProducer(hardwarerenderer0: SafeModeToolkit, canvas1: Canvas, i_2: Int, i_3: Int): AbstractImageProducerSM {
            val class158Sub2Sub3Sub15 = BufferedImageProducerSM(hardwarerenderer0, canvas1, i_2, i_3)
            return class158Sub2Sub3Sub15
        }
    }
}
