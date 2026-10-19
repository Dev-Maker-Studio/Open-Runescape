package com.rs.game.content.bosses.kalphitequeen

import com.rs.game.World.sendProjectile
import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.combat.CombatScript.*
import com.rs.game.model.entity.npc.combat.CombatScriptsHandler.getDefaultCombat
import com.rs.game.model.entity.npc.combat.NPCCombatUtil.Companion.projectileBounce
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils.random
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.npcCombat

class KalphiteQueen(id: Int, tile: Tile, spawned: Boolean) : NPC(id, tile, spawned) {
    init {
        isIgnoreNPCClipping = true
        lureDelay = 0
        isForceAgressive = true
        isIgnoreDocile = true
    }
    override fun sendDeath(source: Entity) {
        if (id == 1158) {
            resetWalkSteps()
            combat.removeTarget()
            anim(-1)
            schedule {
                anim(combatDefinitions.deathEmote)
                wait(combatDefinitions.deathDelay)
                isCantInteract = true
                transformIntoNPC(1160)
                sync(6270, 1055)
                schedule {
                    reset()
                    isCantInteract = false
                }
            }
            return
        }
        super.sendDeath(source)
        schedule {
            wait(combatDefinitions.deathDelay)
            transformIntoNPC(1158)
        }
    }
}

@ServerStartupEvent
fun mapKalphiteQueen() {
    instantiateNpc(1158, 1160) { id, tile -> KalphiteQueen(id, tile, false)}

    npcCombat(1158, 1160) { npc, target ->
        if (target.inMeleeRange(npc) && random(2) == 0)
            return@npcCombat getDefaultCombat().apply(npc, target)
        npc.anim(if (npc.id == 1158) 6240 else 6234)
        if (random(2) == 0) {
            //range
            sendProjectile(npc, target, 288, delay = 30, speed = 5, angle = 15) {
                delayHit(npc, 0, target, Hit.range(npc, getMaxHit(npc, npc.combatDefinitions.maxHit, CombatStyle.RANGE, target, 10000.0)))
                if (target is Player)
                    target.prayer.drainPrayer(10.0)
            }
            return@npcCombat npc.attackSpeed
        }
        //mage
        npc.spotAnim(if (npc.id == 1158) 278 else 279)
        projectileBounce(npc, target, mutableSetOf(target), 280, 281, 5) { nextTarget ->
            delayHit(npc, 0, nextTarget, Hit.magic(npc, getMaxHit(npc, npc.maxHit, CombatStyle.MAGIC, nextTarget, 10000.0)))
        }
        return@npcCombat npc.attackSpeed
    }
}
