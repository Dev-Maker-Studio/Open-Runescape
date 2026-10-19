package com.rs.game.content.quests.legendsquest

import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

internal class LegendsGuardian(owner: Player, id: Int, tile: Tile, private val finalBattle: Boolean) : OwnedNPC(owner, id, tile, true) {
    private var credited = false
    private var challenged = false
    init {
        setRandomWalk(!finalBattle)
        setAutoDespawnAtDistance(finalBattle)
        if (!finalBattle) faceTile(owner.tile)
    }
    override fun canBeAttackedBy(player: Player): Boolean {
        if (player !== owner || player.legendsStage < SOURCE || (finalBattle && player.legendsStage != TOTEM)) return false
        if (!challenged && !finalBattle) {
            challenged = true
            forceTalk(if (id == Npc.npc_936) "Prepare yourself..." else "Upon my honour I will defend the source to the bitter end.")
            if (id == Npc.npc_936) owner.sendMessage("San Tojalon will test your mettle.")
            WorldTasks.schedule(2) {
                if (!hasFinished() && !isDead) forceTalk(when (id) {
                    Npc.npc_936 -> "Prepare yourself..."
                    Npc.npc_937 -> "Ready your weapon and defend yourself."
                    else -> "May your aim be true, challenger."
                })
            }
        }
        return true
    }
    override fun processNPC() {
        if (owner.isDead || !owner.mapChunkIds.contains(tile.getChunkId()) || owner.legendsStage < SOURCE || (finalBattle && !isDead && (owner.legendsStage != TOTEM || !owner.atLegendsTotemBattle()))) { finish(); return }
        super.processNPC()
    }
    override fun sendDeath(source: Entity) {
        if (!credited && !owner.hasFinished() && !owner.isDead && owner.legendsStage >= SOURCE && (!finalBattle || owner.legendsStage == TOTEM)) {
            credited = true
            if (finalBattle) {
                val index = CRYSTAL_REWARDS.keys.indexOf(id)
                owner.legends.setI("finalHeroes", owner.legends.getI("finalHeroes") or (1 shl index))
                owner.sendMessage("A nerve tingling scream echoes around you as you slay ${definitions.name}.")
                forceTalk("Argh!")
                WorldTasks.schedule(2) {
                    if (!hasFinished()) forceTalk(when (id) {
                        Npc.npc_936 -> "I beg you... slay the beast! Free me from this torment!"
                        Npc.npc_937 -> "Doomed! Doomed to eternal torment..."
                        else -> "Forever will I languish in torment..."
                    })
                }
                if (id == Npc.npc_938) WorldTasks.schedule(4) { if (!hasFinished()) forceTalk("...until the beast is slain!") }
                WorldTasks.schedule(7) { spawnFinalGuardian(owner) }
            } else {
                owner.legends.setB("guardian$id", true)
                val reward = CRYSTAL_REWARDS.getValue(id)
                val bit = 1 shl CRYSTAL_REWARDS.keys.indexOf(id)
                val prize = !owner.hasLegendsItem(reward) && owner.legends.getI("crystals") and bit == 0 && !owner.legends.getB("barrier")
                if (prize) {
                    owner.inventory.addItemDrop(reward, 1)
                    val piece = when (id) { Npc.npc_936 -> "chunk"; Npc.npc_937 -> "hunk"; else -> "lump" }
                    owner.sendMessage("A $piece of crystal forms mid-air and falls to the floor.")
                }
                forceTalk(when (id) {
                    Npc.npc_936 -> if (prize) "You have proved yourself an honourable foe." else "You have fought the good fight this day."
                    Npc.npc_937 -> if (prize) "I am humbled by your honourable deeds." else "You fought well, adventurer."
                    else -> if (prize) "You have proved yourself worthy of this prize." else "Your aim was true, challenger."
                })
            }
        }
        super.sendDeath(source)
    }
}
