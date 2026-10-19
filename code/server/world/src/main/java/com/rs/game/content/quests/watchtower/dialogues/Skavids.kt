package com.rs.game.content.quests.watchtower.dialogues

import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.tasks.WorldTasks
import com.rs.game.content.quests.watchtower.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

private const val SKAVID_1_COMPLETE = "watchtowerSkavid1Complete"
private const val SKAVID_2_COMPLETE = "watchtowerSkavid2Complete"
private const val SKAVID_3_COMPLETE = "watchtowerSkavid3Complete"
private const val SKAVID_5_COMPLETE = "watchtowerSkavid5Complete"

@ServerStartupEvent
fun mapWatchtowerSkavids() {
    onNpcClick(
        Npc.scared_skavid,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        scaredSkavidDialogue(player, npc)
    }

    onNpcClick(
        Npc.skavidtalker1,
        Npc.skavidtalker2,
        Npc.skavidtalker3,
        Npc.skavidtalker4,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        normalSkavidDialogue(player, npc)
    }

    onNpcClick(
        Npc.mad_skavid,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        madSkavidDialogue(player, npc)
    }
}

private fun scaredSkavidDialogue(player: Player, npc: NPC) {
    player.startConversation {
        val stage = player.getQuestStage(Quest.WATCHTOWER)

        if (stage < STAGE_LEARN_SKAVID_LANGUAGE) {
            npc(
                npc,
                CALM_TALK,
                "Tanath cur, tanath cur!"
            )

            player(
                CALM_TALK,
                "???"
            )

            simple(
                "The skavid is trying to communicate..."
            )

            simple(
                "You don't know any skavid words yet!"
            )

            return@startConversation
        }

        if (stage == STAGE_LEARN_SKAVID_LANGUAGE) {
            npc(
                npc,
                WORRIED,
                "Tanath cur, tanath cur!"
            )

            player(
                CALM_TALK,
                "???"
            )

            npc(
                npc,
                WORRIED,
                "Don't hurt me, don't hurt me!"
            )

            player(
                CALM_TALK,
                "Stop moaning, creature. I know about you skavids - you serve those monsters, the ogres."
            )

            npc(
                npc,
                WORRIED,
                "Please don't touch me!"
            )

            player(
                CALM_TALK,
                "You have something that belongs to me..."
            )

            npc(
                npc,
                WORRIED,
                "I don't have anything; please believe me!"
            )

            player(
                CALM_TALK,
                "Somehow, I find your words hard to believe."
            )

            npc(
                npc,
                WORRIED,
                "I'm begging your kindness; I don't have it!"
            )

            options {
                op("I don't believe you, hand it over!") {
                    player(
                        ANGRY,
                        "I don't believe you, hand it over!"
                    )

                    npc(
                        npc,
                        WORRIED,
                        "Ahhhhh, help!"
                    )

                    player(
                        CALM_TALK,
                        "Oh great, I'm not getting anywhere."
                    )
                }

                op("Okay, okay, I'm not going to hurt you.") {
                    player(
                        CALM_TALK,
                        "Okay, okay, I'm not going to hurt you."
                    )

                    npc(
                        npc,
                        HAPPY_TALKING,
                        "Thank you, kind ${if (player.appearance.isMale) "sir" else "madam"}. I'll tells you where that things you wants is. The mad skavids have it in their cave in the city. You will have to learn skavid, otherwise they will not talks to you. Here, I'll tell you what you need to know..."
                    )

                    npc(
                        Npc.watchtower_cutscene_ogre_shaman,
                        ANGRY,
                        "You shut your mouth! Don't tell them anything!"
                    ) {
                        spawnInterruptingShaman(player, npc)
                    }

                    npc(
                        npc,
                        WORRIED,
                        "Tanath cur, tanath cur!"
                    )

                    npc(
                        npc,
                        CALM_TALK,
                        "I cannot help you, but the others can. Let me tells you the most common skavid words: Ar, nod, gor, ig and cur. Those will gets you started."
                    ) {
                        player.setQuestStage(
                            Quest.WATCHTOWER,
                            STAGE_SEARCH_SKAVID_CAVES
                        )
                    }
                }
            }

            return@startConversation
        }

        if (stage >= STAGE_SEARCH_SKAVID_CAVES) {
            if (hasLearnedSkavid(player)) {
                npc(
                    npc,
                    HAPPY_TALKING,
                    "Master, my kinsmen tell me you have learned skavid. You should speak to the mad ones in their cave."
                ) {
                    if (player.getQuestStage(Quest.WATCHTOWER) == STAGE_SEARCH_SKAVID_CAVES) {
                        player.setQuestStage(Quest.WATCHTOWER, STAGE_FIND_SHAMAN_ENCLAVE)
                    }
                }
            } else {
                npc(
                    npc,
                    CALM_TALK,
                    "Master, how are you doing learning our language?"
                )

                player(
                    CALM_TALK,
                    "I am studying the speech of your kind..."
                )
            }
        }
    }
}

private fun spawnInterruptingShaman(player: Player, skavid: NPC): NPC {
    val spawnTile = World.findAdjacentFreeSpace(
        skavid.tile,
        2
    ) ?: skavid.tile

    return OwnedNPC(
        player,
        Npc.watchtower_cutscene_ogre_shaman,
        spawnTile,
        true
    ).apply {
        setRandomWalk(false)
        lock(8)
        setNextFaceEntity(skavid)
        WorldTasks.schedule(1) {
            anim(Seq.chieftan_ogre_attack_staff)
            skavid.anim(Seq.qip_watchtower_skavid_hit_by_ogre)
        }
        WorldTasks.schedule(8) { finish() }
    }
}

private fun normalSkavidDialogue(player: Player, npc: NPC) {
    player.startConversation {
        if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_SEARCH_SKAVID_CAVES) {
            npc(
                npc,
                CALM_TALK,
                if (Utils.random(2) == 0) {
                    "Gor cur..."
                } else {
                    "Bidith tanath..."
                }
            )

            player(
                CALM_TALK,
                "???"
            )

            simple(
                "The skavid is trying to communicate..."
            )

            simple(
                "You don't know any skavid words yet!"
            )

            return@startConversation
        }

        when {
            npc.tile.y >= 9460 ->
                skavidOne(player, npc)

            npc.tile.x >= 2510 && npc.tile.y >= 9448 ->
                skavidTwo(player, npc)

            npc.tile.y >= 9445 ->
                skavidThree(player, npc)

            else ->
                skavidFive(player, npc)
        }
    }
}

private fun DialogueBuilder.skavidOne(
    player: Player,
    npc: NPC
) {
    val questAttribs =
        player.questManager.getAttribs(Quest.WATCHTOWER)

    if (questAttribs.getB(SKAVID_1_COMPLETE)) {
        npc(
            npc,
            HAPPY_TALKING,
            "Bidith ig!"
        )

        simple(
            "You have already talked to this skavid."
        )

        return
    }

    npc(
        npc,
        CALM_TALK,
        "Cur bidith..."
    )

    simple(
        "The skavid is trying to communicate..."
    )

    skavidOptions(
        player = player,
        npc = npc,
        correct = "Ig.",
        response = "Ig! Bidith ig!",
        attribute = SKAVID_1_COMPLETE
    )
}

private fun DialogueBuilder.skavidTwo(
    player: Player,
    npc: NPC
) {
    val questAttribs =
        player.questManager.getAttribs(Quest.WATCHTOWER)

    if (questAttribs.getB(SKAVID_2_COMPLETE)) {
        npc(
            npc,
            HAPPY_TALKING,
            "Ar cur!"
        )

        simple(
            "You have already talked to this skavid."
        )

        return
    }

    npc(
        npc,
        CALM_TALK,
        "Gor cur..."
    )

    simple(
        "The skavid is trying to communicate..."
    )

    skavidOptions(
        player = player,
        npc = npc,
        correct = "Ar.",
        response = "Ar, ar cur!",
        attribute = SKAVID_2_COMPLETE
    )
}

private fun DialogueBuilder.skavidThree(
    player: Player,
    npc: NPC
) {
    val questAttribs =
        player.questManager.getAttribs(Quest.WATCHTOWER)

    if (questAttribs.getB(SKAVID_3_COMPLETE)) {
        npc(
            npc,
            HAPPY_TALKING,
            "Cur tanath!"
        )

        simple(
            "You have already talked to this skavid."
        )

        return
    }

    npc(
        npc,
        CALM_TALK,
        "Bidith tanath..."
    )

    simple(
        "The skavid is trying to communicate..."
    )

    skavidOptions(
        player = player,
        npc = npc,
        correct = "Cur.",
        response = "Cur, cur tanath!",
        attribute = SKAVID_3_COMPLETE
    )
}

private fun DialogueBuilder.skavidFive(
    player: Player,
    npc: NPC
) {
    val questAttribs =
        player.questManager.getAttribs(Quest.WATCHTOWER)

    if (questAttribs.getB(SKAVID_5_COMPLETE)) {
        npc(
            npc,
            HAPPY_TALKING,
            "Gor nod!"
        )

        simple(
            "You have already talked to this skavid."
        )

        return
    }

    npc(
        npc,
        CALM_TALK,
        "Tanath gor..."
    )

    simple(
        "The skavid is trying to communicate..."
    )

    skavidOptions(
        player = player,
        npc = npc,
        correct = "Nod.",
        response = "Nod, gor nod!",
        attribute = SKAVID_5_COMPLETE
    )
}

private fun DialogueBuilder.skavidOptions(
    player: Player,
    npc: NPC,
    correct: String,
    response: String,
    attribute: String
) {
    options {
        listOf(
            "Cur.",
            "Ar.",
            "Ig.",
            "Nod.",
            "Gor."
        ).forEach { answer ->
            op(answer) {
                player(
                    CALM_TALK,
                    answer
                )

                if (answer == correct) {
                    npc(
                        npc,
                        HAPPY_TALKING,
                        response
                    )

                    simple(
                        "It seems the skavid understood you."
                    ) {
                        player.questManager
                            .getAttribs(Quest.WATCHTOWER)
                            .setB(attribute, true)
                    }
                } else {
                    npc(
                        npc,
                        CONFUSED,
                        "???"
                    )

                    simple(
                        "It seems that was the wrong reply."
                    )
                }
            }
        }
    }
}

private fun madSkavidDialogue(
    player: Player,
    npc: NPC
) {
    recoverWatchtowerSkavidProgress(player)
    player.startConversation {
        val questAttribs =
            player.questManager.getAttribs(Quest.WATCHTOWER)

        if (!hasLearnedSkavid(player)) {
            npc(
                npc,
                CALM_TALK,
                "Tanath gor ar bidith?"
            )

            player(
                CALM_TALK,
                "???"
            )

            simple(
                "You cannot communicate with the skavid."
            )

            simple(
                "It seems you haven't learned enough of their language yet..."
            )

            return@startConversation
        }

        if (questAttribs.getB(MAD_SKAVID_COMPLETE)) {
            madSkavidCrystalRecovery(player, npc)
            return@startConversation
        }

        val test = when (Utils.random(4)) {
            0 -> MadSkavidTest(
                phrase = "Ar cur...",
                answer = "Gor."
            )

            1 -> MadSkavidTest(
                phrase = "Bidith ig...",
                answer = "Cur."
            )

            2 -> MadSkavidTest(
                phrase = "Cur tanath...",
                answer = "Bidith."
            )

            else -> MadSkavidTest(
                phrase = "Gor nod...",
                answer = "Tanath."
            )
        }

        npc(
            npc,
            CALM_TALK,
            test.phrase
        )

        madSkavidOptions(
            player = player,
            npc = npc,
            correct = test.answer
        )
    }
}

private fun DialogueBuilder.madSkavidOptions(
    player: Player,
    npc: NPC,
    correct: String
) {
    options {
        listOf(
            "Cur.",
            "Ar.",
            "Bidith.",
            "Tanath.",
            "Gor."
        ).forEach { answer ->
            op(answer) {
                player(
                    CALM_TALK,
                    answer
                )

                if (answer == correct) {

                    npc(
                        npc,
                        HAPPY_TALKING,
                        "Hehe! So you speak a little skavid, eh? I'm impressed. Here, take this prize. The others are with the shamans in the enclave - you'll never get them back. They hid one in the sacred rock - nothing can touch it now."
                    ) {
                        player.inventory.addItemDrop(
                            Obj.powering_crystal2,
                            1
                        )

                        player.questManager
                            .getAttribs(Quest.WATCHTOWER)
                            .setB(MAD_SKAVID_COMPLETE, true)
                        recoverWatchtowerSkavidProgress(player)
                    }

                    simple(
                        "The skavid gives you a large crystal."
                    )
                } else {
                    npc(
                        npc,
                        CONFUSED,
                        "???"
                    )

                    simple(
                        "The response was wrong."
                    )
                }
            }
        }
    }
}

private fun DialogueBuilder.madSkavidCrystalRecovery(
    player: Player,
    npc: NPC
) {
    npc(
        npc,
        CALM_TALK,
        "What, you gots the crystal..."
    )

    options {
        op("But I've lost it!") {
            player(
                WORRIED,
                "But I've lost it!"
            )

            if (
                player.inventory.containsItem(
                    Obj.powering_crystal2,
                    1
                ) ||
                player.bank.containsItem(
                    Obj.powering_crystal2,
                    1
                ) ||
                player.vars.getVarBit(Varbit.watchtower_pillar_3) != 0
            ) {
                npc(
                    npc,
                    CALM_TALK,
                    "I haves no more for you."
                )
            } else {

                npc(
                    npc,
                    CALM_TALK,
                    "Alright, take this one then..."
                ) {
                    player.inventory.addItemDrop(
                        Obj.powering_crystal2,
                        1
                    )
                }
            }
        }

        op("Oh, okay then.") {
            player(
                CALM_TALK,
                "Oh, okay then."
            )

            npc(
                npc,
                CALM_TALK,
                "I'll be on my way, then."
            )
        }
    }
}

private fun hasLearnedSkavid(player: Player): Boolean {
    val questAttribs =
        player.questManager.getAttribs(Quest.WATCHTOWER)

    return questAttribs.getB(SKAVID_1_COMPLETE) &&
        questAttribs.getB(SKAVID_2_COMPLETE) &&
        questAttribs.getB(SKAVID_3_COMPLETE) &&
        questAttribs.getB(SKAVID_5_COMPLETE)
}

private data class MadSkavidTest(
    val phrase: String,
    val answer: String
)
