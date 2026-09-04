package com.darkan.bot.scripts.impl

import com.darkan.bot.Bot.alch
import com.darkan.bot.Bot.clickClosestObject
import com.darkan.bot.Bot.depositAllExcept
import com.darkan.bot.Bot.interfaceOpen
import com.darkan.bot.Bot.inventory
import com.darkan.bot.Bot.isAnimating
import com.darkan.bot.Bot.openClosestBank
import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptDescription
import com.darkan.bot.scripts.StringConfigItem
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.item.ItemTypeList

@ScriptDescription(
    author = "Trent",
    name = "Alch",
    version = "1.0",
    description = "Alchs items by name"
)
class Alch : Script(), ConfigurableScript {
    val itemName = StringConfigItem(
        name = "Target",
        description = "Select target to alch",
        initialValue = "None"
    )

    override suspend fun loop() {
        val targetName = itemName.value.trim()

        if (targetName.isEmpty() || targetName.equals("None", ignoreCase = true)) {
            delay(600)
            return
        }

        val itemSlot = inventory.container.itemIds.indexOfFirst { itemId ->
            if (itemId > 0) TypeLists.OBJ!!.list(itemId).name.contains(targetName, ignoreCase = true) else false
        }

        if (itemSlot != -1) {
            alch(itemSlot)
            delay(310)
        } else
            delay(600)
    }
}