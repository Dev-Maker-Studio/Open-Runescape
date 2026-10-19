package org.darkan.world.server.packet

import com.rs.engine.pathfinder.RouteEvent
import com.rs.game.World
import com.rs.game.content.Effect
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.GroundItemClickEvent
import com.rs.plugin.events.PickupItemEvent
import com.rs.utils.ItemConfig
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.GroundItem
import org.darkan.core.net.prot.packet.OpGroundItem
import org.darkan.core.net.prot.handler.PacketHandler
import world.gregs.voidps.type.Tile

private const val TAKE = 3
private const val EXAMINE = 6

class OpGroundItemHandler : PacketHandler<Player, OpGroundItem> {
    override suspend fun handle(player: Player, packet: OpGroundItem) {
        if (!player.hasStarted() || !player.clientHasLoadedMapRegion() || player.isDead)
            return
        if (player.isLocked || player.hasEffect(Effect.FREEZE))
            return
        val tile = Tile.of(packet.x, packet.y, player.plane)
        if (!player.mapChunkIds.contains(tile.getChunkId()))
            return
        fun current() = ChunkManager.getChunk(tile.getChunkId()).getGroundItem(packet.itemId, tile, player)
        val item = current() ?: return

        if (PluginManager.handle(GroundItemClickEvent(player, item, packet.opNum, false)))
            return

        if (packet.opNum == EXAMINE) {
            val def = ItemDefinitions.getDefs(item.id)
            if (item.getMetaData("combatCharges") != null)
                player.sendMessage("<col=FF0000>It looks like it will last another ${Utils.ticksToTime(item.getMetaDataI("combatCharges", 0).toDouble())}")
            player.packets.sendGroundItemMessage(item, "${ItemConfig.get(item.id).getExamine(item.item)} General store: ${Utils.formatTypicalInteger(def.getSellPrice())} High Alchemy: ${Utils.formatTypicalInteger(def.getHighAlchPrice())}")
            return
        }

        player.stopAll()
        if (packet.forceRun)
            player.setRun(true)

        if (packet.opNum != TAKE) {
            if (GroundItemClickEvent(player, item, packet.opNum, true).method == null) return
            player.setRouteEvent(RouteEvent(item) {
                PluginManager.handle(GroundItemClickEvent(player, current() ?: return@RouteEvent, packet.opNum, true))
            })
            return
        }
        player.setRouteEvent(RouteEvent(item, {
            val groundItem = current() ?: return@RouteEvent
            if (player.controllerManager.canTakeItem(groundItem)) pickUp(player, groundItem)
        }, {
            val groundItem = current()
            if (groundItem == null || !player.controllerManager.canTakeItem(groundItem))
                return@RouteEvent true
            if (!player.lineOfSightTo(groundItem.tile, true)) {
                player.sendMessage("You can't reach that.")
                return@RouteEvent true
            }
            player.anim(Seq.human_pickuptable)
            player.faceTile(groundItem.tile)
            pickUp(player, groundItem)
            true
        }))
    }

    private fun pickUp(player: Player, groundItem: GroundItem) {
        val pickup = PickupItemEvent(player, groundItem, false)
        PluginManager.handle(pickup)
        if (!pickup.isCancelPickup)
            World.removeGroundItem(player, groundItem, true)
    }
}
