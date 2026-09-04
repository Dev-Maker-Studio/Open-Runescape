package com.darkan.bot.scripts.impl

import com.darkan.bot.Bot
import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptDescription
import com.darkan.bot.scripts.StringConfigItem

@ScriptDescription(
    author = "Trent",
    name = "Combat",
    version = "1.0",
    description = "Memes"
)
class Combat : Script(), ConfigurableScript {
    val target = StringConfigItem(
        name = "Target",
        description = "Select target to attack",
        initialValue = "None"
    )

    override fun onStart() {
        addParallelScript(KeepSpecOn())
    }

    override suspend fun loop() {
        if (!Bot.myPlayerInCombat()) {
            Bot.clickNPC(Bot.getClosestNPCNoClipContaining(target.value), "attack")
            waitThenDelayUntil(1200, 6000) { !Bot.myPlayerInCombat() }
        }
    }
}

class KeepSpecOn : Script() {
    override suspend fun loop() {
        if (!Bot.usingSpec() && Bot.specPercent() >= 25) {
            Bot.clickButton(884, 4)
            delayUntil(2000) { Bot.usingSpec() }
        }
    }
}