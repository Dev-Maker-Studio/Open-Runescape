package com.darkan.bot.scripts.impl

import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptDescription
import com.darkan.bot.scripts.State
import com.darkan.bot.scripts.StateMachineScript

@ScriptDescription(
    author = "Trent",
    name = "Nex",
    version = "1.0",
    description = "Kills Nex"
)
class Nex : StateMachineScript<Nex>(), ConfigurableScript {
    override fun getStartState() = Fight

    override fun onStart() {
        addParallelScript(KeepSpecOn())
    }
}

object Fight : State<Nex>() {
    override suspend fun Nex.checkNext() = null
    override suspend fun Nex.stateLoop() {

    }
}
