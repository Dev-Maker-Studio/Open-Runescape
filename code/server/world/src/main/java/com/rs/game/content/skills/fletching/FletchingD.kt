package com.rs.game.content.skills.fletching

import com.rs.engine.dialogue.Conversation
import com.rs.game.content.Toolbelt
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.statements.MakeXStatement
import com.rs.engine.dialogue.statements.MakeXStatement.MakeXType
import com.rs.game.content.skills.fletching.Fletching.Fletch
import com.rs.game.model.entity.player.Player
import org.darkan.core.cache.ItemDefinitions

class FletchingD(player: Player, items: Fletch) : Conversation(player) {
    init {
        val maxQuantityTen = items != Fletch.ACHEY_LOG && Fletching.maxMakeQuantityTen(items) && items.product[0] != Fletching.ARROW_SHAFT
        val names = items.product.map { ItemDefinitions.getDefs(it).name.replace(" (u)", "") }.toTypedArray()
        val makeX = addNext(
            MakeXStatement(
                if (maxQuantityTen) MakeXType.MAKE_SET else MakeXType.MAKE,
                if (maxQuantityTen) 10 else 28,
                "Choose how many you wish to make,<br>then click on the item to begin.",
                items.product,
                if (maxQuantityTen) null else names
            )
        )

        items.product.indices.forEach { option ->
            makeX.addNext {
                val selectedQuantity = MakeXStatement.getQuantity(player)
                val available = player.inventory.items.getNumberOf(items.id)
                val quantity = selectedQuantity.coerceAtMost(available)
                player.actionManager.setAction(Fletching(items, option, quantity))
            }
        }

        create()
    }
}

@ServerStartupEvent
fun mapAcheyLogFletching() {
    onItemClick(Obj.achey_tree_logs, options = arrayOf("Craft")) { e ->
        if (!e.player.inventory.containsOneItem(Obj.knife) && !Toolbelt.containsTool(e.player, Obj.knife)) {
            e.player.sendMessage("You need a knife to fletch the achey tree logs.")
            return@onItemClick
        }
        e.player.startConversation(FletchingD(e.player, Fletch.ACHEY_LOG))
    }
}

@ServerStartupEvent
fun mapWolfBoneFletching() {
    onItemClick(Obj.wolf_bones, options = arrayOf("Craft")) { (player) ->
        if (!player.inventory.containsOneItem(Obj.chisel) && !Toolbelt.containsTool(player, Obj.chisel)) {
            player.sendMessage("You need a chisel to make wolfbone arrowheads.")
            return@onItemClick
        }
        player.startConversation(FletchingD(player, Fletch.WOLFBONE_ARROWHEADS))
    }
}
