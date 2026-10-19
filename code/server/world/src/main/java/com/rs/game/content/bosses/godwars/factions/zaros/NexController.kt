package com.rs.game.content.bosses.godwars.factions.zaros

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.player.Controller
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varbit

class NexController(@Transient private var arena: NexArena? = null) : Controller() {
    private fun war() = arena ?: NexArena.globalInstance.also { arena = it }

    override fun start() {
        war().addPlayer(player)
        seal(1)
        sendInterfaces()
    }

    override fun logout(): Boolean {
        war().removePlayer(player)
        return false
    }

    override fun login(): Boolean {
        war().addPlayer(player)
        seal(1)
        sendInterfaces()
        return false
    }

    override fun sendInterfaces() {
        player.interfaceManager.setFadingInterface(Interface.interface_601)
        player.packets.sendRunScriptReverse(Cs2.cs2_1171)
    }

    override fun sendDeath(): Boolean {
        remove()
        removeController()
        return true
    }

    override fun onTeleported(type: TeleType) {
        remove()
        removeController()
    }

    override fun forceClose() = remove()

    private fun remove() {
        war().removePlayer(player)
        seal(0)
        player.interfaceManager.closeFadingInterface()
    }

    /** The landslide is a wall from the inside: the prison has no way out but death or a teleport. */
    private fun seal(value: Int) = player.vars.setVarBit(Varbit.godwars2_climbdown_multi, value)
}
