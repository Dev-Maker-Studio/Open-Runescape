package com.darkan.bot.scripts.impl

import com.darkan.bot.Bot
import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptDescription
import com.darkan.bot.utils.Tile
import com.jagex.game.runetek5.config.TypeLists

@ScriptDescription(
    author = "Trent",
    name = "Farm It Up",
    version = "1.0",
    description = "Memes"
)
class FarmItUp : Script() {
    val varBit = TypeLists.LOC!!.list(8151).varpbitId

    override suspend fun loop() {
        val value: Int = Bot.getVarBit(varBit)
        when {
            value < 3 -> {
                if (!Bot.isAnimating())
                    Bot.clickObject(8151, Tile(2813, 3463, 0), 1)
            }
            value == 3 -> Bot.sendItemOnObject("seed", 8151)
            value <= 7 -> Bot.clickItem("rotten potato", 2)
            else -> Bot.clickObject(8151, Tile(2813, 3463, 0), 1)
        }
    }
}