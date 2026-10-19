package com.rs.game.content.quests.watchtower

import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Loc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick

private const val FOUND_FINGERNAILS = "watchtowerFoundFingernails"
private const val FOUND_DAMAGED_DAGGER = "watchtowerFoundDamagedDagger"
private const val FOUND_TATTERED_EYE_PATCH = "watchtowerFoundTatteredEyePatch"
private const val FOUND_OLD_ROBE = "watchtowerFoundOldRobe"
private const val FOUND_UNUSUAL_ARMOUR = "watchtowerFoundUnusualArmour"

private val WATCHTOWER_EMPTY_BUSHES = setOf(
    Tile.of(2560, 3119, 0),
    Tile.of(2560, 3131, 0),
    Tile.of(2561, 3124, 0),
    Tile.of(2561, 3126, 0),
    Tile.of(2561, 3129, 0),
    Tile.of(2562, 3122, 0),
    Tile.of(2563, 3125, 0),
    Tile.of(2565, 3130, 0)
)

@ServerStartupEvent
fun mapWatchtowerGrounds() {
    onNpcClick(
        Npc.tower_guard,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        player.startConversation {
            player(
                HeadE.CALM_TALK,
                "Hello. What are you doing here?"
            )

            npc(
                npc,
                HeadE.ANGRY,
                "We are the tower guards – our business is our own!"
            )
        }
    }

    onObjectClick(
        Loc.watchtowerbush,
        Loc.watchtowerbushnail,
        Loc.watchtowerbushdagger,
        Loc.watchtowerbusheyepatch,
        Loc.watchtowerbushrobe,
        Loc.watchtowerbusharmour
    ) { (player, obj) ->
        if (player.getQuestStage(Quest.WATCHTOWER) != STAGE_SEARCH_WATCHTOWER) {
            player.sendMessage("You find nothing of interest.")
            return@onObjectClick
        }

        when (obj.id) {
            Loc.watchtowerbush -> {
                if (obj.tile !in WATCHTOWER_EMPTY_BUSHES)
                    return@onObjectClick

                player.startConversation {
                    player(
                        HeadE.CALM_TALK,
                        "Hmmm, nothing here."
                    )
                }
            }

            Loc.watchtowerbushnail -> {
                if (!obj.tile.isAt(2544, 3134, 0))
                    return@onObjectClick

                searchFingernailsBush(player)
            }

            Loc.watchtowerbushdagger -> {
                if (!obj.tile.isAt(2549, 3135, 0))
                    return@onObjectClick

                searchEvidenceBush(
                    player = player,
                    attribute = FOUND_DAMAGED_DAGGER,
                    itemId = Obj.watchtowerdagger,
                    text = "Aha, a dagger! I wonder if this is evidence..."
                )
            }

            Loc.watchtowerbusheyepatch -> {
                if (!obj.tile.isAt(2546, 3130, 0))
                    return@onObjectClick

                searchEvidenceBush(
                    player = player,
                    attribute = FOUND_TATTERED_EYE_PATCH,
                    itemId = Obj.watch_eye_patch,
                    text = "I've found an eye patch; I had better show this to the Watchtower Wizard."
                )
            }

            Loc.watchtowerbushrobe -> {
                if (!obj.tile.isAt(2559, 3123, 0))
                    return@onObjectClick

                searchEvidenceBush(
                    player = player,
                    attribute = FOUND_OLD_ROBE,
                    itemId = Obj.watchtowerrobe,
                    text = "Aha! A robe. This could be a clue..."
                )
            }

            Loc.watchtowerbusharmour -> {
                if (!obj.tile.isAt(2558, 3132, 0))
                    return@onObjectClick

                searchEvidenceBush(
                    player = player,
                    attribute = FOUND_UNUSUAL_ARMOUR,
                    itemId = Obj.watchtowerarmour,
                    text = "Here's some armour; it could be evidence..."
                )
            }
        }
    }

    onObjectClick(
        Loc.towerladder
    ) { (player, obj) ->
        if (!obj.tile.isAt(2544, 3111, 0))
            return@onObjectClick

        if (player.getQuestStage(Quest.WATCHTOWER) == STAGE_UNSTARTED) {
            player.startConversation {
                npc(
                    Npc.tower_guard,
                    HeadE.ANGRY,
                    "You can't go up there. That's private, that is."
                )
            }
            return@onObjectClick
        }

        player.startConversation {
            npc(
                Npc.tower_guard,
                HeadE.CALM_TALK,
                "It is the wizards' helping hand - let 'em up."
            ) {
                player.useStairs(
                    -1,
                    Tile.of(2544, 3111, 1),
                    0,
                    1
                )
            }
        }
    }

    onObjectClick(
        Loc.qip_watchtower_ladder_top
    ) { (player, obj) ->
        if (!obj.tile.isAt(2544, 3111, 1))
            return@onObjectClick

        player.useStairs(
            -1,
            Tile.of(2544, 3111, 0),
            0,
            1
        )
    }

    onObjectClick(
        Loc.watchladderup
    ) { (player, obj) ->
        if (!obj.tile.isAt(2549, 3111, 1))
            return@onObjectClick

        player.useStairs(
            -1,
            Tile.of(2549, 3111, 2),
            0,
            1
        )
    }

    onObjectClick(
        Loc.watchladderdown
    ) { (player, obj) ->
        if (!obj.tile.isAt(2549, 3111, 2))
            return@onObjectClick

        player.useStairs(
            -1,
            Tile.of(2549, 3111, 1),
            0,
            1
        )
    }

    onItemOnObject(
        objectNamesOrIds = arrayOf(
            Loc.loc_20025,
            Loc.loc_20029,
            Loc.loc_20033,
            Loc.loc_21549
        ),
        itemNamesOrIds = arrayOf(
            Obj.powering_crystal1,
            Obj.powering_crystal2,
            Obj.powering_crystal3,
            Obj.powering_crystal4
        )
    ) { e ->
        val player = e.player
        val obj = e.`object`
        val item = e.item

        if (player.getQuestStage(Quest.WATCHTOWER) != STAGE_RESTORE_WATCHTOWER) {
            return@onItemOnObject
        }

        val validTile = when (obj.id) {
            Loc.loc_20025 ->
                obj.tile.isAt(2545, 3116, 2)

            Loc.loc_20029 ->
                obj.tile.isAt(2545, 3113, 2)

            Loc.loc_20033 ->
                obj.tile.isAt(2548, 3116, 2)

            Loc.loc_21549 ->
                obj.tile.isAt(2548, 3113, 2)

            else -> false
        }

        if (!validTile)
            return@onItemOnObject

        val correctCrystal = when (obj.id) {
            Loc.loc_20025 -> Obj.powering_crystal3
            Loc.loc_20029 -> Obj.powering_crystal1
            Loc.loc_20033 -> Obj.powering_crystal2
            Loc.loc_21549 -> Obj.powering_crystal4
            else -> -1
        }

        if (item.id != correctCrystal) {
            player.sendMessage(
                "That is not the right crystal for this pillar; try another."
            )
            return@onItemOnObject
        }

        val varbit = when (obj.id) {
            Loc.loc_20025 -> Varbit.watchtower_pillar_1
            Loc.loc_20029 -> Varbit.watchtower_pillar_2
            Loc.loc_20033 -> Varbit.watchtower_pillar_3
            Loc.loc_21549 -> Varbit.watchtower_pillar_4
            else -> return@onItemOnObject
        }

        if (player.vars.getVarBit(varbit) != 0)
            return@onItemOnObject

        player.inventory.deleteItem(correctCrystal, 1)
        player.vars.saveVarBit(varbit, 1)

        if (allCrystalsPlaced(player)) {
            player.setQuestStage(
                Quest.WATCHTOWER,
                STAGE_ACTIVATE_WATCHTOWER
            )
        }
    }

    onObjectClick(
        Loc.watchleverup
    ) { (player, obj) ->
        if (!obj.tile.isAt(2543, 3115, 2))
            return@onObjectClick

        val stage =
            player.getQuestStage(Quest.WATCHTOWER)

        if (stage < STAGE_RESTORE_WATCHTOWER)
            return@onObjectClick

        if (player.isQuestComplete(Quest.WATCHTOWER))
            return@onObjectClick

        player.sendMessage(
            "You pull the lever..."
        )

        if (!allCrystalsPlaced(player)) {
            player.sendMessage(
                "You need to put the crystals on the correct pillars before the shield will work."
            )

            player.sendMessage(
                "All that happens now is a rather unsatisfying clunking noise."
            )
            return@onObjectClick
        }

        player.vars.saveVarBit(
            Varbit.watchtower_pillar_1,
            2
        )

        player.vars.saveVarBit(
            Varbit.watchtower_pillar_2,
            2
        )

        player.vars.saveVarBit(
            Varbit.watchtower_pillar_3,
            2
        )

        player.vars.saveVarBit(
            Varbit.watchtower_pillar_4,
            2
        )

        player.sendMessage(
            "The magic force field activates."
        )

        player.startConversation {
            npc(
                Npc.watchtower_wizard,
                HeadE.HAPPY_TALKING,
                "Marvellous! It works! The town will now be safe."
            )

            npc(
                Npc.watchtower_wizard,
                HeadE.HAPPY_TALKING,
                "Your help was invaluable. Take this payment as a token of my gratitude."
            )

            npc(
                Npc.watchtower_wizard,
                HeadE.HAPPY_TALKING,
                "Also, let me improve your Magic level for you."
            )

            npc(
                Npc.watchtower_wizard,
                HeadE.HAPPY_TALKING,
                "Here is a special item for you - it's a new spell. Read the scroll and you will be able to teleport yourself here."
            ) {
                player.questManager.completeQuest(
                    Quest.WATCHTOWER
                )
            }
        }
    }
}

private fun allCrystalsPlaced(
    player: Player
): Boolean {
    return player.vars.getVarBit(Varbit.watchtower_pillar_1) >= 1 &&
            player.vars.getVarBit(Varbit.watchtower_pillar_2) >= 1 &&
            player.vars.getVarBit(Varbit.watchtower_pillar_3) >= 1 &&
            player.vars.getVarBit(Varbit.watchtower_pillar_4) >= 1
}

private fun searchFingernailsBush(player: Player) {
    if (player.getBool(FOUND_FINGERNAILS)) {
        if (!player.inventory.containsItem(Obj.fingernails, 1) &&
            !player.bank.containsItem(Obj.fingernails, 1)
        ) {
            player.startConversation {
                player(
                    HeadE.UPSET,
                    "What's this? Disgusting! Some fingernails. They may be a clue though... I'd better take them."
                ) {
                    player.inventory.addItemDrop(
                        Obj.fingernails,
                        1
                    )
                }
            }
            return
        }

        alreadySearched(player)
        return
    }

    player.startConversation {
        player(
            HeadE.UPSET,
            "What's this? Disgusting! Some fingernails. They may be a clue though... I'd better take them."
        ) {
            player.inventory.addItemDrop(
                Obj.fingernails,
                1
            )

            player.set(
                FOUND_FINGERNAILS,
                true
            )
        }
    }
}

private fun searchEvidenceBush(
    player: Player,
    attribute: String,
    itemId: Int,
    text: String
) {
    if (player.getBool(attribute)) {
        if (!player.inventory.containsItem(itemId, 1) &&
            !player.bank.containsItem(itemId, 1)
        ) {
            player.startConversation {
                player(HeadE.CALM_TALK, text) {
                    player.inventory.addItemDrop(
                        itemId,
                        1
                    )
                }
            }
            return
        }

        alreadySearched(player)
        return
    }

    player.startConversation {
        player(HeadE.CALM_TALK, text) {
            player.inventory.addItemDrop(itemId, 1)
            player.set(attribute, true)
        }
    }
}

private fun alreadySearched(player: Player) {
    player.startConversation {
        player(
            HeadE.CALM_TALK,
            "I have already searched this place."
        )
    }
}
