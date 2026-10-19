package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapButlerCrichton() {
    onNpcClick(Npc.npc_15607, Npc.carni_butler_follower) { (player, npc) ->
        player.startConversation {
            npc(npc, HeadE.HAPPY_TALKING, "'Ello, mate. Welcome back to Sir Ceril's mansion.")
            options {
                op("What happened to the old butler?") {
                    if (player.isCultSide()) {
                        npc(npc, HeadE.CALM_TALK, "Butler Jones? He quit his job and left. Said his 'work here was complete', whatever that means. Weird chap.")
                    } else {
                        npc(npc, HeadE.CALM_TALK, "Butler Jones? He was exposed as one of those Hazeel cultists and sent to prison. Sir Ceril hired me afterwards.")
                    }
                }
                op("What do you know about the Carnilleans?") {
                    npc(npc, HeadE.CALM_TALK, "The Carnilleans are one of Ardougne's richest families. They're snooty, but there's no harm in them.")
                    npc(npc, HeadE.CALM_TALK, "They're bound to have a book on their history somewhere in the house if you want to know more.")
                }
                op("You don't talk like a butler.") {
                    npc(npc, HeadE.HAPPY_TALKING, "Oh, I save the formal stuff for Sir Ceril and the family.")
                }
            }
        }
    }
}
