package com.rs.game.content.skills.firemaking

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.NPCClickEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Varbit

private const val BOW_FIRE_LEVEL = 35

@ServerStartupEvent
fun handleOttoGodblessed() {
    onNpcClick(Npc.brut_otto, options = arrayOf("Talk-to")) { e: NPCClickEvent ->
        val p = e.player
        p.startConversation {
            when {
                p.vars.getVarBit(Varbit.brut_fire) < 1 -> teachBowLighting(p)
                p.vars.getVarBit(Varbit.brut_fire) < 2 -> remindBowLighting()
                p.vars.getVarBit(Varbit.brut_craft_ship) < 1 -> teachPyreShips(p)
                else -> spiritsLore()
            }
        }
    }
}

private fun DialogueBuilder.teachBowLighting(p: Player) {
    npc(Npc.brut_otto, HeadE.CALM_TALK, "Hello there. You look like you could learn a thing or two from a barbarian.")
    player(HeadE.CALM_TALK, "I was told you teach barbarian firemaking.")
    npc(Npc.brut_otto, HeadE.CHEERFUL, "Aye, that I do. We barbarians do not carry tinderboxes like you soft folk. We light our fires with a bow.")
    npc(Npc.brut_otto, HeadE.CALM_TALK, "Take an oak log and use it on any bow you own. The bowstring will set the wood alight.")
    npc(Npc.brut_otto, HeadE.CALM_TALK, "You will need a Firemaking level of $BOW_FIRE_LEVEL to burn an oak log this way. Off you go, and come back when you have done it.")
    exec { p.vars.saveVarBit(Varbit.brut_fire, 1) }
}

private fun DialogueBuilder.remindBowLighting() {
    npc(Npc.brut_otto, HeadE.CALM_TALK, "Have you lit a log with your bow yet? An oak log, used on any bow, and a Firemaking level of $BOW_FIRE_LEVEL.")
    player(HeadE.CALM_TALK, "Not yet.")
    npc(Npc.brut_otto, HeadE.CHEERFUL, "Then what are you waiting for? Off with you.")
}

private fun DialogueBuilder.teachPyreShips(p: Player) {
    npc(Npc.brut_otto, HeadE.CHEERFUL, "You have the makings of a barbarian yet. Now let me tell you of the pyre ships.")
    player(HeadE.CALM_TALK, "Pyre ships?")
    npc(Npc.brut_otto, HeadE.CALM_TALK, "When a barbarian falls, we lay them to rest upon a burning boat, that their spirit may be freed.")
    npc(Npc.brut_otto, HeadE.CALM_TALK, "Take a log to one of the pyre sites near my home and carve it into a boat with a hatchet.")
    npc(Npc.brut_otto, HeadE.CALM_TALK, "Then lay the bones of the fallen upon it. Chewed bones, torn from mithril dragons, hold a peaceful spirit. Mangled bones do not.")
    npc(Npc.brut_otto, HeadE.CALM_TALK, "Light the pyre with your bow or a tinderbox. A spirit at peace may reward you for your kindness.")
    npc(Npc.brut_otto, HeadE.CALM_TALK, "You will need Firemaking and Crafting levels of 11.")
    exec { p.vars.saveVarBit(Varbit.brut_craft_ship, 1) }
}

private fun DialogueBuilder.spiritsLore() {
    npc(Npc.brut_otto, HeadE.CHEERFUL, "Welcome back, friend of the spirits. Is there aught else you would know?")
    options {
        op("Tell me about the spirits.") {
            player(HeadE.CALM_TALK, "Tell me about these spirits you speak of.")
            npc(Npc.brut_otto, HeadE.CALM_TALK, "They are the barbarians of old. Most are at peace, and guide us. Some are restless until their bones are laid to rest. A few are lost to madness, and will attack any who draw near.")
        }
        op("Nothing, thank you.") {
            player(HeadE.CALM_TALK, "Nothing, thank you.")
            npc(Npc.brut_otto, HeadE.CALM_TALK, "Then may the spirits watch over you.")
        }
    }
}
