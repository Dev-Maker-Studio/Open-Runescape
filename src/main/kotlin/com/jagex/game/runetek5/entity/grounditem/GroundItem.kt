package com.jagex.game.runetek5.entity.grounditem

import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.entity.InteractiveEntityContainer
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.PROJECTION
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.client.PreferenceManager
import kotlin.math.atan2
import kotlin.math.min

class GroundItem(
    scene: Scene,
    x: Int,
    y: Int,
    z: Int,
    plane: Int,
    collisionPlane: Int
) : AbstractGroundItem(scene, x, y, z, plane, collisionPlane) {

    @JvmField
    var amount: Int = 0

    @JvmField
    var lastAmount: Int = 0

    @JvmField
    var nodeAmount: Int = 0

    @JvmField
    var nodeId: Int = 0

    @JvmField
    var tileHeightOffset: Int = 0

    @JvmField
    var lastId: Int = -1

    @JvmField
    var itemId: Int = -1

    @JvmField
    var tintHue: Int = 0

    @JvmField
    var tintSaturation: Int = 0

    @JvmField
    var tintLightness: Int = 0

    @JvmField
    var tintOpacity: Int = 0

    @JvmField
    var layerPriorities: Boolean = false

    @JvmField
    var size3d: Int = 0

    init {
        setupBoundingBox(3)
        randomizeAppearance()
    }

    override fun getIconHeight(): Int {
        return tileHeightOffset - getMinY()
    }

    fun randomizeAppearance() {
        tintHue = 32 + (Math.random() * 4.0).toInt()
        tintSaturation = 3 + (Math.random() * 2.0).toInt()
        tintLightness = 16 + (Math.random() * 3.0).toInt()
        tintOpacity = if (PreferenceManager.preferences!!.textures!!.value == 1) {
            (Math.random() * 10.0).toInt()
        } else {
            (Math.random() * 20.0).toInt()
        }
    }

    override fun hasAlpha(): Boolean {
        return layerPriorities
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        return null
    }

    override fun getMinY(): Int {
        return -10
    }

    override fun particleCycle(graphicalrenderer1: AbstractToolkit) {
    }

    override fun disregardTextureMovement(): Boolean {
        return false
    }

    override fun calculateProjection(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean {
        val renderingMatrix = toolkit.getRenderingMatrix()
        renderingMatrix.translateFromCoordspace(getAbsolutePosition())
        renderingMatrix.translate(0.0f, -10.0f, 0.0f)
        var model = TypeLists.OBJ!!.list(nodeId).getAnimatedModel(toolkit, PROJECTION, nodeAmount, null, null, 0, 0, 0, 0)
        if (model != null && model.calculateProjection(mouseX, mouseY, renderingMatrix, true, 0)) {
            return true
        }
        if (lastId != -1) {
            model = TypeLists.OBJ!!.list(lastId).getAnimatedModel(toolkit, PROJECTION, lastAmount, null, null, 0, 0, 0, 0)
            if (model != null && model.calculateProjection(mouseX, mouseY, renderingMatrix, true, 0)) {
                return true
            }
        }

        if (itemId != -1) {
            model = TypeLists.OBJ!!.list(itemId).getAnimatedModel(toolkit, PROJECTION, amount, null, null, 0, 0, 0, 0)
            return model != null && model.calculateProjection(mouseX, mouseY, renderingMatrix, true, 0)
        }

        return false
    }

    override fun getMaxSizeShift(): Int {
        val objType = TypeLists.OBJ!!.list(nodeId)
        var sizeShift = objType.pickSizeShift
        if (lastId != -1) {
            val nextObjType = TypeLists.OBJ!!.list(lastId)
            if (nextObjType.pickSizeShift > sizeShift) {
                sizeShift = nextObjType.pickSizeShift
            }
        }

        if (itemId != -1) {
            val nextObjType = TypeLists.OBJ!!.list(itemId)
            if (nextObjType.pickSizeShift > sizeShift) {
                sizeShift = nextObjType.pickSizeShift
            }
        }

        return sizeShift
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity {
        val vector33 = Vector3Pool.vector3From(getAbsolutePosition().coords)
        val interactive = scene!!.getInteractiveObjects(plane.toInt(), vector33.x.toInt() shr 9, vector33.z.toInt() shr 9)
        val groundDecoration = scene!!.getGroundDecoration(plane.toInt(), vector33.x.toInt() shr 9, vector33.z.toInt() shr 9)
        var offsetY = 0
        if (interactive != null && interactive.interactiveObject!!.supportsItems) {
            offsetY = interactive.interactiveObject!!.getMinY()
        }

        if (groundDecoration != null && groundDecoration.floorOffsetY > -offsetY) {
            offsetY = -groundDecoration.floorOffsetY
        }

        if (offsetY != tileHeightOffset) {
            vector33.y += (offsetY - tileHeightOffset).toFloat()
            setBasePosition(vector33)
            tileHeightOffset = offsetY
        }

        val renderingMatrix = toolkit.getRenderingMatrix()
        renderingMatrix.reset()
        if (tileHeightOffset == 0) {
            val heightmap = scene!!.localHeightmap!![collisionPlane.toInt()]!!
            val sampleSize = size3d shl 1
            val nwOffsetX = -sampleSize / 2
            val nwOffsetZ = -sampleSize / 2
            val nwHeight = heightmap!!.getAverageHeight(nwOffsetX + vector33.x.toInt(), nwOffsetZ + vector33.z.toInt())
            val neOffsetX = sampleSize / 2
            val neOffsetZ = -sampleSize / 2
            val neHeight = heightmap!!.getAverageHeight(neOffsetX + vector33.x.toInt(), neOffsetZ + vector33.z.toInt())
            val swOffsetX = -sampleSize / 2
            val swOffsetZ = sampleSize / 2
            val swHeight = heightmap!!.getAverageHeight(swOffsetX + vector33.x.toInt(), swOffsetZ + vector33.z.toInt())
            val seOffsetX = sampleSize / 2
            val seOffsetZ = sampleSize / 2
            val seHeight = heightmap!!.getAverageHeight(seOffsetX + vector33.x.toInt(), seOffsetZ + vector33.z.toInt())
            val minNorthHeight = min(nwHeight, neHeight)
            val minSouthHeight = min(swHeight, seHeight)
            val minEastHeight = min(neHeight, seHeight)
            val minWestHeight = min(nwHeight, swHeight)
            var contourAngle: Int
            if (sampleSize != 0) {
                contourAngle = (atan2((minNorthHeight - minSouthHeight).toDouble(), sampleSize.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
                if (contourAngle != 0) {
                    renderingMatrix.rotate(1.0f, 0.0f, 0.0f, MathUtils.getTheta(contourAngle))
                }
            }

            if (sampleSize != 0) {
                contourAngle = (atan2((minWestHeight - minEastHeight).toDouble(), sampleSize.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
                if (contourAngle != 0) {
                    renderingMatrix.rotate(0.0f, 0.0f, 1.0f, MathUtils.getTheta(-contourAngle))
                }
            }

            contourAngle = nwHeight + seHeight
            if (neHeight + swHeight < contourAngle) {
                contourAngle = neHeight + swHeight
            }

            contourAngle = (contourAngle shr 1) - vector33.y.toInt()
            if (contourAngle != 0) {
                renderingMatrix.translate(0.0f, contourAngle.toFloat(), 0.0f)
            }
        }

        vector33.pushVectorStack()
        val absolute = getAbsolutePosition().coords
        renderingMatrix.translate(absolute.x, absolute.y - 10.0f, absolute.z)
        val renderedEntity = RenderedEntityCache.generate(true)
        layerPriorities = false
        size3d = 0
        var model: Model?
        if (itemId != -1) {
            model = TypeLists.OBJ!!.list(itemId).getAnimatedModel(toolkit, 526336, amount, null, null, tintHue, tintSaturation, tintLightness, tintOpacity)
            if (model != null) {
                model.drawMatrixThreaded(renderingMatrix, boundingBoxPolygons!![2], ModelFunctionMaskGL.EMPTY)
                layerPriorities = layerPriorities or model.accountForLayers()
                size3d = model.getSize3d()
            }
        }

        if (lastId != -1) {
            model = TypeLists.OBJ!!.list(lastId).getAnimatedModel(toolkit, 526336, lastAmount, null, null, tintHue, tintSaturation, tintLightness, tintOpacity)
            if (model != null) {
                model.drawMatrixThreaded(renderingMatrix, boundingBoxPolygons!![1], ModelFunctionMaskGL.EMPTY)
                layerPriorities = layerPriorities or model.accountForLayers()
                if (model.getSize3d() > size3d) {
                    size3d = model.getSize3d()
                }
            }
        }

        model = TypeLists.OBJ!!.list(nodeId).getAnimatedModel(toolkit, 526336, nodeAmount, null, null, tintHue, tintSaturation, tintLightness, tintOpacity)
        if (model != null) {
            model.drawMatrixThreaded(renderingMatrix, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
            layerPriorities = layerPriorities or model.accountForLayers()
            if (model.getSize3d() > size3d) {
                size3d = model.getSize3d()
            }
        }

        return renderedEntity
    }
}
