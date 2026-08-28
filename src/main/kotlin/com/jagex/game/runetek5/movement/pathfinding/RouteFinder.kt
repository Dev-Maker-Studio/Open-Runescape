package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.periodic.PulseEventQueue
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.scene.CollisionFlag
import com.jagex.game.runetek5.scene.CollisionMap

object RouteFinder {
    const val DIR_NORTH = 0x1
    const val DIR_EAST = 0x2
    const val DIR_SOUTH = 0x4
    const val DIR_WEST = 0x8
    const val GRAPH_SIZE = 128
    const val QUEUE_SIZE = 4095

    @JvmField
    var exitY: Int = 0
    @JvmField
    var exitX: Int = 0

    private fun createDirectionsArray(): Array<IntArray> {
        val result = arrayOfNulls<IntArray>(128)
        var i = 0
        while (i < 128) {
            result[i] = IntArray(128)
            i++
        }
        @Suppress("UNCHECKED_CAST")
        return result as Array<IntArray>
    }

    private fun createDistancesArray(): Array<IntArray> {
        val result = arrayOfNulls<IntArray>(128)
        var i = 0
        while (i < 128) {
            result[i] = IntArray(128)
            i++
        }
        @Suppress("UNCHECKED_CAST")
        return result as Array<IntArray>
    }

    @JvmField
    val directions: Array<IntArray> = createDirectionsArray()
    @JvmField
    val distances: Array<IntArray> = createDistancesArray()
    @JvmField
    val bufferX: IntArray = IntArray(4096)
    @JvmField
    val bufferY: IntArray = IntArray(4096)

    @JvmStatic
    fun findRoute(
        srcX: Int,
        srcY: Int,
        entitySize: Int,
        strategy: RouteStrategy,
        clipping: CollisionMap,
        findAlternative: Boolean,
        stepsX: IntArray,
        stepsY: IntArray
    ): Int {
        for (sceneX in 0 until 128) {
            for (sceneY in 0 until 128) {
                directions[sceneX][sceneY] = 0
                distances[sceneX][sceneY] = 99999999
            }
        }

        val canReach: Boolean = when (entitySize) {
            1 -> performCalculationS1(srcX, srcY, strategy, clipping)
            2 -> performCalculationS2(srcX, srcY, strategy, clipping)
            else -> performCalculationSX(srcX, srcY, entitySize, strategy, clipping)
        }

        val graphBaseX = srcX - 64
        val graphBaseY = srcY - 64
        var pathExitX = exitX
        var pathExitY = exitY
        val nearestDistSq: Int
        val nearestSteps: Int

        if (!canReach) {
            if (!findAlternative) {
                return -1
            }
            nearestDistSq = Int.MAX_VALUE
            var bestSteps = Int.MAX_VALUE
            val searchRadius: Byte = 10
            val destX = strategy.destinationX
            val destY = strategy.destinationY
            val destSizeX = strategy.destinationSizeX
            val destSizeY = strategy.destinationSizeY

            for (x in destX - searchRadius..destX + searchRadius) {
                for (y in destY - searchRadius..destY + searchRadius) {
                    val graphX = x - graphBaseX
                    val graphY = y - graphBaseY
                    if (graphX >= 0 && graphY >= 0 && graphX < 128 && graphY < 128 && distances[graphX][graphY] < 100) {
                        var dx = 0
                        if (x < destX) {
                            dx = destX - x
                        } else if (x > destSizeX + destX - 1) {
                            dx = x - (destX + destSizeX - 1)
                        }
                        var dy = 0
                        if (y < destY) {
                            dy = destY - y
                        } else if (y > destSizeY + destY - 1) {
                            dy = y - (destY + destSizeY - 1)
                        }
                        val distSq = dx * dx + dy * dy
                        if (distSq < nearestDistSq || distSq == nearestDistSq && distances[graphX][graphY] < bestSteps) {
                            bestSteps = distances[graphX][graphY]
                            pathExitX = x
                            pathExitY = y
                        }
                    }
                }
            }
            if (nearestDistSq == Int.MAX_VALUE) {
                return -1
            }
            nearestSteps = bestSteps
        } else {
            nearestSteps = 0
        }

        if (srcX == pathExitX && pathExitY == srcY) {
            return 0
        } else {
            val startIndex: Byte = 0
            bufferX[startIndex.toInt()] = pathExitX
            var stepCount = startIndex + 1
            bufferY[startIndex.toInt()] = pathExitY
            var lastDirection = directions[pathExitX - graphBaseX][pathExitY - graphBaseY]
            var direction = lastDirection

            while (srcX != pathExitX || pathExitY != srcY) {
                if (lastDirection != direction) {
                    lastDirection = direction
                    bufferX[stepCount] = pathExitX
                    bufferY[stepCount++] = pathExitY
                }
                if ((direction and 0x2) != 0) {
                    ++pathExitX
                } else if ((direction and 0x8) != 0) {
                    --pathExitX
                }
                if ((direction and 0x1) != 0) {
                    ++pathExitY
                } else if ((direction and 0x4) != 0) {
                    --pathExitY
                }
                direction = directions[pathExitX - graphBaseX][pathExitY - graphBaseY]
            }

            var stepIndex = 0
            while (stepCount-- > 0) {
                stepsX[stepIndex] = bufferX[stepCount]
                stepsY[stepIndex++] = bufferY[stepCount]
                if (stepIndex >= stepsX.size) {
                    break
                }
            }
            return stepIndex
        }
    }

    @JvmStatic
    fun performCalculationS1(srcX: Int, srcY: Int, strategy: RouteStrategy, clipMap: CollisionMap): Boolean {
        var currentX = srcX
        var currentY = srcY
        val graphBaseX = srcX - 64
        val graphBaseY = srcY - 64
        directions[64][64] = 99
        distances[64][64] = 0
        var read = 0
        var write = 1
        bufferX[0] = srcX
        bufferY[0] = srcY
        val clip = clipMap.collisionFlags

        while (read != write) {
            currentX = bufferX[read]
            currentY = bufferY[read]
            read = read + 1 and QUEUE_SIZE
            val currentGraphX = currentX - graphBaseX
            val currentGraphY = currentY - graphBaseY
            val clipX = currentX - clipMap.offsetX
            val clipY = currentY - clipMap.offsetY

            if (strategy.canExit(1, currentX, currentY, clipMap)) {
                exitX = currentX
                exitY = currentY
                return true
            }

            val nextDistance = distances[currentGraphX][currentGraphY] + 1

            if (currentGraphX > 0 && directions[currentGraphX - 1][currentGraphY] == 0 &&
                CollisionMap.notFlagged(
                    clip[clipX - 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX - 1
                bufferY[write] = currentY
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX - 1][currentGraphY] = DIR_EAST
                distances[currentGraphX - 1][currentGraphY] = nextDistance
            }

            if (currentGraphX < 127 && directions[currentGraphX + 1][currentGraphY] == 0 &&
                CollisionMap.notFlagged(
                    clip[clipX + 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX + 1
                bufferY[write] = currentY
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX + 1][currentGraphY] = DIR_WEST
                distances[currentGraphX + 1][currentGraphY] = nextDistance
            }

            if (currentGraphY > 0 && directions[currentGraphX][currentGraphY - 1] == 0 &&
                CollisionMap.notFlagged(
                    clip[clipX][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX
                bufferY[write] = currentY - 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX][currentGraphY - 1] = DIR_NORTH
                distances[currentGraphX][currentGraphY - 1] = nextDistance
            }

            if (currentGraphY < 127 && directions[currentGraphX][currentGraphY + 1] == 0 &&
                CollisionMap.notFlagged(
                    clip[clipX][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX
                bufferY[write] = currentY + 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX][currentGraphY + 1] = DIR_SOUTH
                distances[currentGraphX][currentGraphY + 1] = nextDistance
            }

            if (currentGraphX > 0 && currentGraphY > 0 && directions[currentGraphX - 1][currentGraphY - 1] == 0 &&
                CollisionMap.notFlagged(
                    clip[clipX - 1][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) &&
                CollisionMap.notFlagged(
                    clip[clipX - 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) &&
                CollisionMap.notFlagged(
                    clip[clipX][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX - 1
                bufferY[write] = currentY - 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX - 1][currentGraphY - 1] = 3
                distances[currentGraphX - 1][currentGraphY - 1] = nextDistance
            }

            if (currentGraphX < 127 && currentGraphY > 0 && directions[currentGraphX + 1][currentGraphY - 1] == 0 &&
                CollisionMap.notFlagged(
                    clip[clipX + 1][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) &&
                CollisionMap.notFlagged(
                    clip[clipX + 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) &&
                CollisionMap.notFlagged(
                    clip[clipX][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX + 1
                bufferY[write] = currentY - 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX + 1][currentGraphY - 1] = 9
                distances[currentGraphX + 1][currentGraphY - 1] = nextDistance
            }

            if (currentGraphX > 0 && currentGraphY < 127 && directions[currentGraphX - 1][currentGraphY + 1] == 0 &&
                CollisionMap.notFlagged(
                    clip[clipX - 1][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) &&
                CollisionMap.notFlagged(
                    clip[clipX - 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) &&
                CollisionMap.notFlagged(
                    clip[clipX][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX - 1
                bufferY[write] = currentY + 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX - 1][currentGraphY + 1] = 6
                distances[currentGraphX - 1][currentGraphY + 1] = nextDistance
            }

            if (currentGraphX < 127 && currentGraphY < 127 && directions[currentGraphX + 1][currentGraphY + 1] == 0 &&
                CollisionMap.notFlagged(
                    clip[clipX + 1][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) &&
                CollisionMap.notFlagged(
                    clip[clipX + 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) &&
                CollisionMap.notFlagged(
                    clip[clipX][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX + 1
                bufferY[write] = currentY + 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX + 1][currentGraphY + 1] = 12
                distances[currentGraphX + 1][currentGraphY + 1] = nextDistance
            }
        }

        exitX = currentX
        exitY = currentY
        return false
    }

    @JvmStatic
    fun performCalculationS2(srcX: Int, srcY: Int, strategy: RouteStrategy, clipMap: CollisionMap): Boolean {
        var currentX = srcX
        var currentY = srcY
        val centerX: Byte = 64
        val centerY: Byte = 64
        val graphBaseX = srcX - centerX
        val graphBaseY = srcY - centerY
        directions[centerX.toInt()][centerY.toInt()] = 99
        distances[centerX.toInt()][centerY.toInt()] = 0
        val startIndex: Byte = 0
        var read = 0
        bufferX[startIndex.toInt()] = srcX
        val startIndexCopy = startIndex
        var write = startIndex + 1
        bufferY[startIndexCopy.toInt()] = srcY
        val clip = clipMap.collisionFlags

        while (read != write) {
            currentX = bufferX[read]
            currentY = bufferY[read]
            read = read + 1 and QUEUE_SIZE
            val currentGraphX = currentX - graphBaseX
            val currentGraphY = currentY - graphBaseY
            val clipX = currentX - clipMap.offsetX
            val clipY = currentY - clipMap.offsetY

            if (strategy.canExit(2, currentX, currentY, clipMap)) {
                exitX = currentX
                exitY = currentY
                return true
            }

            val nextDistance = distances[currentGraphX][currentGraphY] + 1

            if (currentGraphX > 0 && directions[currentGraphX - 1][currentGraphY] == 0 && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX - 1
                bufferY[write] = currentY
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX - 1][currentGraphY] = 2
                distances[currentGraphX - 1][currentGraphY] = nextDistance
            }

            if (currentGraphX < 126 && directions[currentGraphX + 1][currentGraphY] == 0 && CollisionMap.notFlagged(
                    clip[clipX + 2][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + 2][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX + 1
                bufferY[write] = currentY
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX + 1][currentGraphY] = 8
                distances[currentGraphX + 1][currentGraphY] = nextDistance
            }

            if (currentGraphY > 0 && directions[currentGraphX][currentGraphY - 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + 1][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX
                bufferY[write] = currentY - 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX][currentGraphY - 1] = 1
                distances[currentGraphX][currentGraphY - 1] = nextDistance
            }

            if (currentGraphY < 126 && directions[currentGraphX][currentGraphY + 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX][clipY + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + 1][clipY + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX
                bufferY[write] = currentY + 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX][currentGraphY + 1] = 4
                distances[currentGraphX][currentGraphY + 1] = nextDistance
            }

            if (currentGraphX > 0 && currentGraphY > 0 && directions[currentGraphX - 1][currentGraphY - 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX - 1
                bufferY[write] = currentY - 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX - 1][currentGraphY - 1] = 3
                distances[currentGraphX - 1][currentGraphY - 1] = nextDistance
            }

            if (currentGraphX < 126 && currentGraphY > 0 && directions[currentGraphX + 1][currentGraphY - 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX + 1][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + 2][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + 2][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX + 1
                bufferY[write] = currentY - 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX + 1][currentGraphY - 1] = 9
                distances[currentGraphX + 1][currentGraphY - 1] = nextDistance
            }

            if (currentGraphX > 0 && currentGraphY < 126 && directions[currentGraphX - 1][currentGraphY + 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX][clipY + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX - 1
                bufferY[write] = currentY + 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX - 1][currentGraphY + 1] = 6
                distances[currentGraphX - 1][currentGraphY + 1] = nextDistance
            }

            if (currentGraphX < 126 && currentGraphY < 126 && directions[currentGraphX + 1][currentGraphY + 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX + 1][clipY + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + 2][clipY + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + 2][clipY + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[write] = currentX + 1
                bufferY[write] = currentY + 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX + 1][currentGraphY + 1] = 12
                distances[currentGraphX + 1][currentGraphY + 1] = nextDistance
            }
        }

        exitX = currentX
        exitY = currentY
        return false
    }

    @JvmStatic
    fun performCalculationSX(
        srcX: Int,
        srcY: Int,
        entitySize: Int,
        strategy: RouteStrategy,
        clipMap: CollisionMap
    ): Boolean {
        var currentX = srcX
        var currentY = srcY
        val centerX: Byte = 64
        val centerY: Byte = 64
        val graphBaseX = srcX - centerX
        val graphBaseY = srcY - centerY
        directions[centerX.toInt()][centerY.toInt()] = 99
        distances[centerX.toInt()][centerY.toInt()] = 0
        val startIndex: Byte = 0
        var read = 0
        bufferX[startIndex.toInt()] = srcX
        var write = startIndex + 1
        bufferY[startIndex.toInt()] = srcY
        val clip = clipMap.collisionFlags

        bfs@ while (write != read) {
            currentX = bufferX[read]
            currentY = bufferY[read]
            read = read + 1 and QUEUE_SIZE
            val currentGraphX = currentX - graphBaseX
            val currentGraphY = currentY - graphBaseY
            val clipX = currentX - clipMap.offsetX
            val clipY = currentY - clipMap.offsetY

            if (strategy.canExit(entitySize, currentX, currentY, clipMap)) {
                exitX = currentX
                exitY = currentY
                return true
            }

            val nextDistance = distances[currentGraphX][currentGraphY] + 1
            var i: Int

            if (currentGraphX > 0 && directions[currentGraphX - 1][currentGraphY] == 0 && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY + entitySize - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i = 1
                while (true) {
                    if (i >= entitySize - 1) {
                        bufferX[write] = currentX - 1
                        bufferY[write] = currentY
                        write = write + 1 and QUEUE_SIZE
                        directions[currentGraphX - 1][currentGraphY] = 2
                        distances[currentGraphX - 1][currentGraphY] = nextDistance
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            clip[clipX - 1][i + clipY],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.NE_ALT_OBJ,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        )
                    ) {
                        break
                    }
                    ++i
                }
            }

            if (currentGraphX < 128 - entitySize && directions[currentGraphX + 1][currentGraphY] == 0 && CollisionMap.notFlagged(
                    clip[clipX + entitySize][clipY],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + entitySize][clipY + entitySize - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i = 1
                while (true) {
                    if (i >= entitySize - 1) {
                        bufferX[write] = currentX + 1
                        bufferY[write] = currentY
                        write = write + 1 and QUEUE_SIZE
                        directions[currentGraphX + 1][currentGraphY] = 8
                        distances[currentGraphX + 1][currentGraphY] = nextDistance
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            clip[clipX + entitySize][i + clipY],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.NW_ALT_OBJ,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.SW_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        )
                    ) {
                        break
                    }
                    ++i
                }
            }

            if (currentGraphY > 0 && directions[currentGraphX][currentGraphY - 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + entitySize - 1][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i = 1
                while (true) {
                    if (i >= entitySize - 1) {
                        bufferX[write] = currentX
                        bufferY[write] = currentY - 1
                        write = write + 1 and QUEUE_SIZE
                        directions[currentGraphX][currentGraphY - 1] = 1
                        distances[currentGraphX][currentGraphY - 1] = nextDistance
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            clip[i + clipX][clipY - 1],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.NW_ALT_OBJ,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.NE_ALT_OBJ,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        )
                    ) {
                        break
                    }
                    ++i
                }
            }

            if (currentGraphY < 128 - entitySize && directions[currentGraphX][currentGraphY + 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX][clipY + entitySize],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    clip[clipX + entitySize - 1][clipY + entitySize],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i = 1
                while (true) {
                    if (i >= entitySize - 1) {
                        bufferX[write] = currentX
                        bufferY[write] = currentY + 1
                        write = write + 1 and QUEUE_SIZE
                        directions[currentGraphX][currentGraphY + 1] = 4
                        distances[currentGraphX][currentGraphY + 1] = nextDistance
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            clip[clipX + i][clipY + entitySize],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.SW_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        )
                    ) {
                        break
                    }
                    ++i
                }
            }

            if (currentGraphX > 0 && currentGraphY > 0 && directions[currentGraphX - 1][currentGraphY - 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i = 1
                while (true) {
                    if (i >= entitySize) {
                        bufferX[write] = currentX - 1
                        bufferY[write] = currentY - 1
                        write = write + 1 and QUEUE_SIZE
                        directions[currentGraphX - 1][currentGraphY - 1] = 3
                        distances[currentGraphX - 1][currentGraphY - 1] = nextDistance
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            clip[clipX - 1][i + (clipY - 1)],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.NE_ALT_OBJ,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        ) || !CollisionMap.notFlagged(
                            clip[i + (clipX - 1)][clipY - 1],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.NW_ALT_OBJ,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.NE_ALT_OBJ,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        )
                    ) {
                        break
                    }
                    ++i
                }
            }

            if (currentGraphX < 128 - entitySize && currentGraphY > 0 && directions[currentGraphX + 1][currentGraphY - 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX + entitySize][clipY - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i = 1
                while (true) {
                    if (i >= entitySize) {
                        bufferX[write] = currentX + 1
                        bufferY[write] = currentY - 1
                        write = write + 1 and QUEUE_SIZE
                        directions[currentGraphX + 1][currentGraphY - 1] = 9
                        distances[currentGraphX + 1][currentGraphY - 1] = nextDistance
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            clip[clipX + entitySize][i + (clipY - 1)],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.NW_ALT_OBJ,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.SW_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        ) || !CollisionMap.notFlagged(
                            clip[i + clipX][clipY - 1],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.NW_ALT_OBJ,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.NE_ALT_OBJ,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        )
                    ) {
                        break
                    }
                    ++i
                }
            }

            if (currentGraphX > 0 && currentGraphY < 128 - entitySize && directions[currentGraphX - 1][currentGraphY + 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX - 1][clipY + entitySize],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i = 1
                while (true) {
                    if (i >= entitySize) {
                        bufferX[write] = currentX - 1
                        bufferY[write] = currentY + 1
                        write = write + 1 and QUEUE_SIZE
                        directions[currentGraphX - 1][currentGraphY + 1] = 6
                        distances[currentGraphX - 1][currentGraphY + 1] = nextDistance
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            clip[clipX - 1][i + clipY],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.NE_ALT_OBJ,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        ) || !CollisionMap.notFlagged(
                            clip[i + (clipX - 1)][clipY + entitySize],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.SW_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        )
                    ) {
                        break
                    }
                    ++i
                }
            }

            if (currentGraphX < 128 - entitySize && currentGraphY < 128 - entitySize && directions[currentGraphX + 1][currentGraphY + 1] == 0 && CollisionMap.notFlagged(
                    clip[clipX + entitySize][clipY + entitySize],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i = 1
                while (i < entitySize) {
                    if (!CollisionMap.notFlagged(
                            clip[clipX + i][clipY + entitySize],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.SW_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        ) || !CollisionMap.notFlagged(
                            clip[clipX + entitySize][i + clipY],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.NW_ALT_OBJ,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.SW_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        )
                    ) {
                        continue@bfs
                    }
                    i++
                }
                bufferX[write] = currentX + 1
                bufferY[write] = currentY + 1
                write = write + 1 and QUEUE_SIZE
                directions[currentGraphX + 1][currentGraphY + 1] = 12
                distances[currentGraphX + 1][currentGraphY + 1] = nextDistance
            }
        }

        exitX = currentX
        exitY = currentY
        return false
    }

    @JvmStatic
    fun routeToSceneobject(destinationX: Int, destinationY: Int, locUid: Long) {
        val shape = (locUid.toInt() shr 14) and 0x1f
        val rotation = (locUid.toInt() shr 20) and 0x3
        val locId = ((locUid ushr 32) and 0x7fffffff).toInt()
        val locShape = Identifiable.identify(ObjectShapes.values(), shape) as ObjectShapes
        val strategy: RouteStrategy

        if (locShape != ObjectShapes.SCENERY_INTERACT && locShape != ObjectShapes.GROUND_INTERACT && locShape != ObjectShapes.GROUND_DECORATION) {
            strategy = if (ObjectShapes.isWall(locShape.shape)) {
                RouteStrategies.forWallObject(destinationX, destinationY, 0, 0, locShape, rotation)
            } else {
                RouteStrategies.forInteractiveObject(destinationX, destinationY, locShape, rotation)
            }
        } else {
            val loctype = Resource.mapRegion!!.getLoctypeList()!!.list(locId)
            val sizeX: Int
            val sizeY: Int
            if (rotation != 0 && rotation != 2) {
                sizeX = loctype.sizeY
                sizeY = loctype.sizeX
            } else {
                sizeX = loctype.sizeX
                sizeY = loctype.sizeY
            }

            strategy = RouteStrategies.forWallObject(destinationX, destinationY, sizeX, sizeY, ObjectShapes.WALL_STRAIGHT, 0)
        }

        calculateRoute(destinationX, destinationY, true, strategy)
    }

    @JvmStatic
    fun calculateRoute(destinationX: Int, destinationY: Int, findAlternative: Boolean, strategy: RouteStrategy): Boolean {
        val baseX = PlayerUpdate.MY_PLAYER!!.movementBaseX[0]
        val baseY = PlayerUpdate.MY_PLAYER!!.movementBaseY[0]
        if (baseX >= 0 && baseX < Resource.mapRegion!!.sceneSizeX && baseY >= 0 && baseY < Resource.mapRegion!!.sceneSizeY) {
            if (destinationX >= 0 && destinationX < Resource.mapRegion!!.sceneSizeX && destinationY >= 0 && destinationY < Resource.mapRegion!!.sceneSizeY) {
                val executedStep = findRoute(
                    baseX,
                    baseY,
                    PlayerUpdate.MY_PLAYER!!.getSize(),
                    strategy,
                    Resource.mapRegion!!.getCollisionMap(PlayerUpdate.MY_PLAYER!!.plane.toInt()),
                    findAlternative,
                    Client.WALKING_STEPS_X,
                    Client.WALKING_STEPS_Y
                )
                if (executedStep < 1) {
                    return false
                }
                ComponentMinimap.minimapFlagX = Client.WALKING_STEPS_X[executedStep - 1]
                ComponentMinimap.minimapFlagY = Client.WALKING_STEPS_Y[executedStep - 1]
                ComponentMinimap.minimapContainsFlag = false
                PulseEventQueue.delayedClearMinimapFlagUpdate()
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun routeToGrounditem(x: Int, y: Int) {
        if (Game.STELLAR_DAWN != Client.CURRENT_GAME) {
            if (!calculateRoute(x, y, false, RouteStrategies.forGroundItem(x, y))) {
                calculateRoute(x, y, false, RouteStrategies.forPlayer(x, y, 1, 1))
            }
        } else if (!calculateRoute(x, y, false, RouteStrategies.forPlayer(x, y, 1, 1))) {
            calculateRoute(x, y, false, RouteStrategies.forGroundItem(x, y))
        }
    }
}
