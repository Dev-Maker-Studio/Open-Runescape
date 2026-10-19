package com.rs.game.content.quests.sea_slug.npcs

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import kotlin.random.Random

private val ALL_FISHERMEN = FISHERMEN.map { it as Any }.toTypedArray()

@ServerStartupEvent
fun mapFishermenSeaSlug() {
    onNpcClick(*ALL_FISHERMEN) { (player, npc) ->
        if (!player.validateSeaSlugNpcOwnership(npc)) return@onNpcClick
        player.talkToPossessedFisherman(npc)
    }
    onItemOnNpc(*ALL_FISHERMEN) { (player, item, npc) ->
        if (!player.validateSeaSlugNpcOwnership(npc)) return@onItemOnNpc
        if (item.id == Obj.torch_lit) {
            npc.anim(Seq.seaslug_torch_possessed)
            player.sendMessage("The fisherman covers his face.")
            player.sendMessage("He seems afraid of your torch.")
        }
    }
}

private fun Player.talkToPossessedFisherman(npc: NPC) {
    startConversation {
        player(CALM, "Hello there.")
        simple("His eyes are staring vacantly into space.")
        when (Random.nextInt(6)) {
            0 -> {
                npc(npc, SCARED, "You are not part of our family...")
                player(CONFUSED, "Umm. Not last time I checked.")
                npc(npc, SCARED, "Soon you will be... Soon you will...")
                player(CONFUSED, "Really?")
                npc(npc, SCARED, "She is coming...")
            }
            1 -> {
                npc(npc, SCARED, "Must escape the blue.. Deep deep blue")
                player(CONFUSED, "Pardon?")
                npc(npc, SCARED, "Family... Under the blue... Must escape the blue...")
            }
            2 -> {
                npc(npc, SAD, "Lost to us.. She is Lost to us...")
                player(CONFUSED, "Who is lost?")
                npc(npc, SAD, "Trapped by the light... Lost and trapped...")
                player(CONFUSED, "Ermm... So you don't want to tell me then?")
                npc(npc, SAD, "Trapped... In stone and darkness...")
            }
            3 -> {
                npc(npc, SCARED, "Must find family...")
                player(CONFUSED, "What?")
                npc(npc, SCARED, "Soon we will all be together...")
                player(WORRIED, "Are you ok?")
                npc(npc, SCARED, "Must find family... They are all under the blue... Deep deep under the blue...")
                player(CONFUSED, "Ermm... I'll leave you to it then.")
            }
            4 -> {
                npc(npc, SCARED, "Below the deep, deep blue she waits...")
                player(CONFUSED, "Who waits?")
                npc(npc, SCARED, "They came to her with fire and faith...")
                player(CONFUSED, "Who? Who came to who?")
                npc(npc, SCARED, "Too many... Too many...")
                player(FRUSTRATED, "Too many what? Make sense!")
                npc(npc, SCARED, "Locked away for all eternity...")
                player(FRUSTRATED, "You'd better start making sense Sonny Jim or I'll...")
                npc(npc, SCARED, "Free... Soon to be free...")
            }
            else -> {
                npc(npc, SCARED, "Free of the deep blue we are...")
                npc(npc, SCARED, "We must find...")
                player(CONFUSED, "Yes?")
                npc(npc, SCARED, "a new home...")
                npc(npc, SCARED, "We must leave this place...")
                player(CONFUSED, "Where will you go?")
                npc(npc, SCARED, "Away.. Away to her...")
                player(SKEPTICAL, "Riiight.")
            }
        }
    }
}
