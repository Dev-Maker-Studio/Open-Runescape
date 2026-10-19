package com.rs.game.content.quests.regicide.objects

import com.rs.game.content.quests.regicide.cutscenes.fireRegicideCatapult
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapRegicideCatapult() {
    val parts = arrayOf<Any>(Loc.loc_3976, Loc.loc_3977, Loc.loc_3978, Loc.regicide_catapult_normal, Loc.regicide_catapult_rotten, Loc.regicide_catapult_mahogany, Loc.regicide_catapult_left_normal, Loc.regicide_catapult_left_rotten, Loc.regicide_catapult_left_mahogany, Loc.regicide_catapult_right_normal, Loc.regicide_catapult_right_rotten, Loc.regicide_catapult_right_mahogany)
    onItemOnObject(parts, arrayOf(Obj.regicide_barrel_lid, Obj.regicide_barrel_lid_fused)) { e -> e.player.fireRegicideCatapult() }
    onObjectClick(*parts) { (player) -> player.fireRegicideCatapult() }
}
