package com.rs.game.content.world.areas.mos_le_harmless

import com.rs.engine.quest.Quest
import com.rs.game.content.containsTool
import com.rs.game.content.world.areas.dungeons.UndergroundDungeonController
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.type.Tile

private const val BRIDGE_PLANKS = 4
private const val BRIDGE_NAILS = 8
private const val BRIDGE_CROSS_CYCLES = 90
private val NAILS = intArrayOf(Obj.nails_bronze, Obj.nails_iron, Obj.nails, Obj.nails_black, Obj.nails_mithril, Obj.obj_4823, Obj.nails_rune)

private enum class HarmlessBridge(val varbit: Int, val locs: Array<Any>, val towardsMos: (Player) -> Boolean, val landing: (Player) -> Tile) {
    FAIRY_RING(Varbit.ft3_mos_le_harmless_bridge_1, arrayOf(Loc.loc_52622, Loc.loc_52623, Loc.loc_52624, Loc.loc_52625),
        { false },
        { Tile.of(if (it.x >= 3754) 3751 else 3754, it.y.coerceIn(2926, 2927), 0) }),
    MOS(Varbit.ft3_mos_le_harmless_bridge_2, arrayOf(Loc.loc_52626, Loc.loc_52627, Loc.loc_52628, Loc.loc_52629),
        { it.y < 2940 },
        { Tile.of(it.x.coerceIn(3739, 3740), if (it.y >= 2940) 2937 else 2940, 0) });

    fun repaired(player: Player) = player.vars.getVarBit(varbit) == 1
}

@ServerStartupEvent
fun mapMosLeHarmless() {
    onObjectClick(Loc.fever_gangplank_harmless, Loc.fever_gangplank_exit_harmless) { (player, obj) ->
        val entering = player.y > obj.y
        player.useStairs(-1, player.transform(0, if (entering) -3 else 3, if (entering) 1 else -1), 0, 1)
    }
    onObjectClick(Loc.harmless_cave_entrance_left) { (player) ->
        player.useStairs(-1, Tile.of(3748, 9373, 0), 0, 1)
        player.controllerManager.startController(UndergroundDungeonController(false, true))
    }
    onObjectClick(Loc.harmless_spiral_steps_hole) { (player, obj) ->
        when (obj.x) {
            3829 -> player.useStairs(-1, Tile.of(3830, 9461, 0))
            3814 -> player.useStairs(-1, Tile.of(3815, 9461, 0))
        }
        player.controllerManager.startController(UndergroundDungeonController(false, true))
    }
    onObjectClick(Loc.harmless_dungeon_exit_right, Loc.harmless_dungeon_exit_left) { (player) ->
        player.useStairs(-1, Tile.of(3749, 2973, 0), 0, 1)
    }
    HarmlessBridge.entries.forEach { bridge ->
        onItemOnObject(bridge.locs, arrayOf<Any>(Obj.woodplank, Obj.hammer, *NAILS.toTypedArray())) { (player) -> player.repairBridge(bridge) }
        onObjectClick(*bridge.locs) { (player) -> player.crossBridge(bridge) }
    }
}

private fun Player.repairBridge(bridge: HarmlessBridge) {
    if (bridge.repaired(this)) { sendMessage("This part of the bridge has already been repaired."); return }
    if (!isQuestComplete(Quest.FAIRY_TALE_III_BATTLE_AT_ORKS_RIFT, "to repair this bridge.")) return
    val nails = NAILS.firstOrNull { inventory.containsItem(it, BRIDGE_NAILS) }
    if (!inventory.containsItem(Obj.hammer, 1) && !containsTool(Obj.hammer)) { simpleDialogue("You need a hammer to repair the bridge."); return }
    if (!inventory.containsItem(Obj.woodplank, BRIDGE_PLANKS) || nails == null) { simpleDialogue("You need $BRIDGE_PLANKS planks and $BRIDGE_NAILS nails of one type to repair this part of the bridge."); return }
    lock(3)
    anim(Seq.human_smithing)
    inventory.deleteItem(Obj.woodplank, BRIDGE_PLANKS)
    inventory.deleteItem(nails, BRIDGE_NAILS)
    vars.saveVarBit(bridge.varbit, 1)
    sendMessage("You repair this part of the bridge.")
}

private fun Player.crossBridge(bridge: HarmlessBridge) {
    if (!bridge.repaired(this)) return
    if (bridge.towardsMos(this) && !isQuestComplete(Quest.CABIN_FEVER, "to travel to Mos' Le Harmless.")) return
    forceMove(bridge.landing(this), Seq.human_walk_f, 0, BRIDGE_CROSS_CYCLES)
}
