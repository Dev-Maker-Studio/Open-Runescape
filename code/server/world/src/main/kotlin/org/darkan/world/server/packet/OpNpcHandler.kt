package org.darkan.world.server.packet

import com.rs.game.World
import com.rs.game.content.clans.ClansManager
import com.rs.game.model.entity.player.Player
import com.rs.net.decoders.handlers.NPCHandler
import org.darkan.core.net.prot.packet.OpNpc
import org.darkan.core.net.prot.handler.PacketHandler

class OpNpcHandler : PacketHandler<Player, OpNpc> {
    override suspend fun handle(player: Player, packet: OpNpc) {
        if (!player.hasStarted() || !player.clientHasLoadedMapRegion())
            return
        if (packet.forceRun)
            player.setRun(true)
        val npc = World.NPCs[packet.npcIndex] ?: return
        if (npc.isDead || npc.hasFinished() || !player.mapChunkIds.contains(npc.chunkId))
            return

        if (packet.opNum == 6) {
            NPCHandler.handleExamine(player, npc)
            if (player.nsv.getB("clanifyStuff"))
                ClansManager.clanifyNPC(player.clan, npc)
            return
        }

        if (player.isDead || player.isLocked)
            return

        when (packet.opNum) {
            1 -> NPCHandler.handleOption1(player, npc)
            2 -> NPCHandler.handleOption2(player, npc)
            3 -> NPCHandler.handleOption3(player, npc)
            4 -> NPCHandler.handleOption4(player, npc)
            5 -> NPCHandler.handleOption5(player, npc)
        }
    }
}
