package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.game.World
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Controller
import com.rs.game.tasks.WorldTasks
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq

private const val DEATH_TICKS = 4

class Halloween2010Controller : Controller() {
    override fun start() {
        player.tele(player.hw10Arrival)
        playHouseMusic()
    }

    override fun login(): Boolean {
        player.schedule { playHouseMusic() }
        return false
    }

    private fun playHouseMusic() {
        if (player.hw10Stage < Hw10Stage.PARTY) {
            player.musicsManager.playSongAndUnlock(Midi.midi_907)
            return
        }
        player.musicsManager.unlockMusic(Midi.midi_907)
        player.musicsManager.forcePlayMusic(Midi.midi_908)
    }

    override fun processTeleport(tele: Teleport): Boolean {
        player.sendMessage("A mysterious force prevents you from teleporting.")
        return false
    }

    override fun onTeleported(type: TeleType) {
        if (player.inHw10House)
            return
        player.vanishHw10Items()
        removeController()
    }

    override fun sendDeath(): Boolean {
        player.lock()
        player.stopAll()
        val death = World.spawnNPC(Npc.hw10_grim, player.tile.transform(1, 0, 0), true, true)
        player.schedule {
            player.anim(Seq.human_death)
            death.faceEntityTile(player)
            death.forceTalk("There is no escape!")
            wait(DEATH_TICKS / 2)
            death.forceTalk("I have come for you, ${player.displayName}.")
            wait(DEATH_TICKS / 2)
            death.finish()
            player.sendMessage("Oh dear, you have died.")
            WorldTasks.schedule(0) {
                player.reset()
                player.tele(player.hw10Arrival)
                player.anim(-1)
                player.unlock()
            }
        }
        return false
    }

    override fun logout() = false
}
