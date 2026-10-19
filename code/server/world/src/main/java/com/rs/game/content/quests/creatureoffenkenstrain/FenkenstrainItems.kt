package com.rs.game.content.quests.creatureoffenkenstrain

import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onPickupItem
import com.rs.plugin.kts.onNpcClick
import world.gregs.voidps.type.Tile
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private val BRUSHES = listOf(Obj.fenk_brush0, Obj.fenk_brush1, Obj.fenk_brush2, Obj.fenk_brush3)

@ServerStartupEvent
fun mapFenkenstrainItems() {
    onItemOnItem(intArrayOf(Obj.fenk_marble_amulet), intArrayOf(Obj.fenk_obsidian_amulet)) { event ->
        event.player.combineFenkItems(Obj.fenk_marble_amulet, Obj.fenk_obsidian_amulet, Obj.fenk_star_amulet,
            "The marble and obsidian amulets snap together tightly to form a six-pointed amulet.")
    }
    onItemOnItem(intArrayOf(Obj.fenk_head_empty), intArrayOf(Obj.fenk_brain)) { event ->
        event.player.combineFenkItems(Obj.fenk_head_empty, Obj.fenk_brain, Obj.fenk_head_full,
            "You squeeze the pickled brain into the decapitated head.")
    }
    onItemOnItem(intArrayOf(Obj.fenk_cane), BRUSHES.toIntArray()) { event ->
        val player = event.player
        val brush = listOf(event.item1.id, event.item2.id).firstOrNull { it in BRUSHES } ?: return@onItemOnItem
        when {
            brush == Obj.fenk_brush3 -> player.simpleDialogue("The brush is too long to attach any more canes.")
            !player.inventory.containsItem(Obj.bronzecraftwire) -> player.simpleDialogue("You try to attach the canes, but you need something suitable to hold them together.")
            player.inventory.containsItem(Obj.fenk_cane) && player.inventory.containsItem(brush) -> {
                player.inventory.deleteItem(Obj.bronzecraftwire, 1)
                player.combineFenkItems(Obj.fenk_cane, brush, BRUSHES[BRUSHES.indexOf(brush) + 1], "You attach the cane to the brush.")
            }
        }
    }
    onNpcClick(Npc.werewolfinnkeeper, options = arrayOf("Talk-to")) { (player) -> player.buyFenkBrain() }
    onPickupItem(Obj.fenk_brain, tiles = arrayOf(Tile.of(3495, 3474, 0))) { event ->
        if (!event.item.isRespawn || event.isTelegrabbed) return@onPickupItem
        event.cancelPickup()
        event.player.buyFenkBrain()
    }
    onItemClick(Obj.fenk_letter, options = arrayOf("Read")) { (player) ->
        player.simpleDialogue("Rologarth's letter is addressed to his brother Rologarric. He asks for help and forgiveness: following his doctor's advice, he sent his own people into the forest as tribute to the vampyres. He now suspects the doctor betrayed them to gain the castle, and fears that he will be the next victim.")
    }
}

private fun Player.combineFenkItems(first: Int, second: Int, result: Int, text: String) {
    if (!inventory.containsItem(first, 1, false) || !inventory.containsItem(second, 1, false)) return
    inventory.deleteItem(first, 1)
    inventory.deleteItem(second, 1)
    inventory.addItem(result, 1)
    itemDialogue(result, text)
}

private fun Player.buyFenkBrain() {
    if (inventory.containsItem(Obj.fenk_brain)) {
        startConversation { npc(Npc.werewolfinnkeeper, CALM_TALK, "You can leave that alone, my friend. I've already sold you one of your own - eat that. I can't afford to give away freebies in this business!") }
        return
    }
    startConversation {
        npc(Npc.werewolfinnkeeper, CALM_TALK, "You're interested in our speciality, I see. Would you like to buy some?")
        player(CALM_TALK, "What exactly is in the jar?")
        npc(Npc.werewolfinnkeeper, CALM_TALK, "Pickled brain, my friend. Only 50 gold to you.")
        player(CONFUSED, "Err...pickled brain from what animal?")
        npc(Npc.werewolfinnkeeper, CALM_TALK, "Animal? Don't be disgusting, man! No, this is a human brain - only the best for my customers.")
        if (!inventory.hasCoins(50)) player(CALM_TALK, "That sounds very nice, but I'm afraid I don't have enough gold at the moment.")
        else options {
            op("I'll buy one.") {
                player(CALM_TALK, "I'll buy one, please.")
                fenkExec(this@buyFenkBrain) {
                    val freesCoinSlot = inventory.pouchCoins < 50 && inventory.getNumberOf(Obj.coins, false).toLong() == 50 - inventory.pouchCoins
                    when {
                        inventory.containsItem(Obj.fenk_brain) -> simpleDialogue("You already have a pickled brain.")
                        !inventory.hasCoins(50) -> simpleDialogue("You need 50 coins to buy a pickled brain.")
                        !inventory.hasFreeSlots() && !freesCoinSlot -> simpleDialogue("You need a free inventory space.")
                        else -> {
                            inventory.removeCoins(50)
                            inventory.addItem(Obj.fenk_brain, 1)
                            startConversation {
                                npc(Npc.werewolfinnkeeper, HAPPY_TALKING, "A very wise choice, ${if (appearance.isMale) "sir" else "miss"}. Don't eat it all at once, savour every morsel - that's my advice to you.")
                            }
                        }
                    }
                }
            }
            op("I'm not hungry.") { player(CALM_TALK, "I'm afraid I'm not really hungry at the moment.") }
        }
    }
}
