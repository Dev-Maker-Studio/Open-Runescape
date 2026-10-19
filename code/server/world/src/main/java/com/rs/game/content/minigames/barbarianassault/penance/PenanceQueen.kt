package com.rs.game.content.minigames.barbarianassault.penance

import com.rs.game.World
import com.rs.game.content.minigames.barbarianassault.BarbarianAssaultGame
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.utils.WorldUtil
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile
import kotlin.math.sign

private const val EGG_INTERVAL = 18
private const val SPAWN_INTERVAL = 45
private const val MAX_SPAWNS = 2
private const val RANGED_INTERVAL = 15
private const val RANGED_REACH = 12
private const val RANGED_MAX = 40
private const val MELEE_INTERVAL = 5
private const val MELEE_MAX = 130
private const val MOVE_INTERVAL = 3
private const val RANGED_SPEED = 5

class PenanceQueen(tile: Tile, val game: BarbarianAssaultGame) : NPC(Npc.npc_5247, tile, true) {
    var dying = false
        private set
    private var age = 0
    private val spawns = ArrayList<QueenSpawn>()

    init {
        setRandomWalk(false)
        setForceMultiArea(true)
        setNoDistanceCheck(true)
        setCantInteract(false)
        anim(Seq.barbassault_queen_new_trapdoor)
    }

    override fun processNPC() {
        if (dying || isDead) return
        age++
        spawns.removeIf { it.isDead || it.hasFinished() }
        if (age % EGG_INTERVAL == 1) game.dropYellowEgg(this)
        if (age % SPAWN_INTERVAL == 0 && spawns.size < MAX_SPAWNS) spawnQueenSpawn()
        val players = game.players.filter { game.isPlaying(it) }
        val adjacent = players.firstOrNull { WorldUtil.isInRange(this, it, 0) }
        when {
            adjacent != null && age % MELEE_INTERVAL == 0 -> melee(adjacent)
            age % RANGED_INTERVAL == 0 -> ranged(players.filter { WorldUtil.isInRange(this, it, RANGED_REACH) })
            age % MOVE_INTERVAL == 0 && adjacent == null -> players.minByOrNull { it.tile.distanceTo(middleTile) }?.let { approach(it) }
        }
    }

    private fun spawnQueenSpawn() {
        val tile = (0 until 10).asSequence().map { this.tile.transform(Utils.random(-1, size), Utils.random(-1, size)) }
            .firstOrNull { World.floorAndWallsFree(it, 1) && !WorldUtil.collides(it, this.tile, 1, size) } ?: return
        val spawn = QueenSpawn(tile, game)
        spawns += spawn
        game.queenSpawned(spawn)
    }

    private fun melee(target: Player) {
        faceEntity(target)
        anim(Seq.barbassault_penacne_queen_new_attack)
        target.applyHit(Hit(this, Utils.random(MELEE_MAX + 1), HitLook.MELEE_DAMAGE), 1)
    }

    private fun ranged(targets: List<Player>) {
        if (targets.isEmpty()) return
        anim(Seq.barbassault_penacne_queen_new_attack_ranged)
        spotAnim(Spotanim.barbassault_penance_queen_range_attack_casting)
        for (target in targets) {
            World.sendProjectile(this, target, Spotanim.barbassault_penance_queen_range_attack_travel, 80 to 30, 0, RANGED_SPEED, 16) {
                if (!dying && game.isPlaying(target)) {
                    target.spotAnim(Spotanim.barbassault_penance_queen_range_attack_impact)
                    target.applyHit(Hit(this, Utils.random(RANGED_MAX + 1), HitLook.RANGE_DAMAGE))
                }
            }
        }
    }

    private fun approach(target: Player) {
        val dx = (target.x - x).sign
        val dy = (target.y - y).sign
        val direction = Direction.forDelta(dx, dy) ?: return
        if (World.checkWalkStep(tile, direction, size)) walkOneStep(dx, dy, true)
    }

    fun omegaHit(source: Player, damage: Int) {
        if (dying) return
        applyHit(Hit(source, damage.coerceAtMost(hitpoints), HitLook.CANNON_DAMAGE))
    }

    override fun getPossibleTargets(): List<Entity> = emptyList()

    override fun sendDeath(source: Entity) {
        if (dying) return
        dying = true
        spawns.forEach(NPC::finish)
        game.sendTeamMessage("The Penance Queen has been slain!")
        super.sendDeath(source)
    }

    override fun drop() {
        mostDamageReceivedSourcePlayer?.sendNPCKill(name)
    }
}

class QueenSpawn(tile: Tile, val game: BarbarianAssaultGame) : NPC(Npc.barbassault_pen_queen_spawn, tile, true) {
    init {
        setRandomWalk(false)
        setForceMultiArea(true)
        setNoDistanceCheck(true)
        setForceAgressive(true)
        setForceAggroDistance(RANGED_REACH)
    }

    override fun getPossibleTargets(): List<Entity> = game.players.filter { game.isPlaying(it) && withinDistance(it.tile, RANGED_REACH) }

    override fun drop() {
        mostDamageReceivedSourcePlayer?.sendNPCKill(name)
    }
}
