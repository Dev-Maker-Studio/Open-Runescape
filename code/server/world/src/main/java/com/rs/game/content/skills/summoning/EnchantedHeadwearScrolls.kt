package com.rs.game.content.skills.summoning

import com.rs.game.model.item.ItemTooltips
import com.rs.game.model.item.TooltipSection
import com.rs.plugin.annotations.ServerStartupEvent

@ServerStartupEvent
fun mapEnchantedHeadwearTooltip() {
    ItemTooltips.metadata("summScrollsStored", TooltipSection.CONTENTS) { _, item, _ ->
        val scroll = item.getMetaDataI("summScrollId")
        val stored = item.getMetaDataI("summScrollsStored")
        if (scroll < 0 || stored <= 0) "No scrolls" else "<obj=$scroll,$stored>"
    }
}
