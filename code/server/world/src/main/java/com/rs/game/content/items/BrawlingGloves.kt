package com.rs.game.content.items

import com.rs.game.model.item.ItemTooltips
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.gameval.gen.Obj

private val combatBrawlingGloves = setOf(Obj.obj_13845, Obj.obj_13846, Obj.obj_13847)
private const val COMBAT_XP_CHARGES = 300000
private const val SKILLING_CHARGES = 464

@ServerStartupEvent
fun mapBrawlingGlovesTooltip() {
    ItemTooltips.metadata("brawlerCharges") { _, item, _ ->
        val charges = item.getMetaDataI("brawlerCharges").coerceAtLeast(0)
        if (item.id in combatBrawlingGloves) ItemTooltips.count("Bonus xp left", charges, COMBAT_XP_CHARGES)
        else ItemTooltips.count("Charges", charges, SKILLING_CHARGES)
    }
}
