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
import com.jagex.game.runetek5.unidentified.Class455_Sub1
import com.jagex.game.runetek5.unidentified.NativeBloomRenderer
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
    i_3: Int,
    i_4: Int
) : AbstractToolkit(materials) {

    @JvmField var texturesSizeOccupied: Int = 0
    @JvmField var textureBuffersSizeOccupied: Int = 0
    @JvmField var GLEXTFramebufferObject: Boolean = false
    @JvmField var maxConcurrentTextures: Int = 0
    @JvmField val particleMatrix: Matrix44 = Matrix44()
    @JvmField val projectionMatrix: Matrix44 = Matrix44()
    @JvmField val aClass3848729: Matrix44 = Matrix44()
    @JvmField val sunPosition: FloatArray = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f)
    @JvmField var baseLightColorRed: Float = 1.0f
    @JvmField var baseLightColorGreen: Float = 1.0f
    @JvmField var baseLightColorBlue: Float = 1.0f
    @JvmField var sunColorIntensity: Float = -1.0f
    @JvmField var anInt8709: Int = 0
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
    @JvmField val aClass3848683: Matrix44 = Matrix44()
    @JvmField var depthRangeNear: Float = 0.0f
    @JvmField var depthRangeFar: Float = 1.0f
    @JvmField var camNear: Float = 50.0f
    @JvmField var camFar: Float = 3584.0f
    @JvmField var cTop: Int = 0
    @JvmField var cBottom: Int = 0
    @JvmField var cLeft: Int = 0
    @JvmField var cRight: Int = 0
    @JvmField var anInt8749: Int = 0
    @JvmField var anInt8776: Int = 0
    @JvmField var anInt8751: Int = 0
    @JvmField var anInt8752: Int = 0
    @JvmField var anInt8823: Int = 0
    @JvmField var anInt8754: Int = 0
    @JvmField var depthBufferingEnabled: Boolean = true
    @JvmField var aBool8756: Boolean = false
    @JvmField var enableDepthTest: Boolean = false
    @JvmField var aBool8684: Boolean = false
    @JvmField val sunEnabled: Boolean = true
    @JvmField val aBool8760: Boolean = false
    @JvmField val lightPosition: FloatArray = floatArrayOf(0.0f, 0.0f, -1.0f, 0.0f)
    @JvmField var lightIntensity: Float = 1.0f
    @JvmField var antiSunColorIntensity: Float = -1.0f
    @JvmField var currentTextureSlot: Int = 0
    @JvmField val fogSupported: Boolean = true
    @JvmField var fogColorRgb: Int = -1
    @JvmField var fogDistance: Int = -1
    @JvmField var anInt8680: Int = 0
    @JvmField var blendFuncMode: BlendFuncMode = BlendFuncMode.ALPHA_BLENDING
    @JvmField var enableBlending: Boolean = true
    @JvmField var enableAlphaTest: Boolean = true
    @JvmField var anInt8844: Int = -1
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
    @JvmField val aClass2948704: Matrix43 = Matrix43()
    @JvmField val modelMatrix: Matrix44 = Matrix44()
    @JvmField var aClass2948706: Matrix43 = Matrix43()
    @JvmField val aClass2948804: Matrix43 = Matrix43()
    @JvmField val aClass3848814: Matrix44 = Matrix44()
    @JvmField val aClass2948710: Matrix43 = Matrix43()
    @JvmField val modelViewMatrix: Matrix44 = Matrix44()
    @JvmField val unsure: Matrix43 = Matrix43()
    @JvmField val aClass3848715: Matrix44 = Matrix44()
    @JvmField val pointLightVector: Vector3 = Vector3()
    @JvmField val aFloatArray8717: FloatArray = FloatArray(4)
    @JvmField var viewPositionCalibrated: Boolean = false
    @JvmField var matrixMode: MatrixMode = MatrixMode.aClass33380
    @JvmField var aBool8720: Boolean = false
    @JvmField val aClass3848774: Matrix44 = Matrix44()
    @JvmField val worldViewProjectionMatrix: Matrix44 = Matrix44()
    @JvmField var aBool8723: Boolean = false
    @JvmField val aClass3848724: Matrix44 = Matrix44()
    @JvmField val aClass3848725: Matrix44 = Matrix44()
    @JvmField var aClass3848708: Matrix44
    @JvmField val transformMatrix: Matrix44 = Matrix44()
    @JvmField val aFloatArrayArray8730: Array<FloatArray> = Array(6) { FloatArray(4) }
    @JvmField var aFloat8737: Float = 0.0f
    @JvmField var aFloat8738: Float = 1.0f
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
    @JvmField val aBool8805: Boolean = false
    @JvmField var blendFuncType: Int = 1
    @JvmField var aBool8681: Boolean = false
    @JvmField val copyByTypeSrc: Array<Model?> = arrayOfNulls(7)
    @JvmField val copyByTypeDst: Array<Model?> = arrayOfNulls(7)
    @JvmField val aClass3848841: Matrix44 = Matrix44()
    @JvmField val aClass2948842: Matrix43 = Matrix43()
    @JvmField var shaderResource: Js5ResourceProvider? = null
    @JvmField var endiannessType: Int = 0
    @JvmField var materialCache: NativeMaterialCache? = null
    @JvmField var hslToRgb: IntArray? = null
    @JvmField var aClass498694: NativeWaterTexture? = null
    @JvmField var environmentShader: AbstractEnvironmentShader? = null
    @JvmField var spriteShader: AbstractSpriteShader? = null
    @JvmField var abstractParticleShader: AbstractParticleShader? = null
    @JvmField var nativeEvtWaterMapper: NativeEvtWaterMapper? = null
    @JvmField var nativeWaterfall: NativeWaterfall? = null
    @JvmField var aClass328714: NativeBloomRenderer? = null
    @JvmField var aClass418753: AbstractWaterShader? = null
    @JvmField var particleRenderer: NativeParticleRenderer? = null
    @JvmField var nativeFunctionMask: Int = 0
    @JvmField var shadowOffsetX: Int = 0
    @JvmField var shadowOffsetZ: Int = 0
    @JvmField var nativeHeap: NativeHeap? = null
    @JvmField var aClass152Sub28731: AbstractNativeCubeTextureGenerator? = null
    @JvmField var viewportCenterX: Float = 0.0f
    @JvmField var viewportCenterY: Float = 0.0f
    @JvmField var aFloat8797: Float = 0.0f
    @JvmField var aFloat8741: Float = 0.0f
    @JvmField var anInterface48746: NativeStream? = null
    @JvmField var aClass708786: VertexDeclaration? = null
    @JvmField var renderedWaterColor: Int = 0
    @JvmField var renderedWaterTile: WaterTile? = null
    @JvmField var missingWaterColor: Boolean = false
    @JvmField var vertexStream: NativeStream? = null
    @JvmField var vertexDeclaration: VertexDeclaration? = null
    @JvmField var textureSamplers: Array<TextureSampler?>? = null
    @JvmField var anInterface68795: NativeTextureSampler? = null
    @JvmField var aClass708832: VertexDeclaration? = null
    @JvmField var aClass708836: VertexDeclaration? = null
    @JvmField var aClass708721: VertexDeclaration? = null
    @JvmField var anInterface328834: NativeToolkitBuffer? = null
    @JvmField var aNativeSpriteSub38790: NativeSprite? = null
    @JvmField var aNativeSpriteSub38791: NativeSprite? = null
    @JvmField var anInterface48693: NativeStream? = null

    init {
        aClass3848708 = aClass3848725
        Stream()

        try {
            shaderResource = shaders
            anInt8709 = i_3
            endiannessType = i_4
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

    abstract fun setMatrixTransform(var1: Matrix44, var2: Matrix44, projectionMatrix: Matrix44)

    internal abstract fun blendDrawBuffer()

    fun method13895() {
        aClass498694 = NativeWaterTexture(this)
        enableLights = false

        try {
            environmentShader = NativeEnvironmentShaderHd(this)
            spriteShader = NativeSpriteShaderHd(this)
            abstractParticleShader = NativeParticleShaderHd(this)
            nativeEvtWaterMapper = NativeEvtWaterMapper(this, aClass498694!!)
            nativeWaterfall = NativeWaterfall(this, aClass498694!!)
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

        aClass328714 = NativeBloomRenderer(this, displayWidth, displayHeight)
        if (!enableLights) {
            bloomManager[1] = NativeFilterBloomShader(this)
            aClass328714!!.initialize(bloomManager[1]!!)
        }

        if (aClass418753 != null) {
            aClass418753!!.method885()
            aClass418753 = null
        }

        method13899()
    }

    fun getShaderByEffect(effect: Int): AbstractWaterShader? {
        return waterShadersByEffect[effect]
    }

    fun setupGraphics() {
        colorMask(7)
        method13976()
    }

    fun getRenderedWaterTile(): WaterTile? {
        return renderedWaterTile
    }

    fun method13899() {
        aClass328714!!.getFormat()

        for (i_1 in bloomManager.indices) {
            if (bloomManager[i_1] != null && bloomManager[i_1]!!.method1107()) {
                bloomManager[i_1]!!.method1099()
            }
        }

        particleRenderer = NativeParticleRenderer(this)
        method13944()
        method14009()
        method14177()
        particleRenderer!!.method1089()
    }

    fun deallocate() {
        aClass328714!!.destroy()
        anInterface48746!!.deallocate()
        vertexStream!!.deallocate()
        anInterface48693!!.deallocate()

        for (i_1 in 0 until 7) {
            // Model deallocation removed (deallocate not in base Model class)
        }

        particleRenderer!!.deallocate()
        anInterface328834!!.deallocate()
    }

    override fun createNativeSprite(i_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean): NativeSprite {
        throw UnsupportedOperationException("createNativeSprite not implemented for this toolkit")
    }

    override fun utilisesNativeHeap(): Boolean {
        return true
    }

    override fun shutdown() {
        if (!aBool8681) {
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
                    waterShadersByEffect[index]!!.method878()
                    waterShadersByEffect[index] = null
                }
            }

            for (index in bloomManager.indices) {
                if (bloomManager[index] != null) {
                    bloomManager[index]!!.method1105()
                    bloomManager[index] = null
                }
            }

            resetWeakReferences()
            animateTextures(0)
            aBool8681 = true
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

    fun method13903() {
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

    abstract fun method13905(): Float

    override fun animateTextures(cycle: Int) {
        materialCache?.processCycle()
        animatingCycle = cycle and 0x7fffffff
    }

    fun autoResize2() {
        autoResize()
    }

    fun autoResize() {
        method13936()
        method13938()
        method13934()
        calculateViewportCenter()
        resetClipping()
    }

    override fun bloomSupported(): Boolean {
        if (bloomManager[1] != null && !bloomManager[1]!!.method1107()) {
            val reset = aClass328714!!.initialize(bloomManager[1]!!)
            if (reset) {
                materialCache!!.reset()
            }
            return reset
        }
        return false
    }

    override fun resetBloom() {
        if (bloomManager[1] != null && bloomManager[1]!!.method1107()) {
            aClass328714!!.method822(bloomManager[1]!!)
            materialCache!!.reset()
        }
    }

    override fun bloomEnabled(): Boolean {
        return bloomManager[1] != null && bloomManager[1]!!.method1107()
    }

    override fun setBloomFilter(r: Float, g: Float, b: Float) {
        NativeFilterBloomShader.red = r
        NativeFilterBloomShader.blue = g
        NativeFilterBloomShader.green = b
        NativeFilterBloomShader.baseBlur = 1.0f
    }

    fun method13908(f_1: Float, f_2: Float, f_4: Float, f_5: Float): Boolean {
        directBuffer.clear()
        directBuffer.putFloat(f_1)
        directBuffer.putFloat(f_2)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(f_4)
        directBuffer.putFloat(f_5)
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

    fun method13911(i_1: Int): NativeToolkitBuffer {
        if (anInterface328834!!.getCapacity() < i_1 * 2) {
            anInterface328834!!.create(i_1)
        }
        return anInterface328834!!
    }

    override fun getOffheap(): Int {
        return textureBuffersSizeOccupied + texturesSizeOccupied + renderBufferSizeOccupied
    }

    override fun generateSprite(spriteType: SpriteType?, bool2: Boolean): NativeSprite {
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

    override fun createNativeSprite(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean): NativeSprite {
        throw UnsupportedOperationException("createNativeSprite not implemented for this toolkit")
    }

    fun getProjectionMatrix(): Matrix44 {
        return projectionMatrix
    }

    override fun createComponentSurface(i_1: Int, i_2: Int, ints_3: IntArray, ints_4: IntArray): ComponentSurface {
        return Class455_Sub1.createFromRowMask(this, i_1, i_2, ints_3, ints_4)
    }

    internal abstract fun toggleBlending()

    override fun createModel(rsmesh1: Mesh, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Model {
        throw UnsupportedOperationException("createModel must be implemented by subclass")
    }

    override fun mergeFunctionMasks(mask1: Int, mask2: Int): Int {
        return mask1 or mask2
    }

    override fun createGround(i_1: Int, i_2: Int, ints_3: Array<IntArray>, ints_4: Array<IntArray>, i_6: Int, i_7: Int): Ground {
        throw UnsupportedOperationException("createGround not implemented for this toolkit")
    }

    override fun getRenderingMatrix(): Matrix43 {
        return aClass2948842
    }

    override fun setSunCubeTexture(class1521: AbstractCubeTextureGenerator?) {
        aClass152Sub28731 = class1521 as? AbstractNativeCubeTextureGenerator
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
            anInt8749 = viewportX
            anInt8776 = viewportY
            anInt8751 = viewportWidth
            anInt8752 = viewportHeight
            depthRangeNear = aFloat8737
            depthRangeFar = aFloat8738
        } else {
            anInt8749 = 0
            anInt8776 = 0
            if (display != null) {
                anInt8751 = display!!.getWidth()
                anInt8752 = display!!.getHeight()
            } else {
                anInt8751 = 0
                anInt8752 = 0
            }
            depthRangeNear = 0.0f
            depthRangeFar = 1.0f
        }

        setViewport()
        viewportCenterX = viewportWidth / 2.0f
        viewportCenterY = viewportHeight / 2.0f
        aFloat8797 = viewportX + viewportCenterX
        aFloat8741 = viewportY + viewportCenterY
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

    fun method13919() {
        anInt8823 = 0
        anInt8754 = 0
        setViewport()
        setScissorRect()
    }

    internal abstract fun setViewport()

    internal abstract fun setScissorRect()

    internal abstract fun toggleScissorTest()

    abstract fun drawPrimitive(var1: PrimitiveDrawMode, var2: Int, var3: Int)

    fun method13926() {
        transformMatrix.copyFrom(particleMatrix)
        transformMatrix.multiplyBy(aClass3848724)
        transformMatrix.method6539(aFloatArrayArray8730[0])
        transformMatrix.method6540(aFloatArrayArray8730[1])
        transformMatrix.method6535(aFloatArrayArray8730[2])
        transformMatrix.method6536(aFloatArrayArray8730[3])
        transformMatrix.method6613(aFloatArrayArray8730[4])
        transformMatrix.method6538(aFloatArrayArray8730[5])
        aClass3848729.copyFrom(transformMatrix)
        loadWorldViewProjectionMatrix(aClass3848729)
    }

    override fun copyModelMatrix(): Matrix44 {
        return aClass3848724
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

    fun method13931(): Matrix44 {
        return aClass3848729
    }

    fun loadWorldViewProjectionMatrix() {
        if (!aBool8720) {
            val height: Int
            val width: Int
            if (display != null) {
                width = display!!.getWidth()
                height = display!!.getHeight()
            } else {
                height = 0
                width = 0
            }

            val matrix443 = aClass3848774
            if (width != 0 && height != 0) {
                matrix443.method6530(0.0f, width.toFloat(), 0.0f, height.toFloat(), -1.0f, 1.0f)
            } else {
                matrix443.identity()
            }

            worldViewProjectionMatrix.copyFrom(matrix443)
            loadWorldViewProjectionMatrix(worldViewProjectionMatrix)
            aBool8720 = true
        }
    }

    abstract fun loadWorldViewProjectionMatrix(cacheInto: Matrix44)

    fun method13934() {
        if (matrixMode != MatrixMode.aClass33380) {
            val lastMode = matrixMode
            matrixMode = MatrixMode.aClass33380
            if (lastMode == MatrixMode.MODEL_VIEW) {
                requestViewPositionUpdate()
            }

            aClass3848708 = aClass3848725
            calculateViewRange()
            calculateViewport()
            nativeFunctionMask = nativeFunctionMask and -16
        }
    }

    fun method13935() {
        if (matrixMode != MatrixMode.PROJECTION) {
            val lastMode = matrixMode
            matrixMode = MatrixMode.PROJECTION
            if (lastMode == MatrixMode.MODEL_VIEW) {
                requestViewPositionUpdate()
            }

            loadWorldViewProjectionMatrix()
            aClass3848708 = aClass3848774
            calculateViewRange()
            calculateViewport()
            nativeFunctionMask = nativeFunctionMask and -9
        }
    }

    fun method13936() {
        aBool8720 = false
        if (matrixMode == MatrixMode.PROJECTION) {
            loadWorldViewProjectionMatrix()
            calculateViewRange()
        }
    }

    fun method13937() {
        if (matrixMode != MatrixMode.MODEL_VIEW) {
            matrixMode = MatrixMode.MODEL_VIEW
            requestViewPositionUpdate()
            method13940()
            aClass3848708 = aClass3848724
            calculateViewRange()
            calculateViewport()
            nativeFunctionMask = nativeFunctionMask and -8
        }
    }

    fun method13938() {
        aBool8723 = false
        method13940()
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            calculateViewRange()
        }
    }

    fun method13940() {
        if (!aBool8723) {
            aBool8723 = true
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

    fun method13942(bool1: Boolean) {
        if (aBool8756 != bool1) {
            aBool8756 = bool1
            setDepthRenderState()
            nativeFunctionMask = nativeFunctionMask and -16
        }
    }

    fun method13944() {
        anInterface48746 = createStream(false)
        val s_1: Short = 160
        anInterface48746!!.create(s_1.toInt(), 32)
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
        anInterface48746!!.upload(0, directBuffer.position(), directBufferAddress)
        aClass708786 = method13995(arrayOf(VertexDeclarationComponent(arrayOf(VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69690, VertexDeclarationAction.aClass69692, VertexDeclarationAction.aClass69692))))
    }

    override fun calculateProjection(f_1: Float, f_2: Float, f_3: Float, floats_4: FloatArray) {
        val f_5 = transformMatrix.faceList[15] + transformMatrix.faceList[3] * f_1 + transformMatrix.faceList[7] * f_2 + transformMatrix.faceList[11] * f_3
        val f_6 = transformMatrix.faceList[12] + transformMatrix.faceList[0] * f_1 + transformMatrix.faceList[4] * f_2 + transformMatrix.faceList[8] * f_3
        val f_7 = transformMatrix.faceList[13] + transformMatrix.faceList[1] * f_1 + transformMatrix.faceList[5] * f_2 + transformMatrix.faceList[9] * f_3
        val f_8 = particleMatrix.faceList[14] + particleMatrix.faceList[2] * f_1 + particleMatrix.faceList[6] * f_2 + particleMatrix.faceList[10] * f_3
        floats_4[0] = aFloat8797 + viewportCenterX * f_6 / f_5
        floats_4[1] = aFloat8741 + viewportCenterY * f_7 / f_5
        floats_4[2] = f_8
    }

    override fun computeWorldToScreen(x: Float, y: Float, z: Float, screenCoords: FloatArray) {
        val wX = transformMatrix.faceList[14] + transformMatrix.faceList[2] * x + transformMatrix.faceList[6] * y + transformMatrix.faceList[10] * z
        val wY = transformMatrix.faceList[15] + transformMatrix.faceList[3] * x + transformMatrix.faceList[7] * y + transformMatrix.faceList[11] * z
        if (wX >= -wY && wX <= wY) {
            val wZ = transformMatrix.faceList[12] + transformMatrix.faceList[0] * x + transformMatrix.faceList[4] * y + transformMatrix.faceList[8] * z
            if (wZ >= -wY && wZ <= wY) {
                val f_8 = transformMatrix.faceList[13] + transformMatrix.faceList[1] * x + transformMatrix.faceList[5] * y + transformMatrix.faceList[9] * z
                if (f_8 >= -wY && f_8 <= wY) {
                    val screenZ = particleMatrix.faceList[14] + particleMatrix.faceList[2] * x + particleMatrix.faceList[6] * y + particleMatrix.faceList[10] * z
                    screenCoords[0] = aFloat8797 + viewportCenterX * wZ / wY
                    screenCoords[1] = aFloat8741 + viewportCenterY * f_8 / wY
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

    fun toggleLights(bool1: Boolean) {
        if (aBool8684 != bool1) {
            aBool8684 = bool1
            toggleLights()
            nativeFunctionMask = nativeFunctionMask and -8
        }
    }

    override fun copyLights(copyLength: Int, lightNodesSrc: Array<PointLight>?) {
        if (lightSources == null || lightNodesSrc == null) return
        if (copyLength >= 0) System.arraycopy(lightNodesSrc, 0, lightSources!!, 0, copyLength)

        lastSceneLightCount = copyLength
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            method13903()
        }
    }

    override fun updateSun(intensity: Float) {
        if (lightIntensity != intensity) {
            lightIntensity = intensity
            setFogColorAndIntensity()
            enableLights()
        }
    }

    override fun applyLighting(color: Int, intensity: Float, f_3: Float, positionX: Float, positionY: Float, positionZ: Float) {
        val lightChanged = baseLightColor != color
        if (lightChanged || sunColorIntensity != intensity || antiSunColorIntensity != f_3) {
            baseLightColor = color
            sunColorIntensity = intensity
            antiSunColorIntensity = f_3
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

    fun setColourType(i_1: Int) {
        when (i_1) {
            0 -> method13966(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE)
            1 -> method13966(TextureCombineMode.REPLACE, TextureCombineMode.REPLACE)
            2 -> method13966(TextureCombineMode.INTERPOLATE, TextureCombineMode.REPLACE)
            3 -> method13966(TextureCombineMode.ADD, TextureCombineMode.MODULATE)
            4 -> method13966(TextureCombineMode.SUBTRACT, TextureCombineMode.SUBTRACT)
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

    abstract fun method13954(var1: GLFormat, var2: GLActionFormat): Boolean

    abstract fun method13955(var1: GLFormat, var2: GLActionFormat): Boolean

    internal abstract fun toggleDepthTest()

    fun createTextureSampler(class1501: GLFormat, i_2: Int, i_3: Int, bool4: Boolean, bytes_5: ByteArray?): NativeTextureSampler {
        return createNativeTextureSampler(class1501, i_2, i_3, bool4, bytes_5)
    }

    fun createHdrTexture(format: GLFormat, width: Int, length: Int, pixels: FloatArray): NativeTextureSampler {
        return createNativeTexture(format, width, length, pixels)
    }

    fun method13960() {
        if (textureStates!![currentTextureSlot] != TextureState.INITIALIZE) {
            textureStates!![currentTextureSlot] = TextureState.INITIALIZE
            textureMatrices!![currentTextureSlot].identity()
            method14133()
        }
    }

    internal abstract fun createNativeTexture(format: GLFormat, width: Int, length: Int, pixels: FloatArray): NativeTextureSampler

    override fun copySunPosition(): Matrix43 {
        return aClass2948706
    }

    abstract fun method13962(var1: GLFormat, var6: ByteArray): Texture3DSampler

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
        return aClass152Sub28731?.getNativeCubeTexture()
    }

    fun method13966(class681: TextureCombineMode, class682: TextureCombineMode) {
        var bool3 = false
        if (class681 != textureRgbCombineModes!![currentTextureSlot]) {
            textureRgbCombineModes!![currentTextureSlot] = class681
            setTextureColorStage()
            bool3 = true
        }

        if (class682 != textureAlphaCombineModes!![currentTextureSlot]) {
            textureAlphaCombineModes!![currentTextureSlot] = class682
            setTextureAlphaStage()
            bool3 = true
        }

        if (bool3) {
            nativeFunctionMask = nativeFunctionMask and -14
        }
    }

    fun method13967(i_1: Int, class672: TextureStageState) {
        setTextureStageState(i_1, class672)
    }

    internal abstract fun setTextureStageState(var1: Int, var2: TextureStageState)

    fun method13969(i_1: Int, class672: TextureStageState) {
        method13970(i_1, class672)
    }

    internal abstract fun method13970(var1: Int, var2: TextureStageState)

    fun setColourValue(i_1: Int) {
        if (envColor != i_1) {
            envColor = i_1
            setTextureEnvironmentColor()
        }
    }

    fun method13973(): Matrix44 {
        return textureMatrices!![currentTextureSlot]
    }

    fun setTextureState(textureState: TextureState) {
        textureStates!![currentTextureSlot] = textureState
        method14133()
    }

    fun method13976() {
        setFogColorAndIntensity()
        setSunParameters()
        toggleLights()
        method13903()
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
            method13960()
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

    fun method13985() {
        if (anInt8844 != 0) {
            anInt8844 = 0
            blendDrawBuffer()
        }
    }

    abstract fun colorMask(var1: Int)

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

    override fun calculateFog(color: Int, distance: Int, i_3: Int) {
        if (fogColorRgb != color || distance != fogDistance || i_3 != anInt8680) {
            fogColorRgb = color
            fogDistance = distance
            anInt8680 = i_3
            method14062()
            toggleFog()
        }
    }

    override fun setSunPosition(matrix44var1: Matrix43) {
        aClass2948706 = matrix44var1
        particleMatrix.fromMatrix(aClass2948706)
        aClass2948804.copyFrom(matrix44var1)
        aClass2948804.updateIdentitiyMatrix()
        aClass3848814.fromMatrix(aClass2948804)
        method13926()
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            requestViewPositionUpdate()
        }
    }

    internal abstract fun toggleFog()

    internal abstract fun toggleMultisampling(var1: Boolean)

    abstract fun method13993(var1: Boolean): NativeToolkitBuffer

    abstract fun createStream(var1: Boolean): NativeStream

    abstract fun method13995(var1: Array<VertexDeclarationComponent>): VertexDeclaration

    abstract fun setVertexDeclaration(var1: VertexDeclaration)

    override fun clearRectangle(class4552: ComponentSurface, i_3: Int, i_4: Int) {
        setDepthBuffering(false)
        aNativeSpriteSub38791!!.method2766(0.0f, 0.0f, display!!.getWidth().toFloat(), 0.0f, 0.0f, display!!.getHeight().toFloat(), 0, class4552, i_3, i_4)
        setDepthBuffering(true)
    }

    abstract fun method13997(var1: NativeToolkitBuffer)

    fun getEyePositionOsMatrix(): Matrix44 {
        return if (matrixMode == MatrixMode.MODEL_VIEW) aClass3848814 else aClass3848712
    }

    override fun drawRectangleOutline(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        drawTransparentLine(i_1, i_2, i_3 + i_1, i_2, i_5, i_6)
        drawTransparentLine(i_1, i_2 + i_4, i_3 + i_1, i_2 + i_4, i_5, i_6)
        drawTransparentLine(i_1, i_2, i_1, i_2 + i_4, i_5, i_6)
        drawTransparentLine(i_3 + i_1, i_2, i_3 + i_1, i_2 + i_4, i_5, i_6)
    }

    override fun renderWorldmapTile(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, bytes_7: ByteArray, i_8: Int, i_9: Int) {
    }

    override fun benchmark() {
    }

    override fun drawCircle(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
    }

    override fun drawTransparentLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        drawThickLine(i_1, i_2, i_3, i_4, i_5, 1, i_6)
    }

    override fun drawLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_7: Int, i_8: Int, i_9: Int) {
        var f_10 = (i_3 - i_1).toFloat()
        var f_11 = (i_4 - i_2).toFloat()
        if (f_10 == 0.0f && f_11 == 0.0f) {
            f_10 = 1.0f
        } else {
            val f_23 = (1.0 / sqrt((f_10 * f_10 + f_11 * f_11).toDouble())).toFloat()
            f_10 *= f_23
            f_11 *= f_23
        }

        method13935()
        val class4112 = waterShadersByEffect[13]!!
        class4112.method873()
        class4112.method875()
        toggleBlending(1)
        class4112.method879()
        toggleMultisampling(false)
        var i_9 = i_9 % (i_8 + i_7)
        val f_13 = f_10 * i_7
        val f_14 = f_11 * i_7
        var f_15 = 0.0f
        var f_16 = 0.0f
        var f_17 = f_13
        var f_18 = f_14
        if (i_9 > i_7) {
            f_15 = f_10 * (i_8 + i_7 - i_9)
            f_16 = f_11 * (i_8 + i_7 - i_9)
        } else {
            f_17 = f_10 * (i_7 - i_9)
            f_18 = f_11 * (i_7 - i_9)
        }

        var f_19 = i_1 + f_15
        var f_20 = i_2 + f_16
        val f_21 = f_10 * i_8
        val f_22 = f_11 * i_8

        while (true) {
            if (i_3 > i_1) {
                if (f_19 > i_3) {
                    break
                }
                if (f_19 + f_17 > i_3) {
                    f_17 = i_3 - f_19
                }
            } else {
                if (f_19 < i_3) {
                    break
                }
                if (f_19 + f_17 < i_3) {
                    f_17 = i_3 - f_19
                }
            }

            if (i_4 > i_2) {
                if (f_20 > i_4) {
                    break
                }
                if (f_20 + f_18 > i_4) {
                    f_18 = i_4 - f_20
                }
            } else {
                if (f_20 < i_4) {
                    break
                }
                if (f_20 + f_18 < i_4) {
                    f_18 = i_4 - f_20
                }
            }

            if (!method13908(f_19, f_20, f_19 + f_17, f_20 + f_18)) {
                return
            }

            drawPrimitiveLines()
            f_19 += f_21 + f_17
            f_20 += f_22 + f_18
            f_17 = f_13
            f_18 = f_14
        }

        toggleMultisampling(true)
        class4112.method885()
    }

    override fun drawTexturedLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, class4557: ComponentSurface, i_8: Int, i_9: Int, i_10: Int, i_11: Int, i_12: Int) {
    }

    override fun drawThickLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        var i_1 = (i_1 + 1.0f).toInt()
        var i_2 = (i_2 + 1.0f).toInt()
        var i_3 = (i_3 + 1.0f).toInt()
        var i_4 = (i_4 + 1.0f).toInt()
        var f_8 = (i_3 - i_1).toFloat()
        var f_9 = (i_4 - i_2).toFloat()
        val f_10 = 1.0f / sqrt((f_8 * f_8 + f_9 * f_9).toDouble()).toFloat()
        f_8 *= f_10
        f_9 *= f_10
        i_1 = (i_1 - f_8).toInt()
        i_2 = (i_2 - f_9).toInt()
        var f_11 = -f_9
        f_11 *= 0.5f * i_6
        val f_12 = f_8 * 0.5f * i_6
        aNativeSpriteSub38790!!.method2763(i_1 - f_11, i_2 - f_12, i_3 - f_11, i_4 - f_12, i_1 + f_11, i_2 + f_12, 0, i_5, i_7)
    }

    override fun renderParticles(particleWrapper: ParticleWrapper) {
        particleRenderer!!.render(this, particleWrapper)
    }

    fun drawPrimitiveLines() {
        setStreamSource(0, vertexStream!!)
        setVertexDeclaration(vertexDeclaration!!)
        drawPrimitive(PrimitiveDrawMode.LINES, 0, 1)
    }

    fun method14001(class3521: PrimitiveDrawMode) {
        setStreamSource(0, anInterface48746!!)
        setVertexDeclaration(aClass708786!!)
        drawPrimitive(class3521, 0, 2)
    }

    abstract fun drawIndexedPrimitive(var1: PrimitiveDrawMode, var2: Int, var3: Int, var4: Int, var5: Int)

    fun method14003() {
        if (nativeFunctionMask != 2) {
            method13935()
            toggleFog(false)
            toggleLights(false)
            toggleDepthTest(false)
            method13942(false)
            nativeFunctionMask = 2
        }
    }

    fun method14004() {
        if (nativeFunctionMask != 8) {
            method13937()
            toggleFog(true)
            toggleDepthTest(true)
            method13942(true)
            toggleBlending(1)
            nativeFunctionMask = 8
        }
    }

    override fun setModelMatrix(matrix441: Matrix44) {
        aClass3848724.copyFrom(matrix441)
        method13926()
        if (matrixMode == MatrixMode.MODEL_VIEW) {
            calculateViewRange()
        }
    }

    fun method14009() {
        vertexStream = createStream(true)
        vertexStream!!.create(24, 12)
        vertexDeclaration = method13995(arrayOf(VertexDeclarationComponent(VertexDeclarationAction.aClass69695)))
    }

    internal abstract fun configureFog()

    override fun method8404(): Boolean {
        return true
    }

    override fun createCubeTexture(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int): AbstractCubeTextureGenerator {
        return NativeCubeTextureGenerator(this, i_1, i_2, i_3, i_4, i_5, i_6)
    }

    fun createTexture(width: Int, length: Int, bool3: Boolean, pixels: IntArray): NativeTextureSampler {
        return createTextureSampler(width, length, bool3, pixels, 0, 0)
    }

    override fun resetDepthClippingMask() {
        aFloat8737 = 0.0f
        aFloat8738 = 1.0f
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

    override fun method8449(): Matrix44 {
        return aClass3848841
    }

    fun calculateViewRange() {
        projectionMatrix.copyFrom(aClass3848708)
        loadWorldViewProjectionMatrix(projectionMatrix)
        camFar = (projectionMatrix.faceList[14] - projectionMatrix.faceList[15]) / (projectionMatrix.faceList[11] - projectionMatrix.faceList[10])
        camNear = -projectionMatrix.faceList[14] / projectionMatrix.faceList[10]
        method14062()
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

    override fun setClipping(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        val displayHeight: Int
        val displayWidth: Int
        if (display != null) {
            displayWidth = display!!.getWidth()
            displayHeight = display!!.getHeight()
        } else {
            displayHeight = 0
            displayWidth = 0
        }

        if (i_1 <= 0 && i_3 >= displayWidth - 1 && i_2 <= 0 && i_4 >= displayHeight - 1) {
            resetClipping()
        } else {
            cLeft = 0.coerceAtLeast(i_1)
            cRight = i_3.coerceAtMost(displayWidth)
            cTop = 0.coerceAtLeast(i_2)
            cBottom = i_4.coerceAtMost(displayHeight)
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

    override fun method8525(i_3: Int, i_4: Int) {
        aClass328714!!.method830(i_3, i_4)
    }

    fun method14062() {
        aClass418753?.method877()
        configureFog()
    }

    abstract fun method14064(var1: GLFormat, var2: GLActionFormat, var3: Int, var4: Int): RectangleTextureSampler

    abstract fun method14072(var1: GLFormat, var2: GLActionFormat, var3: Int, var4: Int): NativeTextureSampler

    fun fetchShader(archiveName: String, fileName: String): ByteArray? {
        return shaderResource!!.getNamedFile(archiveName, fileName)
    }

    override fun addWaterTile(color: Int, waterTile: WaterTile) {
        if (!missingWaterColor) {
            throw RuntimeException("")
        }
        renderedWaterColor = color
        renderedWaterTile = waterTile
        if (aBool8805) {
            waterShadersByEffect[3]!!.method876()
            waterShadersByEffect[3]!!.method877()
        }
    }

    abstract fun createTextureSampler(var1: Int, var2: Int, var3: Boolean, var4: IntArray, var5: Int, var6: Int): NativeTextureSampler

    abstract fun setPolygonMode(var1: Int)

    override fun method8669(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, class4557: ComponentSurface, i_8: Int, i_9: Int) {
    }

    override fun onTilesDrawn() {
        missingWaterColor = false
    }

    override fun isOnScreen(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int): Int {
        var i_7 = 0
        val f_8 = transformMatrix.faceList[14] + transformMatrix.faceList[2] * i_1 + transformMatrix.faceList[6] * i_2 + transformMatrix.faceList[10] * i_3
        val f_9 = transformMatrix.faceList[14] + transformMatrix.faceList[2] * i_4 + transformMatrix.faceList[6] * i_5 + transformMatrix.faceList[10] * i_6
        val f_10 = transformMatrix.faceList[15] + transformMatrix.faceList[3] * i_1 + transformMatrix.faceList[7] * i_2 + transformMatrix.faceList[11] * i_3
        val f_11 = transformMatrix.faceList[15] + transformMatrix.faceList[3] * i_4 + transformMatrix.faceList[7] * i_5 + transformMatrix.faceList[11] * i_6
        if (f_8 < -f_10 && f_9 < -f_11) {
            i_7 = i_7 or 0x10
        } else if (f_8 > f_10 && f_9 > f_11) {
            i_7 = i_7 or 0x20
        }

        val f_12 = transformMatrix.faceList[12] + transformMatrix.faceList[0] * i_1 + transformMatrix.faceList[4] * i_2 + transformMatrix.faceList[8] * i_3
        val f_13 = transformMatrix.faceList[12] + transformMatrix.faceList[0] * i_4 + transformMatrix.faceList[4] * i_5 + transformMatrix.faceList[8] * i_6
        if (f_12 < -f_10 && f_13 < -f_11) {
            i_7 = i_7 or 0x1
        }

        if (f_12 > f_10 && f_13 > f_11) {
            i_7 = i_7 or 0x2
        }

        val f_14 = transformMatrix.faceList[13] + transformMatrix.faceList[1] * i_1 + transformMatrix.faceList[5] * i_2 + transformMatrix.faceList[9] * i_3
        val f_15 = transformMatrix.faceList[13] + transformMatrix.faceList[1] * i_4 + transformMatrix.faceList[5] * i_5 + transformMatrix.faceList[9] * i_6
        if (f_14 < -f_10 && f_15 < -f_11) {
            i_7 = i_7 or 0x4
        }

        if (f_14 > f_10 && f_15 > f_11) {
            i_7 = i_7 or 0x8
        }

        return i_7
    }

    fun getViewMatrix(): Matrix44 {
        return if (matrixMode == MatrixMode.MODEL_VIEW) particleMatrix else aClass3848712
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
                aClass2948710.copyFrom(aClass2948704)
                aClass2948710.multiply(aClass2948706)
                modelViewMatrix.fromMatrix(aClass2948710)
                viewPositionCalibrated = true
            }
            aClass2948710
        } else {
            aClass2948704
        }
    }

    fun method14133() {
        setTextureMatrix()
    }

    internal abstract fun createNativeTextureSampler(var1: GLFormat, var2: Int, var3: Int, var4: Boolean, var5: ByteArray?): NativeTextureSampler

    override fun supportsFullscreen(): Boolean {
        return true
    }

    override fun generateEmptySprite(i_1: Int, i_2: Int, bool3: Boolean, bool4: Boolean): NativeSprite {
        throw UnsupportedOperationException("generateEmptySprite not implemented for this toolkit")
    }

    protected abstract fun createModel(i_1: Int, i_2: Int, bool3: Boolean, bool4: Boolean): Model

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

        val ints_3 = intArrayOf(-1)
        activeTextureSampler = createTextureSampler(1, 1, false, ints_3, 0, 0)
        ints_3[0] = 0xffffff.inv()
        anInterface68795 = createTextureSampler(1, 1, false, ints_3, 0, 0)
        setNativeHeap(getNativeMemory(262144))
        aClass708832 = method13995(arrayOf(VertexDeclarationComponent(arrayOf(VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69692))))
        method13995(arrayOf(VertexDeclarationComponent(arrayOf(VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69690))))
        aClass708836 = method13995(arrayOf(
            VertexDeclarationComponent(VertexDeclarationAction.aClass69695),
            VertexDeclarationComponent(VertexDeclarationAction.aClass69690),
            VertexDeclarationComponent(VertexDeclarationAction.aClass69692),
            VertexDeclarationComponent(VertexDeclarationAction.NORMAL_POINTER_ACTION)
        ))
        aClass708721 = method13995(arrayOf(
            VertexDeclarationComponent(VertexDeclarationAction.aClass69695),
            VertexDeclarationComponent(VertexDeclarationAction.aClass69690),
            VertexDeclarationComponent(VertexDeclarationAction.aClass69692)
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

        method13895()
        anInterface328834 = method13993(true)
        setupGraphics()
        getImageProducer()
        resetClipping()
        renderLastFrame()
        aNativeSpriteSub38790 = createNativeSprite(activeTextureSampler!!)
        aNativeSpriteSub38791 = createNativeSprite(anInterface68795!!)
        applyClearFlags(3, 0)
    }

    fun method14150() {
        method14001(PrimitiveDrawMode.TRIANGLE_FAN)
    }

    abstract fun setStreamSource(var1: Int, var2: NativeStream)

    fun setTextureSlot(textureSlot: Int) {
        if (currentTextureSlot != textureSlot) {
            currentTextureSlot = textureSlot
            activateCurrentTexture()
        }
    }

    fun method14177() {
        anInterface48693 = createStream(false)
        anInterface48693!!.create(3096, 12)
        directBuffer.clear()
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)
        directBuffer.putFloat(0.0f)

        for (i_1 in 0..256) {
            val d_2 = (i_1 * 2) * Math.PI / 256.0
            val f_4 = Math.cos(d_2).toFloat()
            val f_5 = Math.sin(d_2).toFloat()
            directBuffer.putFloat(f_5)
            directBuffer.putFloat(f_4)
            directBuffer.putFloat(0.0f)
        }

        anInterface48693!!.upload(0, directBuffer.position(), directBufferAddress)
    }

    override fun drawFilledRectangle(x: Int, y: Int, width: Int, height: Int, color: Int, alpha: Int) {
        if (depthBufferingEnabled) {
            setDepthBuffering(false)
            aNativeSpriteSub38790!!.drawCircle(x, y, width, height, 0, color, alpha)
            setDepthBuffering(true)
        } else {
            aNativeSpriteSub38790!!.drawCircle(x, y, width, height, 0, color, alpha)
        }
    }

    override fun drawTransparentVerticalLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        drawTransparentLine(i_1, i_2, i_1, i_3 + i_2, i_4, i_5)
    }

    override fun method8474() {
        aClass328714!!.method833()
    }

    internal abstract fun toggleLights()

    internal abstract fun setTextureMatrix()

    internal abstract fun toggleAlpha()

    companion object {
        @JvmField
        val aClass3848712: Matrix44 = Matrix44()

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
