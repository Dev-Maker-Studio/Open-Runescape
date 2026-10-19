package com.rs.game.content.world.areas.rellekka

import com.rs.game.World
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.Ticks
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq

@ServerStartupEvent
fun mapSeerHouse() {
    onObjectClick(Loc.viking_seer_up_ladder, Loc.viking_seer_down_ladder) { e ->
        if (e.option == "Climb-up")
            e.player.useStairs(Seq.human_reachforladder, e.player.transform(0, 0, 2))
    }
    onObjectClick(Loc.viking_seer_trapdoor_closed) { e ->
        if (e.option != "Open")
            return@onObjectClick
        val opened = GameObject(Loc.viking_seer_trapdoor_open, e.obj.type, e.obj.rotation, e.obj.x, e.obj.y, e.obj.level)
        e.player.anim(Seq.human_openchest)
        e.player.lock(2)
        e.player.faceObject(opened)
        World.spawnObjectTemporary(opened, Ticks.fromMinutes(1))
    }
    onObjectClick(Loc.viking_seer_trapdoor_open) { e ->
        if (e.option == "Climb-down")
            e.player.useStairs(Seq.human_reachforladder, e.player.transform(0, 0, -2))
    }
}
