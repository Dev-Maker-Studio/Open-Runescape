package com.rs.game.content.skills.crafting

import com.rs.game.content.skills.crafting.GemTipCutting.GemTips
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapOysterPearlCutting() {
    onItemOnItem(intArrayOf(Obj.chisel), intArrayOf(Obj.bigoysterpearls)) { event ->
        GemTipCutting.cut(event.player, GemTips.PEARLS)
    }
}
