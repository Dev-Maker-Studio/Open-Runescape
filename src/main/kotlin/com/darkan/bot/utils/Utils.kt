package com.darkan.bot.utils

import com.jagex.game.runetek5.movement.pathfinding.RouteFinder
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategy
import com.jagex.game.runetek5.scene.CollisionMap
import java.util.Random
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

object Utils {

    private val RANDOM = Random()

    fun distance(t1: Tile, t2: Tile): Int {
        return sqrt(((t1.x - t2.x) * (t1.x - t2.x) + (t1.y - t2.y) * (t1.y - t2.y)).toDouble()).toInt()
    }

    fun getRandom(maxValue: Int): Int {
        return (Math.random() * (maxValue + 1)).toInt()
    }

    fun random(min: Int, max: Int): Int {
        val n = abs(max - min)
        return min(min, max) + if (n == 0) 0 else random(n)
    }

    fun random(min: Double, max: Double): Double {
        val n = abs(max - min)
        return min(min, max) + if (n == 0.0) 0.0 else random(n.toInt()).toDouble()
    }

    fun random(maxValue: Int): Int {
        if (maxValue <= 0)
            return 0
        return RANDOM.nextInt(maxValue)
    }

    fun findRouteDistance(
        srcX: Int,
        srcY: Int,
        srcSizeXY: Int,
        strategy: RouteStrategy,
        clipMap: CollisionMap,
        findAlternative: Boolean,
        bufferX: IntArray,
        bufferY: IntArray
    ): Int {
        for (x in 0 until 128) {
            for (y in 0 until 128) {
                RouteFinder.directions[x][y] = 0
                RouteFinder.distances[x][y] = 99999999
            }
        }
        val found: Boolean = when (srcSizeXY) {
            1 -> RouteFinder.performCalculationS1(srcX, srcY, strategy, clipMap)
            2 -> RouteFinder.performCalculationS2(srcX, srcY, strategy, clipMap)
            else -> RouteFinder.performCalculationSX(srcX, srcY, srcSizeXY, strategy, clipMap)
        }
        val graphBaseX = srcX - 64
        val graphBaseY = srcY - 64
        var endX = RouteFinder.exitX
        var endY = RouteFinder.exitY
        if (!found) {
            if (findAlternative) {
                var lowestCost = Int.MAX_VALUE
                var lowestDistance = Int.MAX_VALUE
                val alternativeRouteRange = 10
                val approxDestX = strategy.destinationX
                val approxDestY = strategy.destinationY
                val approxDestinationSizeX = strategy.destinationSizeX
                val approxDestinationSizeY = strategy.destinationSizeY
                for (checkX in approxDestX - alternativeRouteRange..approxDestX + alternativeRouteRange) {
                    for (checkY in approxDestY - alternativeRouteRange..approxDestY + alternativeRouteRange) {
                        val graphX = checkX - graphBaseX
                        val graphY = checkY - graphBaseY
                        if (graphX >= 0 && graphY >= 0 && graphX < 128 && graphY < 128 && (RouteFinder.distances[graphX][graphY] < 100)) {
                            var deltaX = 0
                            if (checkX < approxDestX)
                                deltaX = approxDestX - checkX
                            else if (checkX > approxDestinationSizeX + approxDestX - 1)
                                deltaX = checkX - (approxDestX + approxDestinationSizeX - 1)
                            var deltaY = 0
                            if (checkY < approxDestY)
                                deltaY = approxDestY - checkY
                            else if (checkY > approxDestinationSizeY + approxDestY - 1)
                                deltaY = checkY - (approxDestY + approxDestinationSizeY - 1)
                            val cost = deltaX * deltaX + deltaY * deltaY
                            if (cost < lowestCost || (cost == lowestCost && (RouteFinder.distances[graphX][graphY]) < lowestDistance)) {
                                lowestCost = cost
                                lowestDistance = RouteFinder.distances[graphX][graphY]
                                endX = checkX
                                endY = checkY
                            }
                        }
                    }
                }
                if (lowestCost == Int.MAX_VALUE)
                    return -1
            } else {
                if (srcX == endX && srcY == endY)
                    return 0
                return -1
            }
        }
        if (srcX == endX && srcY == endY)
            return 0
        var steps = 0
        var realSteps = 0
        RouteFinder.bufferX[steps] = endX
        RouteFinder.bufferY[steps++] = endY
        var lastwritten: Int
        var direction = RouteFinder.directions[endX - graphBaseX][endY - graphBaseY].also { lastwritten = it }
        while (srcX != endX || endY != srcY) {
            if (lastwritten != direction) {
                lastwritten = direction
                RouteFinder.bufferX[steps] = endX
                RouteFinder.bufferY[steps++] = endY
            }
            if ((direction and 0x2) != 0)
                endX++
            else if ((direction and 0x8) != 0)
                endX--
            if ((direction and 0x1) != 0)
                endY++
            else if ((direction and 0x4) != 0)
                endY--
            realSteps++
            direction = RouteFinder.directions[endX - graphBaseX][endY - graphBaseY]
        }
        return realSteps
    }

    fun stringContainsIgnoreCase(options: Array<String>?, option: String): Int {
        if (options == null)
            return -1
        for (i in options.indices) {
            if (options[i].equals(option, ignoreCase = true))
                return i + 1
        }
        return -1
    }
}
