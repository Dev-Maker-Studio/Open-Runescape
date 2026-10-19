package com.rs.game.content.skills.herblore

import com.rs.engine.dialogue.statements.MakeXStatement
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

class Herblore(private val potion: CraftablePotion, private var ticks: Int) : PlayerAction() {
    override fun start(player: Player): Boolean {
        if (!player.knowsPotion(potion)) return false
        if (player.skills.getLevel(Constants.HERBLORE) < potion.req) {
            player.simpleDialogue("You need a herblore level of ${potion.req} to combine these ingredients.")
            return false
        }
        return true
    }

    override fun process(player: Player): Boolean {
        if (!player.knowsPotion(potion)) return false
        if (!player.inventory.containsItem(potion.primary) || !player.inventory.containsItems(*potion.secondaries)) {
            player.sendMessage("You don't have enough ingredients left.", true)
            return false
        }
        return true
    }

    override fun processWithDelay(player: Player): Int {
        player.anim(Seq.human_herbing_vial)
        ticks--
        player.inventory.deleteItem(potion.primary)
        val secondaries = potion.secondaries.toMutableList()
        val cleansingProc = player.hasScrollOfCleansing && Utils.random(100) <= 10
        if (cleansingProc) player.sendMessage("Your scroll of cleansing saves ${Utils.addArticle(secondaries.removeAt(Utils.random(secondaries.size)).name.lowercase())}.")
        if (secondaries.isNotEmpty()) player.inventory.removeItems(secondaries)
        player.inventory.addItemDrop(potion.product.clone())
        player.incrementCount("${potion.product.name} mixed")
        when {
            potion == CraftablePotion.SERUM_207 -> player.sendMessage("You make serum 207.", true)
            potion.product.id == Obj.eadgar_ground_troll_thistle_potion -> player.sendMessage("You add the ground Troll Thistle to the potion.")
            else -> player.sendMessage("You mix ${Utils.addArticle(potion.product.name.lowercase())}.", true)
        }
        player.skills.addXp(Constants.HERBLORE, potion.xp)
        return if (ticks > 0) if (cleansingProc) 0 else 1 else -1
    }

    override fun stop(player: Player) { setActionDelay(player, 3) }
}

@ServerStartupEvent
fun mapHerblore() {
    onItemOnItem(true, CraftablePotion.MAP.keys.map { it as Any }.toTypedArray()) { e ->
        val potion = CraftablePotion.forCombo(e.item1.id, e.item2.id) ?: return@onItemOnItem
        val player = e.player
        if (!player.knowsPotion(potion)) return@onItemOnItem
        if (potion == CraftablePotion.ANCHOVY_OIL && !player.inventory.containsItem(Obj.mourning_sieve, 1)) {
            player.sendMessage("You need a sieve to make anchovy oil.")
            return@onItemOnItem
        }
        player.startConversation {
            makeX(intArrayOf(potion.product.id), 28)
            exec { player.actionManager.setAction(Herblore(potion, MakeXStatement.getQuantity(player))) }
        }
    }
}

private fun Player.knowsPotion(potion: CraftablePotion): Boolean {
    if (potion != CraftablePotion.SERUM_207 || isQuestStarted(Quest.SHADES_OF_MORTTON)) return true
    sendMessage("You need to read Herbi Flax's diary before you can make Serum 207.")
    return false
}
