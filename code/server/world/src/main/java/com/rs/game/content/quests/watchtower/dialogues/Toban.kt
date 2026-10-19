package com.rs.game.content.quests.watchtower.dialogues

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.content.quests.watchtower.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

private const val TOBAN_TASK_STARTED = "watchtowerTobanTaskStarted"
private const val TOBAN_TASK_COMPLETE = "watchtowerTobanTaskComplete"
private const val RELIC_PART_3_HANDED_IN = "watchtowerRelicPart3HandedIn"

@ServerStartupEvent
fun mapWatchtowerToban() {
    onNpcClick(Npc.toban, options = arrayOf("Talk-to")) { (player, npc) ->
        tobanDialogue(player, npc)
    }
}

private fun tobanDialogue(player: Player, npc: NPC) {
    player.startConversation {
        if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_FIND_WAY_INTO_GUTANOTH) {
            npc(
                npc,
                CALM_TALK,
                "What do you want, small t'ing?"
            )
            return@startConversation
        }

        if (player.getBool(TOBAN_TASK_COMPLETE)) {
            if (player.getBool(RELIC_PART_3_HANDED_IN)) {
                npc(
                    npc,
                    HAPPY_TALKING,
                    "Hahaha! Small t'ing has done well."
                )
                return@startConversation
            }

            val hasRelicPart =
                player.inventory.containsItem(Obj.relicpart3, 1) ||
                player.bank.containsItem(Obj.relicpart3, 1)

            if (hasRelicPart) {
                npc(
                    npc,
                    HAPPY_TALKING,
                    "Hahaha! Small t'ing has done well."
                )
                return@startConversation
            }

            npc(
                npc,
                HAPPY_TALKING,
                "Hahaha! Small t'ing returns. Did you bring the dragon bone?"
            )

            if (!player.inventory.containsItem(Obj.dragon_bones, 1)) {
                player(
                    CALM_TALK,
                    "I have nothing for you."
                )

                npc(
                    npc,
                    ANGRY,
                    "Then you shall get nothing from me!"
                )
                return@startConversation
            }

            player(
                HAPPY_TALKING,
                "When I say I will get something, I get it!"
            )

            if (!player.inventory.hasFreeSlots()) {
                npc(
                    npc,
                    HAPPY_TALKING,
                    "Hahaha! Small t'ing has done it. Toban is glad, but you can't hold his reward; you'd best put somet'ing down."
                )
                return@startConversation
            }

            npc(
                npc,
                HAPPY_TALKING,
                "Hahaha! Small t'ing has done it. Toban is glad - take this..."
            ) {
                player.inventory.deleteItem(Obj.dragon_bones, 1)
                player.inventory.addItem(Obj.relicpart3, 1)
            }

            simple(
                "The ogre gives you part of a statue."
            )
            return@startConversation
        }

        if (!player.getBool(TOBAN_TASK_STARTED)) {
            npc(
                npc,
                CALM_TALK,
                "What do you want, small t'ing?"
            )

            options {
                op("I seek entrance to the city of ogres.") {
                    player(
                        CALM_TALK,
                        "I seek entrance to the city of ogres."
                    )

                    npc(
                        npc,
                        HAPPY_TALKING,
                        "Hahaha! You'll never get in there."
                    )

                    player(
                        CALM_TALK,
                        "I'll find a way, trust me."
                    )

                    npc(
                        npc,
                        CALM_TALK,
                        "Bold words for a t'ing so small."
                    )

                    options {
                        op("I could do something for you...") {
                            player(
                                CALM_TALK,
                                "I could do something for you..."
                            )

                            npc(
                                npc,
                                HAPPY_TALKING,
                                "Hahaha! This creature t'inks it can help me! I would eat you now, but for your puny size. Prove to me your might: bring me the bones of an adult dragon to chew on, and I'll eat them, not you."
                            ) {
                                player.set(TOBAN_TASK_STARTED, true)

                                if (player.getQuestStage(Quest.WATCHTOWER) == STAGE_FIND_WAY_INTO_GUTANOTH) {
                                    player.setQuestStage(
                                        Quest.WATCHTOWER,
                                        STAGE_COLLECT_OGRE_RELIC
                                    )
                                }
                            }
                        }

                        op("Die, creature!") {
                            tobanAttack(player, npc)
                        }
                    }
                }

                op("Die, creature!") {
                    tobanAttack(player, npc)
                }
            }

            return@startConversation
        }

        npc(
            npc,
            HAPPY_TALKING,
            "Hahaha! Small t'ing returns. Did you bring the dragon bone?"
        )

        if (!player.inventory.containsItem(Obj.dragon_bones, 1)) {
            player(
                CALM_TALK,
                "I have nothing for you."
            )

            npc(
                npc,
                ANGRY,
                "Then you shall get nothing from me!"
            )

            return@startConversation
        }

        player(
            HAPPY_TALKING,
            "When I say I will get something, I get it!"
        )

        if (!player.inventory.hasFreeSlots()) {
            npc(
                npc,
                HAPPY_TALKING,
                "Hahaha! Small t'ing has done it. Toban is glad, but you can't hold his reward; you'd best put somet'ing down."
            )

            return@startConversation
        }

        npc(
            npc,
            HAPPY_TALKING,
            "Hahaha! Small t'ing has done it. Toban is glad - take this..."
        ) {
            player.inventory.deleteItem(Obj.dragon_bones, 1)
            player.inventory.addItem(Obj.relicpart3, 1)
            player.set(TOBAN_TASK_COMPLETE, true)
        }

        simple(
            "The ogre gives you part of a statue."
        )
    }
}

private fun DialogueBuilder.tobanAttack(
    player: Player,
    npc: NPC
) {
    player(
        ANGRY,
        "Die, creature!"
    )

    npc(
        npc,
        HAPPY_TALKING,
        "Hahaha! It t'inks it's a match for Toban, does it?"
    ) {
        player.applyHit(
            Hit(
                npc,
                150,
                HitLook.TRUE_DAMAGE
            )
        )
    }
}
