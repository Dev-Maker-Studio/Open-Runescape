package com.rs.game.content

import com.rs.engine.dialogue.Dialogue
import com.rs.engine.dialogue.HeadE
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Spotanim

enum class LootBeam(val spotAnimId: Int, val size: String) {
    SMALL(Spotanim.njloot_2013_glow_t1, "small"),
    MEDIUM(Spotanim.njloot_2013_glow_t2, "medium"),
    LARGE(Spotanim.njloot_2013_glow_t3, "large"),
    HUGE(Spotanim.njloot_2013_glow, "huge");

    companion object {
        const val SETTING = "lootbeamAnim"
        const val THRESHOLD = "lootbeamThreshold"
        const val DEFAULT_THRESHOLD = 90000
        const val NONE = -1
        private const val OFF = 0

        @JvmStatic
        fun of(player: Player): LootBeam? = entries.firstOrNull { it.spotAnimId == player.getI(SETTING, OFF) }

        @JvmStatic
        fun settings(player: Player, npcId: Int): Dialogue = Dialogue()
            .addPlayer(HeadE.HAPPY_TALKING, "I'd like to change my threshold for valuable loot notifications.")
            .addNPC(npcId, HeadE.HAPPY_TALKING, "Okay, your current threshold is ${player.getI(THRESHOLD, DEFAULT_THRESHOLD)} GP. What would you like to set it to?")
            .addNext {
                player.sendInputInteger("What would you like to set it to?") { amount ->
                    if (amount >= 0)
                        player.set(THRESHOLD, amount)
                }
            }
            .addNPC(npcId, HeadE.HAPPY_TALKING, "Would you like to enable lootbeams, and how big should they be?")
            .addOptions { options ->
                for (beam in entries)
                    options.option("Yes, the ${beam.size} one!", Dialogue()
                        .addNPC(npcId, HeadE.CHEERFUL_EXPOSITION, "Alright, you will see ${beam.size} golden lootbeams over your valuable drops.") { player.set(SETTING, beam.spotAnimId) })
                options.option("No thanks.", Dialogue()
                    .addNPC(npcId, HeadE.CALM_TALK, "Alright, you won't see lootbeams over your valuable drops.") { player.set(SETTING, OFF) })
            }
    }
}
