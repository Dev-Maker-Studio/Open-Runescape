package com.rs.game.content.dnds.shootingstar

import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile
import kotlin.math.pow

class Star(tier: Int, @JvmField val location: ShootingStars.Location) : GameObject(Loc.loc_38668 - tier, ObjectShape.SCENERY_INTERACT, 0, location.tile) {
    @JvmField var sprite: NPC? = null
    @JvmField val landingTime: Long = World.getServerTicks()
    @JvmField var discovered = false
    @JvmField var minedThisTick = false
    private var life = 0

    val tier: Int get() = Loc.loc_38668 + 1 - id

    private val maxLife: Int get() = (2258.2932 * 0.5726.pow(tier)).toInt()

    val lifePercent: String get() = Utils.formatDouble(life.toDouble() / maxLife * 100.0)

    init {
        World.spawnObject(this)
        ChunkManager.getChunk(tile.getChunkId(), true).flagForProcess(this)
        life = maxLife
        ShootingStars.addDiscoveredStar(this, "Not discovered yet...")
    }

    private fun degrade() {
        if (id >= Loc.loc_38668) {
            sprite = NPC(Npc.npc_8091, Tile.of(tile))
            changeId(Loc.repeatable_content_icon)
            return
        }
        changeId(id + 1)
        life = maxLife
    }

    override fun process(): Boolean {
        if (life <= -1)
            return false
        if (minedThisTick && life > 0) {
            if (--life <= 0)
                degrade()
            minedThisTick = false
        }
        return true
    }

    fun destroy() {
        sprite?.finish()
        World.removeObject(this)
        life = -1
    }
}
