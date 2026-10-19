package com.rs.game.content.skills.crafting

import com.rs.game.content.skills.util.Category
import com.rs.game.content.skills.util.CreationActionD
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

@ServerStartupEvent
fun mapSnakeskinCrafting() {
    onItemOnItem(intArrayOf(Obj.village_snake_hide), intArrayOf(Obj.needle)) { e ->
        e.player.startConversation(CreationActionD(e.player, Category.SNAKESKIN, Obj.village_snake_hide, Seq.human_leather_crafting, 2))
    }
}
