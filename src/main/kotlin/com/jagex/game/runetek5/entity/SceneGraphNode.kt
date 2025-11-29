package com.jagex.game.runetek5.entity

import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Quaternion
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordinateSpace

/**
 * Scene graph node for hierarchical position/rotation management.
 *
 * Based on native jag::graphics::GraphNode class.
 *
 * @see jag::graphics::GraphNode::AddChild
 * @see jag::graphics::GraphNode::CalculateBounds
 * @see jag::graphics::GraphNode::Detach
 * @see jag::graphics::GraphNode::GetWorldTransformationMatrixAligned
 * @see jag::graphics::GraphNode::GetWorldTranslation
 */
open class SceneGraphNode {

    @JvmField var calculateAbsolutePosition: Boolean = true
    @JvmField var aBool5945: Boolean = true
    @JvmField var projectionUpdate: Boolean = false
    @JvmField var aGraphNode5944: SceneGraphNode? = null
    @JvmField var aGraphNode5943: SceneGraphNode? = null
    @JvmField var aGraphNode5940: SceneGraphNode? = null
    @JvmField val basePosition: CoordinateSpace = CoordinateSpace()

    /**
     * Absolute position (upscaled by << 2)
     * Private backing field - use getAbsolutePosition() for public access
     */
    private val _absolutePosition: CoordinateSpace = CoordinateSpace()
    private val _aClass3055939: CoordinateSpace = CoordinateSpace()
    private val _projectionMatrix: Matrix43

    init {
        Matrix44()
        Matrix44()
        _projectionMatrix = Matrix43()
        projectionUpdate = true
        aGraphNode5943 = null
        aGraphNode5944 = null
        aGraphNode5940 = null
    }

    open fun getBasePosition(): CoordinateSpace {
        return basePosition
    }

    /**
     * Gets the absolute position (upscaled by << 2)
     */
    open fun getAbsolutePosition(): CoordinateSpace {
        if (calculateAbsolutePosition) {
            calculateAbsolutePosition = false
            if (aGraphNode5943 != null) {
                _absolutePosition.copy(basePosition)
                _absolutePosition.offset(aGraphNode5943!!.getAbsolutePosition())
            } else {
                _absolutePosition.copy(basePosition)
            }
        }
        return _absolutePosition
    }

    open fun getOffset(): CoordinateSpace {
        if (aBool5945) {
            aBool5945 = false
            _aClass3055939.copy(getAbsolutePosition())
            _aClass3055939.negate()
        }

        return _absolutePosition
    }

    open fun getProjectionMatrix(): Matrix43 {
        if (projectionUpdate) {
            projectionUpdate = false
            _projectionMatrix.fromCoordinateSpace(getAbsolutePosition())
        }
        return _projectionMatrix
    }

    open fun setBasePosition(copyFrom: Vector3) {
        basePosition.coords.copy(copyFrom)
        requestProjectionUpdate()
        if (aGraphNode5944 != null) {
            aGraphNode5944!!.update()
        }
    }

    open fun setBasePosition(baseX: Float, plane: Float, baseY: Float) {
        basePosition.coords.set(baseX, plane, baseY)
        requestProjectionUpdate()
        if (aGraphNode5944 != null) {
            aGraphNode5944!!.update()
        }
    }

    open fun setAbsolutePosition(abs: CoordinateSpace) {
        if (aGraphNode5943 != null) {
            val copy = CoordinateSpace(abs)
            copy.offset(aGraphNode5943!!.getOffset())
            setBasePosition(copy)
        } else {
            setBasePosition(abs)
        }
    }

    open fun setBasePosition(copyFrom: CoordinateSpace) {
        basePosition.copy(copyFrom)
        requestProjectionUpdate()
        if (aGraphNode5944 != null) {
            aGraphNode5944!!.update()
        }
    }

    open fun requestProjectionUpdate() {
        calculateAbsolutePosition = true
        aBool5945 = true
        projectionUpdate = true
    }

    open fun method11176() {
        if (aGraphNode5943 != null) {
            var sceneGraphNode = aGraphNode5943!!.aGraphNode5944
            if (sceneGraphNode === this) {
                aGraphNode5943!!.aGraphNode5944 = aGraphNode5940
            } else {
                while (this !== sceneGraphNode!!.aGraphNode5940) {
                    sceneGraphNode = sceneGraphNode.aGraphNode5940
                }

                sceneGraphNode.aGraphNode5940 = aGraphNode5940
            }
        }

        requestProjectionUpdate()
        if (aGraphNode5944 != null) {
            aGraphNode5944!!.update()
            var sceneGraphNode = aGraphNode5944

            while (true) {
                sceneGraphNode!!.basePosition.offset(basePosition)
                sceneGraphNode.aGraphNode5943 = aGraphNode5943
                if (sceneGraphNode.aGraphNode5940 == null) {
                    sceneGraphNode.aGraphNode5940 = aGraphNode5943!!.aGraphNode5944
                    aGraphNode5943!!.aGraphNode5944 = aGraphNode5944
                    break
                }

                sceneGraphNode = sceneGraphNode.aGraphNode5940
            }
        }

        aGraphNode5943 = null
        aGraphNode5940 = null
        aGraphNode5944 = null
    }

    open fun rotationUpdate(quaternion: Quaternion) {
        basePosition.quaternion.copy(quaternion)
        requestProjectionUpdate()
        if (aGraphNode5944 != null) {
            aGraphNode5944!!.update()
        }
    }

    open fun update() {
        requestProjectionUpdate()
        if (aGraphNode5944 != null) {
            aGraphNode5944!!.update()
        }

        if (aGraphNode5940 != null) {
            aGraphNode5940!!.update()
        }
    }
}
