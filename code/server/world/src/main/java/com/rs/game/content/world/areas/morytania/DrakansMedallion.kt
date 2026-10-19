package com.rs.game.content.world.areas.morytania

import com.rs.game.model.item.ItemTooltips
import com.rs.plugin.annotations.ServerStartupEvent

private const val DRAKAN_MEDALLION_CHARGES = 10

@ServerStartupEvent
fun mapDrakansMedallionTooltip() {
    ItemTooltips.metadata("drakanCharges") { _, item, _ -> ItemTooltips.count("Teleports", item.getMetaDataI("drakanCharges", 0), DRAKAN_MEDALLION_CHARGES) }
}
