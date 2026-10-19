package com.rs.game.content.quests.underground_pass.objects

import org.darkan.core.gameval.gen.Bas
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.ObjectShape
import org.darkan.core.map.ClipFlag
import org.darkan.core.map.pathfinder.Direction
import org.darkan.core.map.WorldCollision
import com.rs.engine.pathfinder.GameObjectCollision
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.underground_pass.STAGE_CROSSED_BRIDGE
import com.rs.game.content.quests.underground_pass.advanceUndergroundPass
import com.rs.game.content.quests.underground_pass.giveUniqueQuestItem
import com.rs.game.content.quests.underground_pass.hasCompletedUndergroundPassUnicorn
import com.rs.game.content.quests.underground_pass.items.litUndergroundPassArrows
import com.rs.game.content.skills.agility.Agility
import com.rs.game.content.transportation.FadingScreen
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.Constants
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import kotlin.math.abs
import kotlin.math.ceil

private const val GUIDE_ROPE_LOW_CHANCE = 120
private const val GUIDE_ROPE_HIGH_CHANCE = 300
private const val SHORTCUT_THIEVING_LEVEL = 50
private const val CAGE_PICK_XP = 3.0
private const val STONE_BRIDGE_FAILURE_DAMAGE = 140
private const val LEDGE_LOW_CHANCE = 200
private const val LEDGE_HIGH_CHANCE = 260
private const val LEDGE_FAILURE_DAMAGE = 50
private const val IBAN_BRIDGE_LOW_CHANCE = 160
private const val IBAN_BRIDGE_HIGH_CHANCE = 300
private const val IBAN_BRIDGE_FAILURE_BASE_DAMAGE = 50
private const val BRIDGE_LOWERED_ATTR = "UndergroundPassFirstBridgeLowered"
private const val BRIDGE_LEVER_CROSSING_ATTR = "UndergroundPassFirstBridgeLeverCrossing"
private const val FOOD_CRATES_ATTR = "searchedFoodCrates"
private const val WALL_TRAP_DISARMED_ATTR = "UndergroundPassWallTrapDisarmed:"
private const val WALL_TRAP_DISARMING_ATTR = "UndergroundPassWallTrapDisarming:"
private const val WALL_TRAP_PASSING_ATTR = "UndergroundPassWallTrapPassing:"
private val BRIDGE_TILE = Tile.of(2443, 9716, 0)
private val BRIDGE_WEST_TILE = Tile.of(2442, 9716, 0)
private val BRIDGE_EAST_TILE = Tile.of(2447, 9716, 0)
private val BRIDGE_LEVER_TILE = Tile.of(2436, 9716, 0)
private val GUIDE_ROPE_SHOOT_TILE = Tile.of(2447, 9720, 0)
private val SWAMP_FALL_TILE = Tile.of(2480, 9627, 0)
private val SWAMP_RETURN_TILE = Tile.of(2490, 9714, 0)
private val MUD_OUTPUT_TILE = Tile.of(2392, 9646, 0)
private val MUD_RETURN_TILE = Tile.of(2393, 9651, 0)
private val LEDGE_APPROACH_TILE = Tile.of(2375, 9644, 0)
private val LEDGE_START_TILE = Tile.of(2374, 9644, 0)
private val LEDGE_OUTPUT_TILE = Tile.of(2374, 9638, 0)
private val UNICORN_ROOM_TUNNELS = setOf(
    Tile.of(2375, 9611, 0), Tile.of(2376, 9611, 0),
    Tile.of(2400, 9611, 0), Tile.of(2401, 9611, 0)
)
private val PALADIN_CAMP_TUNNELS = setOf(Tile.of(2370, 9665, 0), Tile.of(2371, 9665, 0))
private val PALADIN_CAMP_TUNNEL_OUTPUT = Tile.of(2370, 9666, 0)
private val INTACT_UNICORN_ROOM_TUNNEL_OUTPUT = Tile.of(2401, 9610, 0)
private val SMASHED_UNICORN_ROOM_TUNNEL_OUTPUT = Tile.of(2376, 9610, 0)
private val STONE_BRIDGE_RETURN_TILE = Tile.of(2418, 9637, 0)
private val IBAN_BRIDGE_FALL_TILE = Tile.of(2333, 9866, 0)
private val FOOD_CRATES_TILE = Tile.of(2426, 9725, 0)
private val PIPE_ENTRY_TILE = Tile.of(2418, 9605, 0)
private val PIPE_OUTPUT_TILE = Tile.of(2412, 9605, 0)
private val SHORTCUT_START_TILE = Tile.of(2380, 9619, 0)
private val SHORTCUT_BUBBLES = listOf(
    GameObject(Loc.upass_swampbubbles1, ObjectShape.SCENERY_INTERACT, 1, Tile.of(2391, 9620, 0)),
    GameObject(Loc.upass_swampbubbles1, ObjectShape.SCENERY_INTERACT, 0, Tile.of(2392, 9618, 0)),
    GameObject(Loc.upass_swampbubbles1, ObjectShape.SCENERY_INTERACT, 0, Tile.of(2394, 9619, 0)),
    GameObject(Loc.upass_swampbubbles1, ObjectShape.SCENERY_INTERACT, 1, Tile.of(2397, 9618, 0)),
    GameObject(Loc.upass_swampbubbles1, ObjectShape.SCENERY_INTERACT, 2, Tile.of(2399, 9620, 0)),
    GameObject(Loc.upass_swampbubbles1, ObjectShape.SCENERY_INTERACT, 3, Tile.of(2401, 9619, 0))
)
private val SHORTCUT_BUBBLE_TILES = SHORTCUT_BUBBLES.mapTo(mutableSetOf()) { it.tile }
private val ONE_WAY_HILL_SLOPE = (2369..2373).map { x -> Tile.of(x, 9639, 0) }
private val ONE_WAY_HILL_REGION = ONE_WAY_HILL_SLOPE.first().getRegionId()
private val ROPE_BALANCE_WEST_START = Tile.of(2461, 9699, 0)
private val ROPE_BALANCE_EAST_START = Tile.of(2467, 9699, 0)
private val ROPE_BALANCE_OBJECTS = arrayOf(
    GameObject(Loc.di2_tightrope_rope1, ObjectShape.SCENERY_INTERACT, 0, Tile.of(2463, 9699, 0)),
    GameObject(Loc.di2_tightrope_rope2, ObjectShape.SCENERY_INTERACT, 0, Tile.of(2464, 9699, 0)),
    GameObject(Loc.di2_tightrope_rope3, ObjectShape.SCENERY_INTERACT, 2, Tile.of(2465, 9699, 0))
)
private val FRAYED_ROPE_OBJECTS = arrayOf(
    GameObject(Loc.di2_tightrope_rope4, ObjectShape.SCENERY_INTERACT, 0, Tile.of(2463, 9699, 0)),
    GameObject(Loc.di2_tightrope_rope5, ObjectShape.SCENERY_INTERACT, 2, Tile.of(2465, 9699, 0))
)
private val SWAMP_BARRIER_TILES = buildSet {
    for (x in 2466..2473) for (y in 9710..9717) add(Tile.of(x, y, 0))
    for (x in 2433..2440) for (y in 9687..9692) add(Tile.of(x, y, 0))
}

private data class UndergroundPassWallTrap(val tile: Tile, val rotation: Int) {
    private val occupiedTiles: Set<Tile>
        get() = if (rotation % 2 == 1)
            setOf(tile, tile.transform(0, 1))
        else
            setOf(tile, tile.transform(1, 0))

    val triggerTiles: Set<Tile>
        get() = occupiedTiles.flatMapTo(mutableSetOf()) { occupied ->
            CARDINAL_DIRECTIONS.map { direction -> occupied.transform(direction.dx, direction.dy) }
        }
}

private val CARDINAL_DIRECTIONS = listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
private val WALL_TRAPS = listOf(
    UndergroundPassWallTrap(Tile.of(2430, 9675, 0), 1),
    UndergroundPassWallTrap(Tile.of(2432, 9675, 0), 1),
    UndergroundPassWallTrap(Tile.of(2435, 9675, 0), 1),
    UndergroundPassWallTrap(Tile.of(2440, 9677, 0), 3),
    UndergroundPassWallTrap(Tile.of(2443, 9677, 0), 3)
)
private val WALL_TRAPS_BY_TRIGGER_TILE = WALL_TRAPS
    .flatMap { trap -> trap.triggerTiles.map { tile -> tile to trap } }
    .groupBy({ it.first }, { it.second })

private fun Player.syncFirstUndergroundPassBridge() {
    if (tempAttribs.getB(BRIDGE_LOWERED_ATTR))
        lowerFirstUndergroundPassBridge()
    else
        raiseFirstUndergroundPassBridge()
}

private fun Player.lowerFirstUndergroundPassBridge() {
    val bridge = World.getObjectWithId(BRIDGE_TILE, Loc.old_bridge_up) ?: return
    packets.sendAddObject(GameObject(Loc.old_bridge_animated, bridge.type, bridge.rotation, bridge.tile))
    tempAttribs.setB(BRIDGE_LOWERED_ATTR, true)
}

private fun Player.raiseFirstUndergroundPassBridge() {
    val bridge = World.getObjectWithId(BRIDGE_TILE, Loc.old_bridge_up) ?: return
    packets.sendAddObject(bridge)
    tempAttribs.removeB(BRIDGE_LOWERED_ATTR)
}

private fun Player.walkFirstUndergroundPassBridge(destination: Tile, onArrival: (() -> Unit)? = null) {
    val wasRunning = this.run
    resetWalkSteps()
    lock()
    setRunHidden(false)
    sendMessage("You rush across the bridge.", true)
    appearance.setBAS(Bas.human_walk_logbalance)
    addWalkSteps(destination.x, destination.y, -1, false)
    schedule {
        wait(6)
        anim(-1)
        appearance.setBAS(-1)
        setRunHidden(wasRunning)
        unlock()
        onArrival?.invoke()
    }
}

internal fun Player.crossFirstUndergroundPassBridge() {
    if (x <= BRIDGE_TILE.x)
        return
    lowerFirstUndergroundPassBridge()
    walkToAndExecute(BRIDGE_EAST_TILE) {
        walkFirstUndergroundPassBridge(BRIDGE_WEST_TILE) {
            raiseFirstUndergroundPassBridge()
        }
    }
}

private fun Player.useUndergroundPassUnicornTunnel(tunnelTile: Tile) {
    val destination = when (tunnelTile) {
        in UNICORN_ROOM_TUNNELS -> PALADIN_CAMP_TUNNEL_OUTPUT
        in PALADIN_CAMP_TUNNELS -> if (hasCompletedUndergroundPassUnicorn())
            SMASHED_UNICORN_ROOM_TUNNEL_OUTPUT
        else
            INTACT_UNICORN_ROOM_TUNNEL_OUTPUT
        else -> return
    }
    fadeScreen { tele(destination) }
}

private fun Player.searchUndergroundPassFoodCrate() {
    val attributes = questManager.getAttribs(Quest.UNDERGROUND_PASS)
    anim(Seq.human_pickuptable)
    if (attributes.getB(FOOD_CRATES_ATTR)) {
        sendMessage("You search the crate but find nothing.")
        return
    }
    attributes.setB(FOOD_CRATES_ATTR, true)
    inventory.addItemDrop(Obj.salmon, 2)
    inventory.addItemDrop(Obj.meat_pie, 2)
    sendMessage("You search the crate and find two salmon and two meat pies.")
}

private fun Player.pullFirstUndergroundPassBridgeLever(lever: GameObject) {
    walkToAndExecute(lever) {
        lowerFirstUndergroundPassBridge()
        faceObject(lever)
        sendMessage("The lever lowers the bridge across the chasm.")
        tempAttribs.setB(BRIDGE_LEVER_CROSSING_ATTR, true)
        walkToAndExecute(BRIDGE_WEST_TILE) {
            tempAttribs.removeB(BRIDGE_LEVER_CROSSING_ATTR)
            walkFirstUndergroundPassBridge(BRIDGE_EAST_TILE) {
                raiseFirstUndergroundPassBridge()
            }
        }
    }
}

private fun Player.climbUndergroundPassRockslide(obj: GameObject) {
    if (getLongestDelta(obj.tile) > 4) {
        sendMessage("You can't reach that.")
        return
    }

    val dx = x - obj.x
    val dy = y - obj.y
    val destination = if (abs(dx) >= abs(dy))
        obj.tile.transform(if (dx > 0) -1 else 1, 0)
    else
        obj.tile.transform(0, if (dy > 0) -1 else 1)

    resetWalkSteps()
    faceObject(obj)
    sendMessage("You climb onto the rock...", true)
    if (!Utils.skillSuccess(skills.getLevel(Constants.AGILITY), 160, 300)) {
        anim(Seq.human_walk_style)
        applyHit(Hit(30, Hit.HitLook.TRUE_DAMAGE))
        sendMessage("...but you slip back down.", true)
        return
    }
    forceMove(destination, Seq.human_walk_style, 10, 60) {
        sendMessage("...and step down the other side.", true)
    }
}

private fun Player.failUndergroundPassSwamp() {
    resetWalkSteps()
    lock()
    schedule {
        sendMessage("You try to cross the swamp...", true)
        wait(2)
        sendMessage("The swamp clings to your legs and drags you below.", true)
        forceTalk("Gulp!")
        wait(2)
        sendMessage("You tumble deep into the crevasse.", true)
        tele(SWAMP_FALL_TILE)
        applyHit(Hit(140, Hit.HitLook.TRUE_DAMAGE))
        unlock()
    }
}

private fun Player.squeezeThroughUndergroundPassPipe(obj: GameObject) {
    if (obj.id == Loc.upass_pipe5) {
        sendMessage("You can't squeeze through the pipe from this side.")
        return
    }
    val start = Tile.of(obj.x, 9688, obj.plane)
    val destination = Tile.of(obj.x, 9694, obj.plane)
    walkToAndExecute(start) {
        sendMessage("You squeeze through the pipe.", true)
        forceMove(destination, Seq.human_doublepipesqueeze, 10, 90)
    }
}

private fun Player.squeezeThroughUnicornPipe() {
    if (getLongestDelta(PIPE_ENTRY_TILE) > 2) {
        sendMessage("You can't reach that.")
        return
    }
    faceTile(PIPE_OUTPUT_TILE)
    sendMessage("You squeeze through the pipe.", true)
    lock()
    schedule {
        wait(1)
        anim(Seq.human_doublepipesqueeze)
        FadingScreen.fade(this@squeezeThroughUnicornPipe, 0) {
            tele(PIPE_OUTPUT_TILE)
            anim(-1)
            unlockNextTick()
        }
    }
}

private fun Player.crossUndergroundPassLedge() {
    if (plane != LEDGE_APPROACH_TILE.plane || x <= LEDGE_START_TILE.x) {
        sendMessage("You can't cross the ledge from this side.")
        return
    }
    if (getLongestDelta(LEDGE_APPROACH_TILE) > 3) {
        sendMessage("You need to move closer to the ledge.")
        return
    }

    val cross: () -> Unit = {
        resetWalkSteps()
        val success = Agility.rollSuccess(this, LEDGE_LOW_CHANCE, LEDGE_HIGH_CHANCE)
        val distance = Utils.getDistanceI(LEDGE_START_TILE, LEDGE_OUTPUT_TILE)
        val destination = if (success)
            LEDGE_OUTPUT_TILE
        else
            LEDGE_START_TILE.transform(0, -(distance / 2))
        val wasRunning = run

        lock()
        setRunHidden(false)
        faceDir(Direction.WEST)
        schedule {
            anim(Seq.human_into_sidestep)
            setBas(Bas.human_12)
            wait(1)

            addWalkSteps(destination.x, destination.y, -1, false)
            wait(Utils.getDistanceI(LEDGE_START_TILE, destination))

            resetWalkSteps()
            anim(Seq.human_outof_sidestep)
            setBas(-1)
            if (success) {
                setRunHidden(wasRunning)
                unlock()
                return@schedule
            }

            sendMessage("You lose your footing and fall from the ledge.", true)
            val landingTile = destination.transform(-1, 0)
            forceMove(landingTile, Seq.human_spot_jump, 0, 30, false) {
                applyHit(Hit(LEDGE_FAILURE_DAMAGE, Hit.HitLook.TRUE_DAMAGE))
                forceTalk("Ouch!")
                setRunHidden(wasRunning)
                unlock()
            }
        }
    }

    walkToAndExecute(LEDGE_APPROACH_TILE) {
        resetWalkSteps()
        addWalkSteps(LEDGE_START_TILE.x, LEDGE_START_TILE.y, 1, false)
        schedule {
            wait(1)
            if (tile != LEDGE_START_TILE) {
                sendMessage("You can't reach the ledge.")
                return@schedule
            }
            cross()
        }
    }
}

private fun applyUndergroundPassHillCollision() {
    ONE_WAY_HILL_SLOPE.forEach { tile ->
        WorldCollision.removeBlockedTile(tile)
        WorldCollision.addFlag(tile, ClipFlag.blockNorth(true, false, true))
    }
}

private fun Player.crossUndergroundPassChasmBridge(obj: GameObject) {
    val horizontal = obj.rotation % 2 == 1
    val firstEnd = if (horizontal) {
        obj.tile.transform(-1, 0)
    } else {
        obj.tile.transform(0, -1)
    }
    val secondEnd = if (horizontal) {
        obj.tile.transform(3, 0)
    } else {
        obj.tile.transform(0, 3)
    }
    val start = if (getLongestDelta(firstEnd) <= getLongestDelta(secondEnd)) firstEnd else secondEnd
    val destination = if (start == firstEnd) secondEnd else firstEnd
    val direction = when {
        destination.x > start.x -> Direction.EAST
        destination.x < start.x -> Direction.WEST
        destination.y > start.y -> Direction.NORTH
        else -> Direction.SOUTH
    }

    walkToAndExecute(start) {
        resetWalkSteps()
        faceDir(direction)
        sendMessage("You attempt to walk over the remaining bridge...", true)
        if (Agility.rollSuccess(this, IBAN_BRIDGE_LOW_CHANCE, IBAN_BRIDGE_HIGH_CHANCE)) {
            sendMessage("... you manage to cross safely.", true)
            val distance = getLongestDelta(destination)
            Agility.walkToAgility(this, Bas.human_walk_logbalance, direction, distance, distance)
            return@walkToAndExecute
        }

        lock(4)
        sendMessage("... but you slip and tumble into the darkness.", true)
        forceMove(obj.tile, Seq.human_spot_jump, 10, 30)
        schedule {
            wait(2)
            tele(IBAN_BRIDGE_FALL_TILE)
            forceTalk("Ouch!")
            val damage = ceil(hitpoints * 0.2).toInt() + IBAN_BRIDGE_FAILURE_BASE_DAMAGE
            applyHit(Hit(damage, Hit.HitLook.TRUE_DAMAGE))
        }
    }
}

private fun Player.crossUndergroundPassStoneBridge(obj: GameObject) {
    val bridgeTile = Tile.of(obj.x, obj.y, plane)
    if (getLongestDelta(bridgeTile) > 3) {
        sendMessage("You can't reach that.")
        return
    }
    val dx = x - obj.x
    val dy = y - obj.y
    val direction = if (abs(dx) >= abs(dy)) {
        if (dx <= 0) Direction.EAST else Direction.WEST
    } else {
        if (dy <= 0) Direction.NORTH else Direction.SOUTH
    }
    val start = bridgeTile.transform(-direction.dx, -direction.dy)
    val destination = bridgeTile.transform(direction.dx * 2, direction.dy * 2)
    walkToAndExecute(start) {
        resetWalkSteps()
        faceDir(direction)
        if (Agility.rollSuccess(this, 100, 300)) {
            val distance = getLongestDelta(destination)
            sendMessage("You carefully cross the stone bridge.", true)
            Agility.walkToAgility(this, Bas.human_walk_logbalance, direction, distance, distance)
            return@walkToAndExecute
        }

        lock()
        sendMessage("You lose your footing on the stone bridge!", true)
        forceMove(bridgeTile, Seq.human_spot_jump, 10, 30)
        schedule {
            wait(2)
            forceTalk("Ouch!")
            applyHit(Hit(STONE_BRIDGE_FAILURE_DAMAGE, Hit.HitLook.TRUE_DAMAGE))
            wait(1)
            fadeScreen {
                tele(STONE_BRIDGE_RETURN_TILE)
                unlockNextTick()
            }
        }
    }
}

private fun wallTrapAttribute(prefix: String, trap: UndergroundPassWallTrap) = "$prefix${trap.tile.getTileHash()}"

private fun Player.triggerUndergroundPassWallTrap(
    trap: UndergroundPassWallTrap,
    forceActivation: Boolean = false
) {
    val passingAttribute = wallTrapAttribute(WALL_TRAP_PASSING_ATTR, trap)
    if (!forceActivation && tempAttribs.getB(passingAttribute))
        return

    tempAttribs.setB(passingAttribute, true)
    schedule {
        wait(3)
        tempAttribs.removeB(passingAttribute)
    }

    val disarmedAttribute = wallTrapAttribute(WALL_TRAP_DISARMED_ATTR, trap)
    if (tempAttribs.removeB(disarmedAttribute)) {
        sendMessage("You pass the disarmed floor trap safely.", true)
        return
    }

    sendMessage("The floor trap springs as you pass!", true)
    val damage = ceil(hitpoints * 0.2).toInt().coerceAtLeast(1)
    applyHit(Hit(damage, Hit.HitLook.TRUE_DAMAGE))
    forceTalk("Ouch!")
}

private fun Player.disarmUndergroundPassWallTrap(obj: GameObject) {
    val trap = WALL_TRAPS.firstOrNull { it.tile == obj.tile }
    if (trap == null) {
        sendMessage("You find nothing useful.")
        return
    }
    faceObject(obj)
    anim(Seq.human_pickuptable)
    sendMessage("You try to disarm the floor trap...", true)
    if (!Utils.skillSuccess(
            skills.getLevel(Constants.THIEVING),
            auraManager.thievingMul,
            160,
            300
        )
    ) {
        sendMessage("...and fail, activating the trap!", true)
        tempAttribs.removeB(wallTrapAttribute(WALL_TRAP_DISARMED_ATTR, trap))
        triggerUndergroundPassWallTrap(trap, forceActivation = true)
        return
    }
    tempAttribs.setB(wallTrapAttribute(WALL_TRAP_DISARMED_ATTR, trap), true)
    sendMessage("You disarm the floor trap.", true)
}

private fun Player.attemptToDisarmUndergroundPassWallTrap(obj: GameObject) {
    val trap = WALL_TRAPS.firstOrNull { it.tile == obj.tile }
    if (trap == null) {
        sendMessage("You find nothing useful.")
        return
    }

    val disarmingAttribute = wallTrapAttribute(WALL_TRAP_DISARMING_ATTR, trap)
    tempAttribs.setB(disarmingAttribute, true)
    schedule {
        wait(20)
        tempAttribs.removeB(disarmingAttribute)
    }
    walkToAndExecute(obj) {
        tempAttribs.removeB(disarmingAttribute)
        disarmUndergroundPassWallTrap(obj)
    }
}

private fun Player.pickUndergroundPassCageLock(obj: GameObject, requiredLevel: Int) {
    if (skills.getLevel(Constants.THIEVING) < requiredLevel) {
        sendMessage("You need a Thieving level of at least $requiredLevel to pick this lock.")
        return
    }
    lock()
    schedule {
        faceObject(obj)
        anim(Seq.human_pickuptable)
        wait(1)
        if (!Utils.skillSuccess(
                skills.getLevel(Constants.THIEVING),
                auraManager.thievingMul,
                128,
                256
            )
        ) {
            sendMessage("You fail to pick the lock.")
            unlock()
            return@schedule
        }

        skills.addXp(Constants.THIEVING, CAGE_PICK_XP)
        sendMessage("You successfully pick the lock and pass through the cage door.")
        val destination = when (obj.rotation) {
            0 -> obj.tile.transform(if (x < obj.x) 0 else -1, 0)
            1 -> obj.tile.transform(0, if (y > obj.y) 0 else 1)
            2 -> obj.tile.transform(if (x > obj.x) 0 else 1, 0)
            else -> obj.tile.transform(0, if (y < obj.y) 0 else -1)
        }
        val openedTile = when (obj.rotation) {
            0 -> obj.tile.transform(-1, 0)
            1 -> obj.tile.transform(0, 1)
            2 -> obj.tile.transform(1, 0)
            else -> obj.tile.transform(0, -1)
        }
        World.spawnObjectTemporary(GameObject(obj, Loc.inviswall), 3, true)
        World.spawnObjectTemporary(GameObject(obj.id, obj.type, obj.getRotation(1), openedTile), 3, true)
        forceMove(destination, -1, 10, 30)
        wait(2)
        unlock()
    }
}

private fun Player.failUndergroundPassShortcut() {
    resetWalkSteps()
    lock()
    schedule {
        sendMessage("A bubble bursts beneath you and the swamp drags you back!", true)
        forceTalk("Gulp!")
        wait(2)
        tele(SHORTCUT_START_TILE)
        unlock()
    }
}

private fun Player.swingAcrossUndergroundPassRope(obj: GameObject) {
    val westSide = x <= obj.x
    val start = Tile.of(if (westSide) 2460 else 2467, obj.y, obj.plane)
    val destination = Tile.of(if (westSide) 2467 else 2460, obj.y, obj.plane)
    Agility.swingOnRopeSwing(this, start, destination, obj, 0.0)
}

private fun Player.showUndergroundPassRopeBalance() {
    ROPE_BALANCE_OBJECTS.forEach(packets::sendAddObject)
}

private fun Player.removeUndergroundPassRopeBalance() {
    packets.sendAddObject(FRAYED_ROPE_OBJECTS[0])
    packets.sendRemoveObject(ROPE_BALANCE_OBJECTS[1])
    packets.sendAddObject(FRAYED_ROPE_OBJECTS[1])
}

private fun Player.crossUndergroundPassRopeBalance(obj: GameObject, ropeId: Int) {
    val westSide = obj.id == Loc.di2_tightrope_anchor1
    val start = if (westSide) ROPE_BALANCE_WEST_START else ROPE_BALANCE_EAST_START
    val direction = if (westSide) Direction.EAST else Direction.WEST

    if ((westSide && x > obj.x) || (!westSide && x < obj.x)) {
        sendMessage("You can't reach the old spike from this side.")
        return
    }

    walkToAndExecute(start) {
        inventory.deleteItem(ropeId, 1)
        resetWalkSteps()
        faceDir(direction)
        anim(Seq.human_throwrope_up)
        showUndergroundPassRopeBalance()
        sendMessage("You throw the rope around the opposite peg...", true)

        if (Agility.rollSuccess(this, 100, 300)) {
            sendMessage("You balance across the rope.", true)
            Agility.walkToAgility(this, Bas.human_walk_logbalance, direction, 6, 6, 25.0)
            schedule {
                wait(8)
                removeUndergroundPassRopeBalance()
                sendMessage("The rope falls into the darkness below.", true)
            }
            return@walkToAndExecute
        }

        val wasRunning = run
        lock()
        setRunHidden(false)
        setBas(Bas.human_walk_logbalance)
        addWalkSteps(Tile.of(2464, 9699, 0), 3, false)
        schedule {
            wait(3)
            sendMessage("You try to balance but fall into the darkness.", true)
            setBas(-1)
            removeUndergroundPassRopeBalance()
            tele(SWAMP_FALL_TILE)
            applyHit(Hit(140, Hit.HitLook.TRUE_DAMAGE))
            setRunHidden(wasRunning)
            unlock()
        }
    }
}

@ServerStartupEvent
fun mapFirstUndergroundPassCavern() {
    SWAMP_BARRIER_TILES.forEach(WorldCollision::addBlockedTile)
    SHORTCUT_BUBBLES.forEach(GameObjectCollision::unclip)
    applyUndergroundPassHillCollision()

    onLogin { event ->
        if (event.player.getRegionId() == ONE_WAY_HILL_REGION)
            applyUndergroundPassHillCollision()
        if (event.player.getRegionId() == BRIDGE_TILE.getRegionId()) {
            event.player.syncFirstUndergroundPassBridge()
        }
    }
    onChunkEnter { (entity, _) ->
        val player = entity as? Player ?: return@onChunkEnter
        if (player.getRegionId() == ONE_WAY_HILL_REGION)
            applyUndergroundPassHillCollision()
        if (player.getRegionId() == BRIDGE_TILE.getRegionId()) {
            player.syncFirstUndergroundPassBridge()
        } else {
            player.tempAttribs.removeB(BRIDGE_LOWERED_ATTR)
        }
    }
    onPlayerStep(ONE_WAY_HILL_SLOPE.toTypedArray()) { (_, step) ->
        if (step.dir == Direction.NORTH)
            step.setCheckClip(false)
    }
    onPlayerStep(arrayOf(BRIDGE_WEST_TILE, BRIDGE_EAST_TILE)) { (player, step, tile) ->
        if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_CROSSED_BRIDGE)
            return@onPlayerStep
        if (player.tempAttribs.getB(BRIDGE_LEVER_CROSSING_ATTR))
            return@onPlayerStep
        when {
            tile == BRIDGE_WEST_TILE && step.dir == Direction.EAST -> {
                player.lowerFirstUndergroundPassBridge()
                player.walkFirstUndergroundPassBridge(BRIDGE_EAST_TILE) {
                    player.raiseFirstUndergroundPassBridge()
                }
            }
            tile == BRIDGE_EAST_TILE && step.dir == Direction.WEST -> {
                player.lowerFirstUndergroundPassBridge()
                player.walkFirstUndergroundPassBridge(BRIDGE_WEST_TILE) {
                    player.raiseFirstUndergroundPassBridge()
                }
            }
        }
    }

    onPlayerStep(WALL_TRAPS_BY_TRIGGER_TILE.keys.toTypedArray()) { (player, _, tile) ->
        val trap = WALL_TRAPS_BY_TRIGGER_TILE[tile]
            ?.firstOrNull { candidate ->
                !player.tempAttribs.getB(wallTrapAttribute(WALL_TRAP_PASSING_ATTR, candidate)) &&
                    !player.tempAttribs.getB(wallTrapAttribute(WALL_TRAP_DISARMING_ATTR, candidate))
            }
            ?: return@onPlayerStep
        player.triggerUndergroundPassWallTrap(trap)
    }

    onItemOnObject(arrayOf(Loc.di2_tightrope_anchor1, Loc.di2_tightrope_anchor2), arrayOf(Obj.rope)) { (player, obj, item) ->
        player.crossUndergroundPassRopeBalance(obj, item.id)
    }
    onObjectClick(
        Loc.upass_mud, Loc.cavewalltunnel_upass_tocells, Loc.loc_3218, Loc.loc_3219,
        Loc.caverockpile,
        Loc.cavefood1, Loc.loc_3227
    ) { (player, obj) ->
        when (obj.id) {
            Loc.upass_mud -> if (player.inventory.containsItem(Obj.spade)) {
                player.fadeScreen { player.tele(MUD_OUTPUT_TILE) }
            } else {
                player.sendMessage("You need a spade to dig through the mud.")
            }

            Loc.cavewalltunnel_upass_tocells -> player.tele(MUD_RETURN_TILE)
            Loc.loc_3218, Loc.loc_3219 -> player.useUndergroundPassUnicornTunnel(obj.tile)

            Loc.caverockpile -> {
                player.sendMessage("You climb the pile of rocks and surface beside the swamp.", true)
                player.tele(SWAMP_RETURN_TILE)
            }
            Loc.cavefood1 -> {
                player.searchUndergroundPassFoodCrate()
            }

            Loc.loc_3227 -> {
                player.giveUniqueQuestItem(Obj.swamprocks1)
                player.sendMessage("You mine a loose rock from the wall.")
            }
        }
    }

    onObjectClick(Loc.upass_lever_up, tiles = arrayOf(BRIDGE_LEVER_TILE), checkDistance = false) { (player, obj) ->
        player.pullFirstUndergroundPassBridgeLever(obj)
    }

    onObjectClick(
        Loc.upass_speartrap,
        tiles = WALL_TRAPS.map { it.tile }.toTypedArray(),
        checkDistance = false
    ) { (player, obj) ->
        player.attemptToDisarmUndergroundPassWallTrap(obj)
    }

    onObjectClick(Loc.loc_3235, Loc.upass_pipe5, checkDistance = false) { (player, obj) ->
        player.squeezeThroughUndergroundPassPipe(obj)
    }

    onObjectClick(Loc.upass_pipe6, tiles = arrayOf(PIPE_ENTRY_TILE), checkDistance = false) { (player) ->
        player.squeezeThroughUnicornPipe()
    }

    onObjectClick(Loc.upass_ledge, checkDistance = false) { (player) ->
        player.crossUndergroundPassLedge()
    }

    onObjectClick(Loc.bridgecollapsed1, Loc.bridgecollapsed2, checkDistance = false) { (player, obj) ->
        player.crossUndergroundPassChasmBridge(obj)
    }

    onObjectClick(Loc.loc_3276, checkDistance = false) { (player, obj) ->
        player.crossUndergroundPassStoneBridge(obj)
    }

    onObjectClick(Loc.cave_railings2, Loc.cave_railings5) { (player, obj, option) ->
        if (option != "Pick-lock") {
            player.sendMessage("You find nothing useful.")
            return@onObjectClick
        }
        player.pickUndergroundPassCageLock(
            obj,
            if (obj.id == Loc.cave_railings5) SHORTCUT_THIEVING_LEVEL else 1
        )
    }

    onObjectClick(Loc.di2_boxes3, tiles = arrayOf(FOOD_CRATES_TILE)) { (player) ->
        player.searchUndergroundPassFoodCrate()
    }

    onPlayerStep(SHORTCUT_BUBBLE_TILES.toTypedArray()) { (player) ->
        player.failUndergroundPassShortcut()
    }

    onObjectClick(Loc.obstical_rockswing_withrope2, checkDistance = false) { (player, obj) ->
        player.swingAcrossUndergroundPassRope(obj)
    }

    onObjectClick(Loc.upass_swampbubbles1, checkDistance = false) { (player, obj) ->
        if (obj.tile in SHORTCUT_BUBBLE_TILES)
            player.failUndergroundPassShortcut()
        else
            player.failUndergroundPassSwamp()
    }

    onObjectClick(Loc.rockslide2_obstacle_upass, checkDistance = false) { (player, obj) ->
        player.climbUndergroundPassRockslide(obj)
    }

    onObjectClick(Loc.oldbridge_guiderope, checkDistance = false) { (player, _) ->
        if (player.plane != GUIDE_ROPE_SHOOT_TILE.plane || player.getLongestDelta(GUIDE_ROPE_SHOOT_TILE) > 2) {
            player.sendMessage("You can't get a clear shot at the guide rope from here.")
            return@onObjectClick
        }
        val arrow = litUndergroundPassArrows.firstOrNull { player.equipment.ammoId == it }
        if (arrow == null) {
            player.sendMessage("You need to equip a lit fire arrow to ignite the guide rope.")
            return@onObjectClick
        }
        if (!player.equipment.weaponName.contains("shortbow", ignoreCase = true)) {
            player.sendMessage("You need to equip a shortbow to fire the arrow.")
            return@onObjectClick
        }
        player.equipment.deleteItem(arrow, 1)
        if (!Utils.skillSuccess(player.skills.getLevel(Constants.RANGE), GUIDE_ROPE_LOW_CHANCE, GUIDE_ROPE_HIGH_CHANCE)) {
            player.sendMessage("Your arrow misses the guide rope and disappears into the chasm.")
            return@onObjectClick
        }
        player.sendMessage("Your arrow catches the guide rope. The burning rope drops the bridge into place.")
        player.advanceUndergroundPass(STAGE_CROSSED_BRIDGE)
        player.crossFirstUndergroundPassBridge()
    }
}
