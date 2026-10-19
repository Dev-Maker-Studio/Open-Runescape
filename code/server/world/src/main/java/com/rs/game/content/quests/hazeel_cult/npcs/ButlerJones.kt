package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapButlerJones() {
    onNpcClick(Npc.npc_890, Npc.hazeelcult_butlerjones) { (player, npc) -> butlerJonesDialogue(player, npc) }
}

private fun butlerJonesDialogue(player: Player, npc: NPC) {
    val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
    player.startConversation {
        when {
            stage == STAGE_COMPLETE && player.isCultSide() -> {
                player(HeadE.CALM_TALK, "Hello Jones.")
                npc(npc, HeadE.HAPPY_TALKING, "It's an honour to be in your presence again, adventurer. I hope things are well.")
            }
            player.isCultSide() && stage >= STAGE_FIND_SCROLL -> {
                npc(npc, HeadE.HAPPY_TALKING, "Hello again, friend. Our cause is one and the same; the sooner Lord Hazeel is avenged the better!")
                player(HeadE.CONFUSED, "Do you know where the scroll with the enchantment is?")
                npc(npc, HeadE.CALM_TALK, "No idea. I KNOW it's somewhere in this house, but I've searched high and low!")
                if (player.inventory.containsOneItem(Obj.hazeel_scroll))
                    npc(npc, HeadE.HAPPY_TALKING, "You have it! Quick, get it back to our hideout!")
            }
            else -> {
                player(HeadE.CALM_TALK, "Hello. What is this building?")
                npc(npc, HeadE.CALM_TALK, "This is the home of Sir Ceril Carnillean. You're welcome to look around, but I'll have to keep an eye on you.")
                npc(npc, HeadE.CALM_TALK, "We've been having a problem with thieves and strange cultists coming out of the forest.")
            }
        }
    }
}

internal fun exposeButlerJones(player: Player) {
    player.startConversation {
        simple("You search the cupboard thoroughly.<br>You find a bottle of poison and a mysterious amulet.")
        player(HeadE.AMAZED, "Ceril!")
        npc(Npc.sir_ceril_carnillean, HeadE.ANGRY, "What do you want now, you scoundrel?")
        player(HeadE.CALM_TALK, "Look what I've found in the cupboard!")
        npc(Npc.sir_ceril_carnillean, HeadE.AMAZED, "I don't believe it! Poison! JONES!")
        npc(Npc.hazeelcult_butlerjones, HeadE.WORRIED, "P-p-poison? Oh, it's for killing rats. I am but a loyal butler, sir.")
        npc(Npc.sir_ceril_carnillean, HeadE.CALM_TALK, "Rats, eh? Fair enough. Sorry to have doubted you, old boy.")
        player(HeadE.CALM_TALK, "Then how about this amulet?")
        npc(Npc.sir_ceril_carnillean, HeadE.AMAZED, "I've seen this before! The thieves that broke in wore identical amulets! Jones! We trusted you!")
        npc(Npc.hazeelcult_butlerjones, HeadE.ANGRY, "You senile old fool. It was all too easy! I should have killed you and your pathetic family weeks ago!")
        npc(Npc.sir_ceril_carnillean, HeadE.ANGRY, "Guard! Take him away. Attempted murder and burglary.")
        npc(Npc.hazeelcult_butlerjones, HeadE.ANGRY, "You and your family will pay dearly for this. Glory to Hazeel! All praise Zamorak!")
        npc(Npc.hazeelcult_guard, HeadE.CALM_TALK, "Come along quietly. There'll be no revenge where you're going; we've got a nice secure jail cell waiting.")
        npc(Npc.sir_ceril_carnillean, HeadE.HAPPY_TALKING, "It looks like I am indebted to you! I apologise for my harshness before.")
        player(HeadE.HAPPY_TALKING, "Thank you, Sir Ceril.")
        npc(Npc.sir_ceril_carnillean, HeadE.HAPPY_TALKING, "No, thank YOU! Feel free to stop by anytime, adventurer!")
        simple("Sir Ceril gives you 2,000 coins.") {
            player.vars.setVarBit(Varbit.carni_butlerjonesvisible, 2)
            player.inventory.addItemDrop(Obj.coins, 2000)
            player.questManager.completeQuest(Quest.HAZEEL_CULT)
        }
    }
}
