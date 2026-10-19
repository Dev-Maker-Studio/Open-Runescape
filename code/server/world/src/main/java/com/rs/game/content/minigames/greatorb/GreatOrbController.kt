package com.rs.game.content.minigames.greatorb

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item

private val CLEARED_VARBITS = intArrayOf(
    Varbit.rcguild_team, Varbit.rcguild_wand_held, Varbit.rcguild_activity,
    Varbit.rcguild_contributed_1, Varbit.rcguild_contributed_2, Varbit.rcguild_contributed_3, Varbit.rcguild_contributed_4,
    Varbit.rcguild_contributed_5, Varbit.rcguild_contributed_6, Varbit.rcguild_contributed_7, Varbit.rcguild_contributed_8,
)
private const val WAND_NONE = 0
private const val WAND_ATTRACT = 1
private const val WAND_REPEL = 2

val Player.greatOrbController get() = controllerManager.controller as? GreatOrbController
val Player.greatOrbGame get() = greatOrbController?.game
val Player.orbTeam get() = greatOrbController?.team

class GreatOrbController(val team: OrbTeam) : Controller() {
    @Transient
    var game: GreatOrbGame? = null
        set(value) {
            field = value
            sendInterfaces()
        }

    @Transient
    private var stayPut = false

    override fun start() = sendInterfaces()

    override fun sendInterfaces() {
        game?.sendHud(player) ?: GreatOrbLobby.sendHud(player)
    }

    override fun process() {
        player.vars.setVarBit(Varbit.rcguild_wand_held, when (player.equipment.weaponId) {
            team.attractor -> WAND_ATTRACT
            team.repeller -> WAND_REPEL
            else -> WAND_NONE
        })
    }

    override fun processObjectClick1(`object`: GameObject): Boolean {
        if (`object`.id == Loc.rcguild_wiztower_portal) {
            player.controllerManager.forceStop()
            return true
        }
        return game?.handleObject(player, `object`) ?: true
    }

    override fun canDropItem(item: Item) = item.id !in OrbTeam.allTools

    override fun canSummonFamiliar(): Boolean {
        player.sendMessage("You can't bring a familiar into the Great Orb Project.")
        return false
    }

    override fun processTeleport(tele: Teleport) = true

    override fun onTeleported(type: TeleType) {
        stayPut = true
        player.controllerManager.forceStop()
    }

    override fun logout(): Boolean {
        stayPut = true
        if (game != null) player.tile = GUILD_RETURN
        return true
    }

    override fun login(): Boolean {
        stayPut = true
        player.tile = GUILD_RETURN
        return true
    }

    override fun forceClose() {
        val game = game
        GreatOrbLobby.leave(player)
        game?.leave(player)
        player.interactionManager.forceStop()
        for (tool in OrbTeam.allTools) {
            player.inventory.deleteItem(tool, Int.MAX_VALUE)
            player.equipment.deleteItem(tool, 1)
        }
        player.appearance.generateAppearanceData()
        CLEARED_VARBITS.forEach { player.vars.setVarBit(it, 0) }
        player.interfaceManager.removeOverlay()
        if (game != null && !stayPut) player.tele(GUILD_RETURN)
    }
}
