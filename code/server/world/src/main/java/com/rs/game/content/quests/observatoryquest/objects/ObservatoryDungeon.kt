package com.rs.game.content.quests.observatoryquest

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile

private const val KEY_CHEST_ATTR = "observatoryQuestKeyChest"
private const val SEARCHED_CHESTS_ATTR = "observatoryQuestSearchedChests"
private const val GUARD_AWAKE_ATTR = "observatoryQuestGuardAwake"
private const val GUARD_DEFEATED_ATTR = "observatoryQuestGuardDefeated"
private const val KITCHEN_UNLOCKED_ATTR = "observatoryQuestKitchenUnlocked"

private val GOBLIN_GUARD_TILE = Tile.of(2327, 9394, 0)

private val GOBLIN_STOVE_TILE = Tile.of(2326, 9389, 0)

private val KITCHEN_GATE_TILES = arrayOf(Tile.of(2327, 9393, 0), Tile.of(2326, 9393, 0))

private class ObservatoryChest(val closed: Int, val open: Int, val tile: Tile)

private val DUNGEON_CHESTS = listOf(
    ObservatoryChest(Loc.qip_obs_keep_chest_closed, Loc.qip_obs_keep_chest_open, Tile.of(2333, 9405, 0)),
    ObservatoryChest(Loc.qip_obs_keep_chest_closed, Loc.qip_obs_keep_chest_open, Tile.of(2312, 9400, 0)),
    ObservatoryChest(Loc.qip_obs_keep_chest_closed, Loc.qip_obs_keep_chest_open, Tile.of(2310, 9374, 0)),
    ObservatoryChest(Loc.qip_obs_keep_chest_closed, Loc.qip_obs_keep_chest_open, Tile.of(2326, 9360, 0)),
    ObservatoryChest(Loc.qip_obs_dungeon_chest_closed3, Loc.qip_obs_dungeon_chest_open3, Tile.of(2351, 9361, 0)),
    ObservatoryChest(Loc.qip_obs_dungeon_chest_closed, Loc.qip_obs_dungeon_chest_open, Tile.of(2364, 9355, 0)),
    ObservatoryChest(Loc.qip_obs_dungeon_chest_closed2, Loc.qip_obs_dungeon_chest_open2, Tile.of(2360, 9366, 0)),
    ObservatoryChest(Loc.shutdungeonchest, Loc.opendungeonchest, Tile.of(2359, 9376, 0)),
    ObservatoryChest(Loc.qip_obs_keep_chest_closed, Loc.qip_obs_keep_chest_open, Tile.of(2356, 9380, 0)),
    ObservatoryChest(Loc.qip_obs_keep_chest_closed, Loc.qip_obs_keep_chest_open, Tile.of(2348, 9383, 0)),
    ObservatoryChest(Loc.qip_obs_keep_chest_closed, Loc.qip_obs_keep_chest_open, Tile.of(2335, 9374, 0))
)

@ServerStartupEvent
fun mapObservatoryDungeon() {
    instantiateNpc(Npc.qip_obs_goblin_guard) { id, tile ->
        object : NPC(id, tile, false) {
            init {
                setRandomWalk(false)
                setKeepsOwnFacing(true)
                faceDir(Direction.NORTH)
            }
            override fun getRespawnDirection(): Int = Direction.NORTH.angle
            override fun withinDistance(player: Player, distance: Int): Boolean =
                !player.getBool(GUARD_AWAKE_ATTR) && !player.getBool(GUARD_DEFEATED_ATTR) && super.withinDistance(player, distance)
        }
    }
    onLogin { (player) ->
        if (player.getBool(GUARD_AWAKE_ATTR) && !player.getBool(GUARD_DEFEATED_ATTR)) {
            player.set(GUARD_AWAKE_ATTR, false)
        }
    }

    DUNGEON_CHESTS.forEachIndexed { chestIndex, chest ->
        onObjectClick(chest.closed, chest.open, tiles = arrayOf(chest.tile)) { (player, obj, option) ->
            when (option) {
                "Open" -> {
                    player.faceObject(obj)
                    player.anim(Seq.human_openchest)
                    player.lock(2)
                    obj.setIdTemporary(chest.open, Ticks.fromSeconds(30))
                    player.sendMessage("You open the chest.")
                }
                "Close" -> {
                    player.faceObject(obj)
                    player.anim(Seq.human_closechest)
                    player.lock(2)
                    obj.changeId(chest.closed)
                    player.sendMessage("You close the chest.")
                    return@onObjectClick
                }
                "Search" -> player.faceObject(obj)
                else -> return@onObjectClick
            }
            if (player.getQuestStage(Quest.OBSERVATORY_QUEST) < STAGE_FINDING_LENS_MOULD) {
                player.sendMessage("You find nothing interesting.")
                return@onObjectClick
            }
            searchObservatoryChest(player, chestIndex)
        }
    }

    onNpcClick(Npc.qip_obs_goblin_guard) { (player, npc) ->
        if (!npc.tile.matches(GOBLIN_GUARD_TILE))
            return@onNpcClick
        if (player.getBool(GUARD_DEFEATED_ATTR))
            return@onNpcClick
        if (player.getBool(GUARD_AWAKE_ATTR)) {
            player.sendMessage("The goblin guard is already awake.")
            return@onNpcClick
        }
        player.startConversation {
            npc(Npc.qip_obs_goblin_guard, HeadE.ANGRY, "Oi, how dare you wake me up!") {
                wakeObservatoryGuard(player)
            }
        }
    }

    onNpcDeath(Npc.goblin_guard) { e ->
        val guard = e.npc as? OwnedNPC ?: return@onNpcDeath
        val player = guard.owner
        if (!player.getBool(GUARD_AWAKE_ATTR))
            return@onNpcDeath
        player.set(GUARD_AWAKE_ATTR, false)
        player.set(GUARD_DEFEATED_ATTR, true)
    }

    onObjectClick(Loc.keepgate_closed, Loc.keepgate_closed_left, tiles = KITCHEN_GATE_TILES) { (player, obj) ->
        if (player.getBool(KITCHEN_UNLOCKED_ATTR)) {
            Doors.handleDoubleDoor(player, obj)
            return@onObjectClick
        }
        val guard = World.getNPCsInChunkRange(player.chunkId, 6).filterIsInstance<OwnedNPC>()
            .firstOrNull { it.id == Npc.goblin_guard && it.owner == player }
        val guardLuredAway = player.getBool(GUARD_AWAKE_ATTR) && guard != null && !guard.tile.within(GOBLIN_GUARD_TILE, 4)
        if (!player.getBool(GUARD_DEFEATED_ATTR) && !guardLuredAway) {
            player.sendMessage("If you open the gate, the guard will hear you. You need to get rid of him.")
            return@onObjectClick
        }
        if (!player.inventory.containsItem(Obj.keep_key, 1)) {
            player.sendMessage("The gate is locked.")
            return@onObjectClick
        }
        player.inventory.deleteItem(Obj.keep_key, 1)
        player.set(KITCHEN_UNLOCKED_ATTR, true)
        player.vars.setVarBit(Varbit.observatory_gatelock, 1)
        player.sendMessage("The gate unlocks.")
        player.sendMessage("The key is useless now. You discard it.")
        player.startConversation {
            player(HeadE.CALM_TALK, "I had better be quick, there may be more guards about.") {
                Doors.handleDoubleDoor(player, obj)
            }
        }
    }

    onObjectClick(Loc.loc_25442, Loc.qip_obs_dungeon_stove, Loc.qip_obs_dungeon_stove_empty, tiles = arrayOf(GOBLIN_STOVE_TILE)) { (player) ->
        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) !in STAGE_FINDING_LENS_MOULD..STAGE_MAKING_LENS || !player.getBool(KITCHEN_UNLOCKED_ATTR)) {
            player.sendMessage("Just a plain stove. Nothing there.")
            return@onObjectClick
        }
        if (player.inventory.containsItem(Obj.lens_mould, 1) || player.bank.containsItem(Obj.lens_mould, 1) || player.inventory.containsItem(Obj.lens, 1) || player.bank.containsItem(Obj.lens, 1)) {
            player.sendMessage("Just a plain stove. Nothing there.")
            return@onObjectClick
        }
        player.sendMessage("The goblins appear to have been using the lens mould to cook their stew!")
        player.sendMessage("You shake out its contents and take it with you.")
        player.inventory.addItemDrop(Obj.lens_mould, 1)
        player.vars.setVarBit(Varbit.observatory_mould_pres, 1)
        player.startConversation {
            player(HeadE.CALM_TALK, "Euuuw, that smells awful!")
        }
    }
}

private fun searchObservatoryChest(player: Player, chestIndex: Int) {
    var keyChest = player.getI(KEY_CHEST_ATTR)
    if (keyChest <= 0) {
        keyChest = Utils.random(DUNGEON_CHESTS.size) + 1
        player.set(KEY_CHEST_ATTR, keyChest)
    }
    val chestNumber = chestIndex + 1
    if (chestNumber == keyChest && !player.getBool(KITCHEN_UNLOCKED_ATTR)) {
        if (player.inventory.containsItem(Obj.keep_key, 1) || player.bank.containsItem(Obj.keep_key, 1)) {
            player.sendMessage("The chest is empty.")
            return
        }
        player.inventory.addItemDrop(Obj.keep_key, 1)
        player.sendMessage("You find a kitchen key.")
        return
    }

    var searched = player.getI(SEARCHED_CHESTS_ATTR)
    val chestBit = 1 shl chestIndex
    if ((searched and chestBit) != 0) {
        player.sendMessage("Nothing in that chest.")
        return
    }

    searched = searched or chestBit
    player.set(SEARCHED_CHESTS_ATTR, searched)
    when (Utils.random(3)) {
        0 -> {
            player.sendMessage("The chest contains some antipoison.")
            player.inventory.addItemDrop(Obj._1dose2antipoison, 1)
        }
        1 -> {
            player.sendMessage("The chest contains a poisonous spider.")
            val spider = OwnedNPC(player, Npc.dungeonspider, player.tile, true)
            spider.combatTarget = player
        }
        else -> player.sendMessage("You find nothing interesting.")
    }
}

private fun wakeObservatoryGuard(player: Player) {
    if (player.getBool(GUARD_AWAKE_ATTR) || player.getBool(GUARD_DEFEATED_ATTR)) return
    player.anim(Seq.qip_observatory_human_wakes_up_goblin)
    player.set(GUARD_AWAKE_ATTR, true)
    val guard = OwnedNPC(player, Npc.goblin_guard, GOBLIN_GUARD_TILE, true)
    guard.setAutoDespawnAtDistance(false)
    guard.combatTarget = player
}
