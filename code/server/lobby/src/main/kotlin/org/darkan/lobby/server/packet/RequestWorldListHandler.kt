package org.darkan.lobby.server.packet

import org.darkan.core.net.prot.packet.RequestWorldList
import org.darkan.core.net.prot.packet.WorldListPacket
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.lobby.Lobby
import org.darkan.lobby.web.model.LobbyPlayer

class RequestWorldListHandler : PacketHandler<LobbyPlayer, RequestWorldList> {
    override suspend fun handle(player: LobbyPlayer, packet: RequestWorldList) {
        // Always ask the client to decode immediately - we send the whole payload in one
        // packet, so anything it merely buffers is never applied. The client's list is only
        // resent when its version is stale; otherwise just refresh the player counts.
        val upToDate = packet.worldlistVersion == Lobby.worldList.revision
        player.session.send(WorldListPacket(Lobby.worldList.snapshot(), refreshOnClient = true, refreshOnlyPlayerCounts = upToDate))
    }
}