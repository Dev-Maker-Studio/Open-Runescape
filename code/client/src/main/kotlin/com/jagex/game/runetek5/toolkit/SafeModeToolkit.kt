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
import com.jagex.game.runetek5.imageproducer.SafeModeDirectDisplay
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

    constructor(materialProvider: MaterialProvider?) : super(materialProvider) {
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

    override fun animateTextures(clientCycle: Int) {
        val cycle = clientCycle - this.currentCycle
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
                val bufferStart = this.viewportWidth * this.clipTop + this.clipLeft
                val unrolledBlocks = width shr 3
                val remainingPixels = width and 0x7
                width = bufferStart - 1
                for (invertedHeight in -height until 0) {
                    var blockCounter: Int
                    if (unrolledBlocks > 0) {
                        blockCounter = unrolledBlocks
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
                            --blockCounter
                        } while (blockCounter > 0)
                    }

                    if (remainingPixels > 0) {
                        blockCounter = remainingPixels
                        do {
                            ++width
                            this.depthBuffer!![width] = 2.14748365E9f
                            --blockCounter
                        } while (blockCounter > 0)
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

    override fun setDrawableArea(x: Int, y: Int, width: Int, height: Int) {
        this.drawableX = x
        this.drawableY = y
        this.drawableWidth = width
        this.drawableHeight = height
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
        val halfWidth = this.drawableWidth / 2.0f
        this.viewportHalfWidth = halfWidth
        val halfHeight = this.drawableHeight / 2.0f
        this.viewportHalfHeight = halfHeight
        this.viewportOriginX = this.drawableX + halfWidth
        this.viewportOriginY = this.drawableY + halfHeight
        for (threadIndex in 0 until this.executingThreadCount) {
            val renderingThread = this.renderingThreads!![threadIndex]
            val rasterizer = renderingThread.rasterizer
            rasterizer!!.offsetX = halfWidth
            rasterizer.offsetY = halfHeight
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

    override fun setBloomFilter(whitePoint: Float, bloom: Float, brightpass: Float) {
    }

    fun renderParticles(hasColorBuffer: Boolean, hasDepthBuffer: Boolean, particleWrapper: ParticleWrapper) {
        val threadResource = getThreadResource(Thread.currentThread())
        val mainNode = particleWrapper.particleBinaryTree.current

        var particleNode: ParticleNode? = mainNode!!.child
        while (particleNode != mainNode) {
            val pointEntry = particleNode as PointEntity
            val positionX = pointEntry.x shr 12
            val positionY = pointEntry.y shr 12
            val positionZ = pointEntry.z shr 12
            val clipZ = this.floorMatrix!!.elements[14] + this.floorMatrix!!.elements[10] * positionZ + positionX * this.floorMatrix!!.elements[2] + positionY * this.floorMatrix!!.elements[6]
            val clipW = this.floorMatrix!!.elements[15] + this.floorMatrix!!.elements[7] * positionY + this.floorMatrix!!.elements[3] * positionX + this.floorMatrix!!.elements[11] * positionZ
            if (clipZ >= -clipW) {
                val depth = this.depthRangeNear + clipZ * this.depthRangeFar / clipW
                if (clipZ <= threadResource!!.fogStartDistance) {
                    val clipX = this.floorMatrix!!.elements[0] * positionX + this.floorMatrix!!.elements[4] * positionY + positionZ * this.floorMatrix!!.elements[8] + this.floorMatrix!!.elements[12]
                    val clipY = positionZ * this.floorMatrix!!.elements[9] + this.floorMatrix!!.elements[1] * positionX + this.floorMatrix!!.elements[5] * positionY + this.floorMatrix!!.elements[13]
                    if (clipX >= -clipW && clipX <= clipW && clipY >= -clipW && clipY <= clipW) {
                        val scaledSize = pointEntry.size / 4096.0f
                        val scaledClipX = scaledSize * this.modelMatrix!!.elements[0] + clipX
                        val scaledClipW = clipW + this.modelMatrix!!.elements[3] * scaledSize
                        val screenX = this.viewportOriginX + clipX * this.viewportHalfWidth / clipW
                        val screenY = this.viewportHalfHeight * clipY / clipW + this.viewportOriginY
                        val screenXScaled = this.viewportOriginX + scaledClipX * this.viewportHalfWidth / scaledClipW
                        renderMaterializedSprite(hasColorBuffer, hasDepthBuffer, pointEntry, screenX.toInt(), screenY.toInt(), depth, (if (screenXScaled < screenX) screenX - screenXScaled else screenXScaled - screenX).toInt())
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

    override fun hasHardwareRendering(): Boolean {
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

    override fun createDelegatingDisplay(canvas: Canvas, width: Int, height: Int): DelegatingDisplay {
        return createImageProducer(this, canvas, width, height)
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

    override fun createNativeSprite(pixels: IntArray, pixelOffset: Int, stride: Int, width: Int, height: Int, hasAlpha: Boolean): NativeSprite {
        var hasTransparency = false
        var pixelIndex = pixelOffset

        for (row in 0 until height) {
            for (col in 0 until width) {
                val alphaValue = pixels[pixelIndex++] ushr 24
                if (alphaValue != 0 && alphaValue != 255) {
                    hasTransparency = true
                    return TransparentSpriteSM(this, pixels, pixelOffset, stride, width, height, hasAlpha)
                }
            }
        }

        return OpaqueSpriteSM(this, pixels, pixelOffset, stride, width, height, hasAlpha)
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
                val scanlineStride = this.viewportWidth - width
                var bufferOffset = y * this.viewportWidth + x
                val alpha = color ushr 24
                var transparency: Int
                var rowCounter: Int
                var colCounter: Int
                if (type != 0 && (type != 1 || alpha != 255)) {
                    var lastColor: Int
                    if (type == 1) {
                        color = (alpha * ((color and -16711936) ushr 8) and -16711936) + (alpha * (color and 0xff00ff) shr 8 and 0xff00ff)
                        transparency = 256 - alpha
                        rowCounter = 0
                        while (rowCounter < height) {
                            colCounter = -width
                            while (colCounter < 0) {
                                lastColor = this.colourBuffer!![bufferOffset]
                                lastColor = (((lastColor and -16711936) ushr 8) * transparency and -16711936) + ((lastColor and 0xff00ff) * transparency shr 8 and 0xff00ff)
                                this.colourBuffer!![bufferOffset++] = color + lastColor
                                colCounter++
                            }
                            bufferOffset += scanlineStride
                            rowCounter++
                        }
                    } else {
                        if (type != 2) {
                            throw IllegalArgumentException()
                        }
                        transparency = 0
                        while (transparency < height) {
                            rowCounter = -width
                            while (rowCounter < 0) {
                                colCounter = this.colourBuffer!![bufferOffset]
                                lastColor = color + colCounter
                                val channelSum = (colCounter and 0xff00ff) + (color and 0xff00ff)
                                colCounter = (channelSum and 0x1000100) + (lastColor - channelSum and 0x10000)
                                this.colourBuffer!![bufferOffset++] = lastColor - colCounter or (colCounter - (colCounter ushr 8))
                                rowCounter++
                            }
                            bufferOffset += scanlineStride
                            transparency++
                        }
                    }
                } else {
                    transparency = width shr 3
                    rowCounter = width and 0x7
                    width = bufferOffset - 1
                    colCounter = -height
                    while (colCounter < 0) {
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
                        if (rowCounter > 0) {
                            var xx = rowCounter
                            do {
                                ++width
                                this.colourBuffer!![width] = color
                                --xx
                            } while (xx > 0)
                        }
                        width += scanlineStride
                        colCounter++
                    }
                }
            }
        }
    }

    override fun renderWorldmapTile(x: Int, y: Int, width: Int, height: Int, color2: Int, color: Int, tileData: ByteArray, tileWidth: Int, blendMode: Int) {
        var x = x
        var width = width
        var height = height
        if (this.colourBuffer != null && width > 0 && height > 0) {
            var srcX = 0
            var srcY = 0
            val scaleX = (tileWidth shl 16) / width
            val scaleY = (tileData.size / tileWidth shl 16) / height
            var bufferOffset = y * this.viewportWidth + x
            var scanlineStride = this.viewportWidth - width
            if (y + height > this.clipBottom) {
                height -= y + height - this.clipBottom
            }
            var clipAdjust: Int
            if (y < this.clipTop) {
                clipAdjust = this.clipTop - y
                height -= clipAdjust
                bufferOffset += clipAdjust * this.viewportWidth
                srcY += clipAdjust * scaleY
            }
            if (width + x > this.clipRight) {
                clipAdjust = width + x - this.clipRight
                width -= clipAdjust
                scanlineStride += clipAdjust
            }
            if (x < this.clipLeft) {
                clipAdjust = this.clipLeft - x
                width -= clipAdjust
                bufferOffset += clipAdjust
                srcX += clipAdjust * scaleX
                scanlineStride += clipAdjust
            }
            clipAdjust = color2 ushr 24
            val fgAlpha = color ushr 24
            var savedSrcX: Int
            var rowIndex: Int
            var tileRowOffset: Int
            var colIndex: Int
            if (blendMode == 0 || blendMode == 1 && clipAdjust == 255 && fgAlpha == 255) {
                savedSrcX = srcX
                rowIndex = -height
                while (rowIndex < 0) {
                    tileRowOffset = tileWidth * (srcY shr 16)
                    colIndex = -width
                    while (colIndex < 0) {
                        if (tileData[tileRowOffset + (srcX shr 16)].toInt() != 0) {
                            this.colourBuffer!![bufferOffset++] = color
                        } else {
                            this.colourBuffer!![bufferOffset++] = color2
                        }
                        srcX += scaleX
                        colIndex++
                    }
                    srcY += scaleY
                    srcX = savedSrcX
                    bufferOffset += scanlineStride
                    rowIndex++
                }
            } else {
                var selectedColor: Int
                var srcAlpha: Int
                var dstAlpha: Int
                var existingPixel: Int
                if (blendMode == 1) {
                    savedSrcX = srcX
                    rowIndex = -height
                    while (rowIndex < 0) {
                        tileRowOffset = tileWidth * (srcY shr 16)
                        colIndex = -width
                        while (colIndex < 0) {
                            selectedColor = color2
                            if (tileData[tileRowOffset + (srcX shr 16)].toInt() != 0) {
                                selectedColor = color
                            }
                            srcAlpha = selectedColor ushr 24
                            dstAlpha = 255 - srcAlpha
                            existingPixel = this.colourBuffer!![bufferOffset]
                            this.colourBuffer!![bufferOffset++] = (dstAlpha * (existingPixel and 0xff00) + srcAlpha * (selectedColor and 0xff00) and 0xff0000) + ((existingPixel and 0xff00ff) * dstAlpha + (selectedColor and 0xff00ff) * srcAlpha and -16711936) shr 8
                            srcX += scaleX
                            colIndex++
                        }
                        srcY += scaleY
                        srcX = savedSrcX
                        bufferOffset += scanlineStride
                        rowIndex++
                    }
                } else {
                    if (blendMode != 2) {
                        throw IllegalArgumentException()
                    }
                    savedSrcX = srcX
                    rowIndex = -height
                    while (rowIndex < 0) {
                        tileRowOffset = tileWidth * (srcY shr 16)
                        colIndex = -width
                        while (colIndex < 0) {
                            selectedColor = color2
                            if (tileData[tileRowOffset + (srcX shr 16)].toInt() != 0) {
                                selectedColor = color
                            }
                            if (selectedColor != 0) {
                                srcAlpha = this.colourBuffer!![bufferOffset]
                                dstAlpha = selectedColor + srcAlpha
                                existingPixel = (srcAlpha and 0xff00ff) + (selectedColor and 0xff00ff)
                                srcAlpha = (existingPixel and 0x1000100) + (dstAlpha - existingPixel and 0x10000)
                                this.colourBuffer!![bufferOffset++] = dstAlpha - srcAlpha or (srcAlpha - (srcAlpha ushr 8))
                            } else {
                                ++bufferOffset
                            }
                            srcX += scaleX
                            colIndex++
                        }
                        srcY += scaleY
                        srcX = savedSrcX
                        bufferOffset += scanlineStride
                        rowIndex++
                    }
                }
            }
        }
    }

    override fun drawCircle(centerX: Int, centerY: Int, radius: Int, colorArgb: Int) {
        var radius = radius
        var colorArgb = colorArgb
        var centerYLocal = centerY
        if (this.colourBuffer != null) {
            if (radius < 0) {
                radius = -radius
            }

            var topY = centerYLocal - radius
            if (topY < this.clipTop) {
                topY = this.clipTop
            }

            var bottomY = radius + centerYLocal + 1
            if (bottomY > this.clipBottom) {
                bottomY = this.clipBottom
            }

            var scanY = topY
            val radiusSq = radius * radius
            var halfWidth = 0
            var dy = centerYLocal - topY
            var dySq = dy * dy
            var dySqMinusDy = dySq - dy
            if (centerYLocal > bottomY) {
                centerYLocal = bottomY
            }

            val alpha = colorArgb ushr 24
            var transparency: Int
            var leftX: Int
            var rightX: Int
            var bufferOffset: Int
            if ((alpha != 255)) {
                var pixelX: Int
                var existingColor: Int
                colorArgb = (alpha * (colorArgb and 0xff00ff) shr 8 and 0xff00ff) + (alpha * (colorArgb and 0xff00) shr 8 and 0xff00) + (alpha shl 24)

                transparency = 256 - alpha
                while (scanY < centerYLocal) {
                    while (dySqMinusDy <= radiusSq || dySq <= radiusSq) {
                        dySq += halfWidth + halfWidth
                        dySqMinusDy += halfWidth++ + halfWidth
                    }

                    leftX = centerX - halfWidth + 1
                    if (leftX < this.clipLeft) {
                        leftX = this.clipLeft
                    }

                    rightX = halfWidth + centerX
                    if (rightX > this.clipRight) {
                        rightX = this.clipRight
                    }

                    bufferOffset = leftX + scanY * this.viewportWidth

                    pixelX = leftX
                    while (pixelX < rightX) {
                        existingColor = this.colourBuffer!![bufferOffset]
                        existingColor = ((existingColor and 0xff00ff) * transparency shr 8 and 0xff00ff) + (transparency * (existingColor and 0xff00) shr 8 and 0xff00)
                        this.colourBuffer!![bufferOffset++] = existingColor + colorArgb
                        pixelX++
                    }

                    ++scanY
                    dySq -= dy-- + dy
                    dySqMinusDy -= dy + dy
                }

                halfWidth = radius
                dy = -dy
                dySqMinusDy = radiusSq + dy * dy
                dySq = dySqMinusDy - radius

                dySqMinusDy -= dy
                while (scanY < bottomY) {
                    while (dySqMinusDy > radiusSq && dySq > radiusSq) {
                        dySqMinusDy -= halfWidth-- + halfWidth
                        dySq -= halfWidth + halfWidth
                    }

                    leftX = centerX - halfWidth
                    if (leftX < this.clipLeft) {
                        leftX = this.clipLeft
                    }

                    rightX = halfWidth + centerX
                    if (rightX > this.clipRight - 1) {
                        rightX = this.clipRight - 1
                    }

                    bufferOffset = leftX + scanY * this.viewportWidth

                    pixelX = leftX
                    while (pixelX <= rightX) {
                        existingColor = this.colourBuffer!![bufferOffset]
                        existingColor = (transparency * (existingColor and 0xff00) shr 8 and 0xff00) + ((existingColor and 0xff00ff) * transparency shr 8 and 0xff00ff)
                        this.colourBuffer!![bufferOffset++] = existingColor + colorArgb
                        pixelX++
                    }

                    ++scanY
                    dySqMinusDy += dy + dy
                    dySq += dy++ + dy
                }
            } else {
                while (scanY < centerYLocal) {
                    while (dySqMinusDy <= radiusSq || dySq <= radiusSq) {
                        dySq += halfWidth + halfWidth
                        dySqMinusDy += halfWidth++ + halfWidth
                    }

                    transparency = centerX - halfWidth + 1
                    if (transparency < this.clipLeft) {
                        transparency = this.clipLeft
                    }

                    leftX = halfWidth + centerX
                    if (leftX > this.clipRight) {
                        leftX = this.clipRight
                    }

                    rightX = transparency + scanY * this.viewportWidth

                    bufferOffset = transparency
                    while (bufferOffset < leftX) {
                        this.colourBuffer!![rightX++] = colorArgb
                        bufferOffset++
                    }

                    ++scanY
                    dySq -= dy-- + dy
                    dySqMinusDy -= dy + dy
                }

                halfWidth = radius
                dy = scanY - centerYLocal
                dySqMinusDy = radiusSq + dy * dy
                dySq = dySqMinusDy - radius

                dySqMinusDy -= dy
                while (scanY < bottomY) {
                    while (dySqMinusDy > radiusSq && dySq > radiusSq) {
                        dySqMinusDy -= halfWidth-- + halfWidth
                        dySq -= halfWidth + halfWidth
                    }

                    transparency = centerX - halfWidth
                    if (transparency < this.clipLeft) {
                        transparency = this.clipLeft
                    }

                    leftX = halfWidth + centerX
                    if (leftX > this.clipRight - 1) {
                        leftX = this.clipRight - 1
                    }

                    rightX = transparency + scanY * this.viewportWidth

                    bufferOffset = transparency
                    while (bufferOffset <= leftX) {
                        this.colourBuffer!![rightX++] = colorArgb
                        bufferOffset++
                    }

                    ++scanY
                    dySqMinusDy += dy + dy
                    dySq += dy++ + dy
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

            val bufferStart = y * this.viewportWidth + x
            val colorAlpha = color ushr 24
            var invAlpha: Int
            if (alpha == 0 || alpha == 1 && colorAlpha == 255) {
                for (pixelIdx in 0 until width) {
                    this.colourBuffer!![bufferStart + pixelIdx] = color
                }
            } else {
                var pixelIdx: Int
                var existingColor: Int
                if (alpha == 1) {
                    color = (colorAlpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha shl 24) + (colorAlpha * (color and 0xff00) shr 8 and 0xff00)
                    invAlpha = 256 - colorAlpha

                    for (pixelIdx in 0 until width) {
                        existingColor = this.colourBuffer!![pixelIdx + bufferStart]
                        existingColor = ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff) + (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00)
                        this.colourBuffer!![pixelIdx + bufferStart] = existingColor + color
                    }
                } else {
                    if (alpha != 2) {
                        throw IllegalArgumentException()
                    }

                    for (colIdx in 0 until width) {
                        pixelIdx = this.colourBuffer!![bufferStart + colIdx]
                        existingColor = pixelIdx + color
                        val channelSum = (pixelIdx and 0xff00ff) + (color and 0xff00ff)
                        pixelIdx = (channelSum and 0x1000100) + (existingColor - channelSum and 0x10000)
                        this.colourBuffer!![colIdx + bufferStart] = existingColor - pixelIdx or (pixelIdx - (pixelIdx ushr 8))
                    }
                }
            }
        }

    }

    fun drawDashedHorizontalLine(x: Int, y: Int, width: Int, color: Int, dashLength: Int, gapLength: Int, dashOffset: Int) {
        var color = color
        if (this.colourBuffer != null && y >= this.clipTop && y < this.clipBottom) {
            val offset = y * this.viewportWidth + x
            val alphaComponent = color ushr 24
            val dashCycle = gapLength + dashLength
            var dashPhase = dashOffset % dashCycle
            if (alphaComponent == 255) {
                var deltaX = 0
                while (deltaX < width) {
                    if (deltaX + x >= this.clipLeft && deltaX + x < this.clipRight && dashPhase < dashLength) {
                        this.colourBuffer!![deltaX + offset] = color
                    }

                    ++deltaX
                    ++dashPhase
                    dashPhase %= dashCycle
                }
            } else {
                var deltaX: Int
                var currentColor: Int
                color = (alphaComponent * (color and 0xff00) shr 8 and 0xff00) + (alphaComponent * (color and 0xff00ff) shr 8 and 0xff00ff) + (alphaComponent shl 24)
                val transparency = 256 - alphaComponent

                deltaX = 0
                while (deltaX < width) {
                    if (deltaX + x >= this.clipLeft && deltaX + x < this.clipRight && dashPhase < dashLength) {
                        currentColor = this.colourBuffer!![deltaX + offset]
                        currentColor = ((currentColor and 0xff00ff) * transparency shr 8 and 0xff00ff) + (transparency * (currentColor and 0xff00) shr 8 and 0xff00)
                        this.colourBuffer!![deltaX + offset] = currentColor + color
                    }

                    ++deltaX
                    ++dashPhase
                    dashPhase %= dashCycle
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

                var slope: Int
                var colorAlpha: Int
                var invAlpha: Int
                var pixelY: Int
                var bufferIdx: Int
                var existingPixel: Int
                var channelSum: Int
                if (width > height) {
                    y = y shl 16
                    y += 32768
                    height = height shl 16
                    slope = floor(0.5 + height.toDouble() / width).toInt()
                    width += x
                    if (x < this.clipLeft) {
                        y += slope * (this.clipLeft - x)
                        x = this.clipLeft
                    }

                    if (width >= this.clipRight) {
                        width = this.clipRight - 1
                    }

                    colorAlpha = color ushr 24
                    if (alpha != 0 && (alpha != 1 || colorAlpha != 255)) {
                        if (alpha == 1) {
                            color = (colorAlpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha * (color and 0xff00) shr 8 and 0xff00) + (colorAlpha shl 24)

                            invAlpha = 256 - colorAlpha
                            while (x <= width) {
                                pixelY = y shr 16
                                if (pixelY >= this.clipTop && pixelY < this.clipBottom) {
                                    bufferIdx = pixelY * this.viewportWidth + x
                                    existingPixel = this.colourBuffer!![bufferIdx]
                                    existingPixel = (invAlpha * (existingPixel and 0xff00) shr 8 and 0xff00) + ((existingPixel and 0xff00ff) * invAlpha shr 8 and 0xff00ff)
                                    this.colourBuffer!![bufferIdx] = color + existingPixel
                                }

                                y += slope
                                x++
                            }
                        } else {
                            if (alpha != 2) {
                                throw IllegalArgumentException()
                            }

                            while (x <= width) {
                                invAlpha = y shr 16
                                if (invAlpha >= this.clipTop && invAlpha < this.clipBottom) {
                                    pixelY = invAlpha * this.viewportWidth + x
                                    bufferIdx = this.colourBuffer!![pixelY]
                                    existingPixel = color + bufferIdx
                                    channelSum = (bufferIdx and 0xff00ff) + (color and 0xff00ff)
                                    bufferIdx = (channelSum and 0x1000100) + (existingPixel - channelSum and 0x10000)
                                    this.colourBuffer!![pixelY] = existingPixel - bufferIdx or (bufferIdx - (bufferIdx ushr 8))
                                }

                                y += slope
                                ++x
                            }
                        }
                    } else {
                        while (x <= width) {
                            invAlpha = y shr 16
                            if (invAlpha >= this.clipTop && invAlpha < this.clipBottom) {
                                this.colourBuffer!![invAlpha * this.viewportWidth + x] = color
                            }

                            y += slope
                            ++x
                        }
                    }
                } else {
                    x = x shl 16
                    x += 32768
                    width = width shl 16
                    slope = floor(width.toDouble() / height + 0.5).toInt()
                    height += y
                    if (y < this.clipTop) {
                        x += slope * (this.clipTop - y)
                        y = this.clipTop
                    }

                    if (height >= this.clipBottom) {
                        height = this.clipBottom - 1
                    }

                    colorAlpha = color ushr 24
                    if (alpha == 0 || alpha == 1 && colorAlpha == 255) {
                        while (y <= height) {
                            invAlpha = x shr 16
                            if (invAlpha >= this.clipLeft && invAlpha < this.clipRight) {
                                this.colourBuffer!![y * this.viewportWidth + invAlpha] = color
                            }

                            x += slope
                            ++y
                        }
                    } else if (alpha == 1) {
                        color = (colorAlpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha * (color and 0xff00) shr 8 and 0xff00) + (colorAlpha shl 24)

                        invAlpha = 256 - colorAlpha
                        while (y <= height) {
                            pixelY = x shr 16
                            if (pixelY >= this.clipLeft && pixelY < this.clipRight) {
                                bufferIdx = y * this.viewportWidth + pixelY
                                existingPixel = this.colourBuffer!![bufferIdx]
                                existingPixel = ((existingPixel and 0xff00ff) * invAlpha shr 8 and 0xff00ff) + (invAlpha * (existingPixel and 0xff00) shr 8 and 0xff00)
                                this.colourBuffer!![y * this.viewportWidth + pixelY] = color + existingPixel
                            }

                            x += slope
                            y++
                        }
                    } else {
                        if (alpha != 2) {
                            throw IllegalArgumentException()
                        }

                        while (y <= height) {
                            invAlpha = x shr 16
                            if (invAlpha >= this.clipLeft && invAlpha < this.clipRight) {
                                pixelY = y * this.viewportWidth + invAlpha
                                bufferIdx = this.colourBuffer!![pixelY]
                                existingPixel = color + bufferIdx
                                channelSum = (bufferIdx and 0xff00ff) + (color and 0xff00ff)
                                bufferIdx = (channelSum and 0x1000100) + (existingPixel - channelSum and 0x10000)
                                this.colourBuffer!![pixelY] = existingPixel - bufferIdx or (bufferIdx - (bufferIdx ushr 8))
                            }

                            x += slope
                            ++y
                        }
                    }
                }
            }
        }
    }

    override fun getScratchMatrix(): Matrix44 {
        val threadResource = getThreadResource(Thread.currentThread())
        return threadResource!!.scratchMatrix44!!
    }

    override fun drawSurfaceLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, surface: ComponentSurface, surfaceClipLeft: Int, surfaceClipTop: Int) {
        var curX = startX
        var curEndX = endX
        var curY = startY
        var curEndY = endY
        var colorArgb = colorArgb
        if (this.colourBuffer != null) {
            val dimensionSM = surface as ComponentDimensionSM
            val rowStartX = dimensionSM.rowStartX
            val rowWidth = dimensionSM.rowWidth
            val clipTopBound = maxOf(this.clipTop, surfaceClipTop)
            val clipBottomBound = minOf(this.clipBottom, rowStartX!!.size + surfaceClipTop)
            curEndX -= curX
            curEndY -= curY
            if (curEndX + curEndY < 0) {
                curX += curEndX
                curEndX = -curEndX
                curY += curEndY
                curEndY = -curEndY
            }

            var slope: Int
            var colorAlpha: Int
            var invAlpha: Int
            var pixelY: Int
            var surfaceRow: Int
            var surfaceLeft: Int
            var bufferIdx: Int
            var existingColor: Int
            var unused: Int
            if (curEndX > curEndY) {
                curY = curY shl 16
                curY += 32768
                curEndY = curEndY shl 16
                slope = floor(curEndY.toDouble() / curEndX + 0.5).toInt()
                curEndX += curX
                if (curX < this.clipLeft) {
                    curY += slope * (this.clipLeft - curX)
                    curX = this.clipLeft
                }

                if (curEndX >= this.clipRight) {
                    curEndX = this.clipRight - 1
                }

                colorAlpha = colorArgb ushr 24
                if (colorAlpha == 255) {
                    while (curX <= curEndX) {
                        invAlpha = curY shr 16
                        pixelY = invAlpha - surfaceClipTop
                        if (invAlpha >= clipTopBound && invAlpha < clipBottomBound) {
                            surfaceRow = surfaceClipLeft + rowStartX[pixelY]
                            if (curX >= surfaceRow && curX < surfaceRow + rowWidth!![pixelY]) {
                                this.colourBuffer!![invAlpha * this.viewportWidth + curX] = colorArgb
                            }
                        }

                        curY += slope
                        ++curX
                    }
                } else {
                    colorArgb = (colorAlpha * (colorArgb and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha * (colorArgb and 0xff00) shr 8 and 0xff00) + (colorAlpha shl 24)

                    invAlpha = 256 - colorAlpha
                    while (curX <= curEndX) {
                        pixelY = curY shr 16
                        surfaceRow = pixelY - surfaceClipTop
                        if (pixelY >= clipTopBound && pixelY < clipBottomBound) {
                            surfaceLeft = surfaceClipLeft + rowStartX[surfaceRow]
                            if (curX >= surfaceLeft && curX < surfaceLeft + rowWidth!![surfaceRow]) {
                                bufferIdx = pixelY * this.viewportWidth + curX
                                existingColor = this.colourBuffer!![bufferIdx]
                                existingColor = (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00) + ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff)
                                this.colourBuffer!![bufferIdx] = colorArgb + existingColor
                            }
                        }

                        curY += slope
                        curX++
                    }
                }
            } else {
                curX = curX shl 16
                curX += 32768
                curEndX = curEndX shl 16
                slope = floor(0.5 + curEndX.toDouble() / curEndY).toInt()
                curEndY += curY
                if (curY < clipTopBound) {
                    curX += (clipTopBound - curY) * slope
                    curY = clipTopBound
                }

                if (curEndY >= clipBottomBound) {
                    curEndY = clipBottomBound - 1
                }

                colorAlpha = colorArgb ushr 24
                if ((colorAlpha != 255)) {
                    colorArgb = (colorAlpha * (colorArgb and 0xff00) shr 8 and 0xff00) + (colorAlpha * (colorArgb and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha shl 24)

                    invAlpha = 256 - colorAlpha
                    while (curY <= curEndY) {
                        pixelY = curX shr 16
                        surfaceRow = curY - surfaceClipTop
                        surfaceLeft = surfaceClipLeft + rowStartX[surfaceRow]
                        if (pixelY >= this.clipLeft && pixelY < this.clipRight && pixelY >= surfaceLeft && pixelY < surfaceLeft + rowWidth!![surfaceRow]) {
                            bufferIdx = curY * this.viewportWidth + pixelY
                            existingColor = this.colourBuffer!![bufferIdx]
                            existingColor = (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00) + ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff)
                            this.colourBuffer!![curY * this.viewportWidth + pixelY] = colorArgb + existingColor
                        }

                        curX += slope
                        curY++
                    }
                } else {
                    while (curY <= curEndY) {
                        invAlpha = curX shr 16
                        pixelY = curY - surfaceClipTop
                        surfaceRow = surfaceClipLeft + rowStartX[pixelY]
                        if (invAlpha >= this.clipLeft && invAlpha < this.clipRight && invAlpha >= surfaceRow && invAlpha < surfaceRow + rowWidth!![pixelY]) {
                            this.colourBuffer!![curY * this.viewportWidth + invAlpha] = colorArgb
                        }

                        curX += slope
                        ++curY
                    }
                }
            }
        }

    }

    override fun drawTexturedLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, surface: ComponentSurface, surfaceClipLeft: Int, surfaceClipTop: Int, dashLength: Int, gapLength: Int, dashOffset: Int) {
        var startX = startX
        var startY = startY
        var endX = endX
        var endY = endY
        var colorArgb = colorArgb
        var dashLength = dashLength
        var gapLength = gapLength
        var dashOffset = dashOffset
        if (this.colourBuffer != null) {
            val dimensionSM = surface as ComponentDimensionSM
            val rowStartX = dimensionSM.rowStartX
            val rowWidth = dimensionSM.rowWidth
            val clipTopBound = maxOf(this.clipTop, surfaceClipTop)
            val clipBottomBound = minOf(this.clipBottom, rowStartX!!.size + surfaceClipTop)
            dashOffset = dashOffset shl 8
            dashLength = dashLength shl 8
            gapLength = gapLength shl 8
            val dashCycle = gapLength + dashLength
            dashOffset %= dashCycle
            endX -= startX
            endY -= startY
            var slope: Int
            var lineLength: Int
            if (endX + endY < 0) {
                slope = (sqrt((endX * endX + endY * endY).toDouble()) * 256.0).toInt()
                lineLength = slope % dashCycle
                dashOffset = dashLength + dashCycle - dashOffset - lineLength
                dashOffset %= dashCycle
                if (dashOffset < 0) {
                    dashOffset += dashCycle
                }

                startX += endX
                endX = -endX
                startY += endY
                endY = -endY
            }

            var stepLength: Int
            var invAlpha: Int
            var pixelY: Int
            var surfaceRow: Int
            var surfaceLeft: Int
            var bufferIdx: Int
            var existingColor: Int
            if (endX > endY) {
                startY = startY shl 16
                startY += 32768
                endY = endY shl 16
                slope = floor(endY.toDouble() / endX + 0.5).toInt()
                endX += startX
                lineLength = colorArgb ushr 24
                stepLength = sqrt(((slope shr 8) * (slope shr 8) + 65536).toDouble()).toInt()
                if (lineLength == 255) {
                    while (startX <= endX) {
                        invAlpha = startY shr 16
                        pixelY = invAlpha - surfaceClipTop
                        if (startX >= this.clipLeft && startX < this.clipRight && invAlpha >= clipTopBound && invAlpha < clipBottomBound && dashOffset < dashLength) {
                            surfaceRow = surfaceClipLeft + rowStartX[pixelY]
                            if (startX >= surfaceRow && startX < surfaceRow + rowWidth!![pixelY]) {
                                this.colourBuffer!![invAlpha * this.viewportWidth + startX] = colorArgb
                            }
                        }

                        startY += slope
                        ++startX
                        dashOffset += stepLength
                        dashOffset %= dashCycle
                    }
                } else {
                    colorArgb = (lineLength * (colorArgb and 0xff00ff) shr 8 and 0xff00ff) + (lineLength * (colorArgb and 0xff00) shr 8 and 0xff00) + (lineLength shl 24)

                    invAlpha = 256 - lineLength
                    while (startX <= endX) {
                        pixelY = startY shr 16
                        surfaceRow = pixelY - surfaceClipTop
                        if (startX >= this.clipLeft && startX < this.clipRight && pixelY >= clipTopBound && pixelY < clipBottomBound && dashOffset < dashLength) {
                            surfaceLeft = surfaceClipLeft + rowStartX[surfaceRow]
                            if (startX >= surfaceLeft && startX < surfaceLeft + rowWidth!![surfaceRow]) {
                                bufferIdx = pixelY * this.viewportWidth + startX
                                existingColor = this.colourBuffer!![bufferIdx]
                                existingColor = (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00) + ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff)
                                this.colourBuffer!![bufferIdx] = colorArgb + existingColor
                            }
                        }

                        startY += slope
                        ++startX
                        dashOffset += stepLength
                        dashOffset %= dashCycle
                    }
                }
            } else {
                startX = startX shl 16
                startX += 32768
                endX = endX shl 16
                slope = floor(0.5 + endX.toDouble() / endY).toInt()
                lineLength = sqrt(((slope shr 8) * (slope shr 8) + 65536).toDouble()).toInt()
                endY += startY
                stepLength = colorArgb ushr 24
                if (stepLength == 255) {
                    while (startY <= endY) {
                        invAlpha = startX shr 16
                        pixelY = startY - surfaceClipTop
                        if (startY >= clipTopBound && startY < clipBottomBound && invAlpha >= this.clipLeft && invAlpha < this.clipRight && dashOffset < dashLength && invAlpha >= surfaceClipLeft + rowStartX[pixelY] && invAlpha < surfaceClipLeft + rowStartX[pixelY] + rowWidth!![pixelY]) {
                            this.colourBuffer!![startY * this.viewportWidth + invAlpha] = colorArgb
                        }

                        startX += slope
                        ++startY
                        dashOffset += lineLength
                        dashOffset %= dashCycle
                    }
                } else {
                    colorArgb = (stepLength * (colorArgb and 0xff00ff) shr 8 and 0xff00ff) + (stepLength * (colorArgb and 0xff00) shr 8 and 0xff00) + (stepLength shl 24)

                    invAlpha = 256 - stepLength
                    while (startY <= endY) {
                        pixelY = startX shr 16
                        surfaceRow = startY - surfaceClipTop
                        if (startY >= clipTopBound && startY < clipBottomBound && pixelY >= this.clipLeft && pixelY < this.clipRight && dashOffset < dashLength && pixelY >= surfaceClipLeft + rowStartX[surfaceRow] && pixelY < surfaceClipLeft + rowStartX[surfaceRow] + rowWidth!![surfaceRow]) {
                            surfaceLeft = startY * this.viewportWidth + pixelY
                            bufferIdx = this.colourBuffer!![surfaceLeft]
                            bufferIdx = (invAlpha * (bufferIdx and 0xff00) shr 8 and 0xff00) + ((bufferIdx and 0xff00ff) * invAlpha shr 8 and 0xff00ff)
                            this.colourBuffer!![startY * this.viewportWidth + pixelY] = colorArgb + bufferIdx
                        }

                        startX += slope
                        ++startY
                        dashOffset += lineLength
                        dashOffset %= dashCycle
                    }
                }
            }
        }

    }

    override fun isOnScreen(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): Int {
        var flags = 0
        val clipZMin = minZ * this.floorMatrix!!.elements[10] + minY * this.floorMatrix!!.elements[6] + this.floorMatrix!!.elements[14] + minX * this.floorMatrix!!.elements[2]
        val clipZMax = this.floorMatrix!!.elements[6] * maxY + this.floorMatrix!!.elements[14] + this.floorMatrix!!.elements[2] * maxX + this.floorMatrix!!.elements[10] * maxZ
        val clipWMin = this.floorMatrix!!.elements[15] + minX * this.floorMatrix!!.elements[3] + minY * this.floorMatrix!!.elements[7] + minZ * this.floorMatrix!!.elements[11]
        val clipWMax = maxY * this.floorMatrix!!.elements[7] + this.floorMatrix!!.elements[15] + this.floorMatrix!!.elements[3] * maxX + maxZ * this.floorMatrix!!.elements[11]
        if (clipZMin < -clipWMin && clipZMax < -clipWMax) {
            flags = flags or 0x10
        } else if (clipZMin > clipWMin && clipZMax > clipWMax) {
            flags = flags or 0x20
        }

        val clipXMin = this.floorMatrix!!.elements[4] * minY + this.floorMatrix!!.elements[12] + minX * this.floorMatrix!!.elements[0] + minZ * this.floorMatrix!!.elements[8]
        val clipXMax = maxZ * this.floorMatrix!!.elements[8] + maxX * this.floorMatrix!!.elements[0] + this.floorMatrix!!.elements[12] + this.floorMatrix!!.elements[4] * maxY
        if (clipXMin < -clipWMin && clipXMax < -clipWMax) {
            flags = flags or 0x1
        }

        if (clipXMin > clipWMin && clipXMax > clipWMax) {
            flags = flags or 0x2
        }

        val clipYMin = this.floorMatrix!!.elements[5] * minY + minX * this.floorMatrix!!.elements[1] + this.floorMatrix!!.elements[13] + this.floorMatrix!!.elements[9] * minZ
        val clipYMax = this.floorMatrix!!.elements[13] + this.floorMatrix!!.elements[1] * maxX + this.floorMatrix!!.elements[5] * maxY + maxZ * this.floorMatrix!!.elements[9]
        if (clipYMin < -clipWMin && clipYMax < -clipWMax) {
            flags = flags or 0x4
        }

        if (clipYMin > clipWMin && clipYMax > clipWMax) {
            flags = flags or 0x8
        }

        return flags
    }

    override fun setNativeHeap(nativeMemory: NativeMemoryNode) {
    }

    override fun generateEmptySprite(width: Int, height: Int, transparent: Boolean, unused: Boolean): NativeSprite {
        return if (transparent) TransparentSpriteSM(this, width, height) else OpaqueSpriteSM(this, width, height)
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

    override fun createNativeSprite(srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, hasAlpha: Boolean): NativeSprite {
        if (this.colourBuffer == null) {
            throw IllegalStateException("")
        } else {
            val pixelBuffer = IntArray(srcWidth * srcHeight)
            var srcOffset = srcY * this.viewportWidth + srcX
            val scanlineStride = this.viewportWidth - srcWidth

            for (rowIdx in 0 until srcHeight) {
                val rowStart = srcWidth * rowIdx

                for (colIdx in 0 until srcWidth) {
                    pixelBuffer[rowStart + colIdx] = this.colourBuffer!![srcOffset++]
                }

                srcOffset += scanlineStride
            }

            return if (hasAlpha) {
                TransparentSpriteSM(this, pixelBuffer, srcWidth, srcHeight)
            } else {
                OpaqueSpriteSM(this, pixelBuffer, srcWidth, srcHeight)
            }
        }
    }

    override fun createComponentSurface(sizeX: Int, sizeY: Int, rowStartX: IntArray, rowWidth: IntArray): ComponentSurface {
        return ComponentDimensionSM(rowStartX, rowWidth)
    }

    override fun resetDepthClippingMask() {
        this.depthRangeFar = 1.0f
        this.depthRangeNear = 0.0f

        for (threadIdx in 0 until this.executingThreadCount) {
            val threadResource = this.renderingThreads!![threadIdx]
            val rasterizer = threadResource.rasterizer
            rasterizer!!.depthRangeFar = this.depthRangeFar
            rasterizer.depthRangeNear = this.depthRangeNear
        }

    }

    override fun createModel(mesh: Mesh, modelFlags: Int, preferenceFlags: Int, ambient: Int, contrast: Int): Model {
        return ModelSM(this, mesh, modelFlags, ambient, contrast, preferenceFlags)
    }

    override fun mergeFunctionMasks(base: Int, mask: Int): Int {
        return base or mask
    }

    override fun createGround(sizeX: Int, sizeY: Int, heightsNext: Array<IntArray>, heightsCurrent: Array<IntArray>, tileUnits: Int, unused: Int, tileFunctionMask: Int): Ground {
        return GroundSM(this, tileFunctionMask, sizeX, sizeY, tileUnits, heightsNext, heightsCurrent)
    }

    override fun getRenderingMatrix(): Matrix43 {
        val threadResource = getThreadResource(Thread.currentThread())
        return threadResource!!.renderingMatrix43!!
    }

    override fun bloomEnabled(): Boolean {
        return false
    }

    override fun copyLights(lightCount: Int, lights: Array<PointLight>?) {
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

    fun renderMaterializedSprite(hasColorBuffer: Boolean, hasDepthBuffer: Boolean, pointEntity: PointEntity, screenX: Int, screenY: Int, depth: Float, halfSize: Int) {
        var halfSize = halfSize
        val materialId = pointEntity.materialId
        val radius = halfSize
        halfSize = halfSize shl 1
        if (materialId == -1) {
            drawParticleShape(hasDepthBuffer, screenX, screenY, depth, radius, pointEntity.colorArgb, 1)
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
            ++halfSize
            (this.renderedNativeSprite as SpriteSM).drawParticle(hasColorBuffer, hasDepthBuffer, false, screenX - radius, screenY - radius, depth, halfSize, halfSize, 0, pointEntity.colorArgb, 1, false)
        }

    }

    fun drawBillboard(colorWrite: Boolean, depthTest: Boolean, transparent: Boolean, x: Int, y: Int, z: Float, size2d: Int, size3d: Int, materialId: Int, color: Int, blendType: Int, shape: Int) {
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
                    (this.renderedNativeSprite as SpriteSM).drawParticle(colorWrite, depthTest, transparent, x - size2d, y - size3d, z, size2d shl 1, size3d shl 1, blendType, color, shape, material.effectCombiner != 2)
                    return
                }
            }
            drawParticleShape(depthTest, x, y, z, size2d, color, shape)
        }

    }

    fun drawParticleShape(depthTest: Boolean, x: Int, y: Int, z: Float, deltaY: Int, color: Int, shape: Int) {
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

            var scanY = highestY
            val radiusSq = deltaY * deltaY
            var deltaX = 0
            var dy = y - highestY
            var dySq = dy * dy
            var dySqMinusDy = dySq - dy
            if (y > lowestY) {
                y = lowestY
            }

            val alpha = color ushr 24
            var invAlpha: Int
            var leftX: Int
            var rightX: Int
            var bufferOffset: Int
            if (shape != 0 && (shape != 1 || alpha != 255)) {
                var pixelX: Int
                var existingColor: Int
                if (shape == 1) {
                    color = ((color and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha shl 24) + (alpha * (color and 0xff00) shr 8 and 0xff00)

                    invAlpha = 256 - alpha
                    while (scanY < y) {
                        while (dySqMinusDy <= radiusSq || dySq <= radiusSq) {
                            dySq += deltaX + deltaX
                            dySqMinusDy += deltaX++ + deltaX
                        }

                        leftX = x - deltaX + 1
                        if (leftX < this.clipLeft) {
                            leftX = this.clipLeft
                        }

                        rightX = deltaX + x
                        if (rightX > this.clipRight) {
                            rightX = this.clipRight
                        }

                        bufferOffset = leftX + scanY * this.viewportWidth

                        pixelX = leftX
                        while (pixelX < rightX) {
                            if (!depthTest || z < this.depthBuffer!![bufferOffset]) {
                                existingColor = this.colourBuffer!![bufferOffset]
                                existingColor = ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff) + (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00)
                                this.colourBuffer!![bufferOffset] = existingColor + color
                            }

                            ++bufferOffset
                            pixelX++
                        }

                        ++scanY
                        dySq -= dy-- + dy
                        dySqMinusDy -= dy + dy
                    }

                    deltaX = deltaY
                    dy = -dy
                    dySqMinusDy = radiusSq + dy * dy
                    dySq = dySqMinusDy - deltaY

                    dySqMinusDy -= dy
                    while (scanY < lowestY) {
                        while (dySqMinusDy > radiusSq && dySq > radiusSq) {
                            dySqMinusDy -= deltaX-- + deltaX
                            dySq -= deltaX + deltaX
                        }

                        leftX = x - deltaX
                        if (leftX < this.clipLeft) {
                            leftX = this.clipLeft
                        }

                        rightX = deltaX + x
                        if (rightX > this.clipRight - 1) {
                            rightX = this.clipRight - 1
                        }

                        bufferOffset = leftX + scanY * this.viewportWidth

                        pixelX = leftX
                        while (pixelX <= rightX) {
                            if (!depthTest || z < this.depthBuffer!![bufferOffset]) {
                                existingColor = this.colourBuffer!![bufferOffset]
                                existingColor = ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff) + (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00)
                                this.colourBuffer!![bufferOffset] = existingColor + color
                            }

                            ++bufferOffset
                            pixelX++
                        }

                        ++scanY
                        dySqMinusDy += dy + dy
                        dySq += dy++ + dy
                    }
                } else {
                    if (shape != 2) {
                        throw IllegalArgumentException()
                    }

                    var channelSum: Int
                    while (scanY < y) {
                        while (dySqMinusDy <= radiusSq || dySq <= radiusSq) {
                            dySq += deltaX + deltaX
                            dySqMinusDy += deltaX++ + deltaX
                        }

                        invAlpha = x - deltaX + 1
                        if (invAlpha < this.clipLeft) {
                            invAlpha = this.clipLeft
                        }

                        leftX = deltaX + x
                        if (leftX > this.clipRight) {
                            leftX = this.clipRight
                        }

                        rightX = invAlpha + scanY * this.viewportWidth

                        bufferOffset = invAlpha
                        while (bufferOffset < leftX) {
                            if (!depthTest || z < this.depthBuffer!![rightX]) {
                                pixelX = this.colourBuffer!![rightX]
                                existingColor = pixelX + color
                                channelSum = (pixelX and 0xff00ff) + (color and 0xff00ff)
                                pixelX = (channelSum and 0x1000100) + (existingColor - channelSum and 0x10000)
                                this.colourBuffer!![rightX] = existingColor - pixelX or (pixelX - (pixelX ushr 8))
                            }

                            ++rightX
                            bufferOffset++
                        }

                        ++scanY
                        dySq -= dy-- + dy
                        dySqMinusDy -= dy + dy
                    }

                    deltaX = deltaY
                    dy = -dy
                    dySqMinusDy = radiusSq + dy * dy
                    dySq = dySqMinusDy - deltaY

                    dySqMinusDy -= dy
                    while (scanY < lowestY) {
                        while (dySqMinusDy > radiusSq && dySq > radiusSq) {
                            dySqMinusDy -= deltaX-- + deltaX
                            dySq -= deltaX + deltaX
                        }

                        invAlpha = x - deltaX
                        if (invAlpha < this.clipLeft) {
                            invAlpha = this.clipLeft
                        }

                        leftX = deltaX + x
                        if (leftX > this.clipRight - 1) {
                            leftX = this.clipRight - 1
                        }

                        rightX = invAlpha + scanY * this.viewportWidth

                        bufferOffset = invAlpha
                        while (bufferOffset <= leftX) {
                            if (!depthTest || z < this.depthBuffer!![rightX]) {
                                pixelX = this.colourBuffer!![rightX]
                                existingColor = pixelX + color
                                channelSum = (pixelX and 0xff00ff) + (color and 0xff00ff)
                                pixelX = (channelSum and 0x1000100) + (existingColor - channelSum and 0x10000)
                                this.colourBuffer!![rightX] = existingColor - pixelX or (pixelX - (pixelX ushr 8))
                            }

                            ++rightX
                            bufferOffset++
                        }

                        ++scanY
                        dySqMinusDy += dy + dy
                        dySq += dy++ + dy
                    }
                }
            } else {
                while (scanY < y) {
                    while (dySqMinusDy <= radiusSq || dySq <= radiusSq) {
                        dySq += deltaX + deltaX
                        dySqMinusDy += deltaX++ + deltaX
                    }

                    invAlpha = x - deltaX + 1
                    if (invAlpha < this.clipLeft) {
                        invAlpha = this.clipLeft
                    }

                    leftX = deltaX + x
                    if (leftX > this.clipRight) {
                        leftX = this.clipRight
                    }

                    rightX = invAlpha + scanY * this.viewportWidth

                    bufferOffset = invAlpha
                    while (bufferOffset < leftX) {
                        if (!depthTest || z < this.depthBuffer!![rightX]) {
                            this.colourBuffer!![rightX] = color
                        }

                        ++rightX
                        bufferOffset++
                    }

                    ++scanY
                    dySq -= dy-- + dy
                    dySqMinusDy -= dy + dy
                }

                deltaX = deltaY
                dy = scanY - y
                dySqMinusDy = radiusSq + dy * dy
                dySq = dySqMinusDy - deltaY

                dySqMinusDy -= dy
                while (scanY < lowestY) {
                    while (dySqMinusDy > radiusSq && dySq > radiusSq) {
                        dySqMinusDy -= deltaX-- + deltaX
                        dySq -= deltaX + deltaX
                    }

                    invAlpha = x - deltaX
                    if (invAlpha < this.clipLeft) {
                        invAlpha = this.clipLeft
                    }

                    leftX = deltaX + x
                    if (leftX > this.clipRight - 1) {
                        leftX = this.clipRight - 1
                    }

                    rightX = invAlpha + scanY * this.viewportWidth

                    bufferOffset = invAlpha
                    while (bufferOffset <= leftX) {
                        if (!depthTest || z < this.depthBuffer!![rightX]) {
                            this.colourBuffer!![rightX] = color
                        }

                        ++rightX
                        bufferOffset++
                    }

                    ++scanY
                    dySqMinusDy += dy + dy
                    dySq += dy++ + dy
                }

            }
        }

    }

    override fun setViewMatrix(sunPosition: Matrix43) {
        this.currentSunPosition = sunPosition
        updateFrustumPlanes()
    }

    override fun copyViewMatrix(): Matrix43 {
        return Matrix43(this.currentSunPosition!!)
    }

    override fun copyModelMatrix(): Matrix44 {
        return Matrix44(this.modelMatrix!!)
    }

    override fun applyLighting(sunColor: Int, intensity: Float, antiIntensity: Float, x: Float, y: Float, z: Float) {
        this.lowestContrast = (65535.0f * intensity).toInt()
        this.highestContrast = (65535.0f * antiIntensity).toInt()
        val magnitude = sqrt((z * z + y * y + x * x).toDouble()).toFloat()
        this.lightX = (x * 65535.0f / magnitude).toInt()
        this.lightY = (65535.0f * y / magnitude).toInt()
        this.lightZ = (z * 65535.0f / magnitude).toInt()
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
            resource.fogEnabled = count >= 0
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
        val depthPerspective = this.modelMatrix!!.getDepthPerspectiveCD()
        val fogNear = this.modelMatrix!!.elements[10] * (depthPerspective - 255.0f) + this.modelMatrix!!.elements[14]
        val fogFar = this.modelMatrix!!.elements[10] * depthPerspective + this.modelMatrix!!.elements[14]
        val fogRange = fogFar - fogNear

        for (threadIdx in 0 until this.executingThreadCount) {
            val threadResource = this.renderingThreads!![threadIdx]
            threadResource.fogStartDistance = fogNear
            threadResource.fogRange = fogRange
        }

    }

    override fun createCubeTexture(materialAtPositiveX: Int, materialAtNegativeX: Int, materialAtPositiveY: Int, materialAtNegativeY: Int, materialAtPositiveZ: Int, materialAtNegativeZ: Int): AbstractCubeTextureGenerator? {
        return null
    }

    override fun resetBloom() {
    }

    override fun beginBloomCapture(width: Int, height: Int) {
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

    override fun computeWorldToScreen(worldX: Float, worldY: Float, worldZ: Float, screenCoords: FloatArray) {
        val clipZ = this.floorMatrix!!.elements[10] * worldZ + worldX * this.floorMatrix!!.elements[2] + this.floorMatrix!!.elements[14] + worldY * this.floorMatrix!!.elements[6]
        val clipW = this.floorMatrix!!.elements[3] * worldX + this.floorMatrix!!.elements[15] + this.floorMatrix!!.elements[7] * worldY + worldZ * this.floorMatrix!!.elements[11]
        if (clipZ >= -clipW && clipZ <= clipW) {
            val clipX = worldY * this.floorMatrix!!.elements[4] + this.floorMatrix!!.elements[12] + worldX * this.floorMatrix!!.elements[0] + this.floorMatrix!!.elements[8] * worldZ
            if (clipX >= -clipW && clipX <= clipW) {
                val clipY = worldZ * this.floorMatrix!!.elements[9] + this.floorMatrix!!.elements[13] + this.floorMatrix!!.elements[1] * worldX + this.floorMatrix!!.elements[5] * worldY
                if (clipY >= -clipW && clipY <= clipW) {
                    val depth = this.viewMatrix44!!.elements[14] + this.viewMatrix44!!.elements[2] * worldX + this.viewMatrix44!!.elements[6] * worldY + this.viewMatrix44!!.elements[10] * worldZ
                    screenCoords[0] = this.viewportOriginX + clipX * this.viewportHalfWidth / clipW
                    screenCoords[1] = clipY * this.viewportHalfHeight / clipW + this.viewportOriginY
                    screenCoords[2] = depth
                } else {
                    screenCoords[2] = Float.NaN
                    screenCoords[1] = Float.NaN
                    screenCoords[0] = Float.NaN
                }
            } else {
                screenCoords[2] = Float.NaN
                screenCoords[1] = Float.NaN
                screenCoords[0] = Float.NaN
            }
        } else {
            screenCoords[2] = Float.NaN
            screenCoords[1] = Float.NaN
            screenCoords[0] = Float.NaN
        }

    }

    override fun clearRectangle(surface: ComponentSurface, clipOffsetX: Int, clipOffsetY: Int) {
        var offsetY = clipOffsetY
        if (this.colourBuffer != null) {
            val dimensionSM = surface as ComponentDimensionSM
            val rowStartX = dimensionSM.rowStartX
            val rowWidth = dimensionSM.rowWidth
            var visibleRows: Int
            if (this.clipBottom < rowStartX!!.size + offsetY) {
                visibleRows = this.clipBottom - offsetY
            } else {
                visibleRows = rowStartX.size
            }

            var startRow: Int
            if (this.clipTop > offsetY) {
                startRow = this.clipTop - offsetY
                offsetY = this.clipTop
            } else {
                startRow = 0
            }

            if (visibleRows - startRow > 0) {
                var scanlineOffset = offsetY * this.viewportWidth

                for (row in startRow until visibleRows) {
                    var rowX = clipOffsetX + rowStartX[row]
                    var rowW = rowWidth!![row]
                    if (this.clipLeft > rowX) {
                        rowW -= this.clipLeft - rowX
                        rowX = this.clipLeft
                    }

                    if (this.clipRight < rowW + rowX) {
                        rowW = this.clipRight - rowX
                    }

                    rowX += scanlineOffset

                    for (pixel in -rowW until 0) {
                        this.colourBuffer!![rowX++] = -16777216.toInt()
                    }

                    scanlineOffset += this.viewportWidth
                }
            }
        }

    }

    fun isSmallTexture(materialId: Int): Boolean {
        return this.forceSmallTexture || materialProvider!!.getMaterial(materialId).isSmall
    }

    override fun setSunCubeTexture(cubeTexture: AbstractCubeTextureGenerator?) {
    }

    override fun getImageProducer(): DirectDisplay {
        return SafeModeDirectDisplay(this)
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
                        val paletteColor = spritePalette!![spritePixels!![w + currentPos].toInt() and 0xff]
                        spriteTransparentPixels[currentPos + w] = if (paletteColor != 0) -16777216.toInt() or paletteColor else 0
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
            val absWidth = if (width >= 0) width else -width
            val absHeight = if (height >= 0) height else -height
            var dimension = absWidth
            if (absWidth < absHeight) {
                dimension = absHeight
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

    override fun getNativeMemory(size: Int): NativeMemoryNode? {
        return null
    }

    override fun getMaxLights(): Int {
        return 0
    }

    fun drawDashedVerticalLine(x: Int, y: Int, height: Int, color: Int, dashLength: Int, gapLength: Int, dashOffset: Int) {
        var color = color
        if (this.colourBuffer != null && x >= this.clipLeft && x < this.clipRight) {
            val currentPos = y * this.viewportWidth + x
            val alpha = color ushr 24
            val dashCycle = gapLength + dashLength
            var dashPhase = dashOffset % dashCycle
            var loopIdx: Int
            if (alpha == 255) {
                loopIdx = 0
                while (loopIdx < height) {
                    if (loopIdx + y >= this.clipTop && loopIdx + y < this.clipBottom && dashPhase < dashLength) {
                        this.colourBuffer!![currentPos + loopIdx * this.viewportWidth] = color
                    }

                    ++loopIdx
                    ++dashPhase
                    dashPhase %= dashCycle
                }
            } else {
                var deltaIdx: Int
                var bufferIdx: Int
                var existingColor: Int
                color = (alpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (alpha * (color and 0xff00) shr 8 and 0xff00) + (alpha shl 24)
                loopIdx = 256 - alpha
                deltaIdx = 0
                while (deltaIdx < height) {
                    if (deltaIdx + y >= this.clipTop && deltaIdx + y < this.clipBottom && dashPhase < dashLength) {
                        bufferIdx = currentPos + deltaIdx * this.viewportWidth
                        existingColor = this.colourBuffer!![bufferIdx]
                        existingColor = (loopIdx * (existingColor and 0xff00) shr 8 and 0xff00) + ((existingColor and 0xff00ff) * loopIdx shr 8 and 0xff00ff)
                        this.colourBuffer!![bufferIdx] = existingColor + color
                    }
                    ++deltaIdx
                    ++dashPhase
                    dashPhase %= dashCycle
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

            val bufferStart = y * this.viewportWidth + x
            val colorAlpha = color ushr 24
            if (alpha == 0 || alpha == 1 && colorAlpha == 255) {
                for (h in 0 until height) {
                    this.colourBuffer!![bufferStart + h * this.viewportWidth] = color
                }
            } else {
                if (alpha == 1) {
                    color = (colorAlpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha shl 24) + (colorAlpha * (color and 0xff00) shr 8 and 0xff00)
                    val invAlpha = 256 - colorAlpha
                    for (rowIdx in 0 until height) {
                        val bufferIdx = bufferStart + rowIdx * this.viewportWidth
                        var existingColor = this.colourBuffer!![bufferIdx]
                        existingColor = ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff) + (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00)
                        this.colourBuffer!![bufferIdx] = existingColor + color
                    }
                } else {
                    if (alpha != 2) {
                        throw IllegalArgumentException()
                    }
                    for (rowIdx in 0 until height) {
                        val colorIndex = bufferStart + rowIdx * this.viewportWidth
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

    override fun drawQuad(cubeTexture1: AbstractCubeTextureGenerator?, cubeTexture2: AbstractCubeTextureGenerator?, alpha: Float, cubeTexture3: AbstractCubeTextureGenerator?): AbstractCubeTextureGenerator? {
        return null
    }

    fun hasBeenShutdown(): Boolean {
        return this.shutdown
    }

    override fun drawLine(x: Int, y: Int, width: Int, length: Int, color: Int, lineSpacing: Int, lineLength: Int, dashOffset: Int) {
        var x = x
        var y = y
        var width = width
        var length = length
        var color = color
        var lineSpacing = lineSpacing
        var lineLength = lineLength
        var dashOffset = dashOffset
        if (this.colourBuffer != null) {
            width -= x
            length -= y
            var dashCycle: Int
            if (length == 0) {
                if (width >= 0) {
                    drawDashedHorizontalLine(x, y, width + 1, color, lineSpacing, lineLength, dashOffset)
                } else {
                    dashCycle = lineLength + lineSpacing
                    dashOffset %= dashCycle
                    dashOffset = dashCycle + lineSpacing - dashOffset - (-width + 1) % dashCycle
                    dashOffset %= dashCycle
                    if (dashOffset < 0) {
                        dashOffset += dashCycle
                    }

                    drawDashedHorizontalLine(width + x, y, -width + 1, color, lineSpacing, lineLength, dashOffset)
                }
            } else if (width == 0) {
                if (length >= 0) {
                    drawDashedVerticalLine(x, y, length + 1, color, lineSpacing, lineLength, dashOffset)
                } else {
                    dashCycle = lineLength + lineSpacing
                    dashOffset %= dashCycle
                    dashOffset = dashCycle + lineSpacing - dashOffset - (-length + 1) % dashCycle
                    dashOffset %= dashCycle
                    if (dashOffset < 0) {
                        dashOffset += dashCycle
                    }

                    drawDashedVerticalLine(x, y + length, -length + 1, color, lineSpacing, lineLength, dashOffset)
                }
            } else {
                dashOffset = dashOffset shl 8
                lineSpacing = lineSpacing shl 8
                lineLength = lineLength shl 8
                dashCycle = lineLength + lineSpacing
                dashOffset %= dashCycle
                var slope: Int
                var colorAlpha: Int
                if (width + length < 0) {
                    slope = (sqrt((width * width + length * length).toDouble()) * 256.0).toInt()
                    colorAlpha = slope % dashCycle
                    dashOffset = dashCycle + lineSpacing - dashOffset - colorAlpha
                    dashOffset %= dashCycle
                    if (dashOffset < 0) {
                        dashOffset += dashCycle
                    }

                    x += width
                    width = -width
                    y += length
                    length = -length
                }

                var stepLength: Int
                var invAlpha: Int
                var pixelY: Int
                var bufferIdx: Int
                var existingColor: Int
                var unused: Int
                if (width > length) {
                    y = y shl 16
                    y += 32768
                    length = length shl 16
                    slope = floor(length.toDouble() / width + 0.5).toInt()
                    width += x
                    colorAlpha = color ushr 24
                    stepLength = sqrt(((slope shr 8) * (slope shr 8) + 65536).toDouble()).toInt()
                    if (colorAlpha == 255) {
                        while (x <= width) {
                            invAlpha = y shr 16
                            if (x >= this.clipLeft && x < this.clipRight && invAlpha >= this.clipTop && invAlpha < this.clipBottom && dashOffset < lineSpacing) {
                                this.colourBuffer!![invAlpha * this.viewportWidth + x] = color
                            }

                            y += slope
                            ++x
                            dashOffset += stepLength
                            dashOffset %= dashCycle
                        }
                    } else {
                        color = (colorAlpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha * (color and 0xff00) shr 8 and 0xff00) + (colorAlpha shl 24)

                        invAlpha = 256 - colorAlpha
                        while (x <= width) {
                            pixelY = y shr 16
                            if (x >= this.clipLeft && x < this.clipRight && pixelY >= this.clipTop && pixelY < this.clipBottom && dashOffset < lineSpacing) {
                                bufferIdx = pixelY * this.viewportWidth + x
                                existingColor = this.colourBuffer!![bufferIdx]
                                existingColor = ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff) + (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00)
                                this.colourBuffer!![bufferIdx] = color + existingColor
                            }

                            y += slope
                            ++x
                            dashOffset += stepLength
                            dashOffset %= dashCycle
                        }
                    }
                } else {
                    x = x shl 16
                    x += 32768
                    width = width shl 16
                    slope = floor(width.toDouble() / length + 0.5).toInt()
                    length += y
                    colorAlpha = color ushr 24
                    stepLength = sqrt(((slope shr 8) * (slope shr 8) + 65536).toDouble()).toInt()
                    if ((colorAlpha != 255)) {
                        color = (colorAlpha * (color and 0xff00ff) shr 8 and 0xff00ff) + (colorAlpha * (color and 0xff00) shr 8 and 0xff00) + (colorAlpha shl 24)

                        invAlpha = 256 - colorAlpha
                        while (y <= length) {
                            pixelY = x shr 16
                            if (y >= this.clipTop && y < this.clipBottom && pixelY >= this.clipLeft && pixelY < this.clipRight && dashOffset < lineSpacing) {
                                bufferIdx = y * this.viewportWidth + pixelY
                                existingColor = this.colourBuffer!![bufferIdx]
                                existingColor = (invAlpha * (existingColor and 0xff00) shr 8 and 0xff00) + ((existingColor and 0xff00ff) * invAlpha shr 8 and 0xff00ff)
                                this.colourBuffer!![y * this.viewportWidth + pixelY] = color + existingColor
                            }

                            x += slope
                            ++y
                            dashOffset += stepLength
                            dashOffset %= dashCycle
                        }
                    } else {
                        while (y <= length) {
                            invAlpha = x shr 16
                            if (y >= this.clipTop && y < this.clipBottom && invAlpha >= this.clipLeft && invAlpha < this.clipRight && dashOffset < lineSpacing) {
                                this.colourBuffer!![y * this.viewportWidth + invAlpha] = color
                            }

                            x += slope
                            ++y
                            dashOffset += stepLength
                            dashOffset %= dashCycle
                        }
                    }
                }
            }
        }

    }

    override fun createPointLight(x: Int, y: Int, z: Int, radius: Int, color: Int): PointLight? {
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
        fun createImageProducer(hardwarerenderer0: SafeModeToolkit, canvas: Canvas, width: Int, height: Int): AbstractImageProducerSM {
            val imageProducer = BufferedImageProducerSM(hardwarerenderer0, canvas, width, height)
            return imageProducer
        }
    }
}
