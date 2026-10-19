package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import com.rs.game.tasks.WorldTasks
import org.darkan.core.cache.NPCDefinitions
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim

private const val SIPHON_TICKS = 6

object Siphon : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        nex.killBloodReavers()
        nex.forceTalk("A siphon will solve this!")
        nex.voiceEffect(target, Sound.sound_3317, true)
        nex.anim(Seq.godwars2_zaros_blood_siphon)
        nex.spotAnim(Spotanim.zaros_blood_siphone_spotanim)
        nex.tempAttribs.setB("siphoning", true)
        val reaverSize = NPCDefinitions.getDefs(Npc.godwars2_zaros_magic_fighter).size
        var spawned = 0
        repeat(nex.bloodReavers.size) {
            val tile = nex.getNearestTeleTile(reaverSize) ?: return@repeat
            nex.bloodReavers[spawned++] = NPC(Npc.godwars2_zaros_magic_fighter, tile, true)
        }
        WorldTasks.schedule(SIPHON_TICKS) { nex.tempAttribs.setB("siphoning", false) }
        return nex.attackSpeed
    }
}
