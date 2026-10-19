package com.rs.game.content.minigames.barbarianassault

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.minigames.barbarianassault.penance.PenanceHealer
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onItemOnPlayer
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

private val PENANCE_HEALERS = arrayOf<Any>(
    Npc.npc_5238, Npc.npc_5239, Npc.npc_5240, Npc.npc_5241, Npc.npc_5242, Npc.npc_5243, Npc.npc_5244, Npc.npc_5245, Npc.npc_5246,
)
private val HOPPERS = arrayOf<Any>(Loc.loc_20267, Loc.loc_20264, Loc.loc_20265, Loc.loc_20266)
private val DISPENSERS = arrayOf<Any>(Loc.loc_20241, Loc.loc_20242, Loc.loc_20243)
private val HORN_OPTIONS = arrayOf(
    "Tell-red", "Tell-green", "Tell-blue", "Tell-tofu", "Tell-crackers", "Tell-worms", "Tell-meat",
    "Tell-style1", "Tell-style2", "Tell-style3", "Tell-style4", "Medic",
)

@ServerStartupEvent
fun mapBarbarianAssault() {
    onObjectClick(Loc.barbassault_recruitment_entrance) { (player) -> player.useStairs(Seq.human_reachforladder, LOBBY_ARRIVAL, 1, 2) }
    onObjectClick(Loc.barbassault_recruitment_exit) { (player) -> player.useStairs(Seq.human_reachforladder, OUTPOST_LADDER_EXIT, 1, 2) }

    onObjectClick(*WaitingRoom.entries.map { it.door }.toTypedArray()) { (player, obj) ->
        val room = WaitingRoom.forDoor(obj.id) ?: return@onObjectClick
        if (room.contains(player.tile)) {
            player.controllerManager.forceStop()
            player.useStairs(-1, room.outside, 0, 1)
        } else {
            player.useStairs(-1, room.inside, 0, 1)
            BarbarianAssaultWaiting.enterRoom(player, room)
        }
    }
    onObjectClick(Loc.barbassault_door_no_friends) { (player) ->
        player.sendMessage("The quick-start room is closed. Take a scroll in any wave room and recruit a team with it.")
    }
    onObjectClick(Loc.barbassault_scroll_table) { (player) -> BarbarianAssaultTeams.ensureScroll(player) }
    onObjectClick(Loc.barbassault_game_entrance) { (player, obj) ->
        val room = WaitingRoom.forLadder(obj.tile) ?: return@onObjectClick
        BarbarianAssaultWaiting.climbDown(player, room)
    }
    onObjectClick(Loc.barbassault_game_info_scroll, Loc.barbassault_blackboard) { (player) -> explain(player) }

    onItemClick(Obj.barbassault_scroll, options = arrayOf("Read", "Write-Role", "Clear")) { (player, _, option) ->
        val room = WaitingRoom.containing(player.tile)
        when (option) {
            "Read" -> BarbarianAssaultTeams.readScroll(player)
            "Write-Role" -> if (room == null) player.sendMessage("You need to be in a waiting room to set up a team.") else BarbarianAssaultTeams.writeRole(player, room)
            "Clear" -> {
                val team = BarbarianAssaultTeams.of(player)
                when {
                    team == null -> player.sendMessage("You aren't on a team.")
                    team.game != null -> player.sendMessage("You can't do that during a wave.")
                    team.leader == player -> BarbarianAssaultTeams.disband(team)
                    else -> BarbarianAssaultTeams.leave(player)
                }
            }
        }
    }
    onButtonClick(Interface.interface_493) { e -> BarbarianAssaultTeams.handleFollowerButton(e.player, e.component) }
    onButtonClick(Interface.interface_492) { e -> BarbarianAssaultTeams.handleLeaderButton(e.player, e.component) }
    onButtonClick(Interface.interface_497) { e -> e.player.closeInterfaces() }

    onItemClick(*Role.allHorns.toTypedArray(), options = HORN_OPTIONS) { (player, _, option) -> player.baGame?.horn(player, option) }
    onItemClick(*BAIT_OBJS.toTypedArray(), options = arrayOf("Drop")) { (player, item) ->
        player.baGame?.dropBait(player, item) ?: player.inventory.deleteItem(item)
    }
    onItemClick(*COLLECTION_BAGS.toTypedArray(), options = arrayOf("Look-in", "Empty")) { (player, _, option) ->
        val game = player.baGame ?: return@onItemClick
        if (option == "Empty") game.emptyBag(player) else game.lookInBag(player)
    }

    onObjectClick(*DISPENSERS) { (player, obj, option) -> player.baGame?.dispense(player, obj, option) }
    onObjectClick(Loc.barassault_vending_machine_collector) { (player) -> player.baGame?.convertEggs(player) }
    onObjectClick(*HOPPERS) { (player, obj, option) ->
        val game = player.baGame ?: return@onObjectClick
        val cannon = game.cannonAt(obj) ?: return@onObjectClick
        if (option == "Load") game.loadHopper(player, cannon) else game.lookInHopper(player, cannon)
    }
    onObjectClick(Loc.barbassault_turret_bottom) { (player, obj) ->
        val game = player.baGame ?: return@onObjectClick
        game.cannonAt(obj)?.let { game.shoot(player, it) }
    }
    onObjectClick(Loc.barbassault_trap_1use, Loc.barbassault_trap_broken) { (player, obj) -> player.baGame?.fixTrap(player, obj) }
    onObjectClick(Loc.loc_20150) { (player, _, option) -> player.baGame?.spring(player, option) }
    onObjectClick(Loc.loc_20247) { (player) -> player.baGame?.hornOfGlory(player) }
    onObjectClick(Loc.barbassault_game_exit) { (player) ->
        if (player.baGame == null) return@onObjectClick
        player.controllerManager.forceStop()
        player.useStairs(Seq.human_reachforladder, LOBBY_ARRIVAL, 1, 2)
    }
    onObjectClick(Loc.barbassault_cave_big_no_barricade) { (player) -> player.sendMessage("The cave entrance is too wide to block.") }
    onObjectClick(Loc.loc_20250) { (player) -> player.baGame?.takeSpikes(player) }

    onItemOnNpc(*PENANCE_HEALERS) { e ->
        val npc = e.npc as? PenanceHealer ?: return@onItemOnNpc
        if (e.item.id in POISON_OBJS) e.player.baGame?.poisonHealer(e.player, e.item, npc)
    }
    onItemOnPlayer(*VIALS.toTypedArray()) { e -> e.player.baGame?.healWithVial(e.player, e.item, e.target) }
    onItemOnPlayer(*QUEEN_EGG_OBJS.toTypedArray()) { e -> e.player.baGame?.passEgg(e.player, e.item, e.target) }
    onItemOnObject(
        arrayOf(Loc.barbassault_crater_poision, Loc.barbassault_crater_poision_big, Loc.barbassault_crater_lava, Loc.barbassault_crater_lava_big),
        arrayOf(Obj.barbassault_egg_04, Obj.obj_10536),
    ) { (player, obj, item) -> player.baGame?.processQueenEgg(player, item, obj) }
    onItemOnObject(HOPPERS, arrayOf(*EGG_OBJS.toTypedArray(), Obj.barbassault_egg_heated_poison_spikes)) { (player, obj) ->
        val game = player.baGame ?: return@onItemOnObject
        game.cannonAt(obj)?.let { game.loadHopper(player, it) }
    }
    onItemOnItem(intArrayOf(Obj.barbassault_egg_poison), intArrayOf(Obj.barbassault_spikes)) { e ->
        val egg = e.getUsedWith(Obj.barbassault_spikes)?.takeIf { it.id == Obj.barbassault_egg_poison } ?: e.item1.takeIf { it.id == Obj.barbassault_egg_poison } ?: e.item2
        e.player.baGame?.processQueenEgg(e.player, egg, null)
    }

    onNpcClick(Npc.barbassault_barbarian_captain) { (player, npc, option) ->
        when (option) {
            "Tutorial" -> explain(player)
            else -> player.startConversation {
                npc(npc.id, HeadE.CALM_TALK, "Welcome to Barbarian Assault! The Penance are swarming out of the caves and we need teams of five to push them back.")
                npc(npc.id, HeadE.CALM_TALK, "Climb down the ladder, step into the room for the wave you want, take a scroll from the table and use it on the players you want to recruit.")
                npc(npc.id, HeadE.CALM_TALK, "When your team has an Attacker, a Defender, a Collector and a Healer, the leader climbs down the ladder in the room to start.")
            }
        }
    }
    onNpcClick(Npc.barbassault_barbarian_commander) { (player, npc, option) ->
        when (option) {
            "Get-rewards" -> BarbarianAssaultRewards.open(player)
            "Reset-wave" -> player.startConversation {
                npc(npc.id, HeadE.CALM_TALK, "You have completed up to wave ${player.highestBarbarianAssaultWave}. Every team starts at wave 1 unless its leader has fought further.")
            }
            else -> player.startConversation {
                npc(npc.id, HeadE.CALM_TALK, "I reward those who fight the Penance with honour points. Spend them with me to improve your roles or buy Penance equipment.")
                options {
                    opExec("Show me the rewards.") { BarbarianAssaultRewards.open(player) }
                    op("Not now.")
                }
            }
        }
    }

    onLogin { (player) ->
        if (player.baGameController == null) ARENA_ONLY_OBJS.forEach { player.inventory.deleteItem(it, Int.MAX_VALUE) }
    }
}

private fun explain(player: Player) = player.startConversation {
    simple("Attackers kill Fighters and Rangers using the style their Collector calls. Collectors pick up the egg colour their Attacker calls and load the cannons.")
    simple("Defenders lure Runners onto the traps with the food their Healer calls. Healers poison Penance Healers with the food their Defender calls and heal the team.")
    simple("Everyone calls for their partner with their horn every thirty seconds. Wave 10 ends with the Penance Queen, who can only be hurt by omega eggs.")
}
