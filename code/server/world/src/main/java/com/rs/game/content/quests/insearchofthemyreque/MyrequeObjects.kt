package com.rs.game.content.quests.insearchofthemyreque

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onPlayerStep
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private val SOUTH_TREE = Tile.of(3502, 3426, 0)
private val NORTH_TREE = Tile.of(3502, 3431, 0)
private val BRIDGE_START = Tile.of(3502, 3427, 0)
private val BRIDGE_LOCS = (listOf(BRIDGE_START) + BRIDGE_TILES).toTypedArray()
private const val BROKEN_BRIDGE_TICKS = 100

@ServerStartupEvent
fun mapMyrequeObjects() {
    onObjectClick(Loc.route_rowboat_mortton, tiles = arrayOf(Tile.of(3524, 3283, 0))) { (player) ->
        if (!player.canBoardMyrequeBoat()) return@onObjectClick
        player.startConversation {
            options("Do you want to take the boat journey?") {
                op("Yes") { exec { if (player.canBoardMyrequeBoat()) player.myrequeTravel(Tile.of(3500, 3380, 0), "You arrive at the Hollows.", true) } }
                op("No") { }
            }
        }
    }
    onObjectClick(Loc.route_rowboat_hollows, tiles = arrayOf(Tile.of(3498, 3377, 0))) { (player) ->
        if (player.myrequeStage < BOAT) player.simpleDialogue("You have no permission to use this boat.")
        else player.myrequeTravel(Tile.of(3521, 3284, 0), "You arrive at Mort'ton.")
    }
    onObjectClick(Loc.spooky_tree_base_forbridge, tiles = arrayOf(SOUTH_TREE, NORTH_TREE)) { (player, obj) ->
        if (player.skills.getLevelForXp(Skills.AGILITY) < 25 || player.myrequeStage < BOAT) {
            player.simpleDialogue("You need level 25 Agility and Cyreg's permission to cross the Hollows.")
            return@onObjectClick
        }
        player.sendMessage("You climb up the tree.")
        player.useStairs(Seq.human_reachforladder, if (obj.tile == SOUTH_TREE) BRIDGE_START else BRIDGE_TILES.last(), 1, 2)
        player.schedule { wait(3); refreshMyrequeScene(player) }
    }
    onObjectClick(Loc.swamp_bridge1, Loc.swamp_bridgebroken, tiles = BRIDGE_LOCS, checkDistance = false) { (player, obj, option) ->
        if (player.myrequeStage < BOAT || player.plane != 0 || player.tile.distanceTo(obj.tile) > 1) { player.sendMessage("You must cross the bridge one section at a time."); return@onObjectClick }
        val index = BRIDGE_TILES.indexOf(obj.tile)
        if (index == -1) {
            if (option == "Repair") player.sendMessage("This bridge rung already looks fixed; that's why you're able to stand on it.")
            else player.climbMyrequeTreeDown(false)
        } else if (index == 2 && player.tile == obj.tile && option != "Repair") player.climbMyrequeTreeDown(true)
        else if (option == "Repair" || obj.id == Loc.swamp_bridgebroken) player.repairMyrequeBridge(index)
        else if (player.myrequeBridgeFixed(index)) {
            if (index == 2) player.climbMyrequeTreeDown(true)
            else player.useStairs(Seq.human_walk_f, obj.tile, 0, 1)
        } else if (!player.myrequeBridgeBroken()) player.breakMyrequeBridge() else player.repairMyrequeBridge(index)
    }
    onItemOnObject(arrayOf<Any>(Loc.swamp_bridge1, Loc.swamp_bridgebroken), arrayOf(Obj.woodplank, Obj.nails, Obj.hammer), tiles = BRIDGE_LOCS, checkDistance = false) { (player, obj) ->
        val index = BRIDGE_TILES.indexOf(obj.tile)
        if (index == -1) player.sendMessage("This bridge rung already looks fixed; that's why you're able to stand on it.")
        else player.repairMyrequeBridge(index)
    }
    onPlayerStep(BRIDGE_TILES.toTypedArray()) { (player, _, tile) ->
        val index = BRIDGE_TILES.indexOf(tile)
        if (!player.myrequeBridgeFixed(index)) {
            player.stopAll()
            player.tele(BRIDGE_START)
            player.breakMyrequeBridge()
            refreshMyrequeScene(player)
        }
    }
    onObjectClick(Loc.freedomfighterundergroundentrancel_gl, Loc.freedomfighterundergroundentrancer_gl, Loc.freedomfighterentrance,
        tiles = arrayOf(Tile.of(3500, 9813, 0), Tile.of(3501, 9813, 0), Tile.of(3508, 3444, 0))) { (player, obj) ->
        if (obj.tile.y < 6400 && player.myrequeStage < HIDEOUT) player.simpleDialogue("There seems to be a strange combination on the door. It would take ages to work it out.")
        else player.useStairs(-1, if (obj.tile.y < 6400) Tile.of(3500, 9812, 0) else Tile.of(3510, 3448, 0), 1, 2)
    }
    onObjectClick(Loc.route_stalagmite_cave_entrace, tiles = arrayOf(Tile.of(3492, 9824, 0))) { (player) ->
        if (player.myrequeStage < HIDEOUT) player.simpleDialogue("You have no reason to enter this hideout.")
        else player.useStairs(-1, Tile.of(3505, 9833, 0), 1, 2)
    }
    onObjectClick(Loc.route_cavewalltunnel, tiles = arrayOf(Tile.of(3505, 9831, 0))) { (player) ->
        if (player.myrequeStage < HIDEOUT) player.simpleDialogue("You have no reason to enter this hideout.")
        else player.useStairs(-1, Tile.of(3490, 9824, 0), 1, 2)
    }
    onObjectClick(Loc.thrttavernbasementfalsewall, tiles = arrayOf(Tile.of(3480, 9837, 0))) { (player, obj) ->
        if (player.myrequeStage < EXIT) player.simpleDialogue("You see nothing interesting about this wall.")
        else {
            player.sendMessage("You search the wall and find the door which Veliaf told you about.")
            Doors.handleOneWayDoor(player, obj, 1)
            player.sendMessage("You walk through.")
        }
    }
    onObjectClick(Loc.thrttavernbasementladder, tiles = arrayOf(Tile.of(3477, 9846, 0))) { (player) ->
        if (player.myrequeStage < EXIT) player.simpleDialogue("The way out is locked.")
        else {
            player.useStairs(Seq.human_reachforladder, Tile.of(3495, 3466, 0), 1, 3)
            player.schedule { wait(2); if (player.tile == Tile.of(3495, 3466, 0) && player.myrequeStage == EXIT) { player.myreque.setB("ladderUsed", true); player.sendMessage("You emerge to the south of the 'Hair of the Dog' tavern in Canifis.") } }
        }
    }
    onObjectClick(Loc.thrt_tavern_trap_door, tiles = arrayOf(Tile.of(3495, 3465, 0))) { (player) ->
        if (player.myrequeStage < COMPLETE) player.simpleDialogue(if (player.myrequeStage < EXIT) "This trap door seems to be locked. You're not really sure where it would lead to." else "This trap door seems to be locked.")
        else player.ladder(Tile.of(3477, 9845, 0))
    }
}

internal fun Player.myrequeTravel(destination: Tile, message: String, markJourney: Boolean = false) {
    stopAll()
    fadeScreen { if (!isDead && !hasFinished()) { tele(destination); if (markJourney && myrequeStage == BOAT) myreque.setB("travelled", true); sendMessage(message); refreshMyrequeScene(this) } }
}

private fun Player.canBoardMyrequeBoat(): Boolean {
    if (myrequeStage < BOAT) { npcDialogue(Npc.route_cyreg_paddlehorn, CALM_TALK, "Hey, hands off my boat!"); return false }
    if (myrequeStage >= COMPLETE || myreque.getB("travelled")) return true
    val remaining = myrequeBridgeRemaining()
    if (!inventory.containsItem(Obj.druid_pouch, 5) || !hasMyrequeHammer() || !inventory.containsItem(Obj.woodplank, remaining) || !inventory.containsItem(Obj.nails, remaining * 25)) {
        npcDialogue(Npc.route_cyreg_paddlehorn, CALM_TALK, "I cannot let you pass unprepared. You'll need a druid pouch with at least five charges, three wooden planks, a hammer and 75 steel nails for the bridge.")
        return false
    }
    return true
}

private fun Player.repairMyrequeBridge(index: Int) {
    if (myrequeStage < BOAT || index !in 0..2 || plane != 0) { sendMessage("You must repair the bridge one section at a time."); return }
    if (myrequeBridgeFixed(index)) { sendMessage("You have already fixed this part of the bridge."); return }
    if (tile.distanceTo(BRIDGE_TILES[index]) > 1) { sendMessage("You must repair the bridge one section at a time."); return }
    if (!hasMyrequeHammer()) { simpleDialogue("You need a hammer to repair the bridge."); return }
    if (!inventory.containsItem(Obj.woodplank, 1) || !inventory.containsItem(Obj.nails, 25)) { simpleDialogue("You need one plank and 25 steel nails to repair this part of the bridge."); return }
    stopAll()
    lock(3)
    anim(Seq.human_smithing)
    inventory.deleteItem(Obj.woodplank, 1)
    inventory.deleteItem(Obj.nails, 25)
    if (!myrequeBridgeBroken()) breakMyrequeBridge()
    myreque.setB("bridgeFixed$index", true)
    World.getObjectWithId(BRIDGE_TILES[index], Loc.swamp_bridgebroken)?.let(World::removeObject)
    schedule {
        wait(2)
        refreshMyrequeScene(this@repairMyrequeBridge)
        useStairs(Seq.human_walk_f, BRIDGE_TILES[index], 0, 1)
        sendMessage("You repair this part of the bridge.")
        if (index == 2) { wait(2); climbMyrequeTreeDown(true) }
    }
}

private fun Player.climbMyrequeTreeDown(north: Boolean) {
    sendMessage("You climb down the tree.")
    useStairs(Seq.human_reachforladder, Tile.of(3502, if (north) 3432 else 3425, 0), 1, 2)
}

private fun Player.breakMyrequeBridge() {
    myreque.setB("bridgeBroken", true)
    BRIDGE_TILES.forEachIndexed { index, tile ->
        if (myrequeBridgeFixed(index)) return@forEachIndexed
        val bridge = World.getObjectWithId(tile, Loc.swamp_bridge1) ?: return@forEachIndexed
        World.spawnObjectTemporary(GameObject(bridge, Loc.swamp_bridgebroken), BROKEN_BRIDGE_TICKS)
    }
    sendMessage("This bridge looks pretty old. The wooden boards break apart beneath your feet.")
    refreshMyrequeScene(this)
}
