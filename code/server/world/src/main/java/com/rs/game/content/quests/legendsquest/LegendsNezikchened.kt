package com.rs.game.content.quests.legendsquest

import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

internal class LegendsNezikchened(owner: Player, tile: Tile, val round: Int) : OwnedNPC(owner, Npc.nezikchened, tile, true) {
    private var credited = false
    init { setRandomWalk(false) }
    override fun handlePreHit(hit: Hit) {
        if ((round == 1 && owner.legends.getB("holyWater")) || (round == 2 && owner.legends.getB("lightPath"))) hit.damage = hit.damage * 3 / 2
        super.handlePreHit(hit)
    }
    override fun processNPC() {
        val expected = when (round) { 1 -> BINDING; 2 -> DAGGER; else -> TOTEM }
        val outside = when (round) { 1 -> !owner.inOctagram(); 2 -> !owner.atLegendsSpring(); else -> !owner.atLegendsTotemBattle() }
        if ((!isDead && (owner.legendsStage != expected || outside)) || owner.isDead) { finish(); return }
        super.processNPC()
    }
    override fun sendDeath(source: Entity) {
        val expected = when (round) { 1 -> BINDING; 2 -> DAGGER; else -> TOTEM }
        if (!credited && owner.legendsStage == expected && !owner.isDead && !owner.hasFinished()) {
            credited = true
            when (round) {
                1 -> {
                    owner.advanceLegends(SEEDS)
                    forceTalk("Ha ha ha... I shall return for thee when the time is right.")
                    owner.applyHit(Hit.magic(this, 100))
                    owner.sendMessage("The demon vanishes, leaving nothing but a pile of ashes where he stood.")
                }
                2 -> { owner.advanceLegends(WATER_RESTORED); forceTalk("This isn't over, Vacu.") }
                3 -> { owner.advanceLegends(DEMON_DEFEATED); forceTalk("Noooooooooo... To be beaten by a mere mortal..."); owner.sendMessage("You deliver the final killing blow to the foul demon.") }
            }
        }
        super.sendDeath(source)
    }
}
