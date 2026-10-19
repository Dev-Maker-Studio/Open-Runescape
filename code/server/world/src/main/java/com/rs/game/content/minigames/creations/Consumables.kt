package com.rs.game.content.minigames.creations

import com.rs.game.content.Potion
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.model.Item

private const val HEAL_PER_FOOD_CLASS = 40
private const val FOOD_DELAY = 3
private const val POTION_DELAY = 2

enum class ScPotion(val effect: Potion, vararg val doses: Int) {
    PRAYER(Potion.PRAYER_POTION, Obj.sc_potion_prayer_1, Obj.sc_potion_prayer_2, Obj.sc_potion_prayer_3, Obj.sc_potion_prayer_4, Obj.sc_potion_prayer_5),
    ENERGY(Potion.ENERGY_POTION, Obj.sc_potion_energy_1, Obj.sc_potion_energy_2, Obj.sc_potion_energy_3, Obj.sc_potion_energy_4, Obj.sc_potion_energy_5),
    ATTACK(Potion.SUPER_ATTACK, Obj.sc_potion_attack_1, Obj.sc_potion_attack_2, Obj.sc_potion_attack_3, Obj.sc_potion_attack_4, Obj.sc_potion_attack_5),
    STRENGTH(Potion.SUPER_STRENGTH, Obj.sc_potion_strength_1, Obj.sc_potion_strength_2, Obj.sc_potion_strength_3, Obj.sc_potion_strength_4, Obj.sc_potion_strength_5),
    RANGING(Potion.RANGING_POTION, Obj.sc_potion_range_1, Obj.sc_potion_range_2, Obj.sc_potion_range_3, Obj.sc_potion_range_4, Obj.sc_potion_range_5),
    DEFENCE(Potion.DEFENCE_POTION, Obj.sc_potion_defence_1, Obj.sc_potion_defence_2, Obj.sc_potion_defence_3, Obj.sc_potion_defence_4, Obj.sc_potion_defence_5),
    MAGIC(Potion.MAGIC_POTION, Obj.sc_potion_magic_1, Obj.sc_potion_magic_2, Obj.sc_potion_magic_3, Obj.sc_potion_magic_4, Obj.sc_potion_magic_5),
    SUMMONING(Potion.SUMMONING_POTION, Obj.sc_potion_summoning_1, Obj.sc_potion_summoning_2, Obj.sc_potion_summoning_3, Obj.sc_potion_summoning_4, Obj.sc_potion_summoning_5);

    companion object {
        private val byId = entries.flatMap { potion -> potion.doses.withIndex().map { (index, id) -> id to (potion to index + 1) } }.toMap()

        fun forId(id: Int) = byId[id]
    }
}

private val FOOD_CLASS = listOf(Obj.sc_food_1, Obj.sc_food_2, Obj.sc_food_3, Obj.sc_food_4, Obj.sc_food_5).withIndex().associate { (index, id) -> id to index + 1 }

fun Player.consumeScItem(item: Item, slot: Int): Boolean {
    FOOD_CLASS[item.id]?.let {
        eatScFood(item, it)
        return true
    }
    val (potion, doses) = ScPotion.forId(item.id) ?: return false
    drinkScPotion(potion, doses, slot)
    return true
}

private fun Player.eatScFood(item: Item, clayClass: Int) {
    if (!canEat()) return
    anim(Seq.human_eat)
    addFoodDelay(FOOD_DELAY)
    sendMessage("You eat the food.")
    val before = hitpoints
    heal(HEAL_PER_FOOD_CLASS * clayClass)
    if (hitpoints > before) sendMessage("It heals some health.")
    actionManager.actionDelay = actionManager.actionDelay + FOOD_DELAY
    inventory.deleteItem(item)
}

private fun Player.drinkScPotion(potion: ScPotion, doses: Int, slot: Int) {
    if (!canPot() || !controllerManager.canPot(potion.effect)) return
    inventory.items[slot] = if (doses > 1) Item(potion.doses[doses - 2]) else null
    inventory.refresh(slot)
    addPotionDelay(POTION_DELAY)
    potion.effect.effect.invoke(this)
    anim(Seq.human_eat)
    soundEffect(Sound.sound_4580, false)
    sendMessage(if (doses > 1) "You drink a dose of the potion." else "The glass shatters as you drink the last dose of the potion.")
}
