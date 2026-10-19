package com.rs.game.content.quests.bigchompybirdhunting.objects

import com.rs.engine.quest.Quest
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_COOKING_CHOMPY
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_HUNTING_CHOMPY
import com.rs.game.content.quests.bigchompybirdhunting.dialogues.ASKED_BUGS_CHOMPY_INGREDIENT
import com.rs.game.content.quests.bigchompybirdhunting.dialogues.ASKED_FYCIE_CHOMPY_INGREDIENT
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

private const val OGRE_SPIT_ROAST = "Ogre spit-roast"
private val SEASONED_CHOMPY_INGREDIENTS = intArrayOf(Obj.raw_chompy, Obj.onion, Obj.tomato, Obj.cabbage)

@ServerStartupEvent
fun mapBigChompyOgreSpitRoast() {
    onItemOnObject(arrayOf(OGRE_SPIT_ROAST), arrayOf(Obj.raw_chompy)) { (player, obj) ->
        if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) == STAGE_COOKING_CHOMPY)
            cookSeasonedChompy(player, obj)
    }
}

fun reservesQuestChompy(player: Player, locName: String): Boolean {
    when (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING)) {
        STAGE_HUNTING_CHOMPY -> player.sendMessage("You should show this chompy to Rantz before cooking it.")
        STAGE_COOKING_CHOMPY -> if (locName != OGRE_SPIT_ROAST) player.sendMessage("Rantz wants this chompy cooked on his spit-roast.")
        else -> return false
    }
    return true
}

private fun cookSeasonedChompy(player: Player, spitRoast: GameObject) {
    if (player.isLocked) return
    if (!player.inventory.containsItem(Obj.raw_chompy, 1)) return
    val questAttribs = player.questManager.getAttribs(Quest.BIG_CHOMPY_BIRD_HUNTING)
    if (!questAttribs.getB(ASKED_FYCIE_CHOMPY_INGREDIENT) || !questAttribs.getB(ASKED_BUGS_CHOMPY_INGREDIENT)) {
        player.sendMessage("You should find out how Fycie and Bugs want their chompy prepared first.")
        return
    }
    if (!hasSeasonedChompyVegetables(player)) return
    player.lock(6)
    player.faceObject(spitRoast)
    player.schedule {
        player.sendMessage("You carefully place the chompy bird on the spit-roast.")
        player.anim(Seq.human_cooking)
        wait(2)
        player.sendMessage("You add the other ingredients and cook the food.")
        wait(3)
        if (!player.inventory.containsItem(Obj.raw_chompy, 1) || !hasSeasonedChompyVegetables(player)) return@schedule
        SEASONED_CHOMPY_INGREDIENTS.forEach { player.inventory.deleteItem(it, 1) }
        player.inventory.addItemDrop(Obj.cooked_s_chompy, 1)
        player.sendMessage("Eventually the chompy is cooked.")
        player.sendMessage("You use the onion, tomato and the cabbage with the chompy bird to make a seasoned chompy.")
    }
}

private fun hasSeasonedChompyVegetables(player: Player): Boolean {
    if (!player.inventory.containsItem(Obj.onion, 1)) {
        player.sendMessage("You need an onion to prepare the chompy.")
        return false
    }
    if (!player.inventory.containsItem(Obj.tomato, 1)) {
        player.sendMessage("You need a tomato to prepare the chompy.")
        return false
    }
    if (!player.inventory.containsItem(Obj.cabbage, 1)) {
        player.sendMessage("You need a cabbage to prepare the chompy.")
        return false
    }
    return true
}
