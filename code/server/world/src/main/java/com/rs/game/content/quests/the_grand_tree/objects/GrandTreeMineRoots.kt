package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.engine.quest.Quest
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapGrandTreeMineRoots() {
    onObjectClick(Loc.gnome_area_imp_mine_grandtree_rootdoor) { (player, obj) ->
        if (!player.questManager.isComplete(Quest.GRAND_TREE)) {
            player.sendMessage("You push against the roots but they push back at you!")
            return@onObjectClick
        }
        player.tele(player.tile.transform(0, if (player.y <= obj.y) 2 else -2))
    }
}
