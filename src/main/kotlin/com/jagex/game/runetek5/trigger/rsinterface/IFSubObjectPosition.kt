package com.jagex.game.runetek5.trigger.rsinterface

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.movement.CoordGrid

class IFSubObjectPosition(
    @JvmField val position: CoordGrid,
    @JvmField val objectIndex: Int,
    @JvmField val gameobjectId: Int
) {

    fun getObjectType(): Int {
        return client.OBJECT_TYPE_SLOTS[this.objectIndex]
    }

    fun getGameobjectVerifyId(): GameObject? {
        val gameObject = getGameobject()
        return if (gameObject == null) null else if (gameObject.id != this.gameobjectId) null else gameObject
    }

    fun getGameobject(): GameObject? {
        val z = this.position.plane
        val base = Resource.mapRegion!!.getBaseCoordinates()
        val x = this.position.x - base.x
        val y = this.position.y - base.y
        if (x >= 0 && y >= 0 && x < Resource.mapRegion!!.sceneSizeX && y < Resource.mapRegion!!.sceneSizeY) {
            return when (getObjectType()) {
                0 -> Resource.mapRegion!!.getScene()!!.getWall(z, x, y) as GameObject?
                1 -> Resource.mapRegion!!.getScene()!!.getPrimaryWallDecoration(z, x, y) as GameObject?
                2 -> Resource.mapRegion!!.getScene()!!.getInteractiveObject(z, x, y, client.INTERACTIVE_OBJECT_COMPARABLE) as GameObject?
                3 -> Resource.mapRegion!!.getScene()!!.getGroundDecoration(z, x, y) as GameObject?
                else -> null
            }
        }
        return null
    }
}
