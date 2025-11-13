package com.darkan.bot.scripts.impl

import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptDescription

@ScriptDescription(
    author = "Trent",
    name = "Nex",
    version = "1.0",
    description = "Memes"
)
class Nex : Script(), ConfigurableScript {
    override suspend fun loop() {
        TODO("Not yet implemented")
    }

}