package com.darkan.bot.scripts.impl

import com.darkan.bot.Bot.*
import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptDescription
import com.darkan.bot.scripts.StringConfigItem

@ScriptDescription(
    author = "Trent",
    name = "AIO Pickpocket",
    version = "1.0",
    description = "Memes"
)
class AIOPickpocket : Script(), ConfigurableScript {
    val target = StringConfigItem(
        name = "Target",
        description = "Select target to pickpocket",
        initialValue = "None"
    )

    override suspend fun loop() {
        if (inventory.freeSlots() <= 0) {
            openClosestBank()
            delayUntil(15000) { interfaceOpen(762) }
            bankAll()
        } else {
            clickNPC(getClosestNPCNoClip(target.value), "pickpocket")
            delay(200)
        }
    }
}