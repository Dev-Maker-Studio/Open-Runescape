package com.rs.game.content.quests.legendsquest

import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile

internal class LegendsCharacter(owner: Player, id: Int, tile: Tile) : OwnedNPC(owner, id, tile, true) {
    init { setRandomWalk(false); setAutoDespawnAtDistance(false) }
    override fun canBeAttackedBy(player: Player): Boolean {
        if (player !== owner) return false
        if (id == Npc.ungadulu_good) repelUngadulu(player)
        else if (id == Npc.viyeldi) {
            if (player.equipment.weaponId == Obj.deathdagger) killViyeldi(player)
            else viyeldiDialogue(player, true)
        }
        return false
    }
    override fun processNPC() {
        if (owner.isDead || !owner.mapChunkIds.contains(tile.getChunkId()) || (id == Npc.viyeldi && owner.legends.getB("viyeldiDead")) || (id == Npc.echned_zekin && (owner.legendsStage >= WATER_RESTORED || activeLegendsFight(owner))) ||
            (tempAttribs.getB("legendsSpiritTalking") && owner.conversation == null && !owner.isLocked) ||
            (id == Npc.radimus_erkle && (owner.legendsStage >= TRAINING) != (y >= 3374))) {
            finish()
            return
        }
        super.processNPC()
    }
}
