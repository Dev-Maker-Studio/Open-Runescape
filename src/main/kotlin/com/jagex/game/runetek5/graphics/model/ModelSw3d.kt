package com.jagex.game.runetek5.graphics.model

import com.jagex.game.runetek5.entity.BoundingBoxPolygon
import com.jagex.game.runetek5.graphics.model.billboard.Billboard
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.toolkit.SoftwareToolkit
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject
import com.jagex.game.runetek5.toolkit.sw3d.ba

//h
class ModelSw3d : Model, INativeObject, AutoCloseable {

    @JvmField var nativeid: Long = 0
    val aja7140: SoftwareToolkit
    val aba7141: ba?
    @JvmField var aClass87Array7139: Array<EmissiveTriangle>? = null
    @JvmField var aClass172Array7142: Array<EffectiveVertex>? = null

    constructor(ja1: SoftwareToolkit, ba2: ba, rsmesh3: Mesh, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        this.aja7140 = ja1
        this.aba7141 = ba2
        this.aClass87Array7139 = rsmesh3.particleEmitters
        this.aClass172Array7142 = rsmesh3.particleEffectors
        val i_8 = if (rsmesh3.particleEmitters == null) 0 else rsmesh3.particleEmitters!!.size
        val i_9 = if (rsmesh3.particleEffectors == null) 0 else rsmesh3.particleEffectors!!.size
        var i_10 = 0
        val ints_11 = IntArray(i_9 + i_8 * 3)

        var i_12: Int
        i_12 = 0
        while (i_12 < i_8) {
            ints_11[i_10++] = this.aClass87Array7139!![i_12].faceA
            ints_11[i_10++] = this.aClass87Array7139!![i_12].faceB
            ints_11[i_10++] = this.aClass87Array7139!![i_12].faceC
            i_12++
        }

        i_12 = 0
        while (i_12 < i_9) {
            ints_11[i_10++] = this.aClass172Array7142!![i_12].appliesOnVertex
            i_12++
        }

        i_12 = if (rsmesh3.billboards == null) 0 else rsmesh3.billboards!!.size
        val ints_13 = IntArray(i_12 * 8)
        var i_14 = 0

        var i_15: Int
        var class8416: Billboard
        i_15 = 0
        while (i_15 < i_12) {
            class8416 = rsmesh3.billboards!![i_15]
            val class34717 = BillboardTypeList.list(class8416.billboardId)
            ints_13[i_14++] = class8416.appliesOnFace
            ints_13[i_14++] = class34717.size2d
            ints_13[i_14++] = class34717.size3d
            ints_13[i_14++] = class34717.materialId
            ints_13[i_14++] = class34717.blendType
            ints_13[i_14++] = class34717.shape
            ints_13[i_14++] = if (class34717.hasUid) -1 else 0
            i_15++
        }

        i_15 = 0
        while (i_15 < i_12) {
            class8416 = rsmesh3.billboards!![i_15]
            ints_13[i_14++] = class8416.depth
            i_15++
        }

        U(this.aja7140, this.aba7141, rsmesh3.numVertices, rsmesh3.maxDepth, rsmesh3.verticesX, rsmesh3.verticesY, rsmesh3.verticesZ, rsmesh3.vertexGroups, rsmesh3.verticesSubmeshes, rsmesh3.numFaces, rsmesh3.facesA, rsmesh3.facesB, rsmesh3.facesC, rsmesh3.faceRenderType, rsmesh3.facesLayer, rsmesh3.facesAlpha, rsmesh3.facesTexture, rsmesh3.facesColor, rsmesh3.facesMaterial, rsmesh3.faceGroups, rsmesh3.priority, rsmesh3.facesSubmeshes, rsmesh3.numTextures, rsmesh3.texturesMapping, rsmesh3.textureFaceX, rsmesh3.textureFaceY, rsmesh3.textureFaceZ, rsmesh3.texturesScaleX, rsmesh3.texturesScaleY, rsmesh3.texturesScaleZ, rsmesh3.texturesRotation, rsmesh3.texturesDirection, rsmesh3.texturesSpeed, rsmesh3.texturesUtrans, rsmesh3.texturesVtrans, ints_11, i_8, i_9, i_4, i_5, i_6, i_7, ints_13)
    }

    constructor(ja1: SoftwareToolkit) {
        this.aja7140 = ja1
        this.aba7141 = null
        ba(ja1)
    }


    external fun U(var1: SoftwareToolkit, var2: ba?, var3: Int, var4: Int, var5: IntArray?, var6: IntArray?, var7: IntArray?, var8: IntArray?, var9: ShortArray?, var10: Int, var11: ShortArray?, var12: ShortArray?, var13: ShortArray?, var14: ByteArray?, var15: ByteArray?, var16: ByteArray?, var17: ByteArray?, var18: ShortArray?, var19: ShortArray?, var20: IntArray?, var21: Byte, var22: ShortArray?, var23: Int, var24: ByteArray?, var25: ShortArray?, var26: ShortArray?, var27: ShortArray?, var28: IntArray?, var29: IntArray?, var30: IntArray?, var31: ByteArray?, var32: ByteArray?, var33: IntArray?, var34: IntArray?, var35: IntArray?, var36: IntArray, var37: Int, var38: Int, var39: Int, var40: Int, var41: Int, var42: Int, var43: IntArray)

    external fun ba(var1: SoftwareToolkit)

    override fun close() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    external override fun destroy(var1: Boolean)

    override fun updateParticlePosition(projectionMatrix: Matrix43) {
        method11604(SoftwareToolkit.aa, projectionMatrix)
        var i_2 = 0
        var i_3: Int
        if (this.aClass87Array7139 != null) {
            i_3 = 0
            while (i_3 < this.aClass87Array7139!!.size) {
                val class874 = this.aClass87Array7139!![i_3]
                class874.verticeXA = SoftwareToolkit.aa[i_2++]
                class874.verticeYA = SoftwareToolkit.aa[i_2++]
                class874.verticeZA = SoftwareToolkit.aa[i_2++]
                class874.verticeXB = SoftwareToolkit.aa[i_2++]
                class874.verticeYB = SoftwareToolkit.aa[i_2++]
                class874.verticeZB = SoftwareToolkit.aa[i_2++]
                class874.verticeXC = SoftwareToolkit.aa[i_2++]
                class874.verticeYC = SoftwareToolkit.aa[i_2++]
                class874.verticeZC = SoftwareToolkit.aa[i_2++]
                i_3++
            }
        }

        if (this.aClass172Array7142 != null) {
            i_3 = 0
            while (i_3 < this.aClass172Array7142!!.size) {
                val class1726 = this.aClass172Array7142!![i_3]
                var class1725: EffectiveVertex? = class1726
                if (class1726.child != null) {
                    class1725 = class1726.child
                }

                if (class1726.matrix == null) {
                    class1726.matrix = Matrix44()
                }

                class1726.matrix!!.fromMatrix(projectionMatrix)
                class1725!!.verticeX = SoftwareToolkit.aa[i_2++]
                class1725.verticeY = SoftwareToolkit.aa[i_2++]
                class1725.verticeZ = SoftwareToolkit.aa[i_2++]
                i_3++
            }
        }

    }

    external fun BA(var1: ModelSw3d, var2: ModelSw3d, var3: Int, var4: Boolean, var5: Boolean)

    external override fun applyShading(var1: Int)

    external override fun getFunctionMask(): Int

    override fun uploadToGpu() {
        // NOOP
    }

    external override fun getSize3d(): Int


    external override fun zaxisRotateWithoutNormals(var1: Int)

    external override fun translate(var1: Int, var2: Int, var3: Int)

    external override fun mirror()

    external override fun scale(var1: Int, var2: Int, var3: Int)

    override fun awaitLock() {
        if (this.aja7140.`as` > 1) {
            synchronized(this) {
                while (super.isLocked) {
                    try {
                        (this as Object).wait()
                    } catch (ignored: InterruptedException) {
                    }
                }

                super.isLocked = true
            }
        }

    }

    external override fun getMaxZ(): Int

    external override fun startAnimation(): Boolean

    external fun J(var1: Long, var3: Int, var4: IntArray, var5: Int, var6: Int, var7: Int, var8: Int, var9: Boolean)

    external override fun animateTransform(var1: Int, var2: IntArray, var3: Int, var4: Int, var5: Int, var6: Boolean, var7: Int, var8: IntArray)


    external override fun endAnimation()


    external fun aa(var1: FloatArray, var2: Int, var3: Boolean)


    override fun calculateProjection(i_1: Int, i_2: Int, matrix44var3: Matrix43, bool4: Boolean, i_5: Int): Boolean {
        return this.aja7140.nx().method335(this, i_1, i_2, matrix44var3, bool4)
    }

    external override fun tint(var1: Int, var2: Int, var3: Int, var4: Int)

    external fun ft(var1: SoftwareToolkit)


    external override fun getMaxX(): Int

    external override fun getMinY(): Int

    external override fun getMaxY(): Int

    external override fun getMinZ(): Int

    external fun IA(var1: Byte, var2: ByteArray)

    external override fun setContrast(var1: Int)

    override fun unlock() {
        if (this.aja7140.`as` > 1) {
            synchronized(this) {
                super.isLocked = false
                (this as Object).notifyAll()
            }
        }

    }


    external override fun getAlpha(): ByteArray

    external override fun recolor(var1: Short, var2: Short)

    override fun applyEnvironmentAlpha(b_1: Byte, bytes_2: ByteArray) {
        IA(b_1, bytes_2)
    }


    external override fun rematerial(var1: Short, var2: Short)

    override fun offsetSubmesh(matrix44var1: Matrix43, i_2: Int, bool3: Boolean) {
        val matrix444 = this.aja7140.nx().aClass38451
        matrix444.fromMatrix(matrix44var1)
        aa(matrix444.faceList, i_2, bool3)
    }


    external override fun accountForLayers(): Boolean

    external override fun hasAnimatedTexture(): Boolean

    override fun getEmissiveTriangles(): Array<EmissiveTriangle>? {
        return this.aClass87Array7139
    }

    override fun getEffectiveVertices(): Array<EffectiveVertex>? {
        return this.aClass172Array7142
    }




    override fun drawMatrixThreaded(matrix44var1: Matrix43, class275Sub52: BoundingBoxPolygon?, i_3: Int) {
        if (class275Sub52 == null) {
            this.aja7140.nx().method314(this, matrix44var1, IntArray(0), i_3)
        } else {
            SoftwareToolkit.ae[5] = 0
            this.aja7140.nx().method314(this, matrix44var1, SoftwareToolkit.ae, i_3)
            class275Sub52.left = SoftwareToolkit.ae[0]
            class275Sub52.bottom = SoftwareToolkit.ae[1]
            class275Sub52.right = SoftwareToolkit.ae[2]
            class275Sub52.top = SoftwareToolkit.ae[3]
            class275Sub52.size = SoftwareToolkit.ae[4]
            class275Sub52.positionCalculated = SoftwareToolkit.ae[5] != 0
        }

    }



    external override fun setAmbient(var1: Int)



    external override fun getAmbient(): Int



    fun method11604(ints_1: IntArray, matrix44var2: Matrix43) {
        this.aja7140.nx().method331(this, ints_1, matrix44var2)
    }

    external override fun createShadowSprite(var1: Shadow): Shadow


    override fun animatePartialtransform(i_1: Int, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, bool7: Boolean) {
        J(this.nativeid, i_1, ints_2, i_3, i_4, i_5, i_6, bool7)
    }

    external override fun xaxisRotateWithoutNormals(var1: Int)



    external override fun rotateWithNormals(var1: Int)






    external override fun getMinX(): Int

    external override fun animateShadowtransform(var1: Int, var2: Int, var3: Int, var4: Int)





    override fun areTexturesCached(): Boolean {
        return true
    }



    override fun copy(b_1: Byte, i_2: Int, bool3: Boolean): Model {
        return this.aja7140.nx().method325(this, b_1, i_2, bool3)
    }


    external override fun yaxisRotateWithoutNormals(var1: Int)



    override fun applyShading(meshrasterizer1: Model, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean) {
        this.aja7140.nx().method313(this, meshrasterizer1, i_2, i_3, i_4, bool5)
    }



    external override fun getSize2d(): Int



    external override fun getContrast(): Int


    external override fun contourToGround(var1: Int, var2: Int, var3: Ground, var4: Ground, var5: Int, var6: Int, var7: Int)

    external fun fe(var1: SoftwareToolkit, var2: ba, var3: Int, var4: Int, var5: IntArray?, var6: IntArray?, var7: IntArray?, var8: IntArray?, var9: ShortArray?, var10: Int, var11: ShortArray?, var12: ShortArray?, var13: ShortArray?, var14: ByteArray?, var15: ByteArray?, var16: ByteArray?, var17: ByteArray?, var18: ShortArray?, var19: ShortArray?, var20: IntArray?, var21: Byte, var22: ShortArray?, var23: Int, var24: ByteArray?, var25: ShortArray?, var26: ShortArray?, var27: ShortArray?, var28: IntArray?, var29: IntArray?, var30: IntArray?, var31: ByteArray?, var32: ByteArray?, var33: IntArray?, var34: IntArray?, var35: IntArray?, var36: IntArray, var37: Int, var38: Int, var39: Int, var40: Int, var41: Int, var42: Int, var43: IntArray)

    external fun fk(var1: SoftwareToolkit, var2: ba, var3: Int, var4: Int, var5: IntArray?, var6: IntArray?, var7: IntArray?, var8: IntArray?, var9: ShortArray?, var10: Int, var11: ShortArray?, var12: ShortArray?, var13: ShortArray?, var14: ByteArray?, var15: ByteArray?, var16: ByteArray?, var17: ByteArray?, var18: ShortArray?, var19: ShortArray?, var20: IntArray?, var21: Byte, var22: ShortArray?, var23: Int, var24: ByteArray?, var25: ShortArray?, var26: ShortArray?, var27: ShortArray?, var28: IntArray?, var29: IntArray?, var30: IntArray?, var31: ByteArray?, var32: ByteArray?, var33: IntArray?, var34: IntArray?, var35: IntArray?, var36: IntArray, var37: Int, var38: Int, var39: Int, var40: Int, var41: Int, var42: Int, var43: IntArray)

    external fun fh(var1: SoftwareToolkit)

    external fun fg(var1: ModelSw3d, var2: ModelSw3d, var3: Int, var4: Boolean, var5: Boolean)

    external fun fz(var1: Long, var3: Int, var4: IntArray, var5: Int, var6: Int, var7: Int, var8: Int, var9: Boolean)

    external fun fp(var1: Long, var3: Int, var4: IntArray, var5: Int, var6: Int, var7: Int, var8: Int, var9: Boolean)

    external fun fb(var1: Long, var3: Int, var4: IntArray, var5: Int, var6: Int, var7: Int, var8: Int, var9: Boolean)

    external fun fv(var1: Long, var3: Int, var4: IntArray, var5: Int, var6: Int, var7: Int, var8: Int, var9: Boolean)


    external fun ff(var1: Byte, var2: ByteArray)

    external fun fr(var1: Byte, var2: ByteArray)

    external fun fw(var1: Byte, var2: ByteArray)


}
