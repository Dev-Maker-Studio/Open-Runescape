package com.rs.game.content.skills.thieving

import com.rs.game.content.minigames.pyramidplunder.PyramidPlunderController
import com.rs.game.model.entity.player.Equipment
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onXpDrop
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj

private const val SET_BONUS = 0.01
private const val MAX_BOOSTED_XP = 700.0

private val PIECES = mapOf(
    Equipment.HEAD to (Obj.hlr4m_shadowwrap_head_poh to 0.008),
    Equipment.CHEST to (Obj.hlr4m_shadowwrap_body_poh to 0.016),
    Equipment.LEGS to (Obj.hlr4m_shadowwrap_legs_poh to 0.012),
    Equipment.FEET to (Obj.hlr4m_shadowwrap_boots_poh to 0.004),
)

@ServerStartupEvent
fun mapBlackIbis() {
    onXpDrop { e ->
        if (e.skillId != Constants.THIEVING || e.xp > MAX_BOOSTED_XP) return@onXpDrop
        if (e.player.controllerManager.controller is PyramidPlunderController) return@onXpDrop
        val worn = PIECES.filter { (slot, piece) -> e.player.equipment.getId(slot) == piece.first }
        if (worn.isEmpty()) return@onXpDrop
        e.multiplier += worn.values.sumOf { it.second } + if (worn.size == PIECES.size) SET_BONUS else 0.0
    }
}
