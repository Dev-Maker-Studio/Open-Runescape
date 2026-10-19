package com.rs.game.content.quests.bigchompybirdhunting

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj

data class ChompyHat(val itemId: Int, val requiredKills: Int, val rank: String)

val CHOMPY_HATS = listOf(
    ChompyHat(Obj.obj_2978, 30, "Ogre Bowman"),
    ChompyHat(Obj.obj_2979, 40, "Bowman"),
    ChompyHat(Obj.obj_2980, 50, "Ogre Yeoman"),
    ChompyHat(Obj.obj_2981, 70, "Yeoman"),
    ChompyHat(Obj.obj_2982, 95, "Ogre Marksman"),
    ChompyHat(Obj.obj_2983, 125, "Marksman"),
    ChompyHat(Obj.obj_2984, 170, "Ogre Woodsman"),
    ChompyHat(Obj.obj_2985, 225, "Woodsman"),
    ChompyHat(Obj.obj_2986, 300, "Ogre Forester"),
    ChompyHat(Obj.obj_2987, 400, "Forester"),
    ChompyHat(Obj.obj_2988, 550, "Ogre Bowmaster"),
    ChompyHat(Obj.obj_2989, 700, "Bowmaster"),
    ChompyHat(Obj.obj_2990, 1000, "Ogre Expert"),
    ChompyHat(Obj.obj_2991, 1300, "Expert"),
    ChompyHat(Obj.obj_2992, 1700, "Ogre Dragon Archer"),
    ChompyHat(Obj.obj_2993, 2250, "Dragon Archer"),
    ChompyHat(Obj.obj_2994, 3000, "Expert Ogre Dragon Archer"),
    ChompyHat(Obj.obj_2995, 4000, "Expert Dragon Archer")
)

fun claimChompyHats(player: Player): Int {
    val kills = getChompyKills(player)
    val earned = CHOMPY_HATS.filter { kills >= it.requiredKills && !ownsChompyHat(player, it.itemId) }
    earned.forEach { player.inventory.addItemDrop(it.itemId, 1) }
    return earned.size
}

fun nextChompyHat(player: Player): ChompyHat? = getChompyKills(player).let { kills -> CHOMPY_HATS.firstOrNull { kills < it.requiredKills } }

fun highestEarnedChompyHat(player: Player): ChompyHat? = getChompyKills(player).let { kills -> CHOMPY_HATS.lastOrNull { kills >= it.requiredKills } }

private fun ownsChompyHat(player: Player, itemId: Int): Boolean =
    player.inventory.containsItem(itemId, 1) || player.bank.containsItem(itemId) || player.equipment.containsOneItem(itemId)
