package com.rs.game.content.quests.regicide.items

import com.rs.game.content.quests.regicide.giveRegicideItem
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

@ServerStartupEvent
fun mapRegicideIngredients() {
    onObjectClick(Loc.regicide_sulphar1, Loc.regicide_sulphar2, Loc.regicide_sulphar3) { (player) ->
        if (player.giveRegicideItem(Obj.regicide_sulphar))
            player.sendMessage("You break off a piece of the formation.")
    }
    onObjectClick(Loc.regicide_tar_collection) { (player) -> player.sendMessage("I need something to hold the coal tar.") }
    onItemOnObject(arrayOf(Loc.regicide_tar_collection), arrayOf(Obj.regicide_barrel_empty)) { e ->
        if (e.player.replaceRegicideIngredients(Obj.regicide_barrel_empty, null, Obj.regicide_barrel_tar))
            e.player.sendMessage("You fill the barrel with tar.")
    }
    onItemOnItem(intArrayOf(Obj.pestle_and_mortar), intArrayOf(Obj.regicide_sulphar, Obj.regicide_quicklime)) { e ->
        val source = e.getUsedWith(Obj.pestle_and_mortar) ?: return@onItemOnItem
        if (source.id == Obj.regicide_sulphar) {
            if (e.player.replaceRegicideIngredients(Obj.regicide_sulphar, null, Obj.regicide_sulphar_dust)) {
                e.player.anim(Seq.human_herbing_grind)
                e.player.sendMessage("You grind the sulphur to dust")
            }
        } else if (!e.player.inventory.containsItem(Obj.pot_empty)) {
            if (!e.player.inventory.containsItem(Obj.regicide_quicklime))
                return@onItemOnItem
            e.player.inventory.deleteItem(Obj.regicide_quicklime, 1)
            e.player.anim(Seq.human_herbing_grind)
            e.player.sendMessage("You spill the quicklime dust and burn yourself. You need an empty pot to hold it.")
            e.player.applyHit(Hit(e.player, 50, HitLook.TRUE_DAMAGE))
        } else if (e.player.replaceRegicideIngredients(Obj.regicide_quicklime, Obj.pot_empty, Obj.regicide_quicklime_dust)) {
            e.player.anim(Seq.human_herbing_grind)
            e.player.sendMessage("You grind the quicklime to dust.")
        }
    }
    onItemOnItem(intArrayOf(Obj.regicide_barrel_naphtha), intArrayOf(Obj.regicide_sulphar_dust, Obj.regicide_quicklime_dust)) { e ->
        val ingredient = e.getUsedWith(Obj.regicide_barrel_naphtha) ?: return@onItemOnItem
        val result = if (ingredient.id == Obj.regicide_sulphar_dust) Obj.regicide_barrel_naphtha_sulphar_mix else Obj.regicide_barrel_naphtha_quicklime_mix
        if (e.player.replaceRegicideIngredients(Obj.regicide_barrel_naphtha, ingredient.id, result))
            e.player.sendMessage(if (ingredient.id == Obj.regicide_sulphar_dust) "You mix the sulphur dust into the naphta." else "You mix the quicklime dust into the naphta.")
    }
    onItemOnItem(intArrayOf(Obj.regicide_barrel_naphtha_sulphar_mix), intArrayOf(Obj.regicide_quicklime_dust)) { e -> e.player.finishRegicideMixture(Obj.regicide_barrel_naphtha_sulphar_mix, Obj.regicide_quicklime_dust) }
    onItemOnItem(intArrayOf(Obj.regicide_barrel_naphtha_quicklime_mix), intArrayOf(Obj.regicide_sulphar_dust)) { e -> e.player.finishRegicideMixture(Obj.regicide_barrel_naphtha_quicklime_mix, Obj.regicide_sulphar_dust) }
    onItemOnItem(intArrayOf(Obj.regicide_barrel_lid), intArrayOf(Obj.regicide_cloth)) { e ->
        if (e.player.replaceRegicideIngredients(Obj.regicide_barrel_lid, Obj.regicide_cloth, Obj.regicide_barrel_lid_fused)) {
            e.player.sendMessage("You stuff cloth through a hole in the barrel to make a fuse.")
        }
    }
    onObjectClick(Loc.regicide_loom, Loc.loom) { (player) -> player.weaveRegicideCloth() }
    onItemOnObject(arrayOf(Loc.regicide_loom, Loc.loom), arrayOf(Obj.ball_of_wool)) { e -> e.player.weaveRegicideCloth() }
}

private fun Player.weaveRegicideCloth() {
    if (skills.getLevel(Skills.CRAFTING) < 10) {
        sendMessage("You need level 10 Crafting.")
        return
    }
    if (!inventory.containsItem(Obj.ball_of_wool, 4)) {
        sendMessage("You need four balls of wool.")
        return
    }
    inventory.deleteItem(Obj.ball_of_wool, 4)
    inventory.addItem(Obj.regicide_cloth, 1)
    skills.addXp(Skills.CRAFTING, 12.0)
}

private fun Player.finishRegicideMixture(mixture: Int, ingredient: Int) {
    if (!replaceRegicideIngredients(mixture, ingredient, Obj.regicide_barrel_lid))
        return
    sendMessage(if (ingredient == Obj.regicide_quicklime_dust) "You mix the quicklime dust into the naphta and seal the barrel." else "You mix the sulphur dust into the naphta and seal the barrel.")
    sendMessage("Now you just need to make a fuse for it.")
}

internal fun Player.replaceRegicideIngredients(first: Int, second: Int?, result: Int): Boolean {
    if (!inventory.containsItem(first) || (second != null && !inventory.containsItem(second)))
        return false
    inventory.deleteItem(first, 1)
    if (second != null)
        inventory.deleteItem(second, 1)
    inventory.addItem(result, 1)
    return true
}

fun Player.makeRegicideQuicklime(furnace: GameObject?) {
    if (furnace?.id == Loc.regicide_furnace) {
        sendMessage("This furnace is not hot enough to make quicklime.")
        return
    }
    if (!replaceRegicideIngredients(Obj.limestone, null, Obj.regicide_quicklime))
        return
    sendMessage("You place the limestone in the furnace.")
    sendMessage("You retrieve a block of quicklime.")
    if (equipment.getItem(Equipment.HANDS) == null)
        applyHit(Hit(this, 80, HitLook.TRUE_DAMAGE))
}
