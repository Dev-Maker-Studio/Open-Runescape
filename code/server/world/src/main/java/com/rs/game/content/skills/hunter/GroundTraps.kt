package com.rs.game.content.skills.hunter

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onGroundItemClick
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapGroundTraps() {
    onGroundItemClick(Obj.hunting_ojibway_bird_snare, Obj.hunting_box_trap, Obj.ecosystem_plant_trap, Obj.magic_imp_box, options = arrayOf("Lay", "Activate")) { (player, item) ->
        player.actionManager.setAction(BoxAction(BoxTrapType.forId(item.id), item))
    }
}
