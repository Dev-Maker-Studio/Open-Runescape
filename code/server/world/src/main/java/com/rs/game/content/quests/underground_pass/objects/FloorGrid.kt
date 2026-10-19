package com.rs.game.content.quests.underground_pass.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.model.ObjectShape
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import world.gregs.voidps.type.Tile
import com.rs.utils.Ticks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import kotlin.random.Random

private const val FLOOR_TRAP_DISABLED_ATTR = "UndergroundPassFloorTrapDisabled"
private const val IN_FLOOR_GRID_AREA_ATTR = "UndergroundPassInFloorGridArea"
private const val FLOOR_GRID_PATH_ATTR = "UndergroundPassFloorGridPath"
private const val FLOOR_GRID_IDLE_TASK = "UndergroundPassFloorGridIdle"
private const val FLOOR_GRID_IDLE_SECONDS = 20
private const val GRID_SIZE = 5
private const val CELL_SIZE = 2
private val GRID_RETURN_TILE = Tile.of(2477, 9678, 0)
private val GRID_FALL_TILE = Tile.of(2314, 9657, 0)
private val GRID_LEVER_TILE = Tile.of(2466, 9672, 0)
private val GRID_EXIT_TILE = Tile.of(2464, 9676, 0)
private val GRID_PORTCULLISES = (9674..9680 step 2).map { y ->
    GameObject(Loc.loc_3303, ObjectShape.SCENERY_INTERACT, 3, Tile.of(2465, y, 0))
}

private val GRID_TILES = (2467..2476).flatMap { x ->
    (9673..9682).map { y -> Tile.of(x, y, 0) }
}.toTypedArray()

private val GRID_AREA_CHUNKS = buildSet {
    for (x in 2467..2476 step 8)
        for (y in 9673..9682 step 8)
            add(Tile.of(x, y, 0).getChunkId())
}

private fun Player.floorGridSafeTiles(): Set<Tile> {
    var encodedPath = getI(FLOOR_GRID_PATH_ATTR, -1)
    if (encodedPath == -1) {
        val rows = IntArray(GRID_SIZE)
        rows[0] = Random.nextInt(GRID_SIZE)
        for (column in 1 until GRID_SIZE) {
            val previous = rows[column - 1]
            rows[column] = (previous - 1..previous + 1)
                .filter { it in 0 until GRID_SIZE }
                .random()
        }
        encodedPath = rows.foldIndexed(0) { column, encoded, row ->
            encoded or (row shl (column * 3))
        }
        set(FLOOR_GRID_PATH_ATTR, encodedPath)
    }

    return buildSet {
        for (column in 0 until GRID_SIZE) {
            val row = encodedPath shr (column * 3) and 0x7
            val southWestX = 2467 + column * CELL_SIZE
            val southWestY = 9673 + row * CELL_SIZE
            addAll(cell(southWestX, southWestY))
        }
    }
}

private fun Player.openUndergroundPassPortcullises() {
    asyncTasks.cancel(FLOOR_GRID_IDLE_TASK)
    tempAttribs.setB(FLOOR_TRAP_DISABLED_ATTR, true)
    GRID_PORTCULLISES.forEach(packets::sendRemoveObject)
}

private fun Player.restoreUndergroundPassPortcullises() {
    tempAttribs.removeB(FLOOR_TRAP_DISABLED_ATTR)
    GRID_PORTCULLISES.forEach(packets::sendAddObject)
}

internal fun Player.passUndergroundPassPortcullis(lever: GameObject) {
    walkToAndExecute(lever) {
        resetWalkSteps()
        lock()
        faceObject(lever)
        sendMessage("You pull the lever.", true)
        openUndergroundPassPortcullises()
        sendMessage("The portcullis opens.", true)
        forceMove(GRID_EXIT_TILE, -1, 10, 60) {
            restoreUndergroundPassPortcullises()
            sendMessage("The portcullis closes.", true)
            unlock()
        }
    }
}

private fun Player.failUndergroundPassFloorGrid() {
    asyncTasks.cancel(FLOOR_GRID_IDLE_TASK)
    resetWalkSteps()
    lock()
    sendMessage("It's a trap!", true)
    sendMessage("You fall through and onto some spikes.", true)
    fadeScreen {
        tele(GRID_FALL_TILE)
        applyHit(Hit(150, Hit.HitLook.TRUE_DAMAGE))
        forceTalk("Ouch!")
        unlock()
    }
}

private fun Player.startUndergroundPassFloorGridIdleTimer(watchedTile: Tile) {
    schedule(FLOOR_GRID_IDLE_TASK) {
        wait(Ticks.fromSeconds(FLOOR_GRID_IDLE_SECONDS))
        if (
            tile == watchedTile &&
            watchedTile in GRID_TILES &&
            isQuestStarted(Quest.UNDERGROUND_PASS) &&
            !tempAttribs.getB(FLOOR_TRAP_DISABLED_ATTR)
        ) {
            sendMessage("You linger on the floor tile for too long.", true)
            failUndergroundPassFloorGrid()
        }
    }
}

@ServerStartupEvent
fun mapUndergroundPassFloorGrid() {
    onLogin { (player) ->
        player.tempAttribs.removeB(FLOOR_TRAP_DISABLED_ATTR)
        val inGridArea = player.getChunkId() in GRID_AREA_CHUNKS
        if (inGridArea)
            player.restoreUndergroundPassPortcullises()
        player.tempAttribs.setB(IN_FLOOR_GRID_AREA_ATTR, inGridArea)
        if (player.tile in GRID_TILES && player.isQuestStarted(Quest.UNDERGROUND_PASS))
            player.startUndergroundPassFloorGridIdleTimer(player.tile)
    }

    onChunkEnter { (entity, chunkId) ->
        val player = entity as? Player ?: return@onChunkEnter
        val enteringGridArea = chunkId in GRID_AREA_CHUNKS
        val wasInGridArea = player.tempAttribs.getB(IN_FLOOR_GRID_AREA_ATTR)
        if (enteringGridArea && !wasInGridArea)
            player.restoreUndergroundPassPortcullises()
        if (!enteringGridArea)
            player.asyncTasks.cancel(FLOOR_GRID_IDLE_TASK)
        player.tempAttribs.setB(IN_FLOOR_GRID_AREA_ATTR, enteringGridArea)
    }

    onPlayerStep(GRID_TILES) { (player, _, tile) ->
        if (!player.isQuestStarted(Quest.UNDERGROUND_PASS) || player.tempAttribs.getB(FLOOR_TRAP_DISABLED_ATTR)) {
            player.asyncTasks.cancel(FLOOR_GRID_IDLE_TASK)
            return@onPlayerStep
        }

        player.startUndergroundPassFloorGridIdleTimer(tile)
        if (tile in player.floorGridSafeTiles())
            return@onPlayerStep

        player.failUndergroundPassFloorGrid()
    }

    onObjectClick(Loc.portcullis_lever_up, tiles = arrayOf(GRID_LEVER_TILE), checkDistance = false) { (player, obj) ->
        player.passUndergroundPassPortcullis(obj)
    }

    onObjectClick(Loc.regicide_trap_hand_holds, tiles = arrayOf(GRID_FALL_TILE), checkDistance = false) { (player) ->
        player.fadeScreen { player.tele(GRID_RETURN_TILE) }
    }
}

private fun cell(southWestX: Int, southWestY: Int) = setOf(
    Tile.of(southWestX, southWestY, 0),
    Tile.of(southWestX + 1, southWestY, 0),
    Tile.of(southWestX, southWestY + 1, 0),
    Tile.of(southWestX + 1, southWestY + 1, 0)
)
