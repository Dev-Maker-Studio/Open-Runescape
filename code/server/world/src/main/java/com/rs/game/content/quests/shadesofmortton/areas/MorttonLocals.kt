package com.rs.game.content.quests.shadesofmortton

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private data class CureGift(val id: Int, val minimum: Int, val maximum: Int, val weight: Int)

internal val afflictedLocals = mapOf(
    Npc.mort_afflicted_man to Npc.mort_man,
    Npc.mort_afflicted_woman to Npc.mort_woman,
    Npc.mort_afflicted_man2 to Npc.mort_man2,
    Npc.mort_afflicted_woman2 to Npc.mort_woman2
)

private val temporaryGifts = listOf(
    CureGift(Obj.coins, 10, 20, 59),
    CureGift(Obj.swamppaste, 1, 20, 26),
    CureGift(Obj.oliveoil1, 1, 3, 20),
    CureGift(Obj.oliveoil2, 1, 3, 15),
    CureGift(Obj.oliveoil3, 1, 1, 8),
    CureGift(Obj.oliveoil4, 1, 1, 11),
    CureGift(Obj.vial_water, 1, 3, 30),
    CureGift(Obj.logs, 1, 5, 40),
    CureGift(Obj.unidentified_tarromin, 1, 3, 34),
    CureGift(Obj.hammer, 1, 1, 3),
    CureGift(Obj.vial_empty, 1, 3, 10)
)

private val permanentGifts = listOf(
    CureGift(Obj.coins, 30, 40, 30),
    CureGift(Obj.unidentified_tarromin, 2, 5, 16),
    CureGift(Obj.limestonebrick, 2, 9, 14),
    CureGift(Obj.swamppaste, 10, 34, 14),
    CureGift(Obj.oliveoil1, 2, 5, 10),
    CureGift(Obj.oliveoil2, 2, 5, 7),
    CureGift(Obj.oliveoil4, 2, 3, 5),
    CureGift(Obj.logs, 1, 5, 5),
    CureGift(Obj.oliveoil3, 2, 4, 4),
    CureGift(Obj.shadekey_bronze_bloodred, 1, 1, 4),
    CureGift(Obj.oak_logs, 1, 5, 4),
    CureGift(Obj.shadekey_bronze_crimson, 1, 1, 3),
    CureGift(Obj.shadekey_bronze_brown, 1, 1, 3),
    CureGift(Obj.snail_corpse2, 2, 3, 2),
    CureGift(Obj.snail_corpse1, 1, 3, 3),
    CureGift(Obj.willow_logs, 2, 5, 2),
    CureGift(Obj.maple_logs, 3, 5, 1),
    CureGift(Obj.trail_clue_easy_simple001, 1, 1, 1)
)

@ServerStartupEvent
fun mapMorttonLocals() {
    onItemOnNpc(*afflictedLocals.keys.map { it as Any }.toTypedArray()) { (player, item, npc) ->
        val dose = serumDoses[item.id] ?: return@onItemOnNpc
        if (npc.isDead || npc.hasFinished()) return@onItemOnNpc
        val afflicted = npc.id
        val cured = afflictedLocals[afflicted] ?: return@onItemOnNpc
        val gift = player.rollMorttonCureGift(dose.permanent)
        player.consumeMorttonSerum(item, dose)
        npc.combat.reset()
        npc.resetWalkSteps()
        npc.setForceAgressive(false)
        npc.transformIntoNPC(cured)
        WorldTasks.schedule(50) {
            if (npc.id == cured) {
                if (npc.hasFinished() || npc.isDead) npc.setNPC(afflicted)
                else npc.transformIntoNPC(afflicted)
            }
        }
        player.startConversation {
            npc(npc, CALM_TALK, if (dose.permanent)
                "Oh...thank you, you used the permanent serum on me. Here, have something for your trouble."
            else "Oh... thank you for using the serum on me. Here, have something for your trouble.") {
                player.inventory.addItemDrop(gift)
            }
            simple("The local gives you some ${gift.name.lowercase()}.")
        }
    }
}

private fun Player.rollMorttonCureGift(permanent: Boolean): Item {
    val gifts = if (permanent) permanentGifts else temporaryGifts
    var roll = Utils.random(gifts.sumOf { it.weight })
    val gift = gifts.first { roll -= it.weight; roll < 0 }
    val id = if (gift.id == Obj.trail_clue_easy_simple001 && treasureTrailsManager.hasClueScrollItem())
        Obj.shadekey_bronze_bloodred else gift.id
    return Item(id, Utils.random(gift.minimum, gift.maximum + 1))
}
