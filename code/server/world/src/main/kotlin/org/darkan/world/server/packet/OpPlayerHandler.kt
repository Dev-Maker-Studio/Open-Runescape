package org.darkan.world.server.packet

import com.rs.engine.pathfinder.RouteEvent
import com.rs.game.World
import com.rs.game.content.clans.ClansManager
import com.rs.game.model.entity.actions.EntityFollow
import com.rs.game.model.entity.interactions.PlayerCombatInteraction
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.PlayerClickEvent
import org.darkan.core.net.prot.packet.OpPlayer
import org.darkan.core.net.prot.handler.PacketHandler

class OpPlayerHandler : PacketHandler<Player, OpPlayer> {
    override suspend fun handle(player: Player, packet: OpPlayer) {
        if (!player.hasStarted() || !player.clientHasLoadedMapRegion() || player.isDead || player.isLocked)
            return
        val target = World.players[packet.playerIndex] ?: return
        if (target.isDead || target.hasFinished() || !player.mapChunkIds.contains(target.chunkId))
            return
        if (packet.forceRun)
            player.setRun(true)

        if (packet.opNum in 1..10 && PluginManager.handle(PlayerClickEvent(player, target, player.getPlayerOption(packet.opNum), false)))
            return

        when (packet.opNum) {
            1 -> {
                if (!player.controllerManager.canPlayerOption1(target) || !player.isCanPvp || !player.controllerManager.canAttack(target))
                    return
                if (!player.isCanPvp || !target.isCanPvp) {
                    player.sendMessage("You can only attack players in a player-vs-player area.")
                    return
                }
                if (!target.isAtMultiArea || !player.isAtMultiArea) {
                    if (player.attackedBy != target && player.inCombat()) {
                        player.sendMessage("You are already in combat.")
                        return
                    }
                    if (target.attackedBy != player && target.inCombat()) {
                        if (target.attackedBy !is NPC) {
                            player.sendMessage("That player is already in combat.")
                            return
                        }
                        target.attackedBy = player
                    }
                }
                player.stopAll(true)
                player.interactionManager.setInteraction(PlayerCombatInteraction(player, target))
            }
            2 -> {
                if (!player.controllerManager.canPlayerOption2(target))
                    return
                player.stopAll(true)
                player.actionManager.setAction(EntityFollow(target))
            }
            3 -> {
                if (!player.controllerManager.canPlayerOption3(target))
                    return
                player.stopAll(true)
            }
            4 -> {
                player.stopAll(true)
                if (!player.controllerManager.canTrade() || !player.controllerManager.canPlayerOption4(target))
                    return
                player.setRouteEvent(RouteEvent(target) {
                    if (player.isCantTrade) {
                        player.sendMessage("You are busy.")
                        return@RouteEvent
                    }
                    if (target.interfaceManager.containsScreenInter() || target.isCantTrade) {
                        player.sendMessage("The other player is busy.")
                        return@RouteEvent
                    }
                    if (!target.withinDistance(player.tile, 14)) {
                        player.sendMessage("Unable to find target ${target.displayName}")
                        return@RouteEvent
                    }
                    if (target.tempAttribs.getO<Any>("TradeTarget") === player) {
                        target.tempAttribs.removeO<Any>("TradeTarget")
                        if (!player.bank.checkPin()) return@RouteEvent
                        if (!target.bank.checkPin()) return@RouteEvent
                        player.trade.openTrade(target)
                        target.trade.openTrade(player)
                        return@RouteEvent
                    }
                    player.tempAttribs.setO<Any>("TradeTarget", target)
                    player.sendMessage("Sending ${target.displayName} a request...")
                    target.packets.sendTradeRequestMessage(player)
                })
            }
            9 -> {
                player.stopAll()
                ClansManager.openClanInvite(player, target, target.clan)
            }
        }
    }
}
