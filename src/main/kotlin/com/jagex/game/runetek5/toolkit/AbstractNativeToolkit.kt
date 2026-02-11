package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.cache.node.LinkedList
import com.jagex.game.runetek5.cache.node.WeakReferenceMappingCache
import com.jagex.game.runetek5.client.GraphicsCache
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.NativeMaterialCache
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.font.NativeFont
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.shader.Shader
import com.jagex.game.runetek5.graphics.shader.ShaderLoader
import com.jagex.game.runetek5.graphics.shader.program.AbstractBloomShader
import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader
import com.jagex.game.runetek5.graphics.shader.program.AbstractParticleShader
import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader
import com.jagex.game.runetek5.graphics.shader.program.AbstractWaterShader
import com.jagex.game.runetek5.graphics.shader.program.impl.*
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator
import com.jagex.game.runetek5.graphics.texture.AbstractNativeCubeTextureGenerator
import com.jagex.game.runetek5.graphics.texture.NativeCubeTextureGenerator
import com.jagex.game.runetek5.graphics.texture.TextureStageState
import com.jagex.game.runetek5.graphics.texture.TextureState
import com.jagex.game.runetek5.graphics.texture.sampler.*
import com.jagex.game.runetek5.graphics.texture.water.NativeWaterTexture
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.companion.*
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent
import com.jagex.game.runetek5.graphics.NativeBloomRenderer
import com.jagex.game.runetek5.utils.ColorUtil
import jaclib.memory.DirectBufferHelper
import jaclib.memory.Stream
import jaclib.memory.heap.NativeHeap
import jaclib.memory.heap.NativeHeapBuffer
import sun.misc.Unsafe
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

abstract class AbstractNativeToolkit(
    materials: MaterialProvider?,
    shaders: Js5ResourceProvider?,
    multiSampleCount: Int,
    endiannessType: Int
) : AbstractToolkit(materials) {

    @JvmField var texturesSizeOccupied: Int = 0
    @JvmField var textureBuffersSizeOccupied: Int = 0
    @JvmField var GLEXTFramebufferObject: Boolean = false
    @JvmField var maxConcurrentTextures: Int = 0
    @JvmField val particleMatrix: Matrix44 = Matrix44()
    @JvmField val projectionMatrix: Matrix44 = Matrix44()
    @JvmField val frustumClipMatrix: Matrix44 = Matrix44()
    @JvmField val sunPosition: FloatArray = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f)
    @JvmField var baseLightColorRed: Float = 1.0f
    @JvmField var baseLightColorGreen: Float = 1.0f
    @JvmField var baseLightColorBlue: Float = 1.0f
    @JvmField var sunColorIntensity: Float = -1.0f
    @JvmField var multiSampleCount: Int = 0
    @JvmField var theUnsafe: Unsafe? = null
    @JvmField var directBuffer: ByteBuffer
    @JvmField var directBufferAddress: Long = 0
    @JvmField var animatingCycle: Int = 0
    @JvmField var activeTextureSampler: NativeTextureSampler? = null
    @JvmField var renderBufferSizeOccupied: Int = 0
    @JvmField var maxActiveLights: Int = 0
    @JvmField var fogStart: Float = 0.0f
    @JvmField var fogEnd: Float = 0.0f
    @JvmField var SUPPORTS_TEXTURE3D: Boolean = false
    @JvmField var GLARBMultisample: Boolean = false
    @JvmField val perspectiveMatrix: Matrix44 = Matrix44()
    @JvmField var depthRangeNear: Float = 0.0f
    @JvmField var depthRangeFar: Float = 1.0f
    @JvmField var camNear: Float = 50.0f
    @JvmField var camFar: Float = 3584.0f
    @JvmField var cTop: Int = 0
    @JvmField var cBottom: Int = 0
    @JvmField var cLeft: Int = 0
    @JvmField var cRight: Int = 0
    @JvmField var glViewportX: Int = 0
    @JvmField var glViewportY: Int = 0
    @JvmField var glViewportWidth: Int = 0
    @JvmField var glViewportHeight: Int = 0
    @JvmField var viewportOffsetX: Int = 0
    @JvmField var viewportOffsetY: Int = 0
    @JvmField var depthBufferingEnabled: Boolean = true
    @JvmField var depthMaskEnabled: Boolean = false
    @JvmField var enableDepthTest: Boolean = false
    @JvmField var lightingEnabled: Boolean = false
    @JvmField val sunEnabled: Boolean = true
    @JvmField val forceLightsDisabled: Boolean = false
    @JvmField val lightPosition: FloatArray = floatArrayOf(0.0f, 0.0f, -1.0f, 0.0f)
    @JvmField var lightIntensity: Float = 1.0f
    @JvmField var antiSunColorIntensity: Float = -1.0f
    @JvmField var currentTextureSlot: Int = 0
    @JvmField val fogSupported: Boolean = true
    @JvmField var fogColorRgb: Int = -1
    @JvmField var fogDistance: Int = -1
    @JvmField var fogOffset: Int = 0
    @JvmField var blendFuncMode: BlendFuncMode = BlendFuncMode.ALPHA_BLENDING
    @JvmField var enableBlending: Boolean = true
    @JvmField var enableAlphaTest: Boolean = true
    @JvmField var alphaBlendMode: Int = -1
    @JvmField var enableLights: Boolean = false
    @JvmField var boundsCalculated: Boolean = false
    @JvmField var currentSceneLightCount: Int = 0
    @JvmField var lastSceneLightCount: Int = 0
    @JvmField var envColor: Int = 0
    @JvmField var fogEnabled: Boolean = false
    @JvmField var textureMatrices: Array<Matrix44>? = null
    @JvmField var textureStates: Array<TextureState>? = null
    @JvmField var textureRgbCombineModes: Array<TextureCombineMode>? = null
    @JvmField var textureAlphaCombineModes: Array<TextureCombineMode>? = null
    @JvmField var lightSources: Array<PointLight?>? = null
    @JvmField val weakReferences: WeakReferenceMappingCache = WeakReferenceMappingCache()
    @JvmField val activeNativeMemory: LinkedList = LinkedList()
    @JvmField val localModelMatrix43: Matrix43 = Matrix43()
    @JvmField val modelMatrix: Matrix44 = Matrix44()
    @JvmField var viewMatrix43: Matrix43 = Matrix43()
    @JvmField val inverseSunMatrix43: Matrix43 = Matrix43()
    @JvmField val inverseSunMatrix44: Matrix44 = Matrix44()
    @JvmField val combinedModelViewMatrix43: Matrix43 = Matrix43()
    @JvmField val modelViewMatrix: Matrix44 = Matrix44()
    @JvmField val unsure: Matrix43 = Matrix43()
    @JvmField val unusedMatrix: Matrix44 = Matrix44()
    @JvmField val pointLightVector: Vector3 = Vector3()
    @JvmField val lightParameterBuffer: FloatArray = FloatArray(4)
    @JvmField var viewPositionCalibrated: Boolean = false
    @JvmField var matrixMode: MatrixMode = MatrixMode.ORTHO
    @JvmField var isOrthoProjectionValid: Boolean = false
    @JvmField val screenOrthoMatrix: Matrix44 = Matrix44()
    @JvmField val worldViewProjectionMatrix: Matrix44 = Matrix44()
    @JvmField var isModelViewValid: Boolean = false
    @JvmField val currentModelMatrix: Matrix44 = Matrix44()
    @JvmField val orthoProjectionMatrix: Matrix44 = Matrix44()
    @JvmField var activeProjectionSource: Matrix44
    @JvmField val transformMatrix: Matrix44 = Matrix44()
    @JvmField val frustumPlanes: Array<FloatArray> = Array(6) { FloatArray(4) }
    @JvmField var depthRangeNearOverride: Float = 0.0f
    @JvmField var depthRangeFarOverride: Float = 1.0f
    @JvmField var viewportX: Int = 0
    @JvmField var viewportY: Int = 0
    @JvmField var viewportWidth: Int = 0
    @JvmField var viewportHeight: Int = 0
    @JvmField val baseLightPosition: FloatArray = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f)
    @JvmField val baseLightInvertedPosition: FloatArray = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f)
    @JvmField var baseLightColor: Int = 16777215
    @JvmField var activeFlags: Int = 8
    @JvmField var shadowSizeShift: Int = 3
    @JvmField val materialSize: Int = 128
    @JvmField val waterShadersByEffect: Array<AbstractWaterShader?> = arrayOfNulls(16)
    @JvmField val bloomManager: Array<AbstractBloomShader?> = arrayOfNulls(2)
    @JvmField val hasWaterShaderEffect: Boolean = false
    @JvmField var blendFuncType: Int = 1
    @JvmField var isShutdown: Boolean = false
    @JvmField val copyByTypeSrc: Array<Model?> = arrayOfNulls(7)
    @JvmField val copyByTypeDst: Array<Model?> = arrayOfNulls(7)
    @JvmField val scratchMatrix: Matrix44 = Matrix44()
    @JvmField val renderingMatrix: Matrix43 = Matrix43()
    @JvmField var shaderResource: Js5ResourceProvider? = null
    @JvmField var endiannessType: Int = 0
    @JvmField var materialCache: NativeMaterialCache? = null
    @JvmField var hslToRgb: IntArray? = null
    @JvmField var waterTexture: NativeWaterTexture? = null
    @JvmField var environmentShader: AbstractEnvironmentShader? = null
    @JvmField var spriteShader: AbstractSpriteShader? = null
    @JvmField var abstractParticleShader: AbstractParticleShader? = null
    @JvmField var nativeEvtWaterMapper: NativeEvtWaterMapper? = null
    @JvmField var nativeWaterfall: NativeWaterfall? = null
    @JvmField var bloomRenderer: NativeBloomRenderer? = null
    @JvmField var activeWaterShader: AbstractWaterShader? = null
    @JvmField var particleRenderer: NativeParticleRenderer? = null
    @JvmField var nativeFunctionMask: Int = 0
    @JvmField var shadowOffsetX: Int = 0
    @JvmField var shadowOffsetZ: Int = 0
    @JvmField var nativeHeap: NativeHeap? = null
    @JvmField var sunCubeTextureGenerator: AbstractNativeCubeTextureGenerator? = null
    @JvmField var viewportCenterX: Float = 0.0f
    @JvmField var viewportCenterY: Float = 0.0f
    @JvmField var viewportOriginX: Float = 0.0f
    @JvmField var viewportOriginY: Float = 0.0f
    @JvmField var quadVertexStream: NativeStream? = null
    @JvmField var quadVertexDeclaration: VertexDeclaration? = null
    @JvmField var renderedWaterColor: Int = 0
    @JvmField var renderedWaterTile: WaterTile? = null
    @JvmField var missingWaterColor: Boolean = false
    @JvmField var vertexStream: NativeStream? = null
    @JvmField var vertexDeclaration: VertexDeclaration? = null
    @JvmField var textureSamplers: Array<TextureSampler?>? = null
    @JvmField var blackTextureSampler: NativeTextureSampler? = null
    @JvmField var positionTexCoordDeclaration: VertexDeclaration? = null
    @JvmField var positionColorTexCoordNormalDeclaration: VertexDeclaration? = null
    @JvmField var positionColorTexCoordDeclaration: VertexDeclaration? = null
    @JvmField var indexBuffer: NativeToolkitBuffer? = null
    @JvmField var whitePixelSprite: NativeSprite? = null
    @JvmField var blackPixelSprite: NativeSprite? = null
    @JvmField var circleVertexStream: NativeStream? = null

    init {
        activeProjectionSource = orthoProjectionMatrix
        Stream()

        try {
            shaderResource = shaders
            this.multiSampleCount = multiSampleCount
            this.endiannessType = endiannessType
            ColorUtil.initializeTables(false, true)
            materialCache = if (materialProvider != null) {
                NativeMaterialCache(this, materialProvider)
            } else {
                null
            }

            hslToRgb = if (endiannessType == 0) {
                ColorUtil.hslToRgb
            } else {
                ColorUtil.unsignedHslToRgb
            }

            try {
                val field = Unsafe::class.java.getDeclaredField("theUnsafe")
                field.isAccessible = true
                theUnsafe = field.get(null) as Unsafe
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            directBuffer = ByteBuffer.allocateDirect(4194304)
            directBuffer.order(ByteOrder.nativeOrder())
            directBufferAddress = DirectBufferHelper.getDirectBufferAddress(directBuffer)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            cleanupAndShutdown()
            if (throwable is OutOfMemoryError) {
                throw throwable
            }
            throw RuntimeException("")
        }

        setNativeHeap(getNativeMemory(131072))
    }

    fun getTextureMatrix(): Matrix44 {
        return textureMatrices!![currentTextureSlot]
    }

    fun clearReference(interface71: Binding) {
        weakReferences.identify(interface71)
    }

    fun resetWeakReferences() {
        val refs = weakReferences.collect()
        for (ref in refs) {
            val mapping = ref as Binding
            mapping.deallocate()
        }
    }

    abstract fun supportsArbVertexShader(): Boolean

    abstract fun supportsArbFragmentShader(): Boolean

    fun areLightsEnabled(): Boolean {
        return enableLights
    }

    abstract fun loadShader(shaderName: String): Shader?

    fun createShader(shaderData: ByteArray?): ShaderLoader? {
        if (shaderData == null) {
            return null
        }
        return try {
            ShaderLoader(shaderData)
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }

    abstract fun setMatrixTransform(modelMatrix: Matrix44, viewMatrix: Matrix44, projectionMatrix: Matrix44)

    internal abstract fun blendDrawBuffer()

    fun initializeShaders() {
        waterTexture = NativeWaterTexture(this)
        enableLights = false

        try {
            environmentShader = NativeEnvironmentShaderHd(this)
            spriteShader = NativeSpriteShaderHd(this)
            abstractParticleShader = NativeParticleShaderHd(this)
            nativeEvtWaterMapper = NativeEvtWaterMapper(this, waterTexture!!)
            nativeWaterfall = NativeWaterfall(this, waterTexture!!)
        } catch (exception: Exception) {
            exception.printStackTrace()
            environmentShader = NativeEnvironmentShaderLd(this)
            spriteShader = NativeSpriteShaderLd(this)
            abstractParticleShader = NativeParticleShaderLd(this)
            nativeEvtWaterMapper = null
            nativeWaterfall = null
            enableLights = true
        }

        val displayWidth: Int
        val displayHeight: Int
        if (display != null) {
            displayWidth = display!!.getWidth()
            displayHeight = display!!.getHeight()
        } else {
            displayHeight = 0
            displayWidth = 0
        }

        bloomRenderer = NativeBloomRenderer(this, displayWidth, displayHeight)
        if (!enableLights) {
            bloomManager[1] = NativeFilterBloomShader(this)
            bloomRenderer!!.initialize(bloomManager[1]!!)
        }

        if (activeWaterShader != null) {
            activeWaterShader!!.deactivate()
            activeWaterShader = null
        }

        initializeRenderers()
    }

    fun getShaderByEffect(effect: Int): AbstractWaterShader? {
        return waterShadersByEffect[effect]
    }

    fun setupGraphics() {
        colorMask(7)
        resetAllRenderState()
    }

    fun getRenderedWaterTile(): WaterTile? {
        return renderedWaterTile
    }

    fun initializeRenderers() {
        bloomRenderer!!.getFormat()

        for (index in bloomManager.indices) {
            if (bloomManager[index] != null && bloomManager[index]!!.isLinked()) {
                bloomManager[index]!!.reloadShader()
            }
        }

        particleRenderer = NativeParticleRenderer(this)
        initializeQuadStream()
        initializeLineStream()
        initializeCircleStream()
        particleRenderer!!.resetVertexStream()
    }

    fun deallocate() {
        bloomRenderer!!.destroy()
        quadVertexStream!!.deallocate()
        vertexStream!!.deallocate()
        circleVertexStream!!.deallocate()

        for (index in 0 until 7) {
            // Model deallocation removed (deallocate not in base Model class)
        }

        particleRenderer!!.deallocate()
        indexBuffer!!.deallocate()
    }

    override fun createNativeSprite(width: Int, height: Int, textureWidth: Int, textureHeight: Int, hasAlpha: Boolean): NativeSprite {
        throw UnsupportedOperationException("createNativeSprite not implemented for this toolkit")
    }

    override fun utilisesNativeHeap(): Boolean {
        return true
    }

    override fun shutdown() {
        if (!isShutdown) {
            deallocate()
            materialCache!!.reset()

            var memory = activeNativeMemory.getTail()
            while (memory != null) {
                // Memory deallocation - type-specific handling removed
                memory = activeNativeMemory.getPrevious()
            }

            ColorUtil.resetTables(false, true)

            for (index in waterShadersByEffect.indices) {
                if (waterShadersByEffect[index] != null) {
                    waterShadersByEffect[index]!!.destroy()
                    waterShadersByEffect[index] = null
                }
            }

            for (index in bloomManager.indices) {
                if (bloomManager[index] != null) {
                    bloomManager[index]!!.onShutdown()
                    bloomManager[index] = null
                }
            }

            resetWeakReferences()
            animateTextures(0)
            isShutdown = true
        }
    }

    fun addWeakReference(mapping: Binding) {
        weakReferences.put(mapping)
    }

    override fun forceFixedScreenmode(): Boolean {
        return true
    }

    override fun hasBloomSupport(): Boolean {
        return bloomManager[1] != null
    }

    fun updateSceneLights() {
        if (areLightsEnabled()) {
            configureLights()
        }

        currentSceneLightCount = lastSceneLightCount
        lastSceneLightCount = 0
    }

    fun toggleBlending(blendFuncType: Int) {
        if (this.blendFuncType != blendFuncType) {
            val blendFunc: BlendFuncMode
            val alphaTest: Boolean
            val blend: Boolean
            when (blendFuncType) {
                1 -> {
                    blendFunc = BlendFuncMode.ALPHA_BLENDING
                    alphaTest = true
                    blend = true
                }
                2 -> {
                    blendFunc = BlendFuncMode.OPAQUE_BLENDING
                    alphaTest = false
                    blend = true
                }
                128 -> {
                    blendFunc = BlendFuncMode.COLOR_BLENDING
                    alphaTest = true
                    blend = true
                }
                else -> {
                    blendFunc = BlendFuncMode.OPAQUE
                    alphaTest = false
                    blend = false
                }
            }

            if (alphaTest != enableAlphaTest) {
                enableAlphaTest = alphaTest
                toggleAlpha()
            }

            if (blend != enableBlending) {
                enableBlending = blend
                toggleBlending()
            }

            if (blendFunc != blendFuncMode) {
                blendFuncMode = blendFunc
                blendDrawBuffer()
            }

            this.blendFuncType = blendFuncType
            nativeFunctionMask = nativeFunctionMask and -13
        }
    }

    override fun extendBuildArea(): Boolean {
        return true
    }

    override fun replaceCanvasOnDisplayUpdate(): Boolean {
        return false
    }

    override fun hasAntiAliasing(): Boolean {
        return GLARBMultisample
    }

    abstract fun getPixelOffset(): Float

    override fun animateTextures(cycle: Int) {
        materialCache?.processCycle()
        animatingCycle = cycle and 0x7fffffff
    }

    fun autoResize2() {
        autoResize()
    }

    fun autoResize() {
        invalidateOrthoProjection()
        invalidateModelView()
        setOrthoMatrixMode()
        calculateViewportCenter()
        resetClipping()
    }

    override fun bloomSupported(): Boolean {
        if (bloomManager[1] != null && !bloomManager[1]!!.isLinked()) {
            val reset = bloomRenderer!!.initialize(bloomManager[1]!!)
            if (reset) {
                materialCache!!.reset()
            }
            return reset
        }
        return false
    }

    override fun resetBloom() {
        if (bloomManager[1] != null && bloomManager[1]!!.isLinked()) {
            bloomRenderer!!.removeBloomShader(bloomManager[1]!!)
            materialCache!!.reset()
        }
    }

    override fun bloomEnabled(): Boolean {
        return bloomManager[1] != null && bloomManager[1]!!.isLinked()
    }

    override fun setBloomFilter(r: Float, g: Float, b: Float) {
        NativeFilterBloomShader.red = r
        NativeFilterBloomShader.blue = g
        NativeFilterBloomShader.green = b
        NativeFilterBloomShader.baseBlur = 1.0f
    }

    fun uploadLineVertices(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        directBuffer.clear()
        directBuffer.putFloat(startX)
        directBuffer.putFloat(startY)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(endX)
        directBuffer.putFloat(endY)
        directBuffer.putFloat(0.0f)
        return vertexStream!!.upload(0, directBuffer.position(), directBufferAddress)
    }

    override fun getNativeMemory(requiredCapacity: Int): NativeMemoryNode {
        val nativeMemory = createNativeMemory(requiredCapacity)
        activeNativeMemory.insertTail(nativeMemory)
        return nativeMemory
    }

    protected abstract fun createNativeMemory(capacity: Int): NativeMemoryNode

    protected abstract fun extractNativeHeap(memoryNode: NativeMemoryNode): NativeHeap

    override fun setNativeHeap(memoryNode: NativeMemoryNode) {
        nativeHeap = extractNativeHeap(memoryNode)
    }

    fun generateNativeHeapBuffer(initialCapacity: Int, allocateDirect: Boolean): NativeHeapBuffer {
        return nativeHeap!!.createBuffer(initialCapacity, allocateDirect)
    }

    fun getOrResizeIndexBuffer(requiredCapacity: Int): NativeToolkitBuffer {
        if (indexBuffer!!.getCapacity() < requiredCapacity * 2) {
            indexBuffer!!.create(requiredCapacity)
        }
        return indexBuffer!!
    }

    override fun getOffheap(): Int {
        return textureBuffersSizeOccupied + texturesSizeOccupied + renderBufferSizeOccupied
    }

    override fun generateSprite(spriteType: SpriteType?, hasPalette: Boolean): NativeSprite {
        val nativeSprite: NativeSprite
        if (spriteType != null && spriteType.width != 0 && spriteType.height != 0) {
            val spritePixels = IntArray(spriteType.width * spriteType.height)
            var index = 0
            var pixelIndex = 0
            if (spriteType.alpha != null) {
                for (y in 0 until spriteType.height) {
                    for (x in 0 until spriteType.width) {
                        spritePixels[pixelIndex++] = (spriteType.alpha!![index].toInt() and 0xff) shl 24 or spriteType.pallete[spriteType.pixels[index].toInt() and 0xff]
                        index++
                    }
                }
            } else {
                for (y in 0 until spriteType.height) {
                    for (x in 0 until spriteType.width) {
                        val color = spriteType.pallete[spriteType.pixels[index++].toInt() and 0xff]
                        spritePixels[pixelIndex++] = if (color != 0) 0xffffff.inv() or color else 0
                    }
                }
            }
            nativeSprite = createNativeSprite(spritePixels, spriteType.width, spriteType.width, spriteType.height)
        } else {
            nativeSprite = createNativeSprite(intArrayOf(1), 1, 1, 1)
        }
        nativeSprite.setSize(spriteType!!.minX, spriteType.minY, spriteType.borderX, spriteType.borderY)
        return nativeSprite
    }

    override fun createNativeSprite(pixels: IntArray, defaultBackgroundColor: Int, scanlineWidth: Int, useWidth: Int, useHeight: Int, hasAlpha: Boolean): NativeSprite {
        throw UnsupportedOperationException("createNativeSprite not implemented for this toolkit")
    }

    fun getProjectionMatrix(): Matrix44 {
        return projectionMatrix
    }

    override fun createComponentSurface(sizeX: Int, sizeY: Int, rowStartX: IntArray, rowWidth: IntArray): ComponentSurface {
        return MaskedTextureSurface.createFromRowMask(this, sizeX, sizeY, rowStartX, rowWidth)
    }

    internal abstract fun toggleBlending()

    override fun createModel(mesh: Mesh, functionMask: Int, preferenceMask: Int, ambient: Int, contrast: Int): Model {
        throw UnsupportedOperationException("createModel must be implemented by subclass")
    }

    override fun mergeFunctionMasks(mask1: Int, mask2: Int): Int {
        return mask1 or mask2
    }

    override fun createGround(regionSizeX: Int, regionSizeY: Int, tileheightsCurrentLvl: Array<IntArray>, tileheightsNextLvl: Array<IntArray>, floorFunctionMask: Int, waterFunctionMask: Int): Ground {
        throw UnsupportedOperationException("createGround not implemented for this toolkit")
    }

    override fun getRenderingMatrix(): Matrix43 {
        return renderingMatrix
    }

    override fun setSunCubeTexture(cubeTexture: AbstractCubeTextureGenerator?) {
        sunCubeTextureGenerator = cubeTexture as? AbstractNativeCubeTextureGenerator
    }

    override fun calculateViewportCenter() {
        if (display == null) {
            return
        }
        viewportX = 0
        viewportY = 0
        viewportWidth = display!!.getWidth()
        viewportHeight = display!!.getHeight()
        calculateViewport()
    }

    override fun setDrawableArea(x: Int, y: Int, width: Int, height: Int) {
        viewportX = x
        viewportY = y
        viewportWidth = width
        viewportHeight = height
        calculateViewport()
    }

    fun calculateViewport() {
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            glViewportX = viewportX
            glViewportY = viewportY
            glViewportWidth = viewportWidth
            glViewportHeight = viewportHeight
            depthRangeNear = depthRangeNearOverride
            depthRangeFar = depthRangeFarOverride
        } else {
            glViewportX = 0
            glViewportY = 0
            if (display != null) {
                glViewportWidth = display!!.getWidth()
                glViewportHeight = display!!.getHeight()
            } else {
                glViewportWidth = 0
                glViewportHeight = 0
            }
            depthRangeNear = 0.0f
            depthRangeFar = 1.0f
        }

        setViewport()
        viewportCenterX = viewportWidth / 2.0f
        viewportCenterY = viewportHeight / 2.0f
        viewportOriginX = viewportX + viewportCenterX
        viewportOriginY = viewportY + viewportCenterY
    }

    override fun getClipping(clipping: IntArray) {
        clipping[0] = cLeft
        clipping[1] = cTop
        clipping[2] = cRight
        clipping[3] = cBottom
    }

    override fun expandClipping(x: Int, y: Int, width: Int, height: Int) {
        var x = x
        var y = y
        var width = width
        var height = height
        val displayHeight: Int
        val displayWidth: Int
        if (display != null) {
            displayWidth = display!!.getWidth()
            displayHeight = display!!.getHeight()
        } else {
            displayHeight = 0
            displayWidth = 0
        }

        x = x.coerceAtLeast(0)
        width = width.coerceAtMost(displayWidth)
        y = y.coerceAtLeast(0)
        height = height.coerceAtMost(displayHeight)
        var changed = false
        if (cLeft < x) {
            cLeft = x
            changed = true
        }

        if (cRight > width) {
            cRight = width
            changed = true
        }

        if (cTop < y) {
            cTop = y
            changed = true
        }

        if (cBottom > height) {
            cBottom = height
            changed = true
        }

        if (changed) {
            if (!boundsCalculated) {
                boundsCalculated = true
                toggleScissorTest()
            }
            setScissorRect()
        }
    }

    fun resetViewportOffsets() {
        viewportOffsetX = 0
        viewportOffsetY = 0
        setViewport()
        setScissorRect()
    }

    internal abstract fun setViewport()

    internal abstract fun setScissorRect()

    internal abstract fun toggleScissorTest()

    abstract fun drawPrimitive(drawMode: PrimitiveDrawMode, first: Int, primitiveCount: Int)

    fun updateFrustumMatrix() {
        transformMatrix.copyFrom(particleMatrix)
        transformMatrix.multiplyBy(currentModelMatrix)
        transformMatrix.extractNearPlane(frustumPlanes[0])
        transformMatrix.extractFarPlane(frustumPlanes[1])
        transformMatrix.extractLeftPlane(frustumPlanes[2])
        transformMatrix.extractRightPlane(frustumPlanes[3])
        transformMatrix.extractBottomPlane(frustumPlanes[4])
        transformMatrix.extractTopPlane(frustumPlanes[5])
        frustumClipMatrix.copyFrom(transformMatrix)
        loadWorldViewProjectionMatrix(frustumClipMatrix)
    }

    override fun copyModelMatrix(): Matrix44 {
        return currentModelMatrix
    }

    fun requestViewPositionUpdate() {
        viewPositionCalibrated = false
    }

    fun getModelViewMatrix(): Matrix44 {
        return if (matrixMode == MatrixMode.MODEL_VIEW) {
            if (!viewPositionCalibrated) {
                calibrateViewPosition()
            }
            modelViewMatrix
        } else {
            modelMatrix
        }
    }

    fun getFrustumClipMatrix(): Matrix44 {
        return frustumClipMatrix
    }

    fun loadWorldViewProjectionMatrix() {
        if (!isOrthoProjectionValid) {
            val height: Int
            val width: Int
            if (display != null) {
                width = display!!.getWidth()
                height = display!!.getHeight()
            } else {
                height = 0
                width = 0
            }

            val matrix443 = screenOrthoMatrix
            if (width != 0 && height != 0) {
                matrix443.setOrtho(0.0f, width.toFloat(), 0.0f, height.toFloat(), -1.0f, 1.0f)
            } else {
                matrix443.identity()
            }

            worldViewProjectionMatrix.copyFrom(matrix443)
            loadWorldViewProjectionMatrix(worldViewProjectionMatrix)
            isOrthoProjectionValid = true
        }
    }

    abstract fun loadWorldViewProjectionMatrix(cacheInto: Matrix44)

    fun setOrthoMatrixMode() {
        if (matrixMode != MatrixMode.ORTHO) {
            val lastMode = matrixMode
            matrixMode = MatrixMode.ORTHO
            if (lastMode == MatrixMode.MODEL_VIEW) {
                requestViewPositionUpdate()
            }

            activeProjectionSource = orthoProjectionMatrix
            calculateViewRange()
            calculateViewport()
            nativeFunctionMask = nativeFunctionMask and -16
        }
    }

    fun setProjectionMatrixMode() {
        if (matrixMode != MatrixMode.PROJECTION) {
            val lastMode = matrixMode
            matrixMode = MatrixMode.PROJECTION
            if (lastMode == MatrixMode.MODEL_VIEW) {
                requestViewPositionUpdate()
            }

            loadWorldViewProjectionMatrix()
            activeProjectionSource = screenOrthoMatrix
            calculateViewRange()
            calculateViewport()
            nativeFunctionMask = nativeFunctionMask and -9
        }
    }

    fun invalidateOrthoProjection() {
        isOrthoProjectionValid = false
        if (matrixMode == MatrixMode.PROJECTION) {
            loadWorldViewProjectionMatrix()
            calculateViewRange()
        }
    }

    fun setModelViewMatrixMode() {
        if (matrixMode != MatrixMode.MODEL_VIEW) {
            matrixMode = MatrixMode.MODEL_VIEW
            requestViewPositionUpdate()
            markModelViewValid()
            activeProjectionSource = currentModelMatrix
            calculateViewRange()
            calculateViewport()
            nativeFunctionMask = nativeFunctionMask and -8
        }
    }

    fun invalidateModelView() {
        isModelViewValid = false
        markModelViewValid()
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            calculateViewRange()
        }
    }

    fun markModelViewValid() {
        if (!isModelViewValid) {
            isModelViewValid = true
        }
    }

    override fun setDepthBuffering(enabled: Boolean) {
        depthBufferingEnabled = enabled
        setDepthRenderState()
    }

    fun toggleDepthTest(depthTest: Boolean) {
        if (enableDepthTest != depthTest) {
            enableDepthTest = depthTest
            toggleDepthTest()
            nativeFunctionMask = nativeFunctionMask and -16
        }
    }

    fun setDepthMaskEnabled(enabled: Boolean) {
        if (depthMaskEnabled != enabled) {
            depthMaskEnabled = enabled
            setDepthRenderState()
            nativeFunctionMask = nativeFunctionMask and -16
        }
    }

    fun initializeQuadStream() {
        quadVertexStream = createStream(false)
        val vertexBufferSize: Short = 160
        quadVertexStream!!.create(vertexBufferSize.toInt(), 32)
        directBuffer.clear()
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putInt(-1)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putInt(-1)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putInt(-1)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putInt(-1)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(1.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putInt(-1)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        quadVertexStream!!.upload(0, directBuffer.position(), directBufferAddress)
        quadVertexDeclaration = createVertexDeclaration(arrayOf(VertexDeclarationComponent(arrayOf(VertexDeclarationAction.POSITION, VertexDeclarationAction.COLOR, VertexDeclarationAction.TEXCOORD, VertexDeclarationAction.TEXCOORD))))
    }

    override fun calculateProjection(worldX: Float, worldY: Float, worldZ: Float, screenCoords: FloatArray) {
        val clipW = transformMatrix.elements[15] + transformMatrix.elements[3] * worldX + transformMatrix.elements[7] * worldY + transformMatrix.elements[11] * worldZ
        val clipX = transformMatrix.elements[12] + transformMatrix.elements[0] * worldX + transformMatrix.elements[4] * worldY + transformMatrix.elements[8] * worldZ
        val clipY = transformMatrix.elements[13] + transformMatrix.elements[1] * worldX + transformMatrix.elements[5] * worldY + transformMatrix.elements[9] * worldZ
        val viewZ = particleMatrix.elements[14] + particleMatrix.elements[2] * worldX + particleMatrix.elements[6] * worldY + particleMatrix.elements[10] * worldZ
        screenCoords[0] = viewportOriginX + viewportCenterX * clipX / clipW
        screenCoords[1] = viewportOriginY + viewportCenterY * clipY / clipW
        screenCoords[2] = viewZ
    }

    override fun computeWorldToScreen(x: Float, y: Float, z: Float, screenCoords: FloatArray) {
        val wX = transformMatrix.elements[14] + transformMatrix.elements[2] * x + transformMatrix.elements[6] * y + transformMatrix.elements[10] * z
        val wY = transformMatrix.elements[15] + transformMatrix.elements[3] * x + transformMatrix.elements[7] * y + transformMatrix.elements[11] * z
        if (wX >= -wY && wX <= wY) {
            val wZ = transformMatrix.elements[12] + transformMatrix.elements[0] * x + transformMatrix.elements[4] * y + transformMatrix.elements[8] * z
            if (wZ >= -wY && wZ <= wY) {
                val clipY = transformMatrix.elements[13] + transformMatrix.elements[1] * x + transformMatrix.elements[5] * y + transformMatrix.elements[9] * z
                if (clipY >= -wY && clipY <= wY) {
                    val screenZ = particleMatrix.elements[14] + particleMatrix.elements[2] * x + particleMatrix.elements[6] * y + particleMatrix.elements[10] * z
                    screenCoords[0] = viewportOriginX + viewportCenterX * wZ / wY
                    screenCoords[1] = viewportOriginY + viewportCenterY * clipY / wY
                    screenCoords[2] = screenZ
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

    fun toggleLights(enabled: Boolean) {
        if (lightingEnabled != enabled) {
            lightingEnabled = enabled
            toggleLights()
            nativeFunctionMask = nativeFunctionMask and -8
        }
    }

    override fun copyLights(copyLength: Int, lightNodesSrc: Array<PointLight>?) {
        if (lightSources == null || lightNodesSrc == null) return
        if (copyLength >= 0) System.arraycopy(lightNodesSrc, 0, lightSources!!, 0, copyLength)

        lastSceneLightCount = copyLength
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            updateSceneLights()
        }
    }

    override fun updateSun(intensity: Float) {
        if (lightIntensity != intensity) {
            lightIntensity = intensity
            setFogColorAndIntensity()
            enableLights()
        }
    }

    override fun applyLighting(color: Int, intensity: Float, antiIntensity: Float, positionX: Float, positionY: Float, positionZ: Float) {
        val lightChanged = baseLightColor != color
        if (lightChanged || sunColorIntensity != intensity || antiSunColorIntensity != antiIntensity) {
            baseLightColor = color
            sunColorIntensity = intensity
            antiSunColorIntensity = antiIntensity
            if (lightChanged) {
                baseLightColorRed = (baseLightColor and 0xff0000) / 1.671168E7f
                baseLightColorGreen = (baseLightColor and 0xff00) / 65280.0f
                baseLightColorBlue = (baseLightColor and 0xff) / 255.0f
                setFogColorAndIntensity()
            }
            setSunParameters()
        }
        if (baseLightPosition[0] != positionX || baseLightPosition[1] != positionY || baseLightPosition[2] != positionZ) {
            baseLightPosition[0] = positionX
            baseLightPosition[1] = positionY
            baseLightPosition[2] = positionZ
            baseLightInvertedPosition[0] = -positionX
            baseLightInvertedPosition[1] = -positionY
            baseLightInvertedPosition[2] = -positionZ
            val size = (1.0 / sqrt((positionX * positionX + positionY * positionY + positionZ * positionZ).toDouble())).toFloat()
            sunPosition[0] = positionX * size
            sunPosition[1] = positionY * size
            sunPosition[2] = positionZ * size
            lightPosition[0] = -sunPosition[0]
            lightPosition[1] = -sunPosition[1]
            lightPosition[2] = -sunPosition[2]
            setLightPosition()
            shadowOffsetX = (positionX * 256.0f / positionY).toInt()
            shadowOffsetZ = (positionZ * 256.0f / positionY).toInt()
        }
        enableLights()
    }

    override fun mismatchingFunctionMask(maskA: Int, maskB: Int): Int {
        return maskA and maskB xor maskB
    }

    fun setColourType(combineMode: Int) {
        when (combineMode) {
            0 -> setTextureCombineModes(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE)
            1 -> setTextureCombineModes(TextureCombineMode.REPLACE, TextureCombineMode.REPLACE)
            2 -> setTextureCombineModes(TextureCombineMode.INTERPOLATE, TextureCombineMode.REPLACE)
            3 -> setTextureCombineModes(TextureCombineMode.ADD, TextureCombineMode.MODULATE)
            4 -> setTextureCombineModes(TextureCombineMode.SUBTRACT, TextureCombineMode.SUBTRACT)
        }
    }

    internal abstract fun setFogColorAndIntensity()

    internal abstract fun setSunParameters()

    internal abstract fun setLightPosition()

    internal abstract fun enableLights()

    override fun getMaxLights(): Int {
        return maxActiveLights - 2
    }

    internal abstract fun toggleSun()

    internal abstract fun configureLights()

    abstract fun supportsTextureFormat(format: GLFormat, actionFormat: GLActionFormat): Boolean

    abstract fun supports3DTextureFormat(format: GLFormat, actionFormat: GLActionFormat): Boolean

    internal abstract fun toggleDepthTest()

    fun createTextureSampler(format: GLFormat, width: Int, height: Int, mipmapped: Boolean, pixels: ByteArray?): NativeTextureSampler {
        return createNativeTextureSampler(format, width, height, mipmapped, pixels)
    }

    fun createHdrTexture(format: GLFormat, width: Int, length: Int, pixels: FloatArray): NativeTextureSampler {
        return createNativeTexture(format, width, length, pixels)
    }

    fun resetCurrentTextureMatrix() {
        if (textureStates!![currentTextureSlot] != TextureState.INITIALIZE) {
            textureStates!![currentTextureSlot] = TextureState.INITIALIZE
            textureMatrices!![currentTextureSlot].identity()
            applyTextureMatrix()
        }
    }

    internal abstract fun createNativeTexture(format: GLFormat, width: Int, length: Int, pixels: FloatArray): NativeTextureSampler

    override fun copySunPosition(): Matrix43 {
        return viewMatrix43
    }

    abstract fun create3DTexture(format: GLFormat, data: ByteArray): Texture3DSampler

    override fun resetTextureState() {
    }

    fun getCurrentTextureSlot(): Int {
        return currentTextureSlot
    }

    override fun drawTransparentHorizontalLine(x: Int, y: Int, width: Int, colorRgba: Int, blendMode: Int) {
        drawTransparentLine(x, y, width + x, y, colorRgba, blendMode)
    }

    abstract fun setTextureOnCurrentSlot()

    fun getEnvironmentMapSampler(): CubeTextureSampler? {
        return sunCubeTextureGenerator?.getNativeCubeTexture()
    }

    fun setTextureCombineModes(rgbMode: TextureCombineMode, alphaMode: TextureCombineMode) {
        var changed = false
        if (rgbMode != textureRgbCombineModes!![currentTextureSlot]) {
            textureRgbCombineModes!![currentTextureSlot] = rgbMode
            setTextureColorStage()
            changed = true
        }

        if (alphaMode != textureAlphaCombineModes!![currentTextureSlot]) {
            textureAlphaCombineModes!![currentTextureSlot] = alphaMode
            setTextureAlphaStage()
            changed = true
        }

        if (changed) {
            nativeFunctionMask = nativeFunctionMask and -14
        }
    }

    fun setTextureRgbStageState(sourceIndex: Int, state: TextureStageState) {
        setTextureStageState(sourceIndex, state)
    }

    internal abstract fun setTextureStageState(sourceIndex: Int, state: TextureStageState)

    fun setTextureAlphaStageState(sourceIndex: Int, state: TextureStageState) {
        setTextureAlphaStage(sourceIndex, state)
    }

    internal abstract fun setTextureAlphaStage(sourceIndex: Int, state: TextureStageState)

    fun setColourValue(color: Int) {
        if (envColor != color) {
            envColor = color
            setTextureEnvironmentColor()
        }
    }

    fun getCurrentTextureMatrix(): Matrix44 {
        return textureMatrices!![currentTextureSlot]
    }

    fun setTextureState(textureState: TextureState) {
        textureStates!![currentTextureSlot] = textureState
        applyTextureMatrix()
    }

    fun resetAllRenderState() {
        setFogColorAndIntensity()
        setSunParameters()
        toggleLights()
        updateSceneLights()
        setLightPosition()
        enableLights()
        toggleSun()
        toggleDepthTest()
        setDepthRenderState()
        toggleFog()
        configureFog()
        toggleBlending()
        blendDrawBuffer()
        toggleAlpha()

        for (slot in maxConcurrentTextures - 1 downTo 0) {
            setTextureSlot(slot)
            setTextureColorStage()
            setTextureAlphaStage()
            resetCurrentTextureMatrix()
        }

        setTextureEnvironmentColor()
        setViewport()
    }

    internal abstract fun activateCurrentTexture()

    internal abstract fun setTextureColorStage()

    internal abstract fun setTextureAlphaStage()

    override fun addWater(waterTile: WaterTile) {
        renderedWaterColor = -1
        renderedWaterTile = waterTile
        missingWaterColor = true
    }

    fun resetAlphaBlendMode() {
        if (alphaBlendMode != 0) {
            alphaBlendMode = 0
            blendDrawBuffer()
        }
    }

    abstract fun colorMask(flags: Int)

    override fun resetClipping() {
        if (display != null) {
            cTop = 0
            cLeft = 0
            cRight = display!!.getWidth()
            cBottom = display!!.getHeight()
        } else {
            cBottom = 0
            cRight = 0
            cTop = 0
            cLeft = 0
        }

        if (boundsCalculated) {
            boundsCalculated = false
            toggleScissorTest()
        }
    }

    internal abstract fun setTextureEnvironmentColor()

    fun toggleFog(enabled: Boolean) {
        if (fogEnabled != enabled) {
            fogEnabled = enabled
            toggleFog()
            nativeFunctionMask = nativeFunctionMask and -16
        }
    }

    override fun calculateFog(color: Int, distance: Int, offset: Int) {
        if (fogColorRgb != color || distance != fogDistance || offset != fogOffset) {
            fogColorRgb = color
            fogDistance = distance
            fogOffset = offset
            updateFogAndWater()
            toggleFog()
        }
    }

    override fun setSunPosition(viewMatrix: Matrix43) {
        viewMatrix43 = viewMatrix
        particleMatrix.fromMatrix(viewMatrix43)
        inverseSunMatrix43.copyFrom(viewMatrix)
        inverseSunMatrix43.updateIdentitiyMatrix()
        inverseSunMatrix44.fromMatrix(inverseSunMatrix43)
        updateFrustumMatrix()
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            requestViewPositionUpdate()
        }
    }

    internal abstract fun toggleFog()

    internal abstract fun toggleMultisampling(enabled: Boolean)

    abstract fun createIndexBuffer(dynamic: Boolean): NativeToolkitBuffer

    abstract fun createStream(dynamic: Boolean): NativeStream

    abstract fun createVertexDeclaration(components: Array<VertexDeclarationComponent>): VertexDeclaration

    abstract fun setVertexDeclaration(declaration: VertexDeclaration)

    override fun clearRectangle(surface: ComponentSurface, clipLeft: Int, clipTop: Int) {
        setDepthBuffering(false)
        blackPixelSprite!!.drawAffineMasked(0.0f, 0.0f, display!!.getWidth().toFloat(), 0.0f, 0.0f, display!!.getHeight().toFloat(), 0, surface, clipLeft, clipTop)
        setDepthBuffering(true)
    }

    abstract fun bindIndexBuffer(buffer: NativeToolkitBuffer)

    fun getEyePositionOsMatrix(): Matrix44 {
        return if (matrixMode == MatrixMode.MODEL_VIEW) inverseSunMatrix44 else IDENTITY_MATRIX
    }

    override fun drawRectangleOutline(x: Int, y: Int, width: Int, height: Int, color: Int, alpha: Int) {
        drawTransparentLine(x, y, width + x, y, color, alpha)
        drawTransparentLine(x, y + height, width + x, y + height, color, alpha)
        drawTransparentLine(x, y, x, y + height, color, alpha)
        drawTransparentLine(width + x, y, width + x, y + height, color, alpha)
    }

    override fun renderWorldmapTile(startX: Int, startY: Int, width: Int, height: Int, colorArgb: Int, textureEnvColor: Int, pixels: ByteArray, pixelOffset: Int, blendMode: Int) {
    }

    override fun benchmark() {
    }

    override fun drawCircle(x: Int, y: Int, radius: Int, colorArgb: Int) {
    }

    override fun drawTransparentLine(startX: Int, startY: Int, endX: Int, endY: Int, color: Int, alpha: Int) {
        drawThickLine(startX, startY, endX, endY, color, 1, alpha)
    }

    override fun drawLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, lineSpacing: Int, lineLength: Int, offset: Int) {
        var deltaX = (endX - startX).toFloat()
        var deltaY = (endY - startY).toFloat()
        if (deltaX == 0.0f && deltaY == 0.0f) {
            deltaX = 1.0f
        } else {
            val invLength = (1.0 / sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
            deltaX *= invLength
            deltaY *= invLength
        }

        setProjectionMatrixMode()
        val lineShader = waterShadersByEffect[13]!!
        lineShader.activate()
        lineShader.prepareForRendering()
        toggleBlending(1)
        lineShader.loadDefaultModelMatrix()
        toggleMultisampling(false)
        var offset = offset % (lineLength + lineSpacing)
        val dashDeltaX = deltaX * lineSpacing
        val dashDeltaY = deltaY * lineSpacing
        var offsetX = 0.0f
        var offsetY = 0.0f
        var segDeltaX = dashDeltaX
        var segDeltaY = dashDeltaY
        if (offset > lineSpacing) {
            offsetX = deltaX * (lineLength + lineSpacing - offset)
            offsetY = deltaY * (lineLength + lineSpacing - offset)
        } else {
            segDeltaX = deltaX * (lineSpacing - offset)
            segDeltaY = deltaY * (lineSpacing - offset)
        }

        var currentX = startX + offsetX
        var currentY = startY + offsetY
        val gapDeltaX = deltaX * lineLength
        val gapDeltaY = deltaY * lineLength

        while (true) {
            if (endX > startX) {
                if (currentX > endX) {
                    break
                }
                if (currentX + segDeltaX > endX) {
                    segDeltaX = endX - currentX
                }
            } else {
                if (currentX < endX) {
                    break
                }
                if (currentX + segDeltaX < endX) {
                    segDeltaX = endX - currentX
                }
            }

            if (endY > startY) {
                if (currentY > endY) {
                    break
                }
                if (currentY + segDeltaY > endY) {
                    segDeltaY = endY - currentY
                }
            } else {
                if (currentY < endY) {
                    break
                }
                if (currentY + segDeltaY < endY) {
                    segDeltaY = endY - currentY
                }
            }

            if (!uploadLineVertices(currentX, currentY, currentX + segDeltaX, currentY + segDeltaY)) {
                return
            }

            drawPrimitiveLines()
            currentX += gapDeltaX + segDeltaX
            currentY += gapDeltaY + segDeltaY
            segDeltaX = dashDeltaX
            segDeltaY = dashDeltaY
        }

        toggleMultisampling(true)
        lineShader.deactivate()
    }

    override fun drawTexturedLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, surface: ComponentSurface, clipLeft: Int, clipTop: Int, lineSpacing: Int, lineLength: Int, offset: Int) {
    }

    override fun drawThickLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, lineWidth: Int, blendMode: Int) {
        var startX = (startX + 1.0f).toInt()
        var startY = (startY + 1.0f).toInt()
        var endX = (endX + 1.0f).toInt()
        var endY = (endY + 1.0f).toInt()
        var dirX = (endX - startX).toFloat()
        var dirY = (endY - startY).toFloat()
        val invLength = 1.0f / sqrt((dirX * dirX + dirY * dirY).toDouble()).toFloat()
        dirX *= invLength
        dirY *= invLength
        startX = (startX - dirX).toInt()
        startY = (startY - dirY).toInt()
        var perpX = -dirY
        perpX *= 0.5f * lineWidth
        val perpY = dirX * 0.5f * lineWidth
        whitePixelSprite!!.drawAffineTransformed(startX - perpX, startY - perpY, endX - perpX, endY - perpY, startX + perpX, startY + perpY, 0, colorArgb, blendMode)
    }

    override fun renderParticles(particleWrapper: ParticleWrapper) {
        particleRenderer!!.render(this, particleWrapper)
    }

    fun drawPrimitiveLines() {
        setStreamSource(0, vertexStream!!)
        setVertexDeclaration(vertexDeclaration!!)
        drawPrimitive(PrimitiveDrawMode.LINES, 0, 1)
    }

    fun drawQuadPrimitive(drawMode: PrimitiveDrawMode) {
        setStreamSource(0, quadVertexStream!!)
        setVertexDeclaration(quadVertexDeclaration!!)
        drawPrimitive(drawMode, 0, 2)
    }

    abstract fun drawIndexedPrimitive(drawMode: PrimitiveDrawMode, startVertex: Int, vertexCount: Int, startIndex: Int, primitiveCount: Int)

    fun setupUiRenderState() {
        if (nativeFunctionMask != 2) {
            setProjectionMatrixMode()
            toggleFog(false)
            toggleLights(false)
            toggleDepthTest(false)
            setDepthMaskEnabled(false)
            nativeFunctionMask = 2
        }
    }

    fun setupSceneRenderState() {
        if (nativeFunctionMask != 8) {
            setModelViewMatrixMode()
            toggleFog(true)
            toggleDepthTest(true)
            setDepthMaskEnabled(true)
            toggleBlending(1)
            nativeFunctionMask = 8
        }
    }

    override fun setModelMatrix(matrix441: Matrix44) {
        currentModelMatrix.copyFrom(matrix441)
        updateFrustumMatrix()
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            calculateViewRange()
        }
    }

    fun initializeLineStream() {
        vertexStream = createStream(true)
        vertexStream!!.create(24, 12)
        vertexDeclaration = createVertexDeclaration(arrayOf(VertexDeclarationComponent(VertexDeclarationAction.POSITION)))
    }

    internal abstract fun configureFog()

    override fun hasHardwareRendering(): Boolean {
        return true
    }

    override fun createCubeTexture(materialAtPositiveX: Int, materialAtNegativeX: Int, materialAtPositiveY: Int, materialAtNegativeY: Int, materialAtPositiveZ: Int, materialAtNegativeZ: Int): AbstractCubeTextureGenerator {
        return NativeCubeTextureGenerator(this, materialAtPositiveX, materialAtNegativeX, materialAtPositiveY, materialAtNegativeY, materialAtPositiveZ, materialAtNegativeZ)
    }

    fun createTexture(width: Int, length: Int, mipmapped: Boolean, pixels: IntArray): NativeTextureSampler {
        return createTextureSampler(width, length, mipmapped, pixels, 0, 0)
    }

    override fun resetDepthClippingMask() {
        depthRangeNearOverride = 0.0f
        depthRangeFarOverride = 1.0f
        calculateViewport()
    }

    override fun generateFlags() {
        var prime = 32
        shadowSizeShift = 0
        while (prime > 1) {
            shadowSizeShift++
            prime = prime shr 1
        }
        activeFlags = 1 shl shadowSizeShift
    }

    override fun getScratchMatrix(): Matrix44 {
        return scratchMatrix
    }

    fun calculateViewRange() {
        projectionMatrix.copyFrom(activeProjectionSource)
        loadWorldViewProjectionMatrix(projectionMatrix)
        camFar = (projectionMatrix.elements[14] - projectionMatrix.elements[15]) / (projectionMatrix.elements[11] - projectionMatrix.elements[10])
        camNear = -projectionMatrix.elements[14] / projectionMatrix.elements[10]
        updateFogAndWater()
    }

    override fun createFontRenderer(fontMetrics: FontMetrics?, spriteTypes: Array<SpriteType>?, isPlain: Boolean): AbstractFontRenderer {
        return NativeFont(this, fontMetrics!!, spriteTypes!!, isPlain)
    }

    abstract fun generateNativeCubeTexture(largestSize: Int, mipmappingEnabled: Boolean, pixels: Array<IntArray>): CubeTextureSampler

    fun setTextureSampler(textureSampler: TextureSampler?) {
        if (textureSampler != textureSamplers!![currentTextureSlot]) {
            textureSamplers!![currentTextureSlot] = textureSampler
            if (textureSampler != null) {
                textureSampler.bindTexture()
            } else {
                activeTextureSampler!!.bindTexture()
            }
            nativeFunctionMask = nativeFunctionMask and -2
        }
    }

    fun getModelMatrix(): Matrix44 {
        return modelMatrix
    }

    override fun setClipping(left: Int, top: Int, right: Int, bottom: Int) {
        val displayHeight: Int
        val displayWidth: Int
        if (display != null) {
            displayWidth = display!!.getWidth()
            displayHeight = display!!.getHeight()
        } else {
            displayHeight = 0
            displayWidth = 0
        }

        if (left <= 0 && right >= displayWidth - 1 && top <= 0 && bottom >= displayHeight - 1) {
            resetClipping()
        } else {
            cLeft = 0.coerceAtLeast(left)
            cRight = right.coerceAtMost(displayWidth)
            cTop = 0.coerceAtLeast(top)
            cBottom = bottom.coerceAtMost(displayHeight)
            if (!boundsCalculated) {
                boundsCalculated = true
                toggleScissorTest()
            }
            setScissorRect()
        }
    }

    override fun hasShadows(): Boolean {
        return true
    }

    internal abstract fun setDepthRenderState()

    override fun beginBloomCapture(screenWidth: Int, screenHeight: Int) {
        bloomRenderer!!.beginCapture(screenWidth, screenHeight)
    }

    fun updateFogAndWater() {
        activeWaterShader?.updateWaterState()
        configureFog()
    }

    abstract fun createRectangleTexture(format: GLFormat, actionFormat: GLActionFormat, width: Int, height: Int): RectangleTextureSampler

    abstract fun createNativeTexture2D(format: GLFormat, actionFormat: GLActionFormat, width: Int, height: Int): NativeTextureSampler

    fun fetchShader(archiveName: String, fileName: String): ByteArray? {
        return shaderResource!!.getNamedFile(archiveName, fileName)
    }

    override fun addWaterTile(color: Int, waterTile: WaterTile) {
        if (!missingWaterColor) {
            throw RuntimeException("")
        }
        renderedWaterColor = color
        renderedWaterTile = waterTile
        if (hasWaterShaderEffect) {
            waterShadersByEffect[3]!!.beginWaterTile()
            waterShadersByEffect[3]!!.updateWaterState()
        }
    }

    abstract fun createTextureSampler(width: Int, height: Int, mipmapped: Boolean, pixels: IntArray, borderWidth: Int, borderHeight: Int): NativeTextureSampler

    abstract fun setPolygonMode(mode: Int)

    override fun drawSurfaceLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, surface: ComponentSurface, clipLeft: Int, clipTop: Int) {
    }

    override fun onTilesDrawn() {
        missingWaterColor = false
    }

    override fun isOnScreen(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): Int {
        var frustumFlags = 0
        val minNdcZ = transformMatrix.elements[14] + transformMatrix.elements[2] * minX + transformMatrix.elements[6] * minY + transformMatrix.elements[10] * minZ
        val maxNdcZ = transformMatrix.elements[14] + transformMatrix.elements[2] * maxX + transformMatrix.elements[6] * maxY + transformMatrix.elements[10] * maxZ
        val minClipW = transformMatrix.elements[15] + transformMatrix.elements[3] * minX + transformMatrix.elements[7] * minY + transformMatrix.elements[11] * minZ
        val maxClipW = transformMatrix.elements[15] + transformMatrix.elements[3] * maxX + transformMatrix.elements[7] * maxY + transformMatrix.elements[11] * maxZ
        if (minNdcZ < -minClipW && maxNdcZ < -maxClipW) {
            frustumFlags = frustumFlags or 0x10
        } else if (minNdcZ > minClipW && maxNdcZ > maxClipW) {
            frustumFlags = frustumFlags or 0x20
        }

        val minClipX = transformMatrix.elements[12] + transformMatrix.elements[0] * minX + transformMatrix.elements[4] * minY + transformMatrix.elements[8] * minZ
        val maxClipX = transformMatrix.elements[12] + transformMatrix.elements[0] * maxX + transformMatrix.elements[4] * maxY + transformMatrix.elements[8] * maxZ
        if (minClipX < -minClipW && maxClipX < -maxClipW) {
            frustumFlags = frustumFlags or 0x1
        }

        if (minClipX > minClipW && maxClipX > maxClipW) {
            frustumFlags = frustumFlags or 0x2
        }

        val minClipY = transformMatrix.elements[13] + transformMatrix.elements[1] * minX + transformMatrix.elements[5] * minY + transformMatrix.elements[9] * minZ
        val maxClipY = transformMatrix.elements[13] + transformMatrix.elements[1] * maxX + transformMatrix.elements[5] * maxY + transformMatrix.elements[9] * maxZ
        if (minClipY < -minClipW && maxClipY < -maxClipW) {
            frustumFlags = frustumFlags or 0x4
        }

        if (minClipY > minClipW && maxClipY > maxClipW) {
            frustumFlags = frustumFlags or 0x8
        }

        return frustumFlags
    }

    fun getViewMatrix(): Matrix44 {
        return if (matrixMode == MatrixMode.MODEL_VIEW) particleMatrix else IDENTITY_MATRIX
    }

    override fun hasTextures(): Boolean {
        return true
    }

    override fun initialize() {
    }

    fun calibrateViewPosition(): Matrix43 {
        return if (matrixMode == MatrixMode.MODEL_VIEW) {
            if (!viewPositionCalibrated) {
                // Combines model matrix and view matrix into modelview matrix?
                combinedModelViewMatrix43.copyFrom(localModelMatrix43)
                combinedModelViewMatrix43.multiply(viewMatrix43)
                modelViewMatrix.fromMatrix(combinedModelViewMatrix43)
                viewPositionCalibrated = true
            }
            combinedModelViewMatrix43
        } else {
            localModelMatrix43
        }
    }

    fun applyTextureMatrix() {
        setTextureMatrix()
    }

    internal abstract fun createNativeTextureSampler(format: GLFormat, width: Int, height: Int, mipmapped: Boolean, pixels: ByteArray?): NativeTextureSampler

    override fun supportsFullscreen(): Boolean {
        return true
    }

    override fun generateEmptySprite(width: Int, height: Int, transparent: Boolean, mipmapped: Boolean): NativeSprite {
        throw UnsupportedOperationException("generateEmptySprite not implemented for this toolkit")
    }

    protected abstract fun createModel(vertexCount: Int, triangleCount: Int, hasNormals: Boolean, hasTexCoords: Boolean): Model

    protected open fun createNativeSprite(sampler: TextureSampler): NativeSprite {
        throw UnsupportedOperationException("createNativeSprite with TextureSampler not implemented for this toolkit")
    }

    fun initializeTables() {
        textureSamplers = arrayOfNulls(maxConcurrentTextures)
        textureMatrices = Array(maxConcurrentTextures) { Matrix44() }
        textureStates = Array(maxConcurrentTextures) { TextureState.INITIALIZE }
        textureRgbCombineModes = Array(maxConcurrentTextures) { TextureCombineMode.MODULATE }
        textureAlphaCombineModes = Array(maxConcurrentTextures) { TextureCombineMode.MODULATE }

        lightSources = arrayOfNulls(maxActiveLights - 2)

        val defaultPixels = intArrayOf(-1)
        activeTextureSampler = createTextureSampler(1, 1, false, defaultPixels, 0, 0)
        defaultPixels[0] = 0xffffff.inv()
        blackTextureSampler = createTextureSampler(1, 1, false, defaultPixels, 0, 0)
        setNativeHeap(getNativeMemory(262144))
        positionTexCoordDeclaration = createVertexDeclaration(arrayOf(VertexDeclarationComponent(arrayOf(VertexDeclarationAction.POSITION, VertexDeclarationAction.TEXCOORD))))
        createVertexDeclaration(arrayOf(VertexDeclarationComponent(arrayOf(VertexDeclarationAction.POSITION, VertexDeclarationAction.COLOR))))
        positionColorTexCoordNormalDeclaration = createVertexDeclaration(arrayOf(
            VertexDeclarationComponent(VertexDeclarationAction.POSITION),
            VertexDeclarationComponent(VertexDeclarationAction.COLOR),
            VertexDeclarationComponent(VertexDeclarationAction.TEXCOORD),
            VertexDeclarationComponent(VertexDeclarationAction.NORMAL_POINTER_ACTION)
        ))
        positionColorTexCoordDeclaration = createVertexDeclaration(arrayOf(
            VertexDeclarationComponent(VertexDeclarationAction.POSITION),
            VertexDeclarationComponent(VertexDeclarationAction.COLOR),
            VertexDeclarationComponent(VertexDeclarationAction.TEXCOORD)
        ))

        // Initialize copy buffers for model operations
        // Some toolkits may not support createModel, in which case models are created on-demand
        try {
            for (type in 0 until 7) {
                copyByTypeSrc[type] = createModel(0, 0, false, false)
                copyByTypeDst[type] = createModel(0, 0, true, true)
            }
        } catch (_: UnsupportedOperationException) {
            // copyByTypeSrc/copyByTypeDst remain null - models will be created on-demand
        }

        initializeShaders()
        indexBuffer = createIndexBuffer(true)
        setupGraphics()
        getImageProducer()
        resetClipping()
        renderLastFrame()
        whitePixelSprite = createNativeSprite(activeTextureSampler!!)
        blackPixelSprite = createNativeSprite(blackTextureSampler!!)
        applyClearFlags(3, 0)
    }

    fun drawQuadTriangleFan() {
        drawQuadPrimitive(PrimitiveDrawMode.TRIANGLE_FAN)
    }

    abstract fun setStreamSource(slot: Int, stream: NativeStream)

    fun setTextureSlot(textureSlot: Int) {
        if (currentTextureSlot != textureSlot) {
            currentTextureSlot = textureSlot
            activateCurrentTexture()
        }
    }

    fun initializeCircleStream() {
        circleVertexStream = createStream(false)
        circleVertexStream!!.create(3096, 12)
        directBuffer.clear()
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)

        for (segment in 0..256) {
            val angle = (segment * 2) * Math.PI / 256.0
            val cosValue = Math.cos(angle).toFloat()
            val sinValue = Math.sin(angle).toFloat()
            directBuffer.putFloat(sinValue)
            directBuffer.putFloat(cosValue)
            directBuffer.putFloat(0.0f)
        }

        circleVertexStream!!.upload(0, directBuffer.position(), directBufferAddress)
    }

    override fun drawFilledRectangle(x: Int, y: Int, width: Int, height: Int, color: Int, alpha: Int) {
        if (depthBufferingEnabled) {
            setDepthBuffering(false)
            whitePixelSprite!!.drawCircle(x, y, width, height, 0, color, alpha)
            setDepthBuffering(true)
        } else {
            whitePixelSprite!!.drawCircle(x, y, width, height, 0, color, alpha)
        }
    }

    override fun drawTransparentVerticalLine(x: Int, y: Int, height: Int, colorArgb: Int, blendMode: Int) {
        drawTransparentLine(x, y, x, height + y, colorArgb, blendMode)
    }

    override fun compositeBloomEffects() {
        bloomRenderer!!.endCapture()
    }

    internal abstract fun toggleLights()

    internal abstract fun setTextureMatrix()

    internal abstract fun toggleAlpha()

    companion object {
        @JvmField
        val IDENTITY_MATRIX: Matrix44 = Matrix44()

        @JvmField
        val WORLD_VIEW_PROJECTION_MATRIX: Matrix44 = Matrix44()

        @JvmStatic
        fun drawLoadingPleaseWaitHardware() {
            GraphicsCache.resetWorldmapCaches(false)
            if (AbstractToolkit.activeRenderMode >= 0 && AbstractToolkit.activeRenderMode != 0) {
                Client.loadAssets(AbstractToolkit.activeRenderMode, false)
                AbstractToolkit.activeRenderMode = -1
            }
        }
    }
}
