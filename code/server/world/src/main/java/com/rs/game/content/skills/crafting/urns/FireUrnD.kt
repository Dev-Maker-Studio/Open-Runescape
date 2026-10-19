package com.rs.game.content.skills.crafting.urns

import com.rs.engine.dialogue.Conversation
import com.rs.engine.dialogue.impl.MakeXActionD
import com.rs.engine.dialogue.impl.MakeXItem
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item

class FireUrnD(player: Player) : Conversation(player) {

    init {
        val makeX = MakeXActionD()
        for (urn in Urn.entries) {
            if (player.inventory.containsItem(urn.unfired)) {
                makeX.addOption(MakeXItem(player, Item(urn.unfired), Item(urn.noRune), urn.fireXp, Seq.human_furnace, urn.level, Constants.CRAFTING, 2))
            }
        }
        if (!makeX.isEmpty) {
            addNext(makeX)
        }
        create()
    }
}
