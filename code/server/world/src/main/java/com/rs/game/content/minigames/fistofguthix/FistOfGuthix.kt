package com.rs.game.content.minigames.fistofguthix

import com.rs.engine.book.Book
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

private const val BANDAGE_HEAL = 150
private const val BANDAGE_DELAY = 3
private const val STAIRS_TICKS = 2
private val DRUIDS = arrayOf<Any>(Npc.hvh_druid1, Npc.hvh_druid2, Npc.hvh_druid3)

@ServerStartupEvent
fun mapFistOfGuthix() {
    onObjectClick(Loc.fist_of_guthix_portal) { (player) ->
        player.useStairs(-1, FOG_LOBBY_ARRIVAL, 1, STAIRS_TICKS)
        WorldTasks.schedule(STAIRS_TICKS) { enterLobby(player) }
    }

    onObjectClick(Loc.hvh_cave_exit) { (player) ->
        if (player.fogLobbyController != null) player.controllerManager.forceStop()
        player.useStairs(-1, GROTTO_RETURN, 1, STAIRS_TICKS)
    }

    onObjectClick(Loc.hvh_cave_passageway) { (player) ->
        enterLobby(player)
        if (inFogWaitingRoom(player.tile)) FogQueue.leaveWaitingRoom(player) else FogQueue.enterWaitingRoom(player)
    }

    onObjectClick(Loc.hvh_scoreboard) { (player) ->
        val results = FogQueue.results.ifEmpty { listOf("No matches have been completed yet.") }
        object : Book("Fist of Guthix results", results.joinToString("<br>")) {}.open(player)
    }

    onItemClick(Obj.hvh_food, options = arrayOf("Heal")) { (player, item) ->
        if (player.fogMatch == null) return@onItemClick player.sendMessage("You can only use these bandages in the arena.")
        if (!player.canEat()) return@onItemClick
        player.inventory.deleteItem(item.slot, item)
        player.anim(Seq.human_eat)
        player.heal(BANDAGE_HEAL)
        player.addFoodDelay(BANDAGE_DELAY)
        player.sendMessage("You apply the bandages.")
    }

    onItemClick(Obj.hvh_teleport_tablet, options = arrayOf("Teleport")) { (player, item) ->
        player.fogMatch?.useOrb(player, item.slot) ?: player.sendMessage("The tele-orb only works inside the arena.")
    }

    onNpcClick(Npc.npc_7600) { (player, npc) -> player.startConversation {
        npc(npc.id, HeadE.CALM_TALK, "Welcome to the Fist of Guthix. Here the energy of Guthix's fist still seeps from the ground, and those brave enough gather it.")
        options {
            op("How does it work?") {
                npc(npc.id, HeadE.CALM_TALK, "Go through the passageway to the waiting room. A minute after $LOBBY_MINIMUM people are waiting, or straight away once there are $LOBBY_FULL, everyone is paired against an opponent.")
                npc(npc.id, HeadE.CALM_TALK, "Each match has two rounds. In one you are the hunted: take a stone from a dispenser and wield it to gather charge. The nearer the centre, the faster it charges, and the more it hurts.")
                npc(npc.id, HeadE.CALM_TALK, "In the other you are the hunter and must kill your opponent before they gather $CHARGE_CAP charges or ten minutes pass. Whoever gathers more charge wins; a tie is a loss for both of you.")
                npc(npc.id, HeadE.CALM_TALK, "The hunted may hide in the houses, but they hurt to stay in, and you must wait before entering another. The portal inside costs $PORTAL_COST charges and may not take you where you hoped.")
                npc(npc.id, HeadE.CALM_TALK, "You'll be given five bandages, 300 catalytic runes, 1,000 elemental runes and a tele-orb. They must last you both rounds. Food, potions and runes aren't allowed in.")
            }
            op("What's my rating?") { npc(npc.id, HeadE.CALM_TALK, "Your rating is ${FogResults.rating(player)}. It rises when you win and falls when you lose. Tokens for Reggie's shop are earned separately.") }
            op("Goodbye.")
        }
    } }

    onNpcClick(Npc.hvh_shopkeeper) { (player, npc, option) ->
        when (option) {
            "Trade" -> FogRewards.open(player)
            else -> player.startConversation {
                npc(npc.id, HeadE.CHEERFUL, "Hello! I trade Fist of Guthix tokens for rewards, and I'll recharge any of my goods that have run low.")
                options {
                    opExec("Let's see what you have.") { FogRewards.open(player) }
                    opExec("Can you recharge my items?") { FogRewards.offerRecharge(player, npc.id) }
                    op("No thanks.")
                }
            }
        }
    }

    onNpcClick(*DRUIDS) { (player, npc) -> player.startConversation {
        npc(npc.id, HeadE.CALM_TALK, "We druids watch over the fist. Gather its energy wisely: the closer you stand to its heart, the more it gives and the more it takes from you.")
    } }

    onLogin { (player) ->
        if (player.fogController != null) return@onLogin
        MATCH_ITEMS.forEach {
            player.inventory.deleteItem(it, Int.MAX_VALUE)
            player.equipment.deleteItem(it, Int.MAX_VALUE)
        }
        if (player.controllerManager.controller == null && (inFogLobby(player.tile) || inFogWaitingRoom(player.tile))) enterLobby(player)
    }
}

private fun enterLobby(player: Player) {
    if (player.fogLobbyController == null && player.fogController == null) player.controllerManager.startController(FogLobbyController())
}
