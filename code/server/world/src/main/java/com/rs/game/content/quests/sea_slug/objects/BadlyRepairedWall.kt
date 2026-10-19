package com.rs.game.content.quests.sea_slug.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.content.quests.sea_slug.npcs.SPOKE_TO_KENNITH_ABOUT_ESCAPE
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapSeaSlugWall() {
    onObjectClick(Loc.loc_18251, Loc.seaslug_wall_closed, Loc.seaslug_wall_open, tiles = arrayOf(WALL_TILE)) { (player, obj) ->
        player.kickSeaSlugWall(obj)
    }
}

private fun Player.kickSeaSlugWall(obj: GameObject) {
    val stage = getQuestStage(Quest.SEA_SLUG)
    if (stage >= STAGE_OPENED_WALL) {
        sendMessage("The wall already has an opening big enough for Kennith to climb through.")
        return
    }
    val kennithAskedToEscape = questManager.getAttribs(Quest.SEA_SLUG).getB(SPOKE_TO_KENNITH_ABOUT_ESCAPE)
    if (stage < STAGE_NEEDS_LIT_TORCH || !kennithAskedToEscape) {
        sendMessage("You kick the loose panel...")
        sendMessage("... but nothing interesting happens.")
        return
    }
    if (!inventory.containsItem(Obj.torch_lit)) {
        sendMessage("You need your lit torch to keep the fishermen away.")
        return
    }
    lock(4)
    schedule("sea_slug_kick_panel") {
        faceObject(obj)
        anim(Seq.seaslug_human_kick)
        soundEffect(Sound.sound_3024, false)
        sendMessage("You kick the loose panel.")
        wait(2)
        sendMessage("The wood is rotten and crumbles away...")
        sendMessage("... leaving an opening big enough for Kennith to climb through.")
        setQuestStage(Quest.SEA_SLUG, STAGE_OPENED_WALL)
        refreshSeaSlugState()
        unlock()
    }
}
