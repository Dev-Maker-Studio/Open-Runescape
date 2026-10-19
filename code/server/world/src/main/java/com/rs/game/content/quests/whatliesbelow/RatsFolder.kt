package com.rs.game.content.quests.whatliesbelow

import com.rs.game.model.item.ItemTooltips
import com.rs.game.model.item.TooltipSection
import com.rs.plugin.annotations.ServerStartupEvent

private const val FOLDER_PAGES = 5

@ServerStartupEvent
fun mapRatsFolderTooltip() {
    ItemTooltips.metadata("wlbPapersAdded", TooltipSection.CONTENTS) { _, item, _ -> ItemTooltips.count("Pages", item.getMetaDataI("wlbPapersAdded", 0), FOLDER_PAGES) }
}
