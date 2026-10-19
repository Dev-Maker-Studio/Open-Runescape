package com.rs.game.content.minigames

import com.rs.game.content.minigames.clanwars.FFAController
import com.rs.game.content.minigames.creations.StealingCreationController
import com.rs.game.content.minigames.crucible.CrucibleController
import com.rs.game.content.minigames.fistofguthix.FogController
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

private val MINIGAME_RUNES = intArrayOf(Obj.hvh_elemental_rune, Obj.hvh_catalytic_rune)

private val MINIGAME_SUPPLIES = setOf(
    *MINIGAME_RUNES.toTypedArray(),
    Obj.castlewars_bandages, Obj.cert_castlewars_bandages,
    Obj.castlewars_4_dose_attack, Obj.castlewars_3_dose_attack, Obj.castlewars_2_dose_attack, Obj.castlewars_1_dose_attack,
    Obj.dom_potion_1, Obj.dom_potion_1_half, Obj.dom_potion_2, Obj.dom_potion_2_half, Obj.dom_potion_4, Obj.dom_potion_4_half,
)

fun giveFoodAndPotions(player: Player) {
    checkAndDeleteFoodAndPotions(player)
    player.inventory.addItem(Obj.hvh_elemental_rune, 10000)
    player.inventory.addItem(Obj.hvh_catalytic_rune, 10000)
    player.inventory.addItem(Obj.castlewars_4_dose_attack, 1)
    player.inventory.addItem(Obj.dom_potion_1, 2)
    player.inventory.addItem(Obj.dom_potion_4, 2)
    player.inventory.addItem(Obj.dom_potion_2, 1)
    player.inventory.addItem(Obj.castlewars_bandages, player.inventory.freeSlots)
}

fun checkAndDeleteFoodAndPotions(player: Player) {
    player.inventory.items.array().forEach { item ->
        if (item != null && isMinigameSupply(item.id))
            player.inventory.deleteItem(item)
    }
}

fun isMinigameSupply(id: Int): Boolean = id in MINIGAME_SUPPLIES

fun minigameRunesVoidMagicXp(player: Player): Boolean {
    if (MINIGAME_RUNES.none { player.inventory.containsItem(it) }) return false
    return when (val controller = player.controllerManager.controller) {
        is StealingCreationController, is FogController, is CrucibleController -> false
        is FFAController -> !controller.dangerous
        else -> true
    }
}

fun enableMinigameRunes(player: Player) {
    player.vars.setVarBit(Varbit.sc_team, 1)
}

fun disableMinigameRunes(player: Player) {
    player.vars.setVarBit(Varbit.sc_team, 0)
}
