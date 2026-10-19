package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.ownedNpc

private const val TEMP_OFFICE_FOREMAN = "grandTreeOfficeForeman"

private val FOREMAN_OFFICE_PLAYER = Tile.of(2956, 3025, 0)
private val FOREMAN_OFFICE_NPC = Tile.of(2955, 3025, 0)

@ServerStartupEvent
fun mapForemanGrandTree() {
    onNpcClick(Npc.grandtree_foreman, options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc is OwnedNPC && npc.owner !== player)
            return@onNpcClick
        foremanDialogue(player, npc)
    }
    onNpcDeath(Npc.grandtree_foreman) { event ->
        val player = when (val killer = event.killer) {
            is Player -> killer
            is Familiar -> killer.owner
            else -> return@onNpcDeath
        }
        if (event.npc is OwnedNPC && (event.npc as OwnedNPC).owner !== player)
            return@onNpcDeath
        if (player.grandTreeStage !in STAGE_REACHED_SHIPYARD..STAGE_LUMBER_ORDER)
            return@onNpcDeath
        if (player.grandTreeStage == STAGE_LUMBER_ORDER && player.inventory.containsOneItem(Obj.grandtree_order))
            return@onNpcDeath
        player.tempAttribs.removeO<OwnedNPC>(TEMP_OFFICE_FOREMAN)?.let {
            if (it !== event.npc) it.finish()
        }
        player.setGrandTreeStage(STAGE_LUMBER_ORDER)
        World.addGroundItem(Item(Obj.grandtree_order), event.npc.tile, player)
    }
}

private fun spawnOfficeForeman(player: Player): OwnedNPC {
    val existing = player.tempAttribs.getO<OwnedNPC>(TEMP_OFFICE_FOREMAN)
    if (existing != null && !existing.hasFinished())
        return existing
    return ownedNpc(player, Npc.grandtree_foreman, FOREMAN_OFFICE_NPC) {
        setRandomWalk(false)
    }.also {
        player.tempAttribs.setO<OwnedNPC>(TEMP_OFFICE_FOREMAN, it)
    }
}

private fun foremanDialogue(player: Player, npc: NPC) {
    if (player.grandTreeStage != STAGE_REACHED_SHIPYARD &&
        !(player.grandTreeStage == STAGE_LUMBER_ORDER && !player.inventory.containsOneItem(Obj.grandtree_order))) {
        player.startConversation { simple("The foreman is too busy to talk.") }
        return
    }
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello, are you in charge?")
        npc(npc, HeadE.CALM_TALK, "That's right, and you are...?")
        player(HeadE.CALM_TALK, "Glough sent me to check on how you are doing.")
        npc(npc, HeadE.SKEPTICAL, "Right. Glough sent a human?")
        player(HeadE.NERVOUS, "His gnomes are busy.")
        npc(npc, HeadE.SKEPTICAL, "Hmm...in that case we'd better go to my office.")
        npc(npc, HeadE.CALM_TALK, "Follow me.") {
            player.fadeScreen {
                player.tele(FOREMAN_OFFICE_PLAYER)
                foremanOfficeDialogue(player, spawnOfficeForeman(player))
            }
        }
    }
}

private fun foremanOfficeDialogue(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.SKEPTICAL, "Tell me again why you're here.")
        player(HeadE.NERVOUS, "Er...Glough sent me?")
        npc(npc, HeadE.CALM_TALK, "By the way how is Glough? Still with his wife?")
        exec { foremanQuestionOne(player, npc) }
    }
}

private fun foremanQuestionOne(player: Player, npc: NPC) {
    player.startConversation {
        options {
            op("Yes, they're getting on great.") {
                exec { foremanWrong(player, npc, "Yes, they're getting on great.", "Really? That's odd, considering she died last year.") }
            }
            op("Always arguing as usual!") {
                exec { foremanWrong(player, npc, "Always arguing as usual!") }
            }
            op("Sadly his wife is no longer with us!") {
                player(HeadE.SAD_MILD_LOOK_DOWN, "Sadly his wife is no longer with us!")
                npc(npc, HeadE.HAPPY_TALKING, "Right answer. I have to watch for imposters.")
                npc(npc, HeadE.CALM_TALK, "What's Glough's favourite dish?")
                exec { foremanQuestionTwo(player, npc) }
            }
        }
    }
}

private fun foremanQuestionTwo(player: Player, npc: NPC) {
    player.startConversation {
        options {
            op("He loves tangled toads legs.") {
                exec { foremanWrong(player, npc, "He loves tangled toads legs.", "Our survey said.... Bzzzzzz! Wrong answer!") }
            }
            op("He loves worm holes.") {
                player(HeadE.HAPPY_TALKING, "He loves worm holes.")
                npc(npc, HeadE.HAPPY_TALKING, "OK. Just one more. What's the name of his new girlfriend?")
                exec { foremanQuestionThree(player, npc) }
            }
            op("He loves choc bombs.") {
                exec { foremanWrong(player, npc, "He loves choc bombs.", "Our survey said.... Bzzzzzz! Wrong answer!") }
            }
        }
    }
}

private fun foremanQuestionThree(player: Player, npc: NPC) {
    player.startConversation {
        options {
            op("Anita.") {
                player(HeadE.HAPPY_TALKING, "Anita.")
                npc(npc, HeadE.HAPPY_TALKING, "Well, well, you do know Glough. Sorry for the interrogation but I'm sure you understand.")
                player(HeadE.CALM_TALK, "Of course, security is paramount.")
                npc(npc, HeadE.HAPPY_TALKING, "As you can see things are going well.")
                player(HeadE.CALM_TALK, "Indeed.")
                npc(npc, HeadE.HAPPY_TALKING, "When I was asked to build a fleet large enough to invade Port Sarim and carry 300 gnome troops I said: 'If anyone can, I can.'")
                player(HeadE.AMAZED_MILD, "That's a lot of troops!")
                npc(npc, HeadE.CALM_TALK, "True but if the gnomes are really going to take over Gielinor they'll need at least that.")
                player(HeadE.CONFUSED, "Take over?")
                npc(npc, HeadE.CALM_TALK, "Of course, why else would Glough want 30 battleships? Between you and me I don't think he stands a chance.")
                player(HeadE.CONFUSED, "No?")
                npc(npc, HeadE.CALM_TALK, "I mean, for the kind of battleships Glough's ordered I'll need tons and tons of lumber! Still, if he says he can supply the wood I'm sure he can! Anyway, here's the order for the lumber.")
                if (!player.inventory.hasFreeSlots()) {
                    npc(npc, HeadE.WORRIED, "It seems you can't carry anymore. Come back when you have more space.")
                } else {
                    simple("The foreman has given you the lumber order.")
                    player(HeadE.HAPPY_TALKING, "OK. I'll head off and give this order to Glough.") {
                        player.inventory.addItem(Obj.grandtree_order)
                        player.setGrandTreeStage(STAGE_LUMBER_ORDER)
                        if (npc is OwnedNPC && npc.owner === player) {
                            npc.finish()
                            player.tempAttribs.removeO<OwnedNPC>(TEMP_OFFICE_FOREMAN)
                        }
                    }
                }
            }
            op("Alia.") {
                exec { foremanWrong(player, npc, "Alia.", "You almost had me fooled!") }
            }
            op("Elena.") {
                exec { foremanWrong(player, npc, "Elena.", "You almost had me fooled!") }
            }
        }
    }
}

private fun foremanWrong(player: Player, npc: NPC, answer: String, response: String? = null) {
    player.startConversation {
        player(HeadE.CALM_TALK, answer)
        if (response != null) npc(npc, HeadE.SKEPTICAL, response)
        exec {
            npc.forceTalk("Imposter!")
            npc.combatTarget = player
        }
    }
}
