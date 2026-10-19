package com.rs.game.content.quests.sea_slug.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import com.rs.engine.dialogue.HeadE.SCARED
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.sea_slug.validateSeaSlugNpcOwnership
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onItemOnNpc

@ServerStartupEvent
fun mapSeaSlugNpcs() {
    onNpcClick(Npc.seaslug_child, options = arrayOf("Take")) { (player, npc) ->
        player.tryToTakeSeaSlug(npc)
    }
    onItemOnNpc(Npc.seaslug_child) { (player, item, npc) ->
        if (item.id != Obj.torch_lit || !player.validateSeaSlugNpcOwnership(npc)) return@onItemOnNpc
        npc.anim(Seq.seaslug_torch_slug)
        player.sendMessage("The sea slug shrinks away from your torch.")
    }
}

private fun Player.tryToTakeSeaSlug(npc: NPC) {
    if (!validateSeaSlugNpcOwnership(npc)) return
    applyHit(Hit(this, 30, Hit.HitLook.TRUE_DAMAGE))
    startConversation {
        simple("You try to pick up the sea slug.<br><br>It sinks its teeth deep into your hand.")
        player(SCARED, "Ouch!")
    }
}
