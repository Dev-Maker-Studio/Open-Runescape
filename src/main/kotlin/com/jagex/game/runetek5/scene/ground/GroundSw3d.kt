package com.jagex.game.runetek5.scene.ground

import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.SoftwareToolkit
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject
import com.jagex.game.runetek5.toolkit.sw3d.ba

class GroundSw3d(
    ja1: SoftwareToolkit,
    ba2: ba,
    i_3: Int,
    i_4: Int,
    ints_5: Array<IntArray>,
    ints_6: Array<IntArray>,
    i_8: Int,
    i_9: Int
) : Ground(i_3, i_4, 512, ints_5), INativeObject, AutoCloseable {

    @JvmField var nativeid: Long = 0L
    @JvmField val aClass4827143: NodeCollection = NodeCollection()
    @JvmField val aja7144: SoftwareToolkit

    init {
        aja7144 = ja1
        G(aja7144, ba2, i_3, i_4, tileHeights, ints_6, 512, i_8, i_9)
    }

    external fun G(var1: SoftwareToolkit, var2: ba, var3: Int, var4: Int, var5: Array<IntArray>, var6: Array<IntArray>, var7: Int, var8: Int, var9: Int)

    external fun ap(var1: FloatArray)

    override external fun setTileShadow(var1: Int, var2: Int, var3: Int)

    override fun addBlendedTile(i_1: Int, i_2: Int, ints_3: IntArray, ints_4: IntArray, ints_5: IntArray, ints_6: IntArray, ints_7: IntArray, ints_8: IntArray, ints_9: IntArray, ints_10: IntArray, class9011: WaterTile?, bool12: Boolean) {
        ya(i_1, i_2, ints_3, ints_4, ints_5, ints_6, ints_7, ints_8, ints_9, ints_10, class9011!!.color, class9011.scale, class9011.intensity, bool12)
    }

    override fun calculateBlockTiles(i_1: Int, i_2: Int, ints_3: IntArray, ints_4: IntArray, ints_5: IntArray, ints_6: IntArray, ints_7: IntArray, ints_8: IntArray, ints_9: IntArray, ints_10: IntArray, ints_11: IntArray, ints_12: IntArray, ints_13: IntArray, class9014: WaterTile?) {
        var bool16 = false
        val ints_17 = ints_10

        for (i_18 in ints_17.indices) {
            val i_33 = ints_17[i_18]
            if (i_33 != -1) {
                bool16 = true
                break
            }
        }

        assert(ints_10.isNotEmpty())
        val i_31 = ints_10.size
        val ints_32 = IntArray(i_31 * 3)
        val ints_19 = IntArray(i_31 * 3)
        val ints_20 = IntArray(i_31 * 3)
        val ints_21 = IntArray(i_31 * 3)
        val ints_22 = IntArray(i_31 * 3)
        val ints_23 = IntArray(i_31 * 3)
        val ints_24 = IntArray(i_31 * 3)
        val ints_25 = IntArray(i_31 * 3)
        var i_26 = 0

        for (i_27 in 0 until i_31) {
            val i_28 = ints_7[i_27]
            val i_29 = ints_8[i_27]
            val i_30 = ints_9[i_27]
            ints_32[i_26] = ints_3[i_28]
            ints_19[i_26] = ints_5[i_28]
            ints_20[i_26] = ints_10[i_27]
            ints_21[i_26] = ints_12[i_27]
            ints_22[i_26] = ints_13[i_27]
            ints_23[i_26] = ints_11[i_27]

            ints_24[i_26] = ints_4[i_28]

            ints_25[i_26] = ints_6[i_28]

            ++i_26
            ints_32[i_26] = ints_3[i_29]
            ints_19[i_26] = ints_5[i_29]
            ints_20[i_26] = ints_10[i_27]
            ints_21[i_26] = ints_12[i_27]
            ints_22[i_26] = ints_13[i_27]
            ints_23[i_26] = ints_11[i_27]

            ints_24[i_26] = ints_4[i_29]

            ints_25[i_26] = ints_6[i_29]

            ++i_26
            ints_32[i_26] = ints_3[i_30]
            ints_19[i_26] = ints_5[i_30]
            ints_20[i_26] = ints_10[i_27]
            ints_21[i_26] = ints_12[i_27]
            ints_22[i_26] = ints_13[i_27]
            ints_23[i_26] = ints_11[i_27]

            ints_24[i_26] = ints_4[i_30]

            ints_25[i_26] = ints_6[i_30]

            ++i_26
        }

        if (bool16) {
            ya(i_1, i_2, ints_32, ints_24, ints_19, ints_25, ints_20, ints_23, ints_21, ints_22, class9014!!.color, class9014.scale, class9014.intensity, false)
        }

    }

    override fun renderFloor(i_1: Int, i_2: Int, i_3: Int, bools_4: Array<BooleanArray>, bool5: Boolean) {
        var i_7 = 0
        val floats_8 = FloatArray(aClass4827143.size())

        var class282Sub249 = aClass4827143.getFirst() as PointLight?
        while (class282Sub249 != null) {
            floats_8[i_7++] = class282Sub249.getAlpha()
            class282Sub249 = aClass4827143.next() as PointLight?
        }

        t(floats_8)

        for (i_13 in 0 until i_3 + i_3) {
            for (i_10 in 0 until i_3 + i_3) {
                if (bools_4[i_13][i_10]) {
                    val i_11 = i_13 + (i_1 - i_3)
                    val i_12 = i_10 + (i_2 - i_3)
                    if (i_11 >= 0 && i_11 < width && i_12 >= 0 && i_12 < length) {
                        aja7144.nx().method316(this, i_11, i_12)
                    }
                }
            }
        }

    }

    override fun renderMinimapFloor(i_1: Int, i_2: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, bools_8: Array<BooleanArray>) {
        aja7144.nx().method318(this, i_1, i_2, i_4, i_5, i_6, i_7, bools_8)
    }

    override external fun createShadowAt(var1: Int, var2: Int, var3: Shadow): Shadow

    override external fun addShadow(var1: Shadow, var2: Int, var3: Int, var4: Int)

    override external fun removeShadow(var1: Shadow, var2: Int, var3: Int, var4: Int)

    override fun addLight(class282Sub241: PointLight, ints_2: IntArray) {
        aClass4827143.addFirst(class282Sub241)
        A(class282Sub241.hashCode(), class282Sub241.getX(), class282Sub241.getY(), class282Sub241.getZ(), class282Sub241.getSize2d(), class282Sub241.getColor(), ints_2)
    }

    external fun A(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: IntArray)

    external fun t(var1: FloatArray)



    override fun close() {
        if (nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }

    }


    external fun ya(var1: Int, var2: Int, var3: IntArray, var4: IntArray, var5: IntArray, var6: IntArray, var7: IntArray, var8: IntArray, var9: IntArray, var10: IntArray, var11: Int, var12: Int, var13: Int, var14: Boolean)

    override fun hasRenderingShadows(class282Sub50Sub171: Shadow, i_2: Int, i_3: Int, i_4: Int): Boolean {
        return true
    }

    override external fun destroy(var1: Boolean)

    override external fun resetLight()

    external fun az(var1: SoftwareToolkit, var2: ba, var3: Int, var4: Int, var5: Array<IntArray>, var6: Array<IntArray>, var7: Int, var8: Int, var9: Int)

    external fun ax(var1: Int, var2: Int, var3: IntArray, var4: IntArray, var5: IntArray, var6: IntArray, var7: IntArray, var8: IntArray, var9: IntArray, var10: IntArray, var11: Int, var12: Int, var13: Int, var14: Boolean)

}
