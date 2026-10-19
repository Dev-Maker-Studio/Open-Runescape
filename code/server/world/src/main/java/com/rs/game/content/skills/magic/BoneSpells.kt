package com.rs.game.content.skills.magic

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onComponentClick
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit

private val CONVERTIBLE_BONES = intArrayOf(Obj.bones, Obj.bones_burnt)

enum class BoneSpell(val component: Int, val level: Int, val xp: Double, val fruit: Int, val runes: RuneSet) {
    BANANAS(Component.interface_192.component_192_33, 15, 25.0, Obj.banana, RuneSet(Rune.NATURE, 1, Rune.EARTH, 2, Rune.WATER, 2)),
    PEACHES(Component.interface_192.component_192_65, 60, 35.5, Obj.peach, RuneSet(Rune.NATURE, 2, Rune.EARTH, 4, Rune.WATER, 4));

    fun isUnlocked(player: Player): Boolean {
        if (this != PEACHES || player.vars.getVarBit(Varbit.magictraining_bonespeaches) != 0) return true
        player.sendMessage("You must unlock Bones to Peaches at the Mage Training Arena first.")
        return false
    }

    fun payRunes(player: Player): Boolean = player.canCastSpell() && Magic.checkMagicAndRunes(player, level, true, runes)

    fun finish(player: Player) {
        player.anim(Seq.human_castbonestobananas)
        player.spotAnim(Spotanim.bonestobananas_casting)
        player.skills.addXp(Constants.MAGIC, xp)
        player.addSpellDelay(2)
    }

    companion object {
        private val byComponent = entries.associateBy { it.component }

        fun forComponent(component: Int): BoneSpell? = byComponent[component]
    }
}

@ServerStartupEvent
fun mapBoneSpells() {
    onComponentClick(*BoneSpell.entries.map { it.component }.toIntArray()) { e ->
        val spell = BoneSpell.forComponent(e.component) ?: return@onComponentClick
        val player = e.player
        if (!spell.isUnlocked(player)) return@onComponentClick
        val amount = CONVERTIBLE_BONES.sumOf { player.inventory.getAmountOf(it) }
        if (amount == 0) {
            player.sendMessage("You aren't holding any bones!")
            return@onComponentClick
        }
        if (!spell.payRunes(player)) return@onComponentClick
        CONVERTIBLE_BONES.forEach { player.inventory.deleteItem(it, player.inventory.getAmountOf(it)) }
        player.inventory.addItem(spell.fruit, amount)
        spell.finish(player)
    }
}
