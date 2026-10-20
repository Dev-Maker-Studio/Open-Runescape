package com.jagex.game.runetek5.entity

import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Quaternion
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordinateSpace

open class SceneGraphNode {

    @JvmField var calculateAbsolutePosition: Boolean = true
    @JvmField var calculateNegatedPosition: Boolean = true
    @JvmField var projectionUpdate: Boolean = false
    @JvmField var firstChild: SceneGraphNode? = null
    @JvmField var parent: SceneGraphNode? = null
    @JvmField var nextSibling: SceneGraphNode? = null
    @JvmField val basePosition: CoordinateSpace = CoordinateSpace()

    private val _absolutePosition: CoordinateSpace = CoordinateSpace()
    private val _negatedPosition: CoordinateSpace = CoordinateSpace()
    private val _projectionMatrix: Matrix43

    init {
        Matrix44()
        Matrix44()
        _projectionMatrix = Matrix43()
        projectionUpdate = true
        parent = null
        firstChild = null
        nextSibling = null
    }

    open fun getBasePosition(): CoordinateSpace {
        return basePosition
    }

    open fun getAbsolutePosition(): CoordinateSpace {
        if (calculateAbsolutePosition) {
            calculateAbsolutePosition = false
            if (parent != null) {
                _absolutePosition.copy(basePosition)
                _absolutePosition.offset(parent!!.getAbsolutePosition())
            } else {
                _absolutePosition.copy(basePosition)
            }
        }
        return _absolutePosition
    }

    open fun getOffset(): CoordinateSpace {
        if (calculateNegatedPosition) {
            calculateNegatedPosition = false
            _negatedPosition.copy(getAbsolutePosition())
            _negatedPosition.negate()
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
        if (firstChild != null) {
            firstChild!!.update()
        }
    }

    open fun setBasePosition(baseX: Float, plane: Float, baseY: Float) {
        basePosition.coords.set(baseX, plane, baseY)
        requestProjectionUpdate()
        if (firstChild != null) {
            firstChild!!.update()
        }
    }

    open fun setAbsolutePosition(abs: CoordinateSpace) {
        if (parent != null) {
            val copy = CoordinateSpace(abs)
            copy.offset(parent!!.getOffset())
            setBasePosition(copy)
        } else {
            setBasePosition(abs)
        }
    }

    open fun setBasePosition(copyFrom: CoordinateSpace) {
        basePosition.copy(copyFrom)
        requestProjectionUpdate()
        if (firstChild != null) {
            firstChild!!.update()
        }
    }

    open fun requestProjectionUpdate() {
        calculateAbsolutePosition = true
        calculateNegatedPosition = true
        projectionUpdate = true
    }

    open fun detach() {
        if (parent != null) {
            var sceneGraphNode = parent!!.firstChild
            if (sceneGraphNode === this) {
                parent!!.firstChild = nextSibling
            } else {
                while (this !== sceneGraphNode!!.nextSibling) {
                    sceneGraphNode = sceneGraphNode.nextSibling
                }

                sceneGraphNode.nextSibling = nextSibling
            }
        }

        requestProjectionUpdate()
        if (firstChild != null) {
            firstChild!!.update()
            var sceneGraphNode = firstChild

            while (true) {
                sceneGraphNode!!.basePosition.offset(basePosition)
                sceneGraphNode.parent = parent
                if (sceneGraphNode.nextSibling == null) {
                    sceneGraphNode.nextSibling = parent!!.firstChild
                    parent!!.firstChild = firstChild
                    break
                }

                sceneGraphNode = sceneGraphNode.nextSibling
            }
        }

        parent = null
        nextSibling = null
        firstChild = null
    }

    open fun rotationUpdate(quaternion: Quaternion) {
        basePosition.quaternion.copy(quaternion)
        requestProjectionUpdate()
        if (firstChild != null) {
            firstChild!!.update()
        }
    }

    open fun update() {
        requestProjectionUpdate()
        if (firstChild != null) {
            firstChild!!.update()
        }

        if (nextSibling != null) {
            nextSibling!!.update()
        }
    }
}
