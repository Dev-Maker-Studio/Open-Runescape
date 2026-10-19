package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import com.rs.game.World
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.content.quests.the_grand_tree.npcs.shipyardGateChallenge
import com.rs.game.content.world.doors.Doors.handleGate
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val SHIPYARD_GATES = arrayOf(
    Tile.of(2945, 3041, 0),
    Tile.of(2945, 3042, 0)
)

@ServerStartupEvent
fun mapShipyardGate() {
    onObjectClick(Loc.grandtree_fencegate_l, Loc.grandtree_fencegate_r, tiles = SHIPYARD_GATES) { (player, obj) ->
        if (player.grandTreeAttribs().getB(ATTR_SHIPYARD_ACCESS) || player.grandTreeStage > STAGE_REACHED_SHIPYARD) {
            handleGate(player, obj)
            return@onObjectClick
        }
        if (player.grandTreeStage != STAGE_REACHED_SHIPYARD) {
            player.sendMessage("The gate is locked.")
            return@onObjectClick
        }
        val worker = World.getNPCsInChunkRange(player.chunkId, 1).firstOrNull { it.id == Npc.grandtree_shipyardguard }
        if (worker == null)
            player.sendMessage("A shipyard worker stops you from opening the gate.")
        else
            shipyardGateChallenge(player, worker) { handleGate(player, obj) }
    }
}
