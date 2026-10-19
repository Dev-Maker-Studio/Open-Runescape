package com.rs.engine.pathfinder

import org.darkan.core.map.WorldCollision
import org.darkan.core.map.pathfinder.StepValidator
import org.darkan.core.map.pathfinder.collision.CollisionStrategy
import com.rs.game.model.entity.Entity
import world.gregs.voidps.type.Tile
import com.rs.utils.WorldUtil
import java.util.*

object DumbRouteFinder {
    @JvmStatic
	fun addDumbPathfinderSteps(entity: Entity, target: Any, collision: CollisionStrategy): Boolean {
        return addDumbPathfinderSteps(entity, target, 25, collision)
    }

    @JvmStatic
	fun addDumbPathfinderSteps(entity: Entity, target: Any, maxSize: Int, collision: CollisionStrategy): Boolean {
        val tiles = find(entity, target, maxSize, collision)
        if (tiles.size > 0) {
            var last = entity.tile
            for (t in tiles) {
                entity.addWalkStep(t.x, t.y, last.x, last.y, true)
                last = t
            }
            return true
        }
        return false
    }

    private fun find(origin: Any, target: Any, maxSize: Int, collision: CollisionStrategy): Deque<Tile> {
        val originTile = WorldUtil.targetToTile(origin)
        val targetTile = WorldUtil.targetToTile(target)
        val size = if (origin is Entity) origin.size else 1
        var real = originTile
        var curr = if (origin is Entity) origin.middleTile else originTile
        val targ = if (target is Entity) target.middleTile else targetTile
        val positions: Deque<Tile> = ArrayDeque(maxSize)
        val step = StepValidator(WorldCollision.allFlags)
        while (true) {
            val from = curr
            if (curr.x < targ.x && curr.y < targ.y) {
                if (step.canTravel(real.plane, real.x, real.y, 1, 1, size, 0, collision)) {
                    real = add(positions, real.transform(1, 1))
                    curr = curr.transform(1, 1)
                } else if (step.canTravel(real.plane, real.x, real.y, 1, 0, size, 0, collision)) {
                    real = add(positions, real.transform(1, 0))
                    curr = curr.transform(1, 0)
                } else if (step.canTravel(real.plane, real.x, real.y, 0, 1, size, 0, collision)) {
                    real = add(positions, real.transform(0, 1))
                    curr = curr.transform(0, 1)
                }
            } else if (curr.x > targ.x && curr.y > targ.y) {
                if (step.canTravel(real.plane, real.x, real.y, -1, -1, size, 0, collision)) {
                    real = add(positions, real.transform(-1, -1))
                    curr = curr.transform(-1, -1)
                } else if (step.canTravel(real.plane, real.x, real.y, -1, 0, size, 0, collision)) {
                    real = add(positions, real.transform(-1, 0))
                    curr = curr.transform(-1, 0)
                } else if (step.canTravel(real.plane, real.x, real.y, 0, -1, size, 0, collision)) {
                    real = add(positions, real.transform(0, -1))
                    curr = curr.transform(0, -1)
                }
            } else if (curr.x < targ.x && curr.y > targ.y) {
                if (step.canTravel(real.plane, real.x, real.y, 1, -1, size, 0, collision)) {
                    real = add(positions, real.transform(1, -1))
                    curr = curr.transform(1, -1)
                } else if (step.canTravel(real.plane, real.x, real.y, 1, 0, size, 0, collision)) {
                    real = add(positions, real.transform(1, 0))
                    curr = curr.transform(1, 0)
                } else if (step.canTravel(real.plane, real.x, real.y, 0, -1, size, 0, collision)) {
                    real = add(positions, real.transform(0, -1))
                    curr = curr.transform(0, -1)
                }
            } else if (curr.x > targ.x && curr.y < targ.y) {
                if (step.canTravel(real.plane, real.x, real.y, -1, 1, size, 0, collision)) {
                    real = add(positions, real.transform(-1, 1))
                    curr = curr.transform(-1, 1)
                } else if (step.canTravel(real.plane, real.x, real.y, -1, 0, size, 0, collision)) {
                    real = add(positions, real.transform(-1, 0))
                    curr = curr.transform(-1, 0)
                } else if (step.canTravel(real.plane, real.x, real.y, 0, 1, size, 0, collision)) {
                    real = add(positions, real.transform(0, 1))
                    curr = curr.transform(0, 1)
                }
            } else if (curr.x < targ.x) {
                if (step.canTravel(real.plane, real.x, real.y, 1, 0, size, 0, collision)) {
                    real = add(positions, real.transform(1, 0))
                    curr = curr.transform(1, 0)
                }
            } else if (curr.x > targ.x) {
                if (step.canTravel(real.plane, real.x, real.y, -1, 0, size, 0, collision)) {
                    real = add(positions, real.transform(-1, 0))
                    curr = curr.transform(-1, 0)
                }
            } else if (curr.y < targ.y) {
                if (step.canTravel(real.plane, real.x, real.y, 0, 1, size, 0, collision)) {
                    real = add(positions, real.transform(0, 1))
                    curr = curr.transform(0, 1)
                }
            } else if (curr.y > targ.y) if (step.canTravel(real.plane, real.x, real.y, 0, -1, size, 0, collision)) {
                real = add(positions, real.transform(0, -1))
                curr = curr.transform(0, -1)
            }
            if (curr.matches(from)) break
        }
        return positions
    }

    private fun add(positions: Deque<Tile>, att: Tile): Tile {
        positions.add(att)
        return att
    }
}
