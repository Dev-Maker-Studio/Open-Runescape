package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.entity.grounditem.GroundItem
import com.jagex.game.runetek5.entity.grounditem.GroundItemNode
import com.jagex.game.runetek5.movement.CoordGrid

class GroundItemFetcher(
    @JvmField val itemPosition: CoordGrid,
    @JvmField val groundItemId: Int
) {

    fun getGroundItem(): GroundItem? {
        val wrapper = Client.GROUND_ITEMS.get((this.itemPosition.plane.toLong() shl 28) or (this.itemPosition.y.toLong() shl 14) or this.itemPosition.x.toLong())
        if (wrapper != null) {
            val coords = Resource.mapRegion!!.getBaseCoordinates()
            val sceneX = this.itemPosition.x - coords.x
            val sceneY = this.itemPosition.y - coords.y
            if (sceneX >= 0 && sceneY >= 0 && sceneX < Resource.mapRegion!!.sceneSizeX && sceneY < Resource.mapRegion!!.sceneSizeY) {
                var node = wrapper.groundItems.getFirst() as GroundItemNode?
                while (node != null) {
                    if (node.id == this.groundItemId) {
                        return Resource.mapRegion!!.getScene()?.getGroundItem(this.itemPosition.plane, sceneX, sceneY) as? GroundItem
                    }
                    node = wrapper.groundItems.next() as GroundItemNode?
                }
            }
        }
        return null
    }
}
