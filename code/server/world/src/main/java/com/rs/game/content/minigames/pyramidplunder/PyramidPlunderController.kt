package com.rs.game.content.minigames.pyramidplunder

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.model.Rights
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp

const val PLUNDER_INTERFACE = 428

class PyramidPlunderController : Controller() {
    private var tick = 500
    var currentRoom: Int = 0

    var correctDoor: Int = 0
        private set
    private val checkedDoors: MutableList<Int> = ArrayList()
    private val varbits: MutableMap<Int, Int> = HashMap()

    override fun start() {
        player.interfaceManager.sendOverlay(PLUNDER_INTERFACE)
        updatePlunderInterface()
        nextRoom()
    }

    override fun process() {
        if (tick == 0) {
            kickPlayer()
            tick = -1
            return
        }
        if (!player.hasRights(Rights.ADMIN)) tick--
        if (tick % 5 == 0) updatePlunderInterface()
    }

    private fun updatePlunderInterface() {
        player.vars.setVar(Varp.ntk_var_temp, (currentRoom + 1) * 10 + 1)
        player.vars.setVarBit(Varbit.ntk_room_number, currentRoom)
        player.vars.setVarBit(Varbit.ntk_player_timer_count, 500 - tick)
    }

    private fun kickPlayer() {
        player.lock()
        player.npcDialogue(4476, HeadE.CHILD_FRUSTRATED, "You've had your five minutes of plundering! Now be off with you!")
        player.schedule {
            wait(1)
            player.interfaceManager.setFadingInterface(115)
            wait(2)
            exitMinigame()
            wait(3)
            player.interfaceManager.setFadingInterface(170)
            player.unlock()
        }
    }

    override fun login(): Boolean {
        player.interfaceManager.sendOverlay(PLUNDER_INTERFACE)
        for (vb in varbits.keys) player.vars.setVarBit(vb, varbits[vb]!!)
        updatePlunderInterface()
        return false
    }

    override fun logout(): Boolean {
        return false
    }

    override fun sendDeath(): Boolean {
        forceClose()
        return true
    }

    override fun onTeleported(type: TeleType) {
        forceClose()
    }

    override fun forceClose() {
        player.interfaceManager.removeOverlay()
        removeController()
    }

    fun exitMinigame() {
        player.tele(PYRAMID_PLUNDER_EXIT_TILE)
        forceClose()
    }

    fun nextRoom() {
        when (currentRoom) {
            0 -> player.tele(Tile.of(1927, 4477, 0))
            1 -> player.tele(Tile.of(1977, 4471, 0))
            2 -> player.tele(Tile.of(1954, 4477, 0))
            3 -> player.tele(Tile.of(1927, 4453, 0))
            4 -> player.tele(Tile.of(1965, 4444, 0))
            5 -> player.tele(Tile.of(1927, 4424, 0))
            6 -> player.tele(Tile.of(1943, 4421, 0))
            7 -> player.tele(Tile.of(1974, 4420, 0))
            8 -> player.startConversation {
                simple("Opening this door will cause you to leave the pyramid.")
                options("Would you like to exit?") {
                    opExec("Yes") { exitMinigame() }
                    op("No")
                }
            }
        }
        if (currentRoom < 8) {
            correctDoor = PYRAMID_PLUNDER_DOORS[Utils.random(PYRAMID_PLUNDER_DOORS.size)]
            varbits.clear()
            checkedDoors.clear()
            for (i in 2346..2363) player.vars.setVarBit(i, 0)
            for (i in 2366..2369) player.vars.setVarBit(i, 0)
            player.vars.setVarBit(3422, 0)
            currentRoom++
        }
    }

    fun updateObject(obj: GameObject, value: Int) {
        varbits[obj.definitions.varbit] = value
        player.vars.setVarBit(obj.definitions.varbit, value)
    }
}
