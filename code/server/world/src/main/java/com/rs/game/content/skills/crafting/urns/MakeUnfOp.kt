package com.rs.game.content.skills.crafting.urns

import com.rs.engine.dialogue.impl.MakeXActionD
import com.rs.engine.dialogue.impl.MakeXItem
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item

class MakeUnfOp(player: Player, urns: List<Urn>) : MakeXActionD() {

    init {
        for (urn in urns) {
            addOption(MakeXItem(player, Item(Obj.softclay, 2), Item(urn.unfired), urn.wheelXp, Seq.human_furnace, urn.level, Constants.CRAFTING, 4))
        }
    }
}
