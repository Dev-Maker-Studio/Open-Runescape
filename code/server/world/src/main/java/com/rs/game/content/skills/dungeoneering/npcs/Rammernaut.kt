package com.rs.game.content.skills.dungeoneering.npcs

import com.rs.game.World
import com.rs.game.content.skills.dungeoneering.DungeonManager
import com.rs.game.content.skills.dungeoneering.DungeonUtils
import com.rs.game.content.skills.dungeoneering.RoomReference
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.ForceTalk
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.utils.WorldUtil
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile

private const val KNOCKBACK_DISTANCE = 6

class Rammernaut(tile: Tile, manager: DungeonManager, reference: RoomReference) :
    DungeonBoss(DungeonUtils.getClosestToCombatLevel(Utils.range(Npc.rand_furn_rammernaught_1, Npc.rand_furn_rammernaught_14), manager.bossLevel), tile, manager, reference) {

    private var chargeTarget: Player? = null
    private var count = 0
    var isRequestSpecNormalAttack = false

    init {
        setForceFollowClose(true)
    }

    private fun fail() {
        anim(Seq.rand_eliteguardian_stun)
        setNextForceTalk(ForceTalk("Oooof!!"))
        count = -14
    }

    private fun succeed(target: Player) {
        resetWalkSteps()
        anim(Seq.rand_eliteguardian_ready_random_b)
        applyStunHit(target, (target.maxHitpoints * 0.6).toInt())
        isRequestSpecNormalAttack = true
        count = -12
    }

    fun applyStunHit(entity: Entity, maxHit: Int) {
        entity.applyHit(Hit(this, Utils.random(maxHit) + 1, HitLook.TRUE_DAMAGE))
        entity.freeze(2)
        if (entity !is Player) return
        entity.stopAll()
        entity.sendMessage("You've been stunned.")
        entity.freeze(2)
        if (entity.prayer.hasProtectionPrayersOn()) {
            entity.sendMessage("Your prayers have been disabled.")
            entity.setProtectionPrayBlock(2)
        }
        WorldTasks.schedule(1) {
            val destination = knockbackTile(entity.tile)
            entity.faceEntityTile(this)
            entity.forceMove(destination, WorldUtil.getDirectionTo(entity, tile), Seq.seq_10070, 5, 60)
        }
    }

    private fun knockbackTile(from: Tile): Tile {
        val delta = Utils.getDirection(faceAngle)
        val dir = Direction.forDelta(delta[0].toInt(), delta[1].toInt()) ?: return from
        var current = from
        repeat(KNOCKBACK_DISTANCE) {
            val next = current.transform(dir.dx, dir.dy)
            if (!World.checkWalkStep(current, dir, 1) || !manager.isAtBossRoom(next)) return current
            current = next
        }
        return current
    }

    override fun processNPC() {
        if (isDead) return
        val target = chargeTarget
        if (target == null) {
            super.processNPC()
            return
        }
        setNextFaceEntity(target)
        when {
            count == 0 -> {
                setNextForceTalk(ForceTalk("CHAAAAAARGE!"))
                setRun(true)
            }
            count == -10 -> {
                setRun(false)
                resetWalkSteps()
                calcFollow(target, true)
            }
            count == -8 -> setChargeTarget(null)
            count > 2 -> {
                resetWalkSteps()
                calcFollow(target, true)
                when {
                    count != 3 && !World.floorAndWallsFree(tile, size) -> fail()
                    WorldUtil.isInRange(x, y, size, target.x, target.y, target.size, 0) -> succeed(target)
                    !hasWalkSteps() -> fail()
                }
            }
        }
        count++
    }

    fun setChargeTarget(target: Player?) {
        chargeTarget = target
        combat.removeTarget()
        count = 0
    }
}
