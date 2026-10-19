package com.rs.game.content.quests.observatoryquest.objects

import com.rs.engine.quest.Quest
import com.rs.game.content.quests.observatoryquest.STAGE_MAKING_LENS
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapObservatoryLens() {
    onItemOnItem(intArrayOf(Obj.molten_glass), intArrayOf(Obj.lens_mould)) { e ->
        val player = e.player
        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) != STAGE_MAKING_LENS) return@onItemOnItem
        if (player.skills.getLevel(Skills.CRAFTING) < 10) {
            player.sendMessage("You need a Crafting level of 10 to make the lens.")
            return@onItemOnItem
        }
        if (!player.inventory.containsItem(Obj.molten_glass, 1) || !player.inventory.containsItem(Obj.lens_mould, 1)) return@onItemOnItem
        player.inventory.deleteItem(Obj.molten_glass, 1)
        player.inventory.addItemDrop(Obj.lens, 1)
        player.sendMessage("You make the molten glass into a lens.")
    }
}
