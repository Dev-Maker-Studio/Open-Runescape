package com.rs.game.content.quests.underground_pass.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

private val UNDERGROUND_PASS_SLAVES = intArrayOf(Npc.cave_slave1, Npc.cave_slave2, Npc.cave_slave3, Npc.cave_slave4, Npc.cave_slave5, Npc.cave_slave6, Npc.cave_slave7)

@ServerStartupEvent
fun mapUndergroundPassSlaves() {
    onNpcClick(*UNDERGROUND_PASS_SLAVES.toTypedArray(), options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            when (npc.id) {
                Npc.cave_slave1 -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, TERRIFIED, "Eating me... they keep eating me! Eating me from the inside! Please stop them! I can feel them wriggling around inside me right now... please stop them!")
                }

                Npc.cave_slave2 -> {
                    player(CALM_TALK, "Hi.")
                    npc(npc, NERVOUS, "Blood, blood... never enough blood to go round. When I lift up my arm like this, it all pours back into my body.")
                    npc(npc, NERVOUS, "I hope it remembers to go back inside my arm. And don't even ask me about my legs... how much blood are they going to need?")
                    npc(npc, NERVOUS, "Blood is important. We must offer it to Zamorak every day as proof of our devotion. I just hope I don't run out!")
                }

                Npc.cave_slave3 -> {
                    player(WORRIED, "Hello, are you okay?")
                    npc(npc, HAPPY_TALKING, "Oh yes, you're a fine one. Nice red cheeks, shiny hair.")
                    npc(npc, SKEPTICAL_THINKING, "Let's see now, some potatoes, some cabbage... Maybe half a clove of garlic...")
                    npc(npc, CHEERFUL, "Yes I think you'd make a fine soup. You don't mind do you?")
                    player(NERVOUS, "Actually I do.")
                    npc(npc, HAPPY_TALKING, "You're welcome to have some with me of course...")
                }

                Npc.cave_slave4 -> {
                    player(CALM_TALK, "Hello there.")
                    npc(npc, TALKING_ALOT, "Mwaarrr fnnntchh. Gbpp dng sktd delp?")
                    player(CONFUSED, "Pardon?")
                    npc(npc, TALKING_ALOT, "Kjp lar falut: Gbpp dng sktd delp?")
                    player(CONFUSED, "Sorry, I don't under..")
                    npc(npc, VERY_FRUSTRATED, "Mwaarrr fnnntchh. Gbpp dng sktd delp? GBPP DNG SKTD DELP!")
                }

                Npc.cave_slave5 -> {
                    player(NERVOUS, "Hello...")
                    npc(npc, ANGRY, "Kill the villagers, burn them all... every last one! I want nothing to survive... nothing but the sweet smell of burning flesh...")
                    player(SCARED, "You're ill!")
                    npc(npc, EVIL_LAUGH, "What's that? You've never smelt it before? Well, let's just say that it's an acquired taste...")
                }

                Npc.cave_slave6 -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, SCARED, "Danger, everywhere danger! But not from man nor beast! This is the danger that is inside you.")
                    npc(npc, SECRETIVE, "Bring it out, nurture it, cherish it. Stroke it like you would stroke a wounded bird... then strangle it before it takes hold of your very being. Don't say I didn't warn you.")
                }

                Npc.cave_slave7 -> {
                    player(CALM_TALK, "Hello.")
                    npc(npc, FOGGY_STORYTELLING, "What's that... Is that a dagger I see before me? Why should I bear the slings and arrows of outrageous fortune.")
                    npc(npc, FOGGY_STORYTELLING, "For what is a man but this quintessence of dust? And so I say goodnight sweet prince. The rest is silence.")
                }
            }
        }
    }
}
