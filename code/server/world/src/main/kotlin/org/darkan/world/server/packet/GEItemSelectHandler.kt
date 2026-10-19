package org.darkan.world.server.packet

import com.rs.game.ge.GE
import com.rs.game.model.entity.player.Player
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.net.prot.packet.ResumeItemSelect
import org.darkan.core.net.prot.handler.PacketHandler

class GEItemSelectHandler : PacketHandler<Player, ResumeItemSelect> {
    override suspend fun handle(player: Player, packet: ResumeItemSelect) {
        val def = ItemDefinitions.getDefs(packet.itemId) ?: return
        GE.selectItem(player, packet.itemId, 1)
    }
}
