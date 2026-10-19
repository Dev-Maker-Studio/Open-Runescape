package com.rs.game.content.quests.bigchompybirdhunting.dialogues

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.HAPPY_TALKING
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_COMPLETE
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_COOKING_CHOMPY
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_GETTING_BLOATED_TOAD
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_MAKING_OGRE_ARROWS
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_SHOWING_BLOATED_TOAD
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_UNSTARTED
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private const val FYCIE_FEATHER_PRICE = 50
private const val FYCIE_FEATHER_AMOUNT = 25

private const val BOUGHT_FYCIE_FEATHERS = "big_chompy_bought_fycie_feathers"

const val ASKED_FYCIE_CHOMPY_INGREDIENT = "big_chompy_asked_fycie_ingredient"
const val ASKED_BUGS_CHOMPY_INGREDIENT = "big_chompy_asked_bugs_ingredient"

@ServerStartupEvent
fun mapBigChompyOgreFamily() {
    onNpcClick(Npc.fycie, options = arrayOf("Talk")) { (player, npc) ->
        fycieDialogue(player, npc)
    }

    onNpcClick(Npc.bugs, options = arrayOf("Talk")) { (player, npc) ->
        bugsDialogue(player, npc)
    }
}

private fun fycieDialogue(player: Player, npc: NPC) {
    player.startConversation {
        when (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING)) {
            STAGE_UNSTARTED -> npc(npc, CALM_TALK, "You's better talk to Dad, We not talk to wierdly 'umans.")
            STAGE_MAKING_OGRE_ARROWS -> {
                if (player.questManager.getAttribs(Quest.BIG_CHOMPY_BIRD_HUNTING).getB(BOUGHT_FYCIE_FEATHERS)) {
                    npc(npc, CALM_TALK, "I's sorry creature, I don't got no more flufsies! But I got lots of bright pretties! Hope you's get da stabbers for Dad soonly!")
                } else {
                    npc(npc, CALM_TALK, "Hey you Creature, I know's what you is You's a 'uman!")
                    player(CALM_TALK, "That's right... and I'm making some 'stabbers' for Rantz.")
                    npc(npc, HAPPY_TALKING, "Dat's great...Dad want's to hunt da chompy...Da chompy is our bestest yumms! You needsing flufsies for stabbers. Fycie have some but I is wanting some bright pretties for em!")
                    simple("Fycie shows you the flufsies...you count 25 of them.")
                    player(CALM_TALK, "How many 'bright pretties' do you want?")
                    npc(npc, CALM_TALK, "Mee's want lots of bright pretties, this many!")
                    simple("Fycie quickly opens and closes her hands in front", "of you to indicate a number of bright pretties.", "It looks like she wants 50 gold coins.")
                    options {
                        op("Ok, I'll give you 50 bright pretties.") {
                            player(CALM_TALK, "Ok, I'll give you 50 bright pretties.")
                            if (player.inventory.containsItem(Obj.coins, FYCIE_FEATHER_PRICE)) {
                                simple("You offer the 50 coins for the 25 flufsies.")
                                npc(npc, HAPPY_TALKING, "Ok, dat's good 'un. I got da bright pretties and you got da flufsies!") {
                                    val attribs = player.questManager.getAttribs(Quest.BIG_CHOMPY_BIRD_HUNTING)
                                    if (!attribs.getB(BOUGHT_FYCIE_FEATHERS) &&
                                        player.inventory.containsItem(Obj.coins, FYCIE_FEATHER_PRICE)) {
                                        player.inventory.deleteItem(Obj.coins, FYCIE_FEATHER_PRICE)
                                        player.inventory.addItemDrop(Obj.feather, FYCIE_FEATHER_AMOUNT)
                                        attribs.setB(BOUGHT_FYCIE_FEATHERS, true)
                                    }
                                }
                            } else {
                                simple("You don't have enough coins.")
                            }
                        }
                        op("Er, sorry, I can't give you that many...") {
                            player(CALM_TALK, "Er, sorry, I can't give you that many...")
                            npc(npc, CALM_TALK, "Well, you not have da flufsies den!")
                        }
                    }
                }
            }
            STAGE_GETTING_BLOATED_TOAD,
            STAGE_SHOWING_BLOATED_TOAD -> {
                player(CALM_TALK, "Rantz said that you play with the 'fatsy toadies', what are they?")
                npc(npc, HAPPY_TALKING, "Oh we sometimes is using blower on da toadies but Dad don't let us get in da locksy blocksy no more. He, he, big chucklees when make da toadies fat on da swampy gas.")
            }
            STAGE_COOKING_CHOMPY -> {
                npc(npc, HAPPY_TALKING, "Dad say's you's roasting da chompy for us! Slurp! Me's want tomato wiv mine! Yummy, can't wait to eats it.") {
                    player.questManager.getAttribs(Quest.BIG_CHOMPY_BIRD_HUNTING).setB(ASKED_FYCIE_CHOMPY_INGREDIENT, true)
                }
            }
            STAGE_COMPLETE -> npc(npc, CALM_TALK, "I's sorry creature, I don't got no more flufsies! But I got lots of bright pretties!")
            else -> npc(npc, CALM_TALK, "You's better talk to Dad, him chasey sneaky da chompy.")
        }
    }
}

private fun bugsDialogue(player: Player, npc: NPC) {
    player.startConversation {
        when (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING)) {
            STAGE_UNSTARTED,
            STAGE_MAKING_OGRE_ARROWS -> npc(npc, CALM_TALK, "You's better talk to Dad, him chasey sneaky da chompy.")
            STAGE_GETTING_BLOATED_TOAD,
            STAGE_SHOWING_BLOATED_TOAD -> {
                player(CALM_TALK, "Rantz said that you play with the 'fatsy toadies', what are they?")
                npc(npc, HAPPY_TALKING, "Oh we sometimes use da blower on da toadies but Dad don't let us get in da locked box no more. He, he, it was good fun making da toadies fat on da swamp gas.")
            }
            STAGE_COOKING_CHOMPY -> {
                npc(npc, HAPPY_TALKING, "Dad says you's making da chompy for us! Slurp! Me's has to have cabbage wiv mine! Chompy is our favourite yummms!") {
                    player.questManager.getAttribs(Quest.BIG_CHOMPY_BIRD_HUNTING).setB(ASKED_BUGS_CHOMPY_INGREDIENT, true)
                }
            }
            STAGE_COMPLETE -> npc(npc, CALM_TALK, "You's better talk to Dad, him chasey sneaky da chompy.")
            else -> npc(npc, CALM_TALK, "You's better talk to Dad, him chasey sneaky da chompy.")
        }
    }
}
