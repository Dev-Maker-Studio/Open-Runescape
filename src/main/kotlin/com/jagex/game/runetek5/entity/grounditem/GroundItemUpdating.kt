package com.jagex.game.runetek5.entity.grounditem

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.RegionLoadingType

object GroundItemUpdating {

    @JvmStatic
    fun addGroundItem(z: Int, x: Int, y: Int, addedGroundItem: GroundItemNode) {
        val uid = z.toLong() shl 28 or (y.toLong() shl 14) or x.toLong()
        var groundItemWrapper = Client.GROUND_ITEMS.get(uid)
        if (groundItemWrapper == null) {
            groundItemWrapper = GroundItemWrapper()
            Client.GROUND_ITEMS.put(groundItemWrapper, uid)
        } else {
            val addedObjType = TypeLists.OBJ!!.list(addedGroundItem.id)
            var addedItemValue = addedObjType.value
            if (addedObjType.stackable == 1) {
                addedItemValue *= addedGroundItem.amount + 1
            }
            var existingGroundItem = groundItemWrapper.groundItems.getFirst() as GroundItemNode?
            while (existingGroundItem != null) {
                val existingObjType = TypeLists.OBJ!!.list(existingGroundItem.id)
                var existingItemValue = existingObjType.value
                if (existingObjType.stackable == 1) {
                    existingItemValue *= existingGroundItem.amount + 1
                }
                if (addedItemValue > existingItemValue) {
                    Node.replace(addedGroundItem, existingGroundItem)
                    return
                }
                existingGroundItem = groundItemWrapper.groundItems.next() as GroundItemNode?
            }
        }
        groundItemWrapper.groundItems.addFirst(addedGroundItem)
    }

    /**
     * Could use cleanup including naming
     */
    @JvmStatic
    fun removeGroundItem(plane: Int, x: Int, y: Int) {
        val base = Resource.mapRegion!!.getBaseCoordinates()
        val absX = base.x + x
        val absY = base.y + y
        if (Resource.mapRegion!!.getScene()!! != null && Resource.mapRegion!!.loadingType != RegionLoadingType.CUTSCENE && x >= 0 && y >= 0 && x < Resource.mapRegion!!.sceneSizeX && y < Resource.mapRegion!!.sceneSizeY) {
            val hash = plane.toLong() shl 28 or (absY.toLong() shl 14) or absX.toLong()
            val node = Client.GROUND_ITEMS.get(hash)
            if (node == null) {
                Resource.mapRegion!!.getScene()!!.removeGroundItem(plane, x, y)
            } else {
                val groundItemNode = node.groundItems.first() as GroundItemNode?
                if (groundItemNode == null) {
                    Resource.mapRegion!!.getScene()!!.removeGroundItem(plane, x, y)
                } else {
                    var groundItem = Resource.mapRegion!!.getScene()!!.removeGroundItem(plane, x, y) as GroundItem?
                    if (groundItem == null) {
                        groundItem = GroundItem(Resource.mapRegion!!.getScene()!!, x shl 9, Resource.mapRegion!!.getScene()!!.localHeightmap!![plane]!!.getHeight(x, y), y shl 9, plane, plane)
                    } else {
                        groundItem.itemId = -1
                        groundItem.lastId = -1
                        groundItem.scene = Resource.mapRegion!!.getScene()!!
                    }

                    groundItem.nodeId = groundItemNode.id
                    groundItem.nodeAmount = groundItemNode.amount

                    var nextNode = node.groundItems.next() as GroundItemNode?
                    loop@ while (nextNode != null) {
                        if (nextNode.id != groundItem.nodeId) {
                            groundItem.lastId = nextNode.id
                            groundItem.lastAmount = nextNode.amount

                            while (true) {
                                val thirdNode = node.groundItems.next() as GroundItemNode?
                                if (thirdNode == null) {
                                    break@loop
                                }

                                if (thirdNode.id != groundItem.nodeId && groundItem.lastId != thirdNode.id) {
                                    groundItem.itemId = thirdNode.id
                                    groundItem.amount = thirdNode.amount
                                }
                            }
                        }
                        nextNode = node.groundItems.next() as GroundItemNode?
                    }

                    val height = MapRegion.tileheightForAbsolutePosition((x shl 9) + 256, (y shl 9) + 256, plane)
                    groundItem.setBasePosition((x shl 9).toFloat(), height.toFloat(), (y shl 9).toFloat())
                    groundItem.tileHeightOffset = 0
                    groundItem.plane = plane.toByte()
                    groundItem.collisionPlane = plane.toByte()
                    if (Resource.mapRegion!!.settingsBits.areRoofsHidden(x, y)) {
                        groundItem.collisionPlane = (groundItem.collisionPlane + 1).toByte()
                    }

                    Resource.mapRegion!!.getScene()!!.addGroundItem(plane, x, y, height, groundItem)
                }
            }
        }
    }
}
