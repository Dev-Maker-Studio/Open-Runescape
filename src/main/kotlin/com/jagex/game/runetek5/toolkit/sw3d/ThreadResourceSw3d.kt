package com.jagex.game.runetek5.toolkit.sw3d

import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelSw3d
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.ground.GroundSw3d
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.toolkit.SoftwareToolkit

/**
 * 3D graphics thread resource manager for software rendering toolkit.
 */
class ThreadResourceSw3d(
    @JvmField val aja50: SoftwareToolkit,
    width: Int,
    height: Int
) : INativeObject, AutoCloseable {

    @JvmField var nativeid: Long = 0
    @JvmField var aRunnable48: Runnable? = null
    @JvmField val ahArray49: Array<ModelSw3d> = Array(7) { ModelSw3d(aja50) }
    @JvmField val ahArray47: Array<ModelSw3d> = Array(7) { ModelSw3d(aja50) }
    @JvmField val aClass38451: Matrix44 = Matrix44()

    init {
        ha(nativeid, aja50, width, height)
    }

    fun method307() {
        aRunnable48 = Thread.currentThread()
        method310()
    }

    external fun ha(nativeId: Long, toolkit: AbstractToolkit, width: Int, height: Int)

    override fun close() {
        if (nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    override fun destroy(immediate: Boolean) {
        R(nativeid, immediate)
    }

    external fun R(nativeId: Long, immediate: Boolean)
    external fun b(nativeId: Long, toolkit: AbstractToolkit, ints2: IntArray, ints3: IntArray, ints4: IntArray, shorts5: ShortArray, count: Int)
    external fun ac(nativeId: Long, toolkit: AbstractToolkit, ints2: IntArray, ints3: IntArray, ints4: IntArray, shorts5: ShortArray, count: Int)

    fun method309(toolkit: AbstractToolkit, ints2: IntArray, ints3: IntArray, ints4: IntArray, shorts5: ShortArray, count: Int) {
        b(nativeid, toolkit, ints2, ints3, ints4, shorts5, count)
    }

    fun method310() {
        JA(nativeid)
    }

    fun method311(toolkit: AbstractToolkit, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int) {
        K(nativeid, toolkit, p2, p3, p4, p5, p6, p7, p8)
    }

    fun method313(model1: Model, model2: Model, p3: Int, p4: Int, p5: Int, bool6: Boolean) {
        P(nativeid, (model1 as ModelSw3d).nativeid, (model2 as ModelSw3d).nativeid, p3, p4, p5, bool6)
    }

    external fun aq(nativeId: Long, modelId: Long, ints: IntArray, floats: FloatArray)

    fun method314(model: Model, matrix: Matrix43, ints: IntArray, count: Int) {
        aClass38451.fromMatrix(matrix)
        wa(nativeid, (model as ModelSw3d).nativeid, aClass38451.faceList, ints, count)
    }

    external fun wa(nativeId: Long, modelId: Long, floats: FloatArray, ints: IntArray, count: Int)
    external fun ya(nativeId: Long, modelId: Long, p5: Int, p6: Int, floats: FloatArray, bool8: Boolean): Boolean
    external fun a(nativeId: Long)
    external fun J(nativeId: Long, modelId: Long, ints: IntArray, floats: FloatArray)

    fun method316(ground: Ground, p2: Int, p3: Int) {
        v(nativeid, (ground as GroundSw3d).nativeid, p2, p3)
    }

    fun method318(ground: Ground, p2: Int, p3: Int, p5: Int, p6: Int, p7: Int, p8: Int, bools: Array<BooleanArray>) {
        i(nativeid, (ground as GroundSw3d).nativeid, p2, p3, 1024, p5, p6, p7, p8, bools)
    }

    external fun v(nativeId: Long, groundId: Long, p5: Int, p6: Int)
    external fun bs(nativeId: Long, groundId: Long, p5: Int, p6: Int, p7: Int, p8: Int, p9: Int, p10: Int, p11: Int, bools: Array<BooleanArray>)
    external fun s(nativeId: Long, toolkit: AbstractToolkit, p4: Int, p5: Int)
    external fun i(nativeId: Long, groundId: Long, p5: Int, p6: Int, p7: Int, p8: Int, p9: Int, p10: Int, p11: Int, bools: Array<BooleanArray>)
    external fun bz(nativeId: Long, groundId: Long, p5: Int, p6: Int)
    external fun d(nativeId: Long)

    fun method325(model: ModelSw3d, poolId: Byte, param3: Int, bool4: Boolean): Model {
        var usePool = false
        val outputModel: ModelSw3d
        val tempModel: ModelSw3d

        if (poolId > 0 && poolId <= 7) {
            tempModel = ahArray47[poolId - 1]
            outputModel = ahArray49[poolId - 1]
            usePool = true
        } else {
            tempModel = ModelSw3d(aja50)
            outputModel = tempModel
        }

        model.BA(outputModel, tempModel, param3, usePool, bool4)
        outputModel.aClass87Array7139 = model.aClass87Array7139
        outputModel.aClass172Array7142 = model.aClass172Array7142
        return outputModel
    }

    external fun au(nativeId: Long, toolkit: AbstractToolkit, ints2: IntArray, ints3: IntArray, ints4: IntArray, shorts5: ShortArray, count: Int)
    external fun ay(nativeId: Long, toolkit: AbstractToolkit, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int, p9: Int, p10: Int)
    external fun at(nativeId: Long, toolkit: AbstractToolkit, ints2: IntArray, ints3: IntArray, ints4: IntArray, shorts5: ShortArray, count: Int)
    external fun K(nativeId: Long, toolkit: AbstractToolkit, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int, p9: Int, p10: Int)
    external fun aw(nativeId: Long, modelId1: Long, modelId2: Long, p7: Int, p8: Int, p9: Int, bool10: Boolean)
    external fun az(nativeId: Long, modelId1: Long, modelId2: Long, p7: Int, p8: Int, p9: Int, bool10: Boolean)

    fun method331(model: Model, ints: IntArray, matrix: Matrix43) {
        aClass38451.fromMatrix(matrix)
        J(nativeid, (model as ModelSw3d).nativeid, ints, aClass38451.faceList)
    }

    fun method335(model: Model, p2: Int, p3: Int, matrix: Matrix43, bool5: Boolean): Boolean {
        aClass38451.fromMatrix(matrix)
        return ya(nativeid, (model as ModelSw3d).nativeid, p2, p3, aClass38451.faceList, bool5)
    }

    external fun ah(nativeId: Long, modelId: Long, floats: FloatArray, ints: IntArray, count: Int)
    external fun ai(nativeId: Long, modelId: Long, floats: FloatArray, ints: IntArray, count: Int)
    external fun P(nativeId: Long, modelId1: Long, modelId2: Long, p7: Int, p8: Int, p9: Int, bool10: Boolean)
    external fun k(nativeId: Long, toolkit: AbstractToolkit, p4: Int, p5: Int)
    external fun JA(nativeId: Long)
    external fun bm(nativeId: Long, groundId: Long, p5: Int, p6: Int, p7: Int, p8: Int, p9: Int, p10: Int, p11: Int, bools: Array<BooleanArray>)
    external fun u(nativeId: Long, toolkit: AbstractToolkit, p4: Int, p5: Int)
}
