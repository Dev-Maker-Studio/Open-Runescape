package com.rs.game.content.skills.hunter.puropuro

import com.rs.game.model.item.ItemTooltips
import com.rs.game.model.item.TooltipSection
import com.rs.plugin.annotations.ServerStartupEvent

@ServerStartupEvent
fun mapImpRepellentTooltip() {
    ItemTooltips.metadata("used", TooltipSection.STATE) { _, _, value ->
        if (value == true) "<col=ff4040>Used</col>" else "<col=00ff00>Unused</col>"
    }
}
