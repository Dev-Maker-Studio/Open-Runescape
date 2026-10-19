package com.rs.game.content.minigames.pest

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.minigames.pest.npcs.Splatter
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.npcCombat
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private const val SEAL_RADIUS = 2
private const val SEAL_DAMAGE = 100
private const val SEAL_DELAY = 2
private val SEALS = listOf(Obj.obj_11666, Obj.obj_11667, Obj.obj_11668, Obj.obj_11669, Obj.obj_11670, Obj.obj_11671, Obj.obj_11672, Obj.obj_11673)
private val SPLATTERS = arrayOf<Any>(Npc.pest_splatter_1, Npc.pest_splatter_2, Npc.pest_splatter_3, Npc.pest_splatter_4, Npc.pest_splatter_5)
val Player.pestControlGame get() = (controllerManager.controller as? PestControlGameController)?.game

@ServerStartupEvent
fun mapPestControl() {
    onObjectClick(*LanderTier.entries.map { it.gangplank }.toTypedArray()) { (player, obj) ->
        LanderTier.forGangplank(obj.id)?.let { Lander.of(it).board(player) }
    }

    onNpcClick(*LanderTier.entries.map { it.squire }.toTypedArray(), options = arrayOf("Talk-to")) { (player, npc) ->
        val tier = LanderTier.forSquire(npc.id) ?: return@onNpcClick
        player.startConversation {
            npc(npc.id, HeadE.CALM_TALK, "This is the ${tier.displayName.lowercase()} lander. You need a combat level of ${tier.combatLevel} to board it and help the Void Knights defend their outpost.")
            npc(npc.id, HeadE.CALM_TALK, "A victory earns ${tier.commendations} commendations for everyone who does their part, and coins besides.")
        }
    }

    onNpcClick(Npc.pest_squire_instance) { (player, npc, option) ->
        val game = player.pestControlGame ?: return@onNpcClick
        if (option == "Leave") {
            player.controllerManager.forceStop()
            return@onNpcClick
        }
        player.startConversation {
            npc(npc.id, HeadE.WORRIED, "Be quick, we're under attack! Keep the pests away from the Void Knight.")
            options {
                opExec("I'd like to leave.") { if (player.pestControlGame == game) player.controllerManager.forceStop() }
                op("I'll keep fighting.")
            }
        }
    }

    npcCombat(*SPLATTERS) { npc, _ ->
        (npc as? Splatter)?.explode()
        npc.attackSpeed
    }

    onObjectClick(Loc.pest_watchtower_ladder) { (player, obj) ->
        val (dx, dy) = when (obj.rotation) {
            0 -> 0 to -1
            1 -> -1 to 0
            2 -> 0 to 1
            else -> 1 to 0
        }
        val destination = if (player.plane == 0) Tile.of(obj.x + dx, obj.y + dy, 1) else Tile.of(obj.x - dx, obj.y - dy, 0)
        player.useStairs(Seq.human_reachforladder, destination, 1, 2)
    }

    onItemClick(*SEALS.toTypedArray<Any>(), options = arrayOf("Rub")) { (player, item, _, equipped) ->
        val game = player.pestControlGame
        if (game == null) {
            player.sendMessage("The seal's power can only be called upon while defending the Void Knight.")
            return@onItemClick
        }
        if (player.isLocked) return@onItemClick
        player.lock(SEAL_DELAY)
        for (pest in game.pestsNear(player, SEAL_RADIUS))
            pest.applyHit(Hit(player, SEAL_DAMAGE, HitLook.MAGIC_DAMAGE).setData(SEAL_HIT, true))
        val next = SEALS.indexOf(item.id) + 1
        if (next >= SEALS.size) {
            if (equipped) player.equipment.deleteSlot(Equipment.NECK) else player.inventory.deleteItem(item.slot, item)
            player.appearance.generateAppearanceData()
            player.sendMessage("Your seal has used the last of its power and crumbles away.")
            return@onItemClick
        }
        item.id = SEALS[next]
        if (equipped) player.equipment.refresh(Equipment.NECK) else player.inventory.refresh(item.slot)
    }

    onLogin { (player) ->
        player.refreshCommendations()
        PestControlGame.offerHybridBoots(player)
    }
}
