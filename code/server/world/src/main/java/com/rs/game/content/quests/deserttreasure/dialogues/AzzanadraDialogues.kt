package com.rs.game.content.quests.deserttreasure

import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.combat.CombatDefinitions
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

private val ALTAR_TILE = Tile.of(3232, 9311, 0)
private val PYRAMID_EXIT = Tile.of(3233, 2887, 0)

@ServerStartupEvent
fun mapAzzanadraDesertTreasure() {
    onNpcClick(Npc.npc_1971, options = arrayOf("Talk-to")) { (player) ->
        azzanadraDialogue(player)
    }

    onObjectClick(Loc.dt_zaros_altar, tiles = arrayOf(ALTAR_TILE)) { (player) ->
        useAncientAltar(player)
    }

    onObjectClick(Loc.loc_6550, Loc.dt_portal) { (player) ->
        if (player.getQuestStage(Quest.DESERT_TREASURE) == STAGE_COMPLETE)
            player.tele(PYRAMID_EXIT)
    }
}

private fun azzanadraDialogue(player: Player) {
    val stage = player.getQuestStage(Quest.DESERT_TREASURE)

    if (stage < STAGE_RETURN_TO_JALDRAOCHT)
        return

    if (stage == STAGE_COMPLETE) {
        player.startConversation {
            player(CALM_TALK, "I should go and let Eblis know what's happened here...")
        }
        return
    }

    if (stage == STAGE_RETURN_TO_JALDRAOCHT)
        player.setQuestStage(Quest.DESERT_TREASURE, STAGE_TOMB_RAIDER)

    player.startConversation {
        npc(Npc.npc_1971, HAPPY_TALKING, "I knew they could not trap me here for long!")
        npc(Npc.npc_1971, CALM_TALK, "Well done, soldier, tell me, how goes the battle?")
        player(CONFUSED, "Battle?")
        npc(Npc.npc_1971, CONFUSED, "You do not know of the battle?")
        npc(Npc.npc_1971, CONFUSED, "More time must have passed than I had thought...")
        npc(Npc.npc_1971, CALM_TALK, "Tell me, what news of great Paddewwa? Do the shining spires of Lassar still stand? And what of glorious Annakarl? The fortress is still intact?")
        player(CONFUSED, "Uh... Sorry, I've never heard of them...")
        npc(Npc.npc_1971, AMAZED, "No!")
        npc(Npc.npc_1971, SAD, "My lord... What has become of you? I cannot hear your voice in my mind anymore!")
        npc(Npc.npc_1971, CALM_TALK, "My thanks to you brave warrior for your help in freeing me from this accursed tomb, but it seems I have much to do to make amends.")
        npc(Npc.npc_1971, SAD, "If the shining cities no longer stand, then it means that we must have failed my lord...")
        npc(Npc.npc_1971, SAD, "How long have I been trapped here? Master... Have you truly been dispatched from this world?")
        npc(Npc.npc_1971, CALM_TALK, "Warrior, for your efforts in freeing me, I offer you the gift of knowledge.")
        npc(Npc.npc_1971, CALM_TALK, "I bestow upon you the ancient magicks, taught me by my Lord before his disappearance, may you use them well in battle for our people!")
        npc(Npc.npc_1971, CALM_TALK, "They will replace the knowledge you previously had, but you may switch between them by praying at the altar in this room at any time.")
        npc(Npc.npc_1971, CALM_TALK, "I trust that we shall meet again adventurer, I offer you the blessings of myself and my master in all of your endeavours!")
        npc(Npc.npc_1971, CALM_TALK, "Now, I must leave you, for there must be some trace of my master's power left somewhere. Feel free to use the portal I shall create to return here easily in the future!") {
            player.setQuestStage(Quest.DESERT_TREASURE, STAGE_AZZANADRA)
        }
        player(CALM_TALK, "I should go and let Eblis know what's happened here...") {
            player.questManager.completeQuest(Quest.DESERT_TREASURE)
            player.sendMessage("Congratulations! You have completed: 'Bandit Camp' - Activate the lodestone in the Bandit Camp.")
            player.sendMessage("Congratulations! You have completed: 'Desert Treasure' - Complete this quest.")
            player.sendMessage("A strange wisdom has filled your mind...")
        }
    }
}

private fun useAncientAltar(player: Player) {
    if (player.getQuestStage(Quest.DESERT_TREASURE) != STAGE_COMPLETE) {
        player.sendMessage("The altar's power is beyond your understanding.")
        return
    }

    player.startConversation {
        options("Change spellbooks?") {
            op("Yes, replace my spellbook.") {
                exec {
                    player.prayer.drainPrayer()
                    if (player.combatDefinitions.getSpellbook() != CombatDefinitions.Spellbook.ANCIENT) {
                        player.sendMessage("Your mind clears and you switch back to the ancient spellbook.")
                        player.combatDefinitions.setSpellbook(CombatDefinitions.Spellbook.ANCIENT)
                    } else {
                        player.sendMessage("Your mind clears and you switch back to the normal spellbook.")
                        player.combatDefinitions.setSpellbook(CombatDefinitions.Spellbook.MODERN)
                    }
                }
            }
            op("Nevermind.") { }
        }
    }
}
