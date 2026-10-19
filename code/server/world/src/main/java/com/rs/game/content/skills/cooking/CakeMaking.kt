package com.rs.game.content.skills.cooking

import com.rs.game.content.skills.util.CreateActionD
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private val CAKE_INGREDIENTS = intArrayOf(Obj.cake_tin, Obj.egg, Obj.bucket_milk, Obj.pot_flour)

@ServerStartupEvent
fun mapCakeMaking() {
    for (index in 0 until CAKE_INGREDIENTS.lastIndex) {
        onItemOnItem(intArrayOf(CAKE_INGREDIENTS[index]), CAKE_INGREDIENTS.drop(index + 1).toIntArray()) { event ->
            val player = event.player
            if (!CAKE_INGREDIENTS.all { player.inventory.containsItem(it, 1) }) {
                player.sendMessage("You need a cake tin, an egg, a bucket of milk and a pot of flour to make a cake.")
                return@onItemOnItem
            }
            player.startConversation(CreateActionD(player,
                arrayOf(CAKE_INGREDIENTS.map { Item(it, 1) }.toTypedArray()),
                arrayOf(arrayOf(Item(Obj.uncooked_cake, 1), Item(Obj.bucket_empty, 1), Item(Obj.pot_empty, 1))),
                doubleArrayOf(0.0), intArrayOf(-1), intArrayOf(40), Constants.COOKING, 1))
        }
    }
    onItemOnItem(intArrayOf(Obj.cake), intArrayOf(Obj.chocolate_bar, Obj.chocolate_dust)) { event ->
        val chocolate = event.getUsedWith(Obj.cake) ?: return@onItemOnItem
        event.player.startConversation(CreateActionD(event.player,
            arrayOf(arrayOf(Item(Obj.cake, 1), Item(chocolate.id, 1))),
            arrayOf(arrayOf(Item(Obj.chocolate_cake, 1))),
            doubleArrayOf(30.0), intArrayOf(-1), intArrayOf(50), Constants.COOKING, 1))
    }
}
