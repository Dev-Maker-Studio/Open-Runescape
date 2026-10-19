package org.darkan.world.server.packet

import com.rs.game.World
import com.rs.game.content.clans.ClansManager
import com.rs.game.model.entity.player.Player
import com.rs.net.decoders.handlers.ObjectHandler
import org.darkan.core.net.prot.packet.OpObj
import org.darkan.core.net.prot.handler.PacketHandler
import world.gregs.voidps.type.Tile

class OpObjHandler : PacketHandler<Player, OpObj> {
    override suspend fun handle(player: Player, packet: OpObj) {
        if (!player.hasStarted() || !player.clientHasLoadedMapRegion() || player.isDead)
            return
        val tile = Tile.of(packet.x, packet.y, player.plane)
        if (!player.mapChunkIds.contains(tile.getChunkId()))
            return
        val mapObject = World.getObjectWithId(tile, packet.objectId) ?: return
        if (mapObject.id != packet.objectId)
            return

        if (packet.opNum == 6) {
            ObjectHandler.handleOptionExamine(player, mapObject)
            if (player.nsv.getB("clanifyStuff"))
                ClansManager.clanifyObject(player.clan, mapObject)
            return
        }

        if (player.isLocked || player.emotesManager.isAnimating)
            return
        player.stopAll(false)
        if (packet.forceRun)
            player.setRun(true)

        when (packet.opNum) {
            1 -> ObjectHandler.handleOption1(player, mapObject)
            2 -> ObjectHandler.handleOption2(player, mapObject)
            3 -> ObjectHandler.handleOption3(player, mapObject)
            4 -> ObjectHandler.handleOption4(player, mapObject)
            5 -> ObjectHandler.handleOption5(player, mapObject)
        }
    }
}
