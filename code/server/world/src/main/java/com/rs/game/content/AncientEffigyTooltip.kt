package com.rs.game.content

import com.rs.game.model.item.ItemTooltips
import com.rs.game.model.item.TooltipSection
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.Constants

@ServerStartupEvent
fun mapAncientEffigyTooltip() {
    ItemTooltips.metadata("effigyType", TooltipSection.STATE) { _, item, _ ->
        val type = item.getMetaDataI("effigyType")
        if (type !in AncientEffigies.SKILL_1.indices) return@metadata null
        val first = Constants.SKILL_NAME[AncientEffigies.SKILL_1[type]]
        val second = Constants.SKILL_NAME[AncientEffigies.SKILL_2[type]]
        "Skills: ${ItemTooltips.value(first)} or ${ItemTooltips.value(second)}<br>Requires level ${ItemTooltips.value(AncientEffigies.getRequiredLevel(item.id))}"
    }
}
