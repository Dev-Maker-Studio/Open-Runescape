package com.rs.game.content.quests.watchtower.areas

import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Loc
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.CONFUSED
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.tasks.WorldTasks
import com.rs.game.content.quests.watchtower.STAGE_ENTER_GUTANOTH
import com.rs.game.content.quests.watchtower.STAGE_FIND_SHAMAN_ENCLAVE
import com.rs.game.content.quests.watchtower.STAGE_GET_SKAVID_MAP
import com.rs.game.content.quests.watchtower.recoverWatchtowerSkavidProgress
import com.rs.game.content.skills.agility.Agility
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import org.darkan.core.Constants
import world.gregs.voidps.type.Tile

private const val BRIDGE_TOLL = 20
private const val BRIDGE_TOLL_PAID = "watchtower_gu_tanoth_bridge_toll_paid"

private const val ROCK_CAKE_THIEVING_LEVEL = 15
private const val ROCK_CAKE_THIEVING_XP = 10.0
private const val BROKEN_BRIDGE_AGILITY_LEVEL = 25

private const val GU_TANOTH_EJECTION_GROAN =
    "watchtower_gu_tanoth_ejection_groan"

private const val MARKET_GIFT_REQUESTED =
    "watchtower_gu_tanoth_market_gift_requested"

private const val MARKET_GIFT_ACCEPTED =
    "watchtower_gu_tanoth_market_gift_accepted"

private const val GOLDEN_GATEWAY_UNLOCKED =
    "watchtower_gu_tanoth_golden_gateway_unlocked"
private const val GOLDEN_GATEWAY_GOLD_REQUESTED = "watchtower_gu_tanoth_gold_requested"

private const val SHAMAN_CAVE_GUARD_DISTRACTED =
    "watchtower_shaman_cave_guard_distracted"

private val CITY_GATE_TILE = Tile.of(2550, 3028, 0)
private val BROKEN_BRIDGE_GAP_TILE = Tile.of(2531, 3029, 0)
private val BROKEN_BRIDGE_SOUTH = Tile.of(2531, 3029, 0)
private val BROKEN_BRIDGE_NORTH = Tile.of(2531, 3031, 0)
private val ROCK_CAKE_STALL_TILE = Tile.of(2513, 3036, 0)
private val SPIDER_CHEST_VARIANTS = mapOf(
    Loc.shuttanothchest1 to Loc.opentanothchest1,
    Loc.shuttanothchest2 to Loc.opentanothchest2
)
private val SPIDER_CHEST_TILES = arrayOf(Tile.of(2526, 3032, 0), Tile.of(2511, 3024, 0))

private val GUARD_PUNCH_DESTINATION = Tile.of(2545, 3065, 0)

private val GUARD_PUNCH_EXIT_TILES = arrayOf(
    Tile.of(2544, 3064, 0),
    Tile.of(2545, 3064, 0),
    Tile.of(2546, 3064, 0),
    Tile.of(2544, 3065, 0),
    Tile.of(2546, 3065, 0),
    Tile.of(2544, 3066, 0),
    Tile.of(2545, 3066, 0),
    Tile.of(2546, 3066, 0)
)

@ServerStartupEvent
fun mapGuTanoth() {
    onObjectClick(*(SPIDER_CHEST_VARIANTS.keys + SPIDER_CHEST_VARIANTS.values).toTypedArray<Any>(),
        tiles = SPIDER_CHEST_TILES) { (player, obj, option) ->
        when (option) {
            "Close" -> {
                val closed = SPIDER_CHEST_VARIANTS.entries.firstOrNull { it.value == obj.id }?.key
                    ?: return@onObjectClick
                player.faceObject(obj)
                player.anim(Seq.human_closechest)
                player.lock(2)
                obj.changeId(closed)
            }
            "Open" -> {
                val open = SPIDER_CHEST_VARIANTS[obj.id] ?: return@onObjectClick
                val spawnTile = listOf(player.tile.transform(1, 0), player.tile.transform(-1, 0),
                    player.tile.transform(0, 1), player.tile.transform(0, -1), player.tile)
                    .firstOrNull { World.floorAndWallsFree(it, 1) } ?: return@onObjectClick
                player.faceObject(obj)
                player.anim(Seq.human_openchest)
                player.lock(2)
                obj.setIdTemporary(open, 50)
                WorldTasks.schedule(1) {
                    player.sendMessage("A giant spider jumps out of the chest!")
                    val spider = OwnedNPC(player, Npc.sos_pest_giantspider1, spawnTile, true)
                    spider.setRandomWalk(false)
                    spider.combatTarget = player
                }
            }
        }
    }

    onNpcClick(
        Npc.ogre_guard1,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        handleGoldenGatewayGuard(player, npc)
    }

    onNpcClick(
        Npc.ogre_guard,
        Npc.ogre_guard2,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        handleEntranceGuard(player, npc)
    }

    onNpcClick(
        Npc.ogre_guard3,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        handleMarketGuard(player, npc)
    }

    onNpcClick(
        Npc.ogre_guard4,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        handleTollBridgeGuard(player, npc)
    }

    onNpcClick(
        Npc.enclave_guard,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        handleShamanCaveGuard(player, npc)
    }

    onItemOnNpc(Npc.enclave_guard) { (player, item, npc) ->
        if (item.id == Obj.nightshade) {
            handleNightshadeOnShamanCaveGuard(player, npc)
        }
    }

    onNpcClick(
        Npc.ogre_trader2,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        handleOgreTrader(player, npc)
    }

    onObjectClick(
        Loc.ogreguardgate1,
        Loc.ogreguardgate1right,
        tiles = arrayOf(CITY_GATE_TILE, Tile.of(2549, 3028, 0))
    ) { (player, obj) ->
        val questAttribs =
            player.questManager.getAttribs(Quest.WATCHTOWER)

        if (questAttribs.getB(GOLDEN_GATEWAY_UNLOCKED)) {
            Doors.handleDoubleDoor(player, obj)
            return@onObjectClick
        }

        player.sendMessage("The ogre guard won't let you through.")
    }

    onObjectClick(Loc.ogreguardgate2, Loc.ogreguardgate2right,
        tiles = arrayOf(Tile.of(2504, 3062, 0), Tile.of(2504, 3063, 0))) { (player, obj) ->
        if (player.getQuestStage(Quest.WATCHTOWER) >= STAGE_GET_SKAVID_MAP) {
            Doors.handleDoubleDoor(player, obj)
        } else {
            player.sendMessage("The ogre guard won't let you through without proof of friendship.")
        }
    }

    onObjectClick(Loc.ganothbattlement,
        tiles = arrayOf(Tile.of(2507, 3011, 0), Tile.of(2507, 3012, 0))) { (player, obj) ->
        if (!player.questManager.getAttribs(Quest.WATCHTOWER).getB(MARKET_GIFT_ACCEPTED)) {
            player.sendMessage("The ogre guard won't let you over the wall.")
            return@onObjectClick
        }
        val destination = Tile.of(if (player.x > obj.x) 2506 else 2508, obj.y, 0)
        player.resetWalkSteps()
        player.faceTile(destination)
        Agility.handleObstacle(player, Seq.trollheim_wall_kit_climb_over, 2, destination, 1.0)
    }

    onObjectClick(
        Loc.rockcounter_withcakes,
        tiles = arrayOf(ROCK_CAKE_STALL_TILE)
    ) { (player) ->
        handleRockCakeStall(player)
    }

    onObjectClick(
        Loc.tanothjump2,
        tiles = arrayOf(BROKEN_BRIDGE_GAP_TILE), checkDistance = false
    ) { (player) ->
        if (player.isLocked || player.plane != 0 || player.getQuestStage(Quest.WATCHTOWER) < STAGE_GET_SKAVID_MAP)
            return@onObjectClick
        val start = if (player.y > BROKEN_BRIDGE_GAP_TILE.y) BROKEN_BRIDGE_NORTH else BROKEN_BRIDGE_SOUTH
        player.walkToAndExecute(start) {
            if (!player.isLocked && player.tile == start)
                handleBrokenBridge(player, start)
        }
    }

    onPlayerStep(GUARD_PUNCH_EXIT_TILES) { (player) ->
        if (!player.tempAttribs.getB(GU_TANOTH_EJECTION_GROAN))
            return@onPlayerStep

        player.tempAttribs.removeB(GU_TANOTH_EJECTION_GROAN)
        player.sendMessage("Urrrrrgh!")
    }
}

private fun handleEntranceGuard(player: Player, npc: NPC) {
    if (player.getQuestStage(Quest.WATCHTOWER) >= STAGE_GET_SKAVID_MAP) {
        player.startConversation {
            npc(npc, CALM_TALK, "Welcome to Gu'Tanoth, friend of the ogres.")
        }
        return
    }
    if (player.getQuestStage(Quest.WATCHTOWER) != STAGE_ENTER_GUTANOTH)
        return

    player.startConversation {
        npc(
            npc,
            CALM_TALK,
            "Well, what proof of friendship did you bring?"
        )

        if (!player.inventory.containsItem(Obj.ogrerelic, 1)) {
            player(
                CONFUSED,
                "I don't have anything."
            )

            npc(
                npc,
                CALM_TALK,
                "Why have you returned with no proof of companionship? Back to whence you came!"
            ) {
                player.fadeScreen {
                    player.tele(GUARD_PUNCH_DESTINATION)

                    player.applyHit(
                        Hit(
                            player,
                            Utils.random(10, 50),
                            Hit.HitLook.TRUE_DAMAGE
                        )
                    )
                }
            }

            return@startConversation
        }

        player(
            CALM_TALK,
            "I have a relic from a chieftain."
        )

        npc(
            npc,
            CALM_TALK,
            "It's got the statue of Dalgroth. Welcome to Gu'Tanoth, friend of the ogres."
        ) {
            player.inventory.deleteItem(Obj.ogrerelic, 1)

            player.setQuestStage(
                Quest.WATCHTOWER,
                STAGE_GET_SKAVID_MAP
            )

        }
    }
}

private fun handleGoldenGatewayGuard(player: Player, npc: NPC) {
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_GET_SKAVID_MAP)
        return

    val questAttribs =
        player.questManager.getAttribs(Quest.WATCHTOWER)

    if (questAttribs.getB(GOLDEN_GATEWAY_UNLOCKED)) {
        player.startConversation { npc(npc, CALM_TALK, "On your way.") }
        return
    }

    player.startConversation {
        if (!questAttribs.getB(GOLDEN_GATEWAY_GOLD_REQUESTED)) {
            npc(npc, CALM_TALK, "You cannot pass here.")
            player(CALM_TALK, "I am a friend to ogres.")
            npc(npc, CALM_TALK, "Bring me a bar of pure gold and I will let you pass.") {
                questAttribs.setB(GOLDEN_GATEWAY_GOLD_REQUESTED, true)
            }
            return@startConversation
        }
        npc(
            npc,
            CALM_TALK,
            "Creature, did you bring me the gold?"
        )

        if (!player.inventory.containsItem(Obj.gold_bar, 1)) {
            player(CONFUSED, "I don't have a gold bar with me.")
            npc(npc, CALM_TALK, "Without a gold bar, you cannot pass.")
            return@startConversation
        }

        player(
            CALM_TALK,
            "Here it is!"
        ) {
            player.inventory.deleteItem(Obj.gold_bar, 1)
        }

        npc(
            npc,
            CALM_TALK,
            "It's brought it! On your way."
        ) {
            questAttribs.setB(
                GOLDEN_GATEWAY_UNLOCKED,
                true
            )

            player.addWalkSteps(
                CITY_GATE_TILE,
                3,
                false
            )
        }
    }
}

private fun handleMarketGuard(player: Player, npc: NPC) {
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_GET_SKAVID_MAP)
        return

    val questAttribs =
        player.questManager.getAttribs(Quest.WATCHTOWER)

    if (questAttribs.getB(MARKET_GIFT_ACCEPTED))
        return

    if (questAttribs.getB(MARKET_GIFT_REQUESTED) && player.inventory.containsItem(Obj.rockcake, 1)) {
        handleRockCakeHandIn(player, npc)
        return
    }

    player.startConversation {
        npc(
            npc,
            CALM_TALK,
            "Oi! Where do you think you are going? You are for the cooking pot!"
        )

        options {
            op("But I am a friend to ogres...") {
                player(
                    CALM_TALK,
                    "But I am a friend to ogres..."
                )

                npc(
                    npc,
                    CALM_TALK,
                    "Prove it to us with a gift. Get us something from the market."
                )

                player(
                    CONFUSED,
                    "Like what?"
                )

                npc(
                    npc,
                    CALM_TALK,
                    "Surprise us..."
                ) {
                    questAttribs.setB(
                        MARKET_GIFT_REQUESTED,
                        true
                    )
                }
            }

            op("Not if I can help it.") {
                player(
                    CALM_TALK,
                    "Not if I can help it."
                )

                npc(
                    npc,
                    CALM_TALK,
                    "You can help by being tonight's dinner, or you can go away. Now, which shall it be?"
                )

                options {
                    op("Okay, okay, I'm going.") {
                        player(
                            CALM_TALK,
                            "Okay, okay, I'm going."
                        )

                        npc(
                            npc,
                            CALM_TALK,
                            "Back to whence you came!"
                        ) {
                            ejectPlayerFromGuTanoth(player)
                        }
                    }

                    op("I tire of ogres; prepare to die!") {
                        player(
                            CALM_TALK,
                            "I tire of ogres; prepare to die!"
                        )

                        npc(
                            npc,
                            CALM_TALK,
                            "Grrrrr!"
                        ) {
                            npc.setCombatTarget(player)
                        }
                    }
                }
            }
        }
    }
}

private fun handleRockCakeStall(player: Player) {
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_GET_SKAVID_MAP)
        return

    val questAttribs =
        player.questManager.getAttribs(Quest.WATCHTOWER)

    if (!questAttribs.getB(MARKET_GIFT_REQUESTED)) {
        player.sendMessage("You have no reason to steal from the counter.")
        return
    }

    if (questAttribs.getB(MARKET_GIFT_ACCEPTED)) {
        player.sendMessage("You have no reason to steal another rock cake.")
        return
    }

    if (player.skills.getLevel(Skills.THIEVING) < ROCK_CAKE_THIEVING_LEVEL) {
        player.sendMessage(
            "You need a Thieving level of $ROCK_CAKE_THIEVING_LEVEL to steal from this."
        )
        return
    }

    if (!player.inventory.hasFreeSlots()) {
        player.sendMessage("Not enough space in your inventory.")
        return
    }

    player.setNextFaceTile(ROCK_CAKE_STALL_TILE)
    player.anim(Seq.human_pickpocket)
    player.lock(2)

    player.inventory.addItem(Obj.rockcake, 1)
    player.skills.addXp(
        Constants.THIEVING,
        ROCK_CAKE_THIEVING_XP
    )
}

private fun handleRockCakeHandIn(player: Player, npc: NPC) {
    player.startConversation {
        npc(
            npc,
            CALM_TALK,
            "Stop, creature! Oh it's you. Well, what have you got for us, then?"
        )

        player(
            CALM_TALK,
            "How about this?"
        )

        item(
            Obj.rockcake,
            "You give the guard a rock cake."
        )

        npc(
            npc,
            CALM_TALK,
            "Well, well, look at this. My favourite: rock cake! Okay, we will let it through."
        ) {
            player.inventory.deleteItem(Obj.rockcake, 1)

            val questAttribs =
                player.questManager.getAttribs(Quest.WATCHTOWER)

            questAttribs.setB(
                MARKET_GIFT_ACCEPTED,
                true
            )

            if (player.x > 2507) {
                player.walkToAndExecute(Tile.of(2508, 3011, 0)) {
                    player.resetWalkSteps()
                    player.faceTile(Tile.of(2506, 3011, 0))
                    Agility.handleObstacle(player, Seq.trollheim_wall_kit_climb_over,
                        2, Tile.of(2506, 3011, 0), 1.0)
                }
            }
        }
    }
}

private fun handleTollBridgeGuard(player: Player, npc: NPC) {
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_GET_SKAVID_MAP)
        return

    player.startConversation {
        npc(
            npc,
            CALM_TALK,
            "What do you want, small thing? Leave us alone!"
        )
    }
}

private fun handleBrokenBridge(player: Player, start: Tile) {
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_GET_SKAVID_MAP)
        return

    if (player.skills.getLevel(Skills.AGILITY) < BROKEN_BRIDGE_AGILITY_LEVEL) {
        player.startConversation {
            npc(
                Npc.ogre_guard4,
                CALM_TALK,
                "Come back when you've got the moves!"
            )
        }
        return
    }

    if (start == BROKEN_BRIDGE_NORTH) {
        jumpBrokenBridge(player, start, false)
        return
    }

    if (player.questManager.getAttribs(Quest.WATCHTOWER).getB(BRIDGE_TOLL_PAID)) {
        jumpBrokenBridge(player, start, false)
        return
    }

    player.startConversation {
        npc(
            Npc.ogre_guard4,
            CALM_TALK,
            "Oi! Little thing. If you want to cross here, you must pay me 20 gold pieces first!"
        )

        player(
            CONFUSED,
            "You want me to give you 20 gold pieces to let me jump off a bridge?"
        )

        npc(
            Npc.ogre_guard4,
            CALM_TALK,
            "That's what I said, like it or lump it."
        )

        options {
            op("Okay, I'll pay it.") {
                player(
                    CALM_TALK,
                    "Okay, I'll pay it."
                )

                if (!player.inventory.containsItem(Obj.coins, BRIDGE_TOLL)) {
                    player.sendMessage(
                        "You don't have enough gold pieces."
                    )
                    return@op
                }

                npc(
                    Npc.ogre_guard4,
                    CALM_TALK,
                    "A wise choice, little thing."
                ) {
                    jumpBrokenBridge(player, start, true)
                }

                player(
                    CALM_TALK,
                    "Phew! I just made it."
                )
            }

            op("Forget it, I'm not paying.") {
                player(
                    CALM_TALK,
                    "Forget it, I'm not paying."
                )

                npc(
                    Npc.ogre_guard4,
                    CALM_TALK,
                    "In that case you're not crossing."
                )
            }
        }
    }
}

private fun jumpBrokenBridge(player: Player, start: Tile, pay: Boolean) {
    if (player.isLocked || player.tile != start || player.getQuestStage(Quest.WATCHTOWER) < STAGE_GET_SKAVID_MAP)
        return
    if (!Agility.hasLevel(player, BROKEN_BRIDGE_AGILITY_LEVEL))
        return
    val returning = start == BROKEN_BRIDGE_NORTH
    val destination = if (returning) BROKEN_BRIDGE_SOUTH else BROKEN_BRIDGE_NORTH
    if (!World.floorAndWallsFree(destination, 1)) {
        player.sendMessage("The other side of the gap is blocked.")
        return
    }
    val attribs = player.questManager.getAttribs(Quest.WATCHTOWER)
    if (!returning && !attribs.getB(BRIDGE_TOLL_PAID)) {
        if (!pay || !player.inventory.containsItem(Obj.coins, BRIDGE_TOLL)) {
            player.sendMessage("You don't have enough gold pieces.")
            return
        }
        player.inventory.deleteItem(Obj.coins, BRIDGE_TOLL)
        attribs.setB(BRIDGE_TOLL_PAID, true)
    }
    player.forceMove(destination, Seq.human_longjump, 0, 60) {
        if (returning) {
            attribs.setB(BRIDGE_TOLL_PAID, false)
            player.startConversation {
                player(CALM_TALK, "I'm glad that was easier on the way back!")
            }
        } else {
            player.skills.addXp(Constants.AGILITY, 5.0)
            player.sendMessage("You daringly jump across the chasm.")
        }
    }
}

private fun handleShamanCaveGuard(player: Player, npc: NPC) {
    recoverWatchtowerSkavidProgress(player)
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_FIND_SHAMAN_ENCLAVE)
        return

    player.startConversation {
        npc(
            npc,
            CALM_TALK,
            "What do you want?"
        )

        options {
            op("I want to go in there.") {
                player(
                    CALM_TALK,
                    "I want to go in there."
                )

                npc(
                    npc,
                    CALM_TALK,
                    "Oh you do, do you? How about 'no'?"
                ) {
                    npc.setCombatTarget(player)
                }
            }

            op("I want to rid the world of ogres!") {
                player(
                    CALM_TALK,
                    "I want to rid the world of ogres."
                )

                npc(
                    npc,
                    CALM_TALK,
                    "You dare mock me, creature!"
                ) {
                    npc.setCombatTarget(player)
                }
            }
        }
    }
}

private fun handleNightshadeOnShamanCaveGuard(
    player: Player,
    npc: NPC
) {
    recoverWatchtowerSkavidProgress(player)
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_FIND_SHAMAN_ENCLAVE)
        return

    if (!player.inventory.containsItem(Obj.nightshade, 1))
        return

    player.startConversation {
        npc(
            npc,
            CALM_TALK,
            "What is this? Arrrrgh! I cannot stand this plant! Argh, it burns! It burns!"
        )
        options("You can run past, but keep in mind that a dangerous area follows!") {
            op("Proceed regardless") {
                simple("You run past the guard while he's busy.") {
                    if (player.inventory.containsItem(Obj.nightshade, 1)) {
                        player.inventory.deleteItem(Obj.nightshade, 1)
                        player.set(SHAMAN_CAVE_GUARD_DISTRACTED, true)
                    }
                }
            }
            op("Stay out")
        }
    }
}

private fun ejectPlayerFromGuTanoth(player: Player) {
    val damage =
        (player.hitpoints * 0.05).toInt().coerceAtLeast(1)

    player.fadeScreen {
        player.tele(GUARD_PUNCH_DESTINATION)

        player.applyHit(
            Hit(
                player,
                damage,
                Hit.HitLook.TRUE_DAMAGE
            )
        )

        player.tempAttribs.setB(
            GU_TANOTH_EJECTION_GROAN,
            true
        )
    }
}

private fun handleOgreTrader(player: Player, npc: NPC) {
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_GET_SKAVID_MAP)
        return

    player.startConversation {
        npc(
            npc,
            CALM_TALK,
            "Arr, small thing wants my food, does it? I'll teach you to deal with ogres!"
        ) {
            npc.setCombatTarget(player)
        }
    }
}
