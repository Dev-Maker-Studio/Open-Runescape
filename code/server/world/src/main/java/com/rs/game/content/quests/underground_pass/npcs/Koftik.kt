package com.rs.game.content.quests.underground_pass.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import org.darkan.core.map.pathfinder.Direction
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.regicide.npcs.regicideKoftikDialogue
import com.rs.game.content.quests.underground_pass.*
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.ownedNpc

private const val KOFTIK_UNINTERESTED = "Koftik doesn't seem interested in talking."
private const val SAW_KOFTIK_DWARF_WARNING = "sawKoftikDwarfWarning"
private const val RECEIVED_KOFTIK_GRID_FOOD = "receivedKoftikGridFood"
private val KOFTIK_DWARF_WARNING_TILE = Tile.of(2335, 9794, 0)

@ServerStartupEvent
fun mapKoftik() {
    instantiateNpc(Npc.caveguide6) { id, tile -> FinalKoftikNpc(id, tile) }

    onNpcClick(Npc.npc_972, Npc.caveguide2, Npc.caveguide3, Npc.caveguide5, Npc.caveguide6) { (player, npc) ->
        if (player.isQuestComplete(Quest.UNDERGROUND_PASS) && npc.id in intArrayOf(Npc.npc_972, Npc.caveguide2)) {
            regicideKoftikDialogue(player, npc)
            return@onNpcClick
        }
        player.startConversation {
            val stage = player.getQuestStage(Quest.UNDERGROUND_PASS)
            when (npc.id) {
                Npc.npc_972 -> {
                    when (stage) {
                        STAGE_FIND_KOFTIK -> {
                            if (player.vars.getVarBit(Varbit.upass_koftic_chat) == 1) {
                                npc(npc, CALM_TALK, "I know it's scary in there, but you'll have to go in alone. I'll catch up as soon as I can.")
                                exec { player.giveKoftikCloth() }
                                return@startConversation
                            }

                            player(CALM_TALK, "Hello. You must be Koftik.")
                            npc(npc, CALM_TALK, "King Lathas told me to expect you.")
                            npc(npc, SAD, "Your goal lies beyond these caves, but the darkness changes people. I have seen it happen.")
                            options {
                                op("I'll take my chances.") {
                                    player(CALM_TALK, "I'll take my chances.")
                                    npc(npc, CALM_TALK, "Then take this cloth. Use it to prepare an arrow; fire may help you cross the first chasm.")
                                    exec {
                                        player.giveKoftikCloth()
                                        player.vars.setVarBit(Varbit.upass_koftic_chat, 1)
                                    }
                                }
                                op("Tell me more about the caves.") {
                                    player(CONFUSED, "Tell me more about the caves.")
                                    npc(npc, SAD, "My friend Randas entered with me. The deeper we went, the more a voice filled his thoughts.")
                                    npc(npc, SCARED, "At last he turned on me. Whatever Iban is, his power reaches far beyond the temple.")
                                    exec { player.vars.setVarBit(Varbit.upass_koftic_chat, 1) }
                                }
                            }
                        }

                        STAGE_ENTERED_PASS -> {
                            npc(npc, CALM_TALK, "I know it's scary in there, but you'll have to go in alone. I'll catch up as soon as I can.")
                            exec { player.giveKoftikCloth() }
                        }

                        STAGE_CROSSED_BRIDGE -> {
                            player(CALM_TALK, "Hello Koftik.")
                            npc(npc, CALM_TALK, "Once you're over the bridge keep going straight ahead, I'll meet you further up.")
                        }

                        STAGE_DESTROYED_ORBS, STAGE_PASSED_UNICORN -> {
                            player(CALM_TALK, "Hello Koftik.")
                            npc(npc, SCARED, "It scares me in there... The voices, don't you hear them?")
                            player(CALM_TALK, "You'll be okay Koftik...")
                        }

                        STAGE_RETURN_TO_LATHAS -> {
                            player(CALM_TALK, "Thanks for showing me the way out Koftik.")
                            npc(npc, CALM_TALK, "Always a pleasure. Have you informed the King about Iban?")
                            player(CALM_TALK, "No, not yet.")
                            npc(npc, CALM_TALK, "This is no time to linger. The King must know that Iban is dead. This is a truly historical moment for Ardougne.")
                        }

                        STAGE_COMPLETE -> {
                            player(CALM_TALK, "Hello Koftik.")
                            npc(npc, CALM_TALK, "Hello adventurer, how's things?")
                            player(CALM_TALK, "Not bad, yourself?")
                            npc(npc, CALM_TALK, "I'm good... just keeping an eye out.")
                        }

                        else -> simple(KOFTIK_UNINTERESTED)
                    }
                }

                Npc.caveguide2 -> {
                    when (stage) {
                        STAGE_ENTERED_PASS -> {
                            npc(npc, SAD, "The bridge is rotten, but that guide rope reaches the far side.")
                            npc(npc, CALM_TALK, "Wrap an arrow in the cloth I gave you, light it, then fire it at the rope.")
                            exec { player.giveKoftikCloth() }
                        }

                        in STAGE_CROSSED_BRIDGE..STAGE_COMPLETE -> {
                            player(CALM_TALK, "Hi Koftik.")
                            npc(npc, CALM_TALK, "Hello adventurer. I'll catch up with you further on.")
                            exec { player.giveKoftikCloth() }
                        }

                        else -> simple(KOFTIK_UNINTERESTED)
                    }
                }

                Npc.caveguide3 -> {
                    if (stage == STAGE_CROSSED_BRIDGE) {
                        npc(npc, FRUSTRATED, "I cannot remember the safe path. Iban's voice keeps breaking my concentration.")
                        npc(npc, CALM_TALK, "Watch each tile closely. A wrong step will send spikes through the floor.")
                        if (!player.questManager.getAttribs(Quest.UNDERGROUND_PASS).getB(RECEIVED_KOFTIK_GRID_FOOD)) {
                            npc(npc, CALM_TALK, "Take some food. You may need it more than I do.")
                            exec {
                                player.inventory.addItemDrop(Obj.meat_pie, 1)
                                player.questManager.getAttribs(Quest.UNDERGROUND_PASS).setB(RECEIVED_KOFTIK_GRID_FOOD, true)
                            }
                        }
                    } else
                        simple(KOFTIK_UNINTERESTED)
                }

                Npc.caveguide5 -> {
                    if (stage == STAGE_PASSED_PALADINS) {
                        npc(npc, SCARED, "The voice is stronger here. It promises that Iban will make us whole.")
                        player(SAD, "Fight it, Koftik. Remember why King Lathas sent us.")
                    } else
                        simple(KOFTIK_UNINTERESTED)
                }

                Npc.caveguide6 -> {
                    if (stage == STAGE_RETURN_TO_LATHAS) {
                        npc(npc, CONFUSED, "Traveller, where am I? I can't remember a thing!")
                        player(SAD, "We were losing you to Iban's influence...")
                        npc(npc, CONFUSED, "What? Of course, the voices... But they've stopped. What happened?")
                        player(CALM_TALK, "Iban's dead, I destroyed him.")
                        npc(npc, HAPPY_TALKING, "You've done well! Thank you for saving me.")
                        npc(npc, CONFUSED, "But what about you, adventurer? Did the voices not affect you?")
                        simple("Your mind is filled with strange images. A unicorn. A boulder. Some paladins. They seem real... but were they?")
                        player(CONFUSED, "I... I don't think so...")
                        npc(npc, CALM_TALK, "I see. Well we must now inform the King. Follow me, I'll lead you out.")
                        player(CALM_TALK, "At last! I've had enough of caves.")
                    } else
                        simple(KOFTIK_UNINTERESTED)
                }
            }
        }
    }
}

internal fun Player.giveKoftikCloth() {
    val hasSpace = inventory.hasFreeSlots()
    inventory.addItemDrop(Obj.damp_cloth, 1)
    sendMessage(if (hasSpace) "Koftik gives you a damp cloth..." else "Your inventory is full, so Koftik places the damp cloth at your feet.")
}

private class FinalKoftikNpc(id: Int, tile: Tile) : NPC(id, tile) {
    init {
        setRandomWalk(false)
        faceDir(Direction.EAST)
    }

    override fun resetDirection() {
        stopFaceEntity()
        faceDir(Direction.EAST)
    }

    override fun withinDistance(player: Player, distance: Int): Boolean =
        player.getQuestStage(Quest.UNDERGROUND_PASS) == STAGE_RETURN_TO_LATHAS &&
            super.withinDistance(player, distance)
}

internal fun Player.showKoftikDwarfWarning() {
    if (getQuestStage(Quest.UNDERGROUND_PASS) != STAGE_PASSED_PALADINS)
        return
    val attributes = questManager.getAttribs(Quest.UNDERGROUND_PASS)
    if (attributes.getB(SAW_KOFTIK_DWARF_WARNING))
        return
    attributes.setB(SAW_KOFTIK_DWARF_WARNING, true)

    val koftik = ownedNpc(this, Npc.caveguide5, KOFTIK_DWARF_WARNING_TILE) {
        setRandomWalk(false)
        setCantInteract(true)
    }
    koftik.schedule {
        wait(100)
        if (!koftik.hasFinished())
            koftik.finish()
    }
    startConversation {
        npc(koftik, SCARED, "Traveller is that you?.. my friend on a mission!")
        player(SAD, "Koftik, you're still here, you should leave.")
        npc(koftik, SCARED, "Leave?...leave?..this is my home now. Home with my lord, he talks to me, he's my friend.")
        simple("Koftik seems to be in a weak state of mind.")
        player(SAD, "Koftik you really should leave these caverns.")
        npc(koftik, SCARED, "Not now, we're all the same down here. There's just you and those Dwarfs left to be converted.")
        player(CONFUSED, "Dwarfs?")
        npc(koftik, FRUSTRATED, "Foolish Dwarfs, still believing that they can resist. No one resists Iban, go traveller.")
        npc(koftik, SCARED, "The Dwarfs to the south, they're not safe in the south.")
        npc(koftik, SCARED, "We'll show them, go slay them m'lord. He'll be so proud, that's all I want.")
        player(SAD, "I'll pray for you.")
        exec { koftik.finish() }
    }
}
