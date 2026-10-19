package com.rs.game.content.minigames.shadesofmortton

import com.rs.game.World
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Loc

private val straightWallIds = intArrayOf(
    Loc.templewall_base, Loc.templewall_1, Loc.templewall_2, Loc.templewall_3,
    Loc.templewall_4, Loc.templewall_5, Loc.templewall_6, Loc.templewall_7,
    Loc.templewall_8, Loc.templewall_9, Loc.templewall_10
)
private val cornerWallIds = intArrayOf(
    Loc.templewallcorner_base, Loc.templewallcorner_1, Loc.templewallcorner_2,
    Loc.templewallcorner_3, Loc.templewallcorner_4, Loc.templewallcorner_5,
    Loc.templewallcorner_6, Loc.templewallcorner_7, Loc.templewallcorner_8,
    Loc.templewallcorner_9, Loc.templewallcorner_10
)

class TempleWall(obj: GameObject) : GameObject(obj) {
    private val wallIds = if (obj.id in cornerWallIds) cornerWallIds else straightWallIds
    private var buildProgress = 5

    init {
        ShadesOfMortton.addWall(this)
    }

    @JvmOverloads
    fun increaseProgress(amount: Int = 4) {
        buildProgress = (buildProgress + amount).coerceIn(0, 100)
        update()
    }

    fun decreaseProgress() {
        buildProgress--
        if (buildProgress <= 0) {
            destroy()
            return
        }
        update()
    }

    fun destroy() {
        buildProgress = 0
        changeId(wallIds.first())
        World.spawnObject(GameObject(this))
        ShadesOfMortton.deleteWall(this)
    }

    fun update() {
        changeId(wallIds[(buildProgress / 10).coerceIn(0, wallIds.lastIndex)])
        World.spawnObject(GameObject(this))
    }

    var repairPerc: Int
        get() = buildProgress.coerceIn(0, 100)
        set(value) { buildProgress = value.coerceIn(0, 100) }
}
