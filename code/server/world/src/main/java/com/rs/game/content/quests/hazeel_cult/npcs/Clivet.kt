package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.ownedNpc
import com.rs.plugin.kts.walkToAndExecute

private val CLIVET_SPAWN_TILE = Tile.of(2567, 9683, 0)
private val CLIVET_RAFT_TILE = Tile.of(2567, 9679, 0)

@ServerStartupEvent
fun mapClivet() {
    onNpcClick(Npc.clivet_hazeel_cultist) { (player, npc) ->
        if (npc !is OwnedNPC || npc.owner !== player) return@onNpcClick
        clivetDialogue(player, npc)
    }
    onChunkEnter { (entity) ->
        val player = entity as? Player ?: return@onChunkEnter
        ensureClivetSpawned(player)
    }
}

private fun ensureClivetSpawned(player: Player) {
    if (!player.withinDistance(CLIVET_SPAWN_TILE, 14) || !shouldShowClivet(player) || findOwnedClivet(player) != null) return
    ownedNpc(player, Npc.clivet_hazeel_cultist, CLIVET_SPAWN_TILE) {
        setRandomWalk(false)
        isIgnoreNPCClipping = true
    }
}

private fun shouldShowClivet(player: Player): Boolean {
    val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
    return stage <= STAGE_CHOOSE_SIDE || player.isCultSide()
}

private fun findOwnedClivet(player: Player): OwnedNPC? =
    World.getNPCsInChunkRange(CLIVET_SPAWN_TILE.getChunkId(), 2)
        .filterIsInstance<OwnedNPC>()
        .firstOrNull { it.id == Npc.clivet_hazeel_cultist && it.owner === player && !it.hasFinished() }

private fun departOnRaft(player: Player, npc: NPC) {
    player.setSide(SIDE_CARNILLEAN)
    player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_BRANCH_TASK)
    npc.resetWalkSteps()
    npc.setRandomWalk(false)
    npc.setCantInteract(true)
    npc.walkToAndExecute(
        CLIVET_RAFT_TILE,
        onNearest = {
            player.fadeScreen {
                npc.finish()
            }
            true
        },
        onReached = {
            player.fadeScreen {
                npc.finish()
            }
        }
    )
}

private fun clivetDialogue(player: Player, npc: NPC) {
    val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
    when {
        stage == STAGE_UNSTARTED -> player.startConversation {
            npc(npc, HeadE.FRUSTRATED, "You mind your business, I'll mind mine.")
        }
        stage in STAGE_FIND_CLIVET..STAGE_CHOOSE_SIDE -> clivetChoiceDialogue(player, npc)
        player.isCultSide() && stage == STAGE_BRANCH_TASK -> player.startConversation {
            player(HeadE.CALM_TALK, "Hello there.")
            npc(npc, HeadE.FRUSTRATED, "You have a mission with us. Go to the Carnillean household and poison their meal to prove your loyalty.")
            if (!player.inventory.containsOneItem(Obj.poison)) {
                player(HeadE.WORRIED, "I need some more poison.")
                npc(npc, HeadE.ANGRY, "Fool! Be more careful with it this time.") {
                    giveItem(player, Obj.poison, "Clivet gives you some poison.")
                }
            }
        }
        player.isCultSide() && stage == STAGE_BRANCH_PROGRESS -> giveMarkDialogue(player, npc)
        player.isCultSide() && stage >= STAGE_ENTER_HIDEOUT -> player.startConversation {
            player(HeadE.CALM_TALK, "Hello.")
            if (stage >= STAGE_RETURN_SCROLL && player.inventory.containsOneItem(Obj.hazeel_scroll)) {
                npc(npc, HeadE.HAPPY_TALKING, "At last! Quickly, take the scroll to Alomone so that we may begin the ritual!")
            } else {
                npc(npc, HeadE.CALM_TALK, "All we need is Hazeel's magical scroll to bring him forth once more to this world.")
            }
            if (!player.inventory.containsOneItem(Obj.mark_of_hazeel) && !player.bank.containsItem(Obj.mark_of_hazeel)) {
                options {
                    op("I've lost my Mark of Hazeel.") {
                        npc(npc, HeadE.CALM_TALK, "Here, take this.") { giveItem(player, Obj.mark_of_hazeel, "Clivet gives you another Mark of Hazeel.") }
                    }
                    op("All hail Hazeel!") { npc(npc, HeadE.HAPPY_TALKING, "Indeed, all hail Hazeel and Zamorak!") }
                }
            }
        }
        !player.isCultSide() -> player.startConversation {
            player(HeadE.CALM_TALK, "Hello.")
            npc(npc, HeadE.ANGRY, if (stage == STAGE_COMPLETE) "You may have won this battle, meddler, but when Hazeel returns your destruction is assured." else "Oh no... not you again. You will never find our hideout!")
        }
    }
}

private fun clivetChoiceDialogue(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.CALM_TALK, "Do you know the Carnilleans?")
        npc(npc, HeadE.FRUSTRATED, "You mind your business, I'll mind mine.")
        player(HeadE.FRUSTRATED, "Look, I KNOW you're hiding something. I've heard there's a cult hideout down here.")
        npc(npc, HeadE.ANGRY, "If you want to stay healthy you'll leave now.")
        player(HeadE.CALM_TALK, "I have my orders.")
        npc(npc, HeadE.FRUSTRATED, "That two-faced snob has made you fall for his propaganda. There is a lot more to the Carnilleans than meets the eye. Do you want to know the truth?")
        options {
            op("What do you mean?") {
                npc(npc, HeadE.CALM_TALK, "The Carnillean home was built by Lord Hazeel, one of the Mahjarrat followers of Zamorak.")
                npc(npc, HeadE.ANGRY, "Saradominists stormed his home, tortured and butchered his loyal followers, then the Carnilleans claimed it as their own.")
                npc(npc, HeadE.CALM_TALK, "Lord Hazeel prepared for his return. Join us and assist in his restoration, and you will be bestowed with treasures.")
                options {
                    op("I will never help you.") {
                        player(HeadE.ANGRY, "I will never help you.")
                        npc(npc, HeadE.ANGRY, "Then you are a fool. You will never know the glories you could have tasted as one of us!")
                        simple("Clivet jumps onto the raft and pushes off down into the sewer system.") {
                            departOnRaft(player, npc)
                        }
                    }
                    op("So, what would I have to do?") {
                        player(HeadE.CONFUSED, "So what would I have to do?")
                        npc(npc, HeadE.CALM_TALK, "You must prove your loyalty. Killing one of the Carnillean household should be sufficient proof.")
                        options {
                            op("No. I won't do it.") {
                                player(HeadE.ANGRY, "No. I won't do it.")
                                npc(npc, HeadE.ANGRY, "Then you are a fool!") { departOnRaft(player, npc) }
                            }
                            op("Okay, count me in.") {
                                player(HeadE.CALM_TALK, "Okay, count me in.")
                                if (!player.inventory.hasFreeSlots()) {
                                    npc(npc, HeadE.FRUSTRATED, "Clear some space in your inventory, and I shall tell you.")
                                } else {
                                    npc(npc, HeadE.HAPPY_TALKING, "Excellent. It takes a rare character to see through the cursed Carnillean lies.")
                                    item(Obj.poison, "Clivet gives you some poison.") {
                                        player.setSide(SIDE_CULT)
                                        player.inventory.addItem(Obj.poison)
                                        player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_BRANCH_TASK)
                                    }
                                    npc(npc, HeadE.CALM_TALK, "Pour it into their food and, once the deed is done, return here.")
                                }
                            }
                        }
                    }
                }
            }
            op("Not just now, thanks.") {
                player(HeadE.CALM_TALK, "Not just now, thanks.")
                npc(npc, HeadE.ANGRY, "Then leave, fool!") { player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_CHOOSE_SIDE) }
            }
        }
    }
}

private fun giveMarkDialogue(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.CALM_TALK, "I poured the poison into the Carnilleans' meal as requested.")
        npc(npc, HeadE.CALM_TALK, "We heard all about it. The food was fed to the family dog rather than one of the family members.")
        npc(npc, HeadE.HAPPY_TALKING, "Nonetheless, you have proved your loyalty and will be rewarded when Hazeel is revived!")
        player(HeadE.CONFUSED, "So what's next?")
        val hasMark = player.inventory.containsOneItem(Obj.mark_of_hazeel) || player.bank.containsItem(Obj.mark_of_hazeel)
        if (!player.inventory.hasFreeSlots() && !hasMark) {
            npc(npc, HeadE.FRUSTRATED, "I have something to give you. Clear a bit of space in your inventory.")
        } else {
            npc(npc, HeadE.CALM_TALK, "Wear this amulet. It is the Mark of Hazeel, and the key to finding our hideout.")
            item(Obj.mark_of_hazeel, "Clivet gives you the Mark of Hazeel.") {
                if (!hasMark) player.inventory.addItem(Obj.mark_of_hazeel)
                player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_ENTER_HIDEOUT)
            }
            npc(npc, HeadE.CALM_TALK, "Set each sewer valve to follow the amulet's design, then take the raft to Alomone.")
        }
    }
}
