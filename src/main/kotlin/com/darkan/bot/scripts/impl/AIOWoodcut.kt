package com.darkan.bot.scripts.impl

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
import com.darkan.bot.scripts.findAndPickupItems

@ScriptDescription(
    author = "Trent",
    name = "AIOWoodcut",
    version = "1.0",
    description = "Memes"
)
class AIOWoodcut : Script(), ConfigurableScript {
    val target = StringConfigItem(
        name = "Target",
        description = "Select target to woodcut",
        initialValue = "None"
    )

    val option = StringConfigItem(
        name = "Action",
        description = "Select target action",
        initialValue = "chop down"
    )

    override suspend fun loop() {
        if (inventory.freeSlots() <= 0) {
            openClosestBank()
            delayUntil(15000) { interfaceOpen(762) }
            depositAllExcept(5733, 20316, 20317)
        } else {
            if (findAndPickupItems("nest")) return
            if (isAnimating()) return
            clickClosestObject(target.value, option.value)
            delayUntil(2000) { isAnimating() }
        }
    }
}