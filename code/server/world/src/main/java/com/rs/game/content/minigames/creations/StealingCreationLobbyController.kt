package com.rs.game.content.minigames.creations

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.GroundItem
import world.gregs.voidps.type.Tile

class StealingCreationLobbyController : Controller() {

    override fun start() {
        sendInterfaces()
    }

    override fun sendInterfaces() {
        player.interfaceManager.sendOverlay(Interface.interface_804, false)
        StealingCreationLobby.updateTeamInterface(player, StealingCreationLobby.isRed(player))
    }

    override fun canTakeItem(item: GroundItem) = false

    override fun processObjectClick1(`object`: GameObject): Boolean {
        if (`object`.id != Loc.sc_team_a_stile && `object`.id != Loc.sc_team_b_stile) return true
        player.controllerManager.forceStop()
        climbOverStile(player, `object`, false)
        return false
    }

    override fun processTeleport(tele: Teleport): Boolean {
        if (tele.type() != TeleType.OBJECT) {
            player.simpleDialogue("A magical force prevents you from teleporting from the arena.")
            return false
        }
        return true
    }

    override fun onTeleported(type: TeleType) {
        StealingCreationLobby.removePlayer(player)
    }

    override fun forceClose() {
        player.interfaceManager.removeOverlay()
        StealingCreationLobby.removePlayer(player)
    }

    override fun logout(): Boolean {
        StealingCreationLobby.removePlayer(player)
        player.tile = StealingCreation.EXIT
        return true
    }

    override fun login(): Boolean {
        StealingCreationLobby.removePlayer(player)
        player.tile = StealingCreation.EXIT
        return true
    }

    companion object {
        @JvmStatic
        fun climbOverStile(player: Player, stile: GameObject, entrance: Boolean) {
            if (entrance) {
                if (!StealingCreationLobby.enterTeamLobby(player, stile.id == Loc.sc_team_b_stile)) return
            } else
                StealingCreationLobby.removePlayer(player)
            player.lock(3)
            player.forceMove(Tile.of(if (entrance) stile.x else stile.x + 2, stile.y, stile.plane), Seq.human_crossing_stile, 5, 60)
        }
    }
}
