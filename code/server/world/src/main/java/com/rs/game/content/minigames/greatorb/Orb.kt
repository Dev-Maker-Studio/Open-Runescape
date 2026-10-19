package com.rs.game.content.minigames.greatorb

import com.rs.game.World
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.interactions.EntityInteraction
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile
import kotlin.math.abs
import kotlin.math.sign

class Orb(private val game: GreatOrbGame, val team: OrbTeam, tile: Tile) : NPC(team.orb, tile, true) {
    var holder: Player? = null
    var lastMover: Player? = null
        private set
    var scored = false
        private set
    private var attract = true
    private var heldAt = 0
    private var pending: Direction? = null

    init {
        setRandomWalk(false)
        setIgnoreDocile(true)
        anim(Seq.rcguild_orb_spawn)
    }

    override fun processNPC() {}

    fun hold(player: Player, attract: Boolean) {
        holder = player
        lastMover = player
        this.attract = attract
        heldAt = game.clock
    }

    fun release(player: Player) {
        if (holder == player) holder = null
    }

    fun nudge(player: Player, attract: Boolean) {
        pending = steps(player.tile, attract).first
    }

    fun tick() {
        if (scored) return
        if (game.inScoringArea(tile)) {
            scored = true
            holder = null
            pending = null
            resetWalkSteps()
            anim(Seq.rcguild_orb_despawn)
            game.score(this)
            return
        }
        val player = holder?.takeIf { heldAt >= game.clock - 1 }
        val direction = if (player != null) {
            val (first, second) = steps(player.tile, attract)
            pending = second
            first
        } else pending.also { pending = null }
        direction?.let { move(it, player?.tile) }
    }

    private fun steps(from: Tile, attract: Boolean): Pair<Direction?, Direction?> {
        val towards = if (attract) 1 else -1
        val dx = (from.x - x) * towards
        val dy = (from.y - y) * towards
        val ax = abs(dx)
        val ay = abs(dy)
        val horizontal = Direction.forDelta(dx.sign, 0)
        val vertical = Direction.forDelta(0, dy.sign)
        val diagonal = Direction.forDelta(dx.sign, dy.sign)
        return when {
            ax == 0 && ay == 0 -> null to null
            2 * ax < ay -> vertical to vertical
            ax < ay -> diagonal to vertical
            ax == ay -> diagonal to diagonal
            ax <= 2 * ay -> diagonal to horizontal
            else -> horizontal to horizontal
        }
    }

    private fun move(direction: Direction, occupied: Tile?) {
        val options = if (direction.dx != 0 && direction.dy != 0)
            listOfNotNull(direction, Direction.forDelta(direction.dx, 0), Direction.forDelta(0, direction.dy))
        else listOf(direction)
        val step = options.firstOrNull { World.checkWalkStep(tile, it, 1) && tile.transform(it.dx, it.dy) != occupied } ?: return
        resetWalkSteps()
        addWalkSteps(x + step.dx, y + step.dy, 1, false)
    }
}

class OrbControl(private val game: GreatOrbGame, private val orb: Orb, private val attract: Boolean, reach: Int) : EntityInteraction(orb, reach) {
    private var instructed = false

    init {
        keepFacing()
    }

    override fun canStart(entity: Entity) = true

    override fun checkAll(entity: Entity): Boolean {
        val player = entity as? Player ?: return false
        return !orb.scored && game.mayInstruct(player, orb, quiet = instructed)
    }

    override fun interact(entity: Entity) {
        val player = entity as? Player ?: return
        game.instructed(player, orb, attract, first = !instructed)
        if (!instructed && player.orbTeam == orb.team) continueAfterReached()
        instructed = true
    }

    override fun onStop(entity: Entity) {}

    override fun stop(entity: Entity) {
        super.stop(entity)
        (entity as? Player)?.let(orb::release)
    }
}
