package com.jagex.game.runetek5.toolkit

import java.awt.*

import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.framebuffer.SoftwareRenderBuffer
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.font.FontRendererSw3d
import com.jagex.game.runetek5.graphics.font.OpaqueFontRendererSw3d
import com.jagex.game.runetek5.graphics.font.TransparentFontRendererSw3d
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelSw3d
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.model.particle.PointEntity
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteSw3d
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.imageproducer.ImageProducerSw3d
import com.jagex.game.runetek5.imageproducer.ImageProducerSw3d1
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Quaternion
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.rendering.RenderInfo
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.ground.GroundSw3d
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject
import com.jagex.game.runetek5.toolkit.sw3d.ThreadResourceSw3d
import com.jagex.game.runetek5.toolkit.sw3d.ba
import com.jagex.game.runetek5.toolkit.sw3d.wa
import com.jagex.game.runetek5.utils.ColorUtil
import kotlin.math.max

/**
 * EnhancedSoftwareRenderer / ToolkitSw3d
 * @author Brainpower
 *
 */
open class SoftwareToolkit(canvas1: Canvas?, materialProvider: MaterialProvider?, i_3: Int, i_4: Int) : AbstractToolkit(materialProvider), INativeObject {

    @JvmField
    var z: ba? = null
    @JvmField
    var ai: Array<ThreadResourceSw3d>? = null
    @JvmField
    var af: Matrix44? = null
    @JvmField
    var bm: Boolean = false
    @JvmField
    var b: Boolean = false
    @JvmField
    var az: Float = 0f
    @JvmField
    var ap: Float = 0f
    @JvmField
    var ax: Float = 0f
    @JvmField
    var al: Float = 0f
    @JvmField
    var at: Int = 4096
    @JvmField
    var ac: Int = 4096
    @JvmField
    var `as`: Int = 0
    @JvmField
    var ay: Matrix43? = null
    @JvmField
    var an: Matrix44? = null
    @JvmField
    var nativeid: Long = 0
    @JvmField
    val au: NodeCollection = NodeCollection()
    @JvmField
    var ag: Matrix43? = null
    @JvmField
    var ao: Matrix44? = null
    @JvmField
    var aw: Matrix44? = null

    init {
        try {
            if (!getNativeLibraryLoader().loadIntoMemory("sw3d")) {
                throw RuntimeException("")
            }
            FinalizeCacheSw3d.incrementCapacity()
            q(materialProvider, materialProvider?.getSize() ?: 0, 0)
            ColorUtil.initializeTables(false, true)
            this.bm = true
            this.af = Matrix44()
            Vector3()
            this.ag = Matrix43()
            Quaternion()
            this.ao = Matrix44()
            this.an = Matrix44()
            this.aw = Matrix44()
            setSunPosition(Matrix43())
            setModelMatrix(Matrix44())
            nu(1)
            ni()
            if (canvas1 != null) {
                ensureImageproducerCreated(canvas1, i_3, i_4)
                setActiveCanvas(canvas1)
            }

            val i_5 = materialProvider?.getSize() ?: 0

            for (s_6 in 0 until i_5) {
                val class1697: MaterialType? = materialProvider?.getMaterial(s_6)
                if (class1697 != null) {
                    V(s_6.toShort(), class1697.colorHsl, class1697.effectCombiner, class1697.effectId, class1697.effectParam1, class1697.effectParam2, class1697.isSmall, class1697.alpha, class1697.brightness, class1697.textureSpeedU, class1697.textureSpeedV, class1697.detailsOnly, class1697.skipTriangles, class1697.aBool2087, class1697.repeatS, class1697.repeatT, class1697.mipmapping, class1697.hdr, class1697.isBrickTile, class1697.combineMode)
                }
            }

        } catch (throwable: Throwable) {
            throwable.printStackTrace() // Added to debug unsatisfiedLinkErrors
            cleanupAndShutdown()
            throw RuntimeException()
        }
    }

    external fun q(var1: MaterialProvider?, var2: Int, var3: Int)

    external fun R(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: ComponentSurface, var8: Int, var9: Int)

    external fun Y(var1: Int)

    external fun Q(var1: Int, var2: IntArray, var3: FloatArray)

    external fun wa(var1: FloatArray)

    @Synchronized
    override fun close() {
        cleanupAndShutdown()
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    fun h() {
        System.gc()
        FinalizeCacheSw3d.destroyAll()
    }

    override fun copySunPosition(): Matrix43 {
        return this.ay!!
    }

    external override fun setDepthBuffering(var1: Boolean)

    override fun awaitRenderCompletion() {
    }

    override fun animateTextures(i_1: Int) {
        FinalizeCacheSw3d.destroyAll()
        Y(i_1)

        var ba2 = this.au.getFirst() as ba?
        while (ba2 != null) {
            ba2.u()
            ba2 = this.au.next() as ba?
        }
    }

    external override fun getOffheap(): Int

    override fun createNativeSprite(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean): NativeSprite {
        return SpriteSw3d(this, ints_1, i_2, i_3, i_4, i_5)
    }

    override fun supportsFullscreen(): Boolean {
        return true
    }

    override fun forceFixedScreenmode(): Boolean {
        return true
    }

    override fun hasBloomSupport(): Boolean {
        return false
    }

    override fun method8404(): Boolean {
        return false
    }

    override fun calculateProjection(f_1: Float, f_2: Float, f_3: Float, floats_4: FloatArray) {
        val f_5 = this.aw!!.faceList[15] + this.aw!!.faceList[3] * f_1 + this.aw!!.faceList[7] * f_2 + this.aw!!.faceList[11] * f_3
        val f_6 = this.aw!!.faceList[12] + this.aw!!.faceList[0] * f_1 + this.aw!!.faceList[4] * f_2 + this.aw!!.faceList[8] * f_3
        val f_7 = this.aw!!.faceList[13] + this.aw!!.faceList[1] * f_1 + this.aw!!.faceList[5] * f_2 + this.aw!!.faceList[9] * f_3
        val f_8 = this.ao!!.faceList[14] + this.ao!!.faceList[2] * f_1 + this.ao!!.faceList[6] * f_2 + this.ao!!.faceList[10] * f_3
        floats_4[0] = this.ax + this.az * f_6 / f_5
        floats_4[1] = this.al + this.ap * f_7 / f_5
        floats_4[2] = f_8
    }

    override fun extendBuildArea(): Boolean {
        return true
    }

    override fun replaceCanvasOnDisplayUpdate(): Boolean {
        return true
    }

    override fun getMaxLights(): Int {
        return 4
    }

    override fun debugModeChanged() {
    }

    override fun renderLastFrame() {
    }

    override fun createDelegatingDisplay(canvas1: Canvas, i_2: Int, i_3: Int): DelegatingDisplay {
        return ImageProducerSw3d(this, canvas1, i_2, i_3)
    }

    external fun PA(var1: ImageProducerSw3d)

    external fun d(var1: Long, var3: Long)

    override fun calculateViewportCenter() {
        this.az = getDisplay()!!.getWidth() / 2.0f
        this.ap = getDisplay()!!.getHeight() / 2.0f
        this.ax = this.az
        this.al = this.ap
        k()
    }

    override fun setDrawableArea(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        this.az = i_3 / 2.0f
        this.ap = i_4 / 2.0f
        this.ax = i_1 + this.az
        this.al = i_2 + this.ap
        A(i_1, i_2, i_3, i_4)
    }

    external fun A(var1: Int, var2: Int, var3: Int, var4: Int)

    external override fun resetClipping()

    external override fun setClipping(var1: Int, var2: Int, var3: Int, var4: Int)

    external override fun getClipping(var1: IntArray)

    external override fun applyClearFlags(var1: Int, var2: Int)

    override fun drawRectangleOutline(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        drawTransparentHorizontalLine(i_1, i_2, i_3, i_5, i_6)
        drawTransparentHorizontalLine(i_1, i_2 + i_4 - 1, i_3, i_5, i_6)
        drawTransparentVerticalLine(i_1, i_2 + 1, i_4 - 1, i_5, i_6)
        drawTransparentVerticalLine(i_3 + i_1 - 1, i_2 + 1, i_4 - 1, i_5, i_6)
    }

    external override fun drawFilledRectangle(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int)

    external override fun renderWorldmapTile(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: ByteArray, var8: Int, var9: Int)

    override fun benchmark() {
        throw IllegalStateException()
    }

    external override fun drawTransparentHorizontalLine(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int)

    external override fun drawTransparentVerticalLine(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int)

    override fun drawTransparentLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        YA(i_1, i_2, i_3, i_4, i_5, i_6)
    }

    override fun hasShadows(): Boolean {
        return true
    }

    external override fun drawCircle(var1: Int, var2: Int, var3: Int, var4: Int)

    override fun drawThickLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        nx()!!.method311(this, i_1, i_2, i_3, i_4, i_5, i_6, i_7)
    }

    override fun method8669(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, class4557: ComponentSurface, i_8: Int, i_9: Int) {
        R(i_1, i_2, i_3, i_4, i_5, 1, class4557, i_8, i_9)
    }

    override fun drawLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_7: Int, i_8: Int, i_9: Int) {
    }

    override fun isOnScreen(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int): Int {
        var i_7 = 0
        val f_8 = this.aw!!.faceList[14] + this.aw!!.faceList[2] * i_1 + this.aw!!.faceList[6] * i_2 + this.aw!!.faceList[10] * i_3
        val f_9 = this.aw!!.faceList[14] + this.aw!!.faceList[2] * i_4 + this.aw!!.faceList[6] * i_5 + this.aw!!.faceList[10] * i_6
        val f_10 = this.aw!!.faceList[15] + this.aw!!.faceList[3] * i_1 + this.aw!!.faceList[7] * i_2 + this.aw!!.faceList[11] * i_3
        val f_11 = this.aw!!.faceList[15] + this.aw!!.faceList[3] * i_4 + this.aw!!.faceList[7] * i_5 + this.aw!!.faceList[11] * i_6
        if (f_8 < -f_10 && f_9 < -f_11) {
            i_7 = i_7 or 0x10
        } else if (f_8 > f_10 && f_9 > f_11) {
            i_7 = i_7 or 0x20
        }

        val f_12 = this.aw!!.faceList[12] + this.aw!!.faceList[0] * i_1 + this.aw!!.faceList[4] * i_2 + this.aw!!.faceList[8] * i_3
        val f_13 = this.aw!!.faceList[12] + this.aw!!.faceList[0] * i_4 + this.aw!!.faceList[4] * i_5 + this.aw!!.faceList[8] * i_6
        if (f_12 < -f_10 && f_13 < -f_11) {
            i_7 = i_7 or 0x1
        }

        if (f_12 > f_10 && f_13 > f_11) {
            i_7 = i_7 or 0x2
        }

        val f_14 = this.aw!!.faceList[13] + this.aw!!.faceList[1] * i_1 + this.aw!!.faceList[5] * i_2 + this.aw!!.faceList[9] * i_3
        val f_15 = this.aw!!.faceList[13] + this.aw!!.faceList[1] * i_4 + this.aw!!.faceList[5] * i_5 + this.aw!!.faceList[9] * i_6
        if (f_14 < -f_10 && f_15 < -f_11) {
            i_7 = i_7 or 0x4
        }

        if (f_14 > f_10 && f_15 > f_11) {
            i_7 = i_7 or 0x8
        }

        return i_7
    }

    override fun hasTextures(): Boolean {
        return true
    }

    override fun computeWorldToScreen(f_1: Float, f_2: Float, f_3: Float, floats_4: FloatArray) {
        val f_5 = this.aw!!.faceList[14] + this.aw!!.faceList[2] * f_1 + this.aw!!.faceList[6] * f_2 + this.aw!!.faceList[10] * f_3
        val f_6 = this.aw!!.faceList[15] + this.aw!!.faceList[3] * f_1 + this.aw!!.faceList[7] * f_2 + this.aw!!.faceList[11] * f_3
        if (f_5 >= -f_6 && f_5 <= f_6) {
            val f_7 = this.aw!!.faceList[12] + this.aw!!.faceList[0] * f_1 + this.aw!!.faceList[4] * f_2 + this.aw!!.faceList[8] * f_3
            if (f_7 >= -f_6 && f_7 <= f_6) {
                val f_8 = this.aw!!.faceList[13] + this.aw!!.faceList[1] * f_1 + this.aw!!.faceList[5] * f_2 + this.aw!!.faceList[9] * f_3
                if (f_8 >= -f_6 && f_8 <= f_6) {
                    val f_9 = this.ao!!.faceList[14] + this.ao!!.faceList[2] * f_1 + this.ao!!.faceList[6] * f_2 + this.ao!!.faceList[10] * f_3
                    floats_4[0] = this.ax + this.az * f_7 / f_6
                    floats_4[1] = this.al + this.ap * f_8 / f_6
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

    override fun getNativeMemory(i_1: Int): NativeMemoryNode {
        val ba2 = ba(this, i_1)
        this.au.addFirst(ba2)
        return ba2
    }

    override fun setNativeHeap(class282Sub11: NativeMemoryNode) {
        this.z = class282Sub11 as ba
        n(class282Sub11)
    }

    override fun generateEmptySprite(i_1: Int, i_2: Int, bool3: Boolean, bool4: Boolean): NativeSprite {
        return SpriteSw3d(this, i_1, i_2, bool4)
    }

    external override fun clearRectangle(var2: ComponentSurface, var3: Int, var4: Int)

    override fun createFontRenderer(fontmetrics1: FontMetrics?, arr_2: Array<SpriteType>?, bool3: Boolean): AbstractFontRenderer {
        val ints_4 = IntArray(arr_2!!.size)
        val ints_5 = IntArray(arr_2.size)
        var transparent = false

        for (i_7 in arr_2.indices) {
            ints_4[i_7] = arr_2[i_7].width
            ints_5[i_7] = arr_2[i_7].height
            if (arr_2[i_7].alpha != null) {
                transparent = true
            }
        }

        if (bool3) {
            if (transparent) {
                return TransparentFontRendererSw3d(this, this.z!!, fontmetrics1!!, arr_2)
            } else {
                return OpaqueFontRendererSw3d(this, this.z!!, fontmetrics1!!, arr_2)
            }
        } else if (transparent) {
            throw IllegalArgumentException("")
        } else {
            return FontRendererSw3d(this, this.z!!, fontmetrics1!!, arr_2)
        }
    }

    override fun initialize() {
        this.ac = 10000
        this.at = this.ac
        if (this.`as` > 1) {
            throw IllegalStateException()
        } else {
            nu(this.`as`)
            ni()
        }
    }

    override fun createModel(rsmesh1: Mesh, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Model {
        return ModelSw3d(this, this.z!!, rsmesh1, i_2, i_3, i_4, i_5)
    }

    override fun mismatchingFunctionMask(i_1: Int, i_2: Int): Int {
        val i_3 = i_1 and 0xfffff
        val i_4 = i_2 and 0xfffff
        return i_3 and i_4 xor i_4
    }

    override fun hasAntiAliasing(): Boolean {
        return false
    }

    override fun utilisesNativeHeap(): Boolean {
        return true
    }

    fun nu(i_1: Int) {
        this.`as` = i_1
        this.ai = Array(this.`as`) { i_2 ->
            ThreadResourceSw3d(this, this.ac, this.at)
        }
    }

    fun ni() {
        this.ai!![0].method307()
    }

    fun nx(): ThreadResourceSw3d? {
        for (i_1 in 0 until this.`as`) {
            if (this.ai!![i_1].aRunnable48 == Thread.currentThread()) {
                return this.ai!![i_1]
            }
        }
        return null
    }

    override fun renderParticles(class1511: ParticleWrapper) {
        if (class1511.particleBinaryTree.getNodeCount() != 0) {
            ny(class1511)
            nx()!!.method309(this, bi, bw, bx, bz, class1511.particleBinaryTree.getNodeCount())
        }
    }

    override fun setSunPosition(matrix44var1: Matrix43) {
        this.ay = matrix44var1
        this.ao!!.fromMatrix(matrix44var1)
        AA(this.ao!!.faceList)
        this.aw!!.calculatePerspective(this.ao!!, this.an!!)
    }

    external fun oo()

    external fun AA(var1: FloatArray)

    override fun setModelMatrix(matrix441: Matrix44) {
        this.an = matrix441
        wa(this.an!!.faceList)
        this.aw!!.calculatePerspective(this.ao!!, this.an!!)
    }

    override fun copyModelMatrix(): Matrix44 {
        return this.an!!
    }

    external override fun updateSun(var1: Float)

    external override fun applyLighting(var1: Int, var2: Float, var3: Float, var4: Float, var5: Float, var6: Float)

    external override fun generateFlags()

    external fun YA(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int)

    override fun copyLights(i_1: Int, arr_2: Array<PointLight>) {
        var i_3 = 0

        for (i_4 in 0 until i_1) {
            aq[i_3++] = arr_2[i_4].getX()
            aq[i_3++] = arr_2[i_4].getY()
            aq[i_3++] = arr_2[i_4].getZ()
            aq[i_3++] = arr_2[i_4].getSize2d()
            av[i_4] = arr_2[i_4].getAlpha()
            aq[i_3++] = arr_2[i_4].getColor()
        }

        Q(i_1, aq, av)
    }

    override fun resetTextureState() {
    }

    override fun createCubeTexture(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int): AbstractCubeTextureGenerator {
        throw UnsupportedOperationException()
    }

    override fun setSunCubeTexture(class1521: AbstractCubeTextureGenerator?) {
    }

    override fun method8525(i_3: Int, i_4: Int) {
    }

    override fun bloomSupported(): Boolean {
        return false
    }

    override fun resetBloom() {
    }

    override fun bloomEnabled(): Boolean {
        return false
    }

    override fun setBloomFilter(f_1: Float, f_2: Float, f_3: Float) {
    }

    external fun JA(var1: Int, var2: Int, var3: Int, var4: Int)

    external fun M(var1: Int, var2: Int, var3: Int, var4: Int)

    override fun addWater(class902: WaterTile) {
        M(-1, class902.color, class902.scale, class902.intensity)
    }

    override fun getImageProducer(): DirectDisplay {
        return ImageProducerSw3d1(this)
    }

    override fun getRenderBuffer(width: Int, height: Int): RenderBuffer {
        return SoftwareRenderBuffer(width, height)
    }

    override fun addWaterTile(i_1: Int, class902: WaterTile) {
        JA(i_1, class902.color, class902.scale, class902.intensity)
    }

    override fun render() {
        val za3 = getDelegatingDisplay() as ImageProducerSw3d?
            ?: throw IllegalStateException()
        za3.draw(0, 0)
        if (materialProvider != null) {
            materialProvider.cycle()
        }
    }

    external fun F(var1: Short, var2: IntArray, var3: Short, var4: Int, var5: Byte, var6: Byte, var7: Int, var8: Boolean, var9: Byte, var10: Byte, var11: Byte, var12: Byte, var13: Boolean, var14: Boolean, var15: Boolean, var16: Boolean, var17: Boolean, var18: Byte, var19: Boolean, var20: Boolean, var21: Int)

    override fun createNativeSprite(i_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean): NativeSprite {
        return SpriteSw3d(this, i_1, i_2, i_3, i_4, !bool5)
    }

    override fun method8449(): Matrix44 {
        return this.af!!
    }

    external fun n(var1: NativeMemoryNode)

    external fun og(var1: FloatArray)

    override fun shutdown() {
        if (!this.b) {
            this.ai = null
            this.z = null
            this.af = null

            var ba1 = this.au.getFirst() as ba?
            while (ba1 != null) {
                ba1.ha()
                ba1 = this.au.next() as ba?
            }

            this.au.remove_all()
            da()
            if (this.bm) {
                ColorUtil.resetTables(false, true)
                this.bm = false
            }

            h()
            FinalizeCacheSw3d.decrementCapacity()
            this.b = true
        }
    }

    external override fun copyPixelRectangle(var1: Int, var2: Int, var3: Int, var4: Int): IntArray

    override fun drawTexturedLine(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, class4557: ComponentSurface, i_8: Int, i_9: Int, i_10: Int, i_11: Int, i_12: Int) {
    }

    external override fun destroy(var1: Boolean)

    fun ny(class1511: ParticleWrapper) {
        var i_3 = 0
        var i_4 = 0
        var i_5 = 0
        var i_6 = 0

        var class275Sub1Sub18 = class1511.particleBinaryTree.getNext() as PointEntity?
        while (class275Sub1Sub18 != null) {
            bi[i_3++] = class275Sub1Sub18.x
            bi[i_3++] = class275Sub1Sub18.y
            bi[i_3++] = class275Sub1Sub18.z
            bw[i_4++] = class275Sub1Sub18.colorArgb
            bz[i_6++] = class275Sub1Sub18.materialId.toShort()
            bx[i_5++] = class275Sub1Sub18.size
            class275Sub1Sub18 = class1511.particleBinaryTree.method7734() as PointEntity?
        }
    }

    external fun oq(var1: FloatArray)

    external fun da()

    override fun drawQuad(class1521: AbstractCubeTextureGenerator?, class1522: AbstractCubeTextureGenerator?, f_3: Float, class1524: AbstractCubeTextureGenerator?): AbstractCubeTextureGenerator? {
        return null
    }

    external fun oj(var1: Int, var2: Int, var3: Int, var4: Int)

    external override fun calculateFog(var1: Int, var2: Int, var3: Int)

    external override fun expandClipping(var1: Int, var2: Int, var3: Int, var4: Int)

    external fun on(var1: Int, var2: Int, var3: Int, var4: Int)

    external override fun resetDepthClippingMask()

    external fun nn(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int)

    external fun k()

    override fun createGround(i_1: Int, i_2: Int, ints_3: Array<IntArray>, ints_4: Array<IntArray>, i_6: Int, i_7: Int): Ground {
        return GroundSw3d(this, this.z!!, i_1, i_2, ints_3, ints_4, i_6, i_7)
    }

    override fun generateSprite(class911: SpriteType?, bool2: Boolean): NativeSprite {
        val fa3 = SpriteSw3d(this, class911!!.pallete, class911.pixels, class911.alpha!!, class911.width, class911.width, class911.height)
        fa3.setSize(class911.minX, class911.minY, class911.borderX, class911.borderY)
        return fa3
    }

    override fun getRenderInfo(): RenderInfo {
        return RenderInfo(0, "SSE", 1, "CPU", 0L)
    }

    external fun V(var1: Short, var2: Short, var3: Int, var4: Byte, var5: Byte, var6: Int, var7: Boolean, var8: Byte, var9: Byte, var10: Byte, var11: Byte, var12: Boolean, var13: Boolean, var14: Boolean, var15: Boolean, var16: Boolean, var17: Byte, var18: Boolean, var19: Boolean, var20: Int)

    override fun method8474() {
    }

    override fun getRenderingMatrix(): Matrix43 {
        return this.ag!!
    }

    external override fun onTilesDrawn()

    override fun mergeFunctionMasks(i_1: Int, i_2: Int): Int {
        return i_1 or i_2
    }

    override fun createComponentSurface(i_1: Int, i_2: Int, ints_3: IntArray, ints_4: IntArray): ComponentSurface {
        return wa(this, this.z!!, i_1, i_2, ints_3, ints_4)
    }

    companion object {
        @JvmField
        val ab: IntArray = IntArray(max(max(104, 20), 24573))
        @JvmField
        val ar: FloatArray = FloatArray(20)
        @JvmField
        val ae: IntArray = IntArray(6)
        @JvmField
        val aq: IntArray
        @JvmField
        val av: FloatArray
        @JvmField
        val aa: IntArray
        @JvmField
        val bi: IntArray
        @JvmField
        val bw: IntArray
        @JvmField
        val bx: IntArray
        @JvmField
        val bz: ShortArray
        @JvmField
        val bj: ByteArray

        init {
            aq = ab
            av = ar
            aa = ab
            bi = ab
            bw = IntArray(8191)
            bx = IntArray(8191)
            bz = ShortArray(8191)
            bj = ByteArray(8191)
        }

        @JvmStatic
        fun createToolkit(canvas0: Canvas?, interface221: MaterialProvider?, i_2: Int, i_3: Int): AbstractToolkit {
            return SoftwareToolkit(canvas0, interface221, i_2, i_3)
        }
    }
}
