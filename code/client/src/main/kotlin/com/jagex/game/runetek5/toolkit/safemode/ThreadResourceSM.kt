package com.jagex.game.runetek5.toolkit.safemode

import com.jagex.game.runetek5.graphics.model.ModelSM
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.SafeModeToolkit

class ThreadResourceSM(
    @JvmField val toolkit: SafeModeToolkit
) {
    @JvmField var fogEnabled: Boolean = false
    @JvmField var offsetX: Float = 0f
    @JvmField var centerX: Float = 0f
    @JvmField var offsetY: Float = 0f
    @JvmField var centerY: Float = 0f
    @JvmField var clipWidth: Int = 0
    @JvmField var runnable: Runnable? = null
    @JvmField var rasterizer: RasterizerSM? = null
    @JvmField var fogStartDistance = 0.85f
    @JvmField var fogRange: Float = 0f
    @JvmField var fogColor: Int = 0
    @JvmField var lastWaterColor: Int = 0

    @JvmField var hasWaterUpdate: Boolean = false

    @JvmField var waterAverageHeight: Int = 0
    @JvmField var waterScale: Int = 0
    @JvmField var zbuffering: Boolean = false
    @JvmField val scratchMatrix44: Matrix44
    @JvmField val renderingMatrix43: Matrix43
    @JvmField val submeshMatrix43: Matrix43
    @JvmField val particleMatrix: Matrix44
    @JvmField val renderingMatrix: Matrix44
    @JvmField val waterRenderHeight: IntArray
    @JvmField val projectedVerticesX: FloatArray
    @JvmField val projectedVerticesY: FloatArray
    @JvmField val projectedVerticesZ: FloatArray
    @JvmField val projectedVerticesZ2: FloatArray
    @JvmField val matrixX: IntArray
    @JvmField val matrixY: IntArray
    @JvmField val matrixZ: IntArray
    @JvmField val renderCycleByVertex: IntArray
    @JvmField val renderCycleByDepth: IntArray
    @JvmField var cachedDepthRangeFar: Float = 0f
    @JvmField var cachedDepthRangeNear: Float = 0f
    @JvmField var inverseDepthRangeFar: Float = 0f
    @JvmField val uvTypes: FloatArray
    @JvmField val copyDstModels: Array<ModelSM>
    @JvmField val copySrcModels: Array<ModelSM>
    @JvmField val perspectiveX: FloatArray
    @JvmField val perspectiveY: FloatArray
    @JvmField val perspectiveZNear: FloatArray
    @JvmField val perspectiveZFar: FloatArray
    @JvmField val fogAlphaBuffer: FloatArray
    @JvmField val particleOffsets: FloatArray
    @JvmField val billboardIdByFace: IntArray

    init {
        fogRange = 1.0f - fogStartDistance
        fogColor = 0
        lastWaterColor = 0
        hasWaterUpdate = false
        waterAverageHeight = 0
        waterScale = 0
        zbuffering = true
        scratchMatrix44 = Matrix44()
        renderingMatrix43 = Matrix43()
        submeshMatrix43 = Matrix43()
        particleMatrix = Matrix44()
        renderingMatrix = Matrix44()
        waterRenderHeight = IntArray(ModelSM.maxModelVertices)
        projectedVerticesX = FloatArray(ModelSM.maxModelVertices)
        projectedVerticesY = FloatArray(ModelSM.maxModelVertices)
        projectedVerticesZ = FloatArray(ModelSM.maxModelVertices)
        projectedVerticesZ2 = FloatArray(ModelSM.maxModelVertices)
        matrixX = IntArray(8)
        matrixY = IntArray(8)
        matrixZ = IntArray(8)
        renderCycleByVertex = IntArray(10000)
        renderCycleByDepth = IntArray(10000)
        cachedDepthRangeFar = 1.0f
        cachedDepthRangeNear = 0.0f
        inverseDepthRangeFar = 1.0f
        uvTypes = FloatArray(2)
        copyDstModels = Array(7) { ModelSM(toolkit) }
        copySrcModels = Array(7) { ModelSM(toolkit) }
        perspectiveX = FloatArray(64)
        perspectiveY = FloatArray(64)
        perspectiveZNear = FloatArray(64)
        perspectiveZFar = FloatArray(64)
        fogAlphaBuffer = FloatArray(64)
        particleOffsets = FloatArray(3)
        rasterizer = RasterizerSM(toolkit, this)
        billboardIdByFace = IntArray(ModelSM.maxModelTriangles)
        for (index in 0 until ModelSM.maxModelTriangles) {
            billboardIdByFace[index] = -1
        }
    }

    fun setRunnable(runnable: Runnable) {
        this.runnable = runnable
    }

    fun initializeRasterizer() {
        rasterizer = RasterizerSM(toolkit, this)
    }
}
