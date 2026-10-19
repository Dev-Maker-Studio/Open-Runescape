package com.rs.game.content.minigames.greatorb

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onDestroyItem
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Npc

private val APPRENTICES = arrayOf<Any>(
    Npc.rcguild_wizard_male1, Npc.rcguild_wizard_male2, Npc.rcguild_wizard_male3, Npc.rcguild_wizard_male4,
    Npc.rcguild_wizard_female1, Npc.rcguild_wizard_female2, Npc.rcguild_wizard_female3, Npc.rcguild_wizard_female4,
)

@ServerStartupEvent
fun mapGreatOrbProject() {
    ChunkManager.permanentlyPreloadRegions(*OrbAltar.entries.map { it.anchor.getRegionId() }.toIntArray())

    onNpcClick(Npc.rcguild_wizard_acantha, Npc.rcguild_wizard_vief) { (player, npc, option) ->
        val team = OrbTeam.forLeader(npc.id) ?: return@onNpcClick
        when (option) {
            "Join" -> GreatOrbLobby.join(player, team)
            else -> talkToLeader(player, npc, team)
        }
    }

    onNpcClick(*APPRENTICES) { (player, npc, option) ->
        when (option) {
            "Join-game" -> GreatOrbLobby.join(player, null)
            else -> player.startConversation {
                npc(npc.id, HeadE.CALM_TALK, "Wizards Acantha and Vief need runecrafters for the Great Orb Project. Shall I put you on whichever team is short of help?")
                options {
                    opExec("Yes, put me on a team.") { GreatOrbLobby.join(player, null) }
                    op("Not right now.")
                }
            }
        }
    }

    onNpcClick(Npc.npc_8021, Npc.npc_8025, checkDistance = false) { (player, npc, option) ->
        val orb = npc as? Orb ?: return@onNpcClick
        player.greatOrbGame?.instruct(player, orb, option == "Change-wand")
    }

    onNpcClick(Npc.rcguild_portal) { (player) -> player.greatOrbGame?.enterPortal(player) }

    onObjectClick(*OrbTeam.entries.map { it.barrier }.toTypedArray()) { (player, obj, option) ->
        val game = player.greatOrbGame ?: return@onObjectClick
        when (option) {
            "Pass-through" -> game.passBarrier(player, obj)
            "Destroy" -> game.destroyBarrier(player, obj)
        }
    }

    onItemClick(*OrbTeam.entries.map { it.barrierGenerator }.toTypedArray(), options = arrayOf("Make-barrier")) { (player) ->
        player.greatOrbGame?.makeBarrier(player) ?: player.sendMessage("The generator only works during the Great Orb Project.")
    }

    onDestroyItem(*OrbTeam.allTools.toTypedArray()) { (player) ->
        if (player.greatOrbController != null) player.controllerManager.forceStop()
    }

    onLogin { (player) ->
        if (player.greatOrbController == null) OrbTeam.allTools.forEach { player.inventory.deleteItem(it, Int.MAX_VALUE) }
    }
}

private fun talkToLeader(player: Player, npc: NPC, team: OrbTeam) = player.startConversation {
    val favours = "${team.displayName} orbs"
    npc(npc.id, HeadE.CALM_TALK, "Energy is leaking out of the altars as floating orbs. My team guides the $favours back into each altar while keeping the ${team.opponent.displayName} ones away.")
    options {
        if (player.orbTeam == team) opExec("I'd like to leave the ${team.displayName} team.") { player.controllerManager.forceStop() }
        else opExec("I'd like to join the ${team.displayName} team.") { GreatOrbLobby.join(player, team) }
        op("How do I play?") {
            npc(npc.id, HeadE.CALM_TALK, "Attract orbs towards you with your attractor, or push them away with your repeller. Get your team's orbs into the ring around the altar to absorb them.")
            npc(npc.id, HeadE.CALM_TALK, "You can nudge the other team's orbs a square at a time, except in the last thirty seconds. Barriers block orbs, but anyone can pass through them.")
            npc(npc.id, HeadE.CALM_TALK, "Stay busy: if you stand idle for too long you will be sent back here. After two minutes the team with more orbs wins the altar, and we move on to the next of the eight.")
        }
        op("Never mind.")
    }
}
