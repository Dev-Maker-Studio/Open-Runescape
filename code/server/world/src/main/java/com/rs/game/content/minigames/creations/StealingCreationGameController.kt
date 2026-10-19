package com.rs.game.content.minigames.creations

import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import org.darkan.core.Logger
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

private const val ARENA_SIZE = 8
private const val BARRIER_DECAY_TICKS = 64
private const val END_LOCK_TICKS = 7
private const val END_DELAY_TICKS = 6
private const val DESTROY_DELAY_TICKS = 2
private const val FULL_PLOT_DEGRADATION = 30
private val FINAL_COUNTDOWN = Ticks.fromMinutes(1)

class StealingCreationGameController private constructor(val blueTeam: MutableList<Player>, val redTeam: MutableList<Player>) {

    private enum class State { CREATING, RUNNING, ENDED }

    val area = GameArea(ARENA_SIZE)
    private val scores = HashMap<Player, Score>()
    private val crates = mapOf(false to HashMap<Int, Int>(), true to HashMap<Int, Int>())
    private var state = State.CREATING
    private var endTick = 0L

    val ticksLeft get() = (endTick - World.getServerTicks()).coerceAtLeast(0L).toInt()

    init {
        lockPlayers(-1)
        blueTeam.forEach { scores[it] = Score(it.displayName, false) }
        redTeam.forEach { scores[it] = Score(it.displayName, true) }
        area.calculate()
        area.create(::begin)
    }

    private fun begin() {
        blueTeam.removeIf { !it.hasStarted() || it.hasFinished() }
        redTeam.removeIf { !it.hasStarted() || it.hasFinished() }
        if (blueTeam.isEmpty() || redTeam.isEmpty()) {
            (blueTeam + redTeam).forEach { it.unlock() }
            area.destroy()
            return
        }
        state = State.RUNNING
        endTick = World.getServerTicks() + StealingCreation.GAME_DURATION
        blueTeam.forEach { enter(it, false) }
        redTeam.forEach { enter(it, true) }
        WorldTasks.scheduleTimer(0, 0) { tick ->
            if (state != State.RUNNING) return@scheduleTimer false
            if (ticksLeft <= 0) {
                end()
                return@scheduleTimer false
            }
            if (tick > 0 && tick % BARRIER_DECAY_TICKS == 0)
                for (x in 0 until area.size) for (y in 0 until area.size) damageBarrier(x, y)
            true
        }
    }

    private fun enter(player: Player, red: Boolean) {
        StealingCreation.spawn(area, player, red)
        WorldTasks.schedule(1) {
            player.controllerManager.startController(StealingCreationController(this, red))
            StealingCreation.giveCape(player, red)
        }
    }

    fun remove(player: Player) {
        if (!leave(player)) return
        StealingCreation.sendHome(player)
        player.tempAttribs.setL(StealingCreation.DESERTER_KEY, World.getServerTicks() + StealingCreation.DESERTER_PENALTY)
    }

    fun leave(player: Player): Boolean {
        if (state != State.RUNNING) return false
        if (!blueTeam.remove(player)) redTeam.remove(player)
        if (redTeam.isEmpty() || blueTeam.isEmpty()) end()
        return true
    }

    private fun end() {
        if (state != State.RUNNING) return
        state = State.ENDED
        val allScores = scores.values.toList()
        val winner = winningTeam(allScores)
        lockPlayers(END_LOCK_TICKS)
        WorldTasks.schedule(END_DELAY_TICKS) {
            for (player in blueTeam + redTeam) {
                val personal = getScore(player)
                StealingCreation.sendHome(player)
                if (personal == null) continue
                player.showScoreboard(personal, allScores, winner)
                player.awardStealingCreationPoints(personal, winner)
            }
            WorldTasks.schedule(DESTROY_DELAY_TICKS) {
                try {
                    area.destroy()
                } catch (e: Throwable) {
                    Logger.handle(StealingCreationGameController::class.java, "end", e)
                }
            }
        }
    }

    private fun lockPlayers(ticks: Int) {
        for (player in blueTeam + redTeam) {
            player.lock(ticks)
            player.stopAll()
        }
    }

    fun getScore(player: Player): Score? = scores[player]

    fun updateScore(player: Player, update: Score.() -> Unit) {
        val score = scores[player] ?: return
        score.update()
        (player.controllerManager.controller as? StealingCreationController)?.sendScore(score)
    }

    fun buildBarrier(red: Boolean, tier: BarrierTier, x: Int, y: Int): Boolean {
        if (area.getWallTeam(x, y) != 0) return false
        area.setWallTeam(x, y, teamNumber(red))
        area.setWallTier(x, y, tier.tier)
        area.setWallStatus(x, y, tier.maxHealth)
        displayBarrier(x, y)
        return true
    }

    fun repairBarrier(x: Int, y: Int): Boolean {
        val tier = BarrierTier.forTier(area.getWallTier(x, y)) ?: return false
        if (area.getWallTeam(x, y) == 0 || area.getWallStatus(x, y) >= tier.maxHealth) return false
        area.setWallStatus(x, y, minOf(area.getWallStatus(x, y) + 4, tier.maxHealth))
        displayBarrier(x, y)
        return true
    }

    fun damageBarrier(x: Int, y: Int): Boolean {
        val tier = BarrierTier.forTier(area.getWallTier(x, y)) ?: return false
        val health = area.getWallStatus(x, y)
        if (area.getWallTeam(x, y) == 0 || health <= 0) return false
        val newHealth = health - 1
        if (newHealth <= 1) {
            area.setWallTeam(x, y, 0)
            area.setWallTier(x, y, 0)
            area.setWallStatus(x, y, 0)
            displayBarrier(x, y)
            return true
        }
        area.setWallStatus(x, y, newHealth)
        if (BarrierTier.damageLevel(health, tier.tier) != BarrierTier.damageLevel(newHealth, tier.tier)) displayBarrier(x, y)
        return true
    }

    fun useSkillPlot(x: Int, y: Int): Boolean {
        val source = ClaySource.forPlot(area.getType(x, y)) ?: return false
        val tier = area.getTier(x, y)
        val degradation = area.getDegradation(x, y)
        if (tier !in 1..5 || degradation < 1) return false
        area.setDegradation(x, y, degradation - 1)
        if (degradation == 1 && clayExhausted()) startFinalCountdown()
        val look = if (degradation - 1 > FULL_PLOT_DEGRADATION) 0 else if (degradation - 1 > 0) 1 else 2
        val looks = source.spotsForTier(tier)
        val center = area.plotTile(x, y, BarrierTier.PLOT_INSET + 3, BarrierTier.PLOT_INSET + 3)
        for (dx in -1..1) for (dy in -1..1) for (index in looks.indices) {
            val spot = World.getObjectWithId(Tile.of(center.x + dx, center.y + dy, 0), looks[index]) ?: continue
            if (index != look) World.spawnObject(GameObject(looks[look], spot.type, spot.rotation, spot.tile))
            return true
        }
        return true
    }

    fun isEmpty(x: Int, y: Int) = area.getDegradation(x, y) == 0

    private fun clayExhausted() = (0 until area.size).none { x ->
        (0 until area.size).any { y -> ClaySource.forPlot(area.getType(x, y)) != null && area.getTier(x, y) in 1..5 && area.getDegradation(x, y) > 0 }
    }

    private fun startFinalCountdown() {
        if (ticksLeft <= FINAL_COUNTDOWN) return
        endTick = World.getServerTicks() + FINAL_COUNTDOWN
        for (player in blueTeam + redTeam) {
            player.sendMessage("<col=FF0000>All of the sacred clay has been gathered. The game will end in one minute!</col>")
            (player.controllerManager.controller as? StealingCreationController)?.sendTimer()
        }
    }

    private fun spawnLoc(id: Int, shape: ObjectShape, rotation: Int, x: Int, y: Int) {
        World.spawnObject(GameObject(id, shape, rotation, Tile.of(x, y, 0)))
    }

    private fun displayBarrier(x: Int, y: Int) {
        val min = area.plotTile(x, y, BarrierTier.PLOT_INSET, BarrierTier.PLOT_INSET)
        val max = area.plotTile(x, y, BarrierTier.PLOT_OUTSET, BarrierTier.PLOT_OUTSET)
        val tier = BarrierTier.forTier(area.getWallTier(x, y))
        if (tier == null || area.getWallTeam(x, y) == 0) {
            val empty = Loc.sc_barrier_hotspot
            spawnCorners(empty, min, max)
            for (wallX in min.x + 1 until max.x) {
                spawnLoc(empty, ObjectShape.WALL_STRAIGHT, 1, wallX, min.y)
                spawnLoc(empty, ObjectShape.WALL_STRAIGHT, 3, wallX, max.y)
            }
            for (wallY in min.y + 1 until max.y) {
                spawnLoc(empty, ObjectShape.WALL_STRAIGHT, 2, min.x, wallY)
                spawnLoc(empty, ObjectShape.WALL_STRAIGHT, 0, max.x, wallY)
            }
            return
        }
        val red = area.getWallTeam(x, y) == teamNumber(true)
        val wall = tier.wall(red, BarrierTier.damageLevel(area.getWallStatus(x, y), tier.tier))
        val (gateA, gateB) = tier.gates(red).let { it[0] to it[1] }
        for (offset in intArrayOf(1, 4)) {
            spawnLoc(wall, ObjectShape.WALL_STRAIGHT, 2, min.x, min.y + offset)
            spawnLoc(wall, ObjectShape.WALL_STRAIGHT, 0, max.x, min.y + offset)
            spawnLoc(wall, ObjectShape.WALL_STRAIGHT, 1, min.x + offset, min.y)
            spawnLoc(wall, ObjectShape.WALL_STRAIGHT, 3, min.x + offset, max.y)
        }
        spawnCorners(wall, min, max)
        spawnLoc(gateA, ObjectShape.WALL_STRAIGHT, 2, min.x, min.y + 3)
        spawnLoc(gateB, ObjectShape.WALL_STRAIGHT, 2, min.x, min.y + 2)
        spawnLoc(gateB, ObjectShape.WALL_STRAIGHT, 0, max.x, min.y + 3)
        spawnLoc(gateA, ObjectShape.WALL_STRAIGHT, 0, max.x, min.y + 2)
        spawnLoc(gateA, ObjectShape.WALL_STRAIGHT, 1, min.x + 2, min.y)
        spawnLoc(gateB, ObjectShape.WALL_STRAIGHT, 1, min.x + 3, min.y)
        spawnLoc(gateB, ObjectShape.WALL_STRAIGHT, 3, min.x + 2, max.y)
        spawnLoc(gateA, ObjectShape.WALL_STRAIGHT, 3, min.x + 3, max.y)
    }

    private fun spawnCorners(id: Int, min: Tile, max: Tile) {
        spawnLoc(id, ObjectShape.WALL_DIAGONAL_CORNER, 1, min.x, min.y)
        spawnLoc(id, ObjectShape.WALL_DIAGONAL_CORNER, 2, min.x, max.y)
        spawnLoc(id, ObjectShape.WALL_DIAGONAL_CORNER, 3, max.x, max.y)
        spawnLoc(id, ObjectShape.WALL_DIAGONAL_CORNER, 0, max.x, min.y)
    }

    fun deposit(player: Player, item: Item, red: Boolean) {
        if (getScore(player) == null || StealingCreation.crateFor(item.id) == null) return
        crates.getValue(red).merge(item.id, item.amount, Int::plus)
        player.incrementCount("Stealing Creation items deposited", item.amount)
        updateScore(player) { deposit(item) }
    }

    fun crateStock(red: Boolean, crate: Int): List<Item> =
        crates.getValue(red).filter { (id, amount) -> amount > 0 && StealingCreation.crateFor(id) == crate }.map { (id, amount) -> Item(id, amount) }

    fun withdraw(player: Player, red: Boolean, itemId: Int, requested: Int): Boolean {
        val score = getScore(player) ?: return false
        val stock = crates.getValue(red)
        val amount = minOf(requested, stock[itemId] ?: 0)
        if (amount <= 0) return false
        val item = Item(itemId, amount)
        val units = scoreUnits(item)
        if (!score.canWithdraw()) {
            player.sendMessage("You need a score above zero to take items from your base.")
            return false
        }
        if (!player.inventory.hasRoomFor(item)) {
            player.sendMessage("You don't have enough inventory space.")
            return false
        }
        stock[itemId] = (stock[itemId] ?: 0) - amount
        player.inventory.addItem(item)
        updateScore(player) { withdraw(units) }
        return true
    }

    companion object {
        @JvmStatic
        fun start(blueTeam: List<Player>, redTeam: List<Player>) {
            StealingCreationGameController(blueTeam.toMutableList(), redTeam.toMutableList())
        }
    }
}
