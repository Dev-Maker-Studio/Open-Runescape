package com.rs.game.content.bosses.instance

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.player.Controller
import com.rs.game.tasks.WorldTasks
import world.gregs.voidps.type.Tile

/**
 * The instance itself cannot survive a logout, but the way back out of it has to: the key is kept
 * so a player who logs in still lands on the right ground with the right area controller.
 */
class BossInstanceController(@Transient private val instance: BossInstance? = null, private val defKey: String = "") : Controller() {
    private val def: BossInstanceDef? get() = instance?.def ?: BossInstanceDefs[defKey]

    override fun start() {
        val live = instance ?: return leave()
        player.setInstancedArea(live.instance)
        player.tele(live.localTile(live.def.entrance))
        setInside(1)
    }

    /** A teleport already took them somewhere of their own choosing: let them keep it. */
    override fun onTeleported(type: TeleType) = release()

    override fun forceClose() {
        setInside(0)
        instance?.remove(player)
    }

    /** Kept on through the logout so that [login] can put the player back where they belong. */
    override fun logout(): Boolean {
        setInside(0)
        instance?.remove(player)
        player.setTile(returnTile())
        return false
    }

    override fun login(): Boolean {
        player.tele(returnTile())
        WorldTasks.schedule { restoreArea() }
        return true
    }

    fun leaveInstance() = leave()

    private fun leave() {
        release()
        player.tele(returnTile())
        restoreArea()
    }

    private fun release() {
        setInside(0)
        instance?.remove(player)
        removeController()
    }

    /**
     * An arena inside a controlled area leaves the player standing in it, so they get its controller
     * back: without it the godwars doors, altars and the prison entrance stop answering.
     */
    private fun restoreArea() {
        val area = def?.returnController ?: return
        player.controllerManager.startController(area())
    }

    private fun setInside(value: Int) {
        val varbit = def?.insideVarbit ?: return
        if (varbit != -1) player.vars.setVarBit(varbit, value)
    }

    private fun returnTile(): Tile = def?.returnTo ?: player.tile
}
