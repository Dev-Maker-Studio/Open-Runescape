package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.entity.gameobject.GameObject

class LocAction : Node<LocAction>() {

    @JvmField var x = 0
    @JvmField var y = 0
    @JvmField var plane = 0
    @JvmField var srcId = 0
    @JvmField var gameObjectType = 0
    @JvmField var srcRotation = 0
    @JvmField var dstCustomization: ObjectTypeCustomisation? = null
    @JvmField var dstId = 0
    @JvmField var srcShape = 0
    @JvmField var dstShape = 0
    @JvmField var dstRotation = 0
    @JvmField var queued = true
    @JvmField var inUpdateZone = false

    companion object {
        @JvmField var pendingRemovals = NodeCollection()
        @JvmField var pendingCustomizations = NodeCollection()

        @JvmStatic
        fun transformObjects() {
            var locAction = pendingRemovals.getFirst() as LocAction?
            while (locAction != null) {
                MapRegion.applyGameobjectAction(locAction, false)
                locAction = pendingRemovals.next() as LocAction?
            }

            locAction = pendingCustomizations.getFirst() as LocAction?
            while (locAction != null) {
                MapRegion.applyGameobjectAction(locAction, true)
                locAction = pendingCustomizations.next() as LocAction?
            }
        }

        @JvmStatic
        fun onRegionChanged() {
            var locAction = pendingRemovals.getFirst() as LocAction?
            while (locAction != null) {
                if (!locAction.inUpdateZone) {
                    locAction.queued = true
                    if (locAction.x >= 0 && locAction.y >= 0 && locAction.x < Resource.mapRegion!!.getSceneSizeX() && locAction.y < Resource.mapRegion!!.getSceneSizeY()) {
                        fetchGameobject(locAction)
                    }
                } else {
                    locAction.unlink()
                }
                locAction = pendingRemovals.next() as LocAction?
            }

            locAction = pendingCustomizations.getFirst() as LocAction?
            while (locAction != null) {
                if (!locAction.inUpdateZone) {
                    locAction.queued = true
                } else {
                    locAction.unlink()
                }
                locAction = pendingCustomizations.next() as LocAction?
            }
        }

        @JvmStatic
        fun destroyObject(plane: Int, x: Int, y: Int, type: Int, id: Int, shape: Int, rotation: Int) {
            var locAction: LocAction? = null
            var cachedLoc = pendingRemovals.getFirst() as LocAction?
            while (cachedLoc != null) {
                if (plane == cachedLoc.plane && cachedLoc.x == x && y == cachedLoc.y && type == cachedLoc.gameObjectType) {
                    locAction = cachedLoc
                    break
                }
                cachedLoc = pendingRemovals.next() as LocAction?
            }
            if (locAction == null) {
                locAction = LocAction()
                locAction.plane = plane
                locAction.gameObjectType = type
                locAction.x = x
                locAction.y = y
                if (x >= 0 && y >= 0 && x < Resource.mapRegion!!.getSceneSizeX() && y < Resource.mapRegion!!.getSceneSizeY()) {
                    fetchGameobject(locAction)
                }
                pendingRemovals.addFirst(locAction)
            }
            locAction.dstId = id
            locAction.dstShape = shape
            locAction.dstRotation = rotation
            locAction.queued = true
            locAction.inUpdateZone = false
        }

        @JvmStatic
        fun customizeObject(plane: Int, x: Int, y: Int, type: Int, locId: Int, locShape: Int, locCustomization: ObjectTypeCustomisation?) {
            var locAction: LocAction? = null
            var customization = pendingCustomizations.getFirst() as LocAction?
            while (customization != null) {
                if (plane == customization.plane && customization.x == x && y == customization.y && type == customization.gameObjectType) {
                    locAction = customization
                    break
                }
                customization = pendingCustomizations.next() as LocAction?
            }
            if (locAction == null) {
                locAction = LocAction()
                locAction.plane = plane
                locAction.gameObjectType = type
                locAction.x = x
                locAction.y = y
                pendingCustomizations.addFirst(locAction)
            }
            locAction.dstId = locId
            locAction.dstShape = locShape
            locAction.dstCustomization = locCustomization
            locAction.queued = true
            locAction.inUpdateZone = false
        }

        @JvmStatic
        fun fetchGameobject(action: LocAction) {
            val scene = Resource.mapRegion!!.getScene()
            if (scene != null) {
                var gameObject: GameObject? = null
                if (action.gameObjectType == 0) {
                    gameObject = scene.getWall(action.plane, action.x, action.y) as GameObject?
                }
                if (action.gameObjectType == 1) {
                    gameObject = scene.getPrimaryWallDecoration(action.plane, action.x, action.y) as GameObject?
                }
                if (action.gameObjectType == 2) {
                    gameObject = scene.getInteractiveObject(action.plane, action.x, action.y, client.INTERACTIVE_OBJECT_COMPARABLE) as GameObject?
                }
                if (action.gameObjectType == 3) {
                    gameObject = scene.getGroundDecoration(action.plane, action.x, action.y) as GameObject?
                }
                if (gameObject != null) {
                    action.srcId = gameObject.id
                    action.srcShape = gameObject.shape
                    action.srcRotation = gameObject.getRotation()
                } else {
                    action.srcId = -1
                    action.srcShape = 0
                    action.srcRotation = 0
                }
            }
        }
    }
}
