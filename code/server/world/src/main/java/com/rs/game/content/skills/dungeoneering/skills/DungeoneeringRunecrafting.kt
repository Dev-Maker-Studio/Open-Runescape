package com.rs.game.content.skills.dungeoneering.skills

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Item
import kotlin.math.min

class DungeoneeringRunecrafting(
    private var cycles: Int,
    private val runeId: Int,
    private val levelRequirement: Int,
    private val experience: Double,
    private val multipliers: IntArray
) : PlayerAction() {

    override fun start(player: Player): Boolean {
        if (player.skills.getLevel(Constants.RUNECRAFTING) < levelRequirement) {
            player.simpleDialogue("You need a runecrafting level of $levelRequirement to craft this rune.")
            return false
        }
        val essence = player.inventory.getNumberOf(Obj.rand_blankrune)
        if (essence == 0)
            return false
        cycles = min(min(cycles, essence), MAX_CYCLES)
        return true
    }

    override fun process(player: Player): Boolean = cycles > 0 && player.inventory.containsItem(Obj.rand_blankrune, 1)

    override fun processWithDelay(player: Player): Int {
        val crafted = min(min(cycles, ESSENCE_PER_CYCLE), player.inventory.getNumberOf(Obj.rand_blankrune))
        if (crafted <= 0)
            return -1
        cycles -= crafted
        player.anim(Seq.rand_player_enchanting_rune)
        player.spotAnim(Spotanim.rand_player_enchanting_rune_spotanim)
        player.inventory.deleteItem(Item(Obj.rand_blankrune, crafted))
        player.skills.addXp(Constants.RUNECRAFTING, experience * crafted)
        player.inventory.addItem(Item(runeId, crafted * multiplierFor(player.skills.getLevel(Constants.RUNECRAFTING))))
        return 0
    }

    private fun multiplierFor(level: Int): Int {
        for (i in multipliers.size - 2 downTo 0 step 2)
            if (level >= multipliers[i])
                return multipliers[i + 1]
        return 1
    }

    override fun stop(player: Player) {
        setActionDelay(player, 3)
    }

    companion object {
        private const val ESSENCE_PER_CYCLE = 10
        private const val MAX_CYCLES = 200
    }
}
