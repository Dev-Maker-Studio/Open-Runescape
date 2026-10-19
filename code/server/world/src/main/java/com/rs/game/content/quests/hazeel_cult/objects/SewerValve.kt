package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.STAGE_UNSTARTED
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private data class Valve(
    val objectId: Int,
    val varbit: Int,
    val bitMask: Int,
    val number: Int,
    val correctDirection: String,
    val tile: Tile
)

private val valves = listOf(
    Valve(Loc.loc_2844, Varbit.sewervalves1, 1 shl 1, 1, "right", Tile.of(2552, 3247, 0)),
    Valve(Loc.loc_2845, Varbit.sewervalves2, 1 shl 2, 2, "right", Tile.of(2572, 3263, 0)),
    Valve(Loc.loc_2846, Varbit.sewervalves3, 1 shl 3, 3, "left", Tile.of(2585, 3245, 0)),
    Valve(Loc.loc_2847, Varbit.sewervalves4, 1 shl 4, 4, "right", Tile.of(2597, 3263, 0)),
    Valve(Loc.loc_2848, Varbit.sewervalves5, 1 shl 5, 5, "right", Tile.of(2611, 3242, 0))
)

@ServerStartupEvent
fun mapSewerValves() {
    onObjectClick(*arrayOf(*valves.map { it.objectId }.toTypedArray(), Loc.hazeelcult_sewervalve_left, Loc.hazeelcult_sewervalve_right)) { (player, obj, option) ->
        val valve = valves.minByOrNull { Utils.getDistance(it.tile, obj.tile) } ?: return@onObjectClick
        val correctlySet = player.vars.getVarBit(valve.varbit) == 1
        when (option) {
            "Inspect" -> {
                val direction = if (correctlySet) valve.correctDirection else if (valve.correctDirection == "right") "left" else "right"
                player.sendMessage("The valve is currently turned to the $direction.")
                player.sendMessage("The valve has a label on it: 'Valve ${valve.number} of 5'.")
            }
            "Turn-right", "Turn-left" -> {
                if (player.questManager.getStage(Quest.HAZEEL_CULT) == STAGE_UNSTARTED) {
                    player.sendMessage("You have no reason to alter the sewer flow.")
                    return@onObjectClick
                }
                player.vars.saveVar(Varp.sewervalves, player.vars.getVar(Varp.sewervalves) xor valve.bitMask)
                player.sendMessage("You turn the valve. Beneath your feet you hear the sudden sound of rushing water.")
            }
        }
    }
}
