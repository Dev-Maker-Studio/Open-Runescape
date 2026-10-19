package com.rs.engine.pathfinder

import com.rs.game.model.gameobject.GameObject
import org.darkan.core.map.RegionCollision

object GameObjectCollision {
    @JvmStatic
    fun clip(obj: GameObject) = RegionCollision.clip(obj)

    @JvmStatic
    fun unclip(obj: GameObject) = RegionCollision.unclip(obj)
}
