package com.rs.game.content.quests.regicide.npcs

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.regicide.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj

fun regicideChemistDialogue(player: Player, npc: NPC, dialogue: DialogueBuilder) {
    with(dialogue) {
        if (!player.inventory.containsItem(Obj.obj_3230)) {
            player(CALM_TALK, "Good day. I was hoping you could help me with a formula I found in a book.")
            npc(npc, CALM_TALK, "A formula, you say? Show me the book.")
            player(CALM_TALK, "Oh dear, I don't seem to have it anymore. I hope Iorwerth has another copy.")
            return
        }
        player(CALM_TALK, "Good day. I was hoping you could help me with a formula from this book.")
        npc(npc, CALM_TALK, "A formula, you say? Let me see that!")
        simple("The chemist takes the book and starts leafing through the pages.")
        npc(npc, CALM_TALK, "Yes... yessss... Well, I guess you'll be wanting to use the old still.")
        player(CALM_TALK, "Huh?")
        npc(npc, CALM_TALK, "Well, you're going to need Naphtha, quicklime and brimstone.") {
            if (player.getQuestStage(Quest.REGICIDE) == STAGE_PREPARE_EXPLOSIVE)
                player.advanceRegicide(STAGE_DESTROY_TENT)
        }
        label("regicideIngredients")
        options {
            op("What is naphtha and how do I make it?") {
                player(CALM_TALK, "What is naphtha and how do I make it?")
                npc(npc, CALM_TALK, "Well, to make naphtha you're going to need to distil tar of some kind. You can use my still outside, but it's not been used in a long time.")
                player(CALM_TALK, "How do I use it?")
                npc(npc, CALM_TALK, "It's quite simple really. You have to get the still heat up very high to make this fuel. But if it gets too hot it will burn out and you will have to start over. You must also watch the pressure, if it gets too high you will also have to start over. There are valves to control pressure and tar flow. These will also affect the heat of the still.")
                player(CALM_TALK, "Is that all?")
                npc(npc, CALM_TALK, "You'll also need plenty of coal to keep the still hot.")
                goto("regicideIngredients")
            }
            op("What's quicklime?") {
                player(CALM_TALK, "What's quicklime?")
                npc(npc, CALM_TALK, "Quicklime is usually made by the thermal decomposition of limestone. When it gets really hot it glows like the sun. It's also extremely caustic.")
                goto("regicideIngredients")
            }
            op("What's brimstone?") {
                player(CALM_TALK, "What's brimstone?")
                npc(npc, CALM_TALK, "I've never seen it in the quantities this formula talks of but it's a yellow powdery formation. Much more than that I don't know.")
                goto("regicideIngredients")
            }
            op("This all looks too tricky, I'll leave it.") {
                player(CALM_TALK, "This all looks too tricky, I'll leave it.")
            }
        }
    }
}
