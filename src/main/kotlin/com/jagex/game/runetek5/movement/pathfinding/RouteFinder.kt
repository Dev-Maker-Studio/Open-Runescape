package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.periodic.PulseEventQueue
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
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
        bool5: Boolean,
        ints_6: IntArray,
        ints_7: IntArray
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

        val sceneY = srcX - 64
        val i_11 = srcY - 64
        var pathExitX = exitX
        var pathExitY = exitY
        val i_14: Int
        val i_15: Int

        if (!canReach) {
            if (!bool5) {
                return -1
            }
            i_14 = Int.MAX_VALUE
            var tempI15 = Int.MAX_VALUE
            val b_16: Byte = 10
            val i_17 = strategy.destinationX
            val i_18 = strategy.destinationY
            val i_19 = strategy.destinationSizeX
            val i_20 = strategy.destinationSizeY

            for (i_21 in i_17 - b_16..i_17 + b_16) {
                for (i_22 in i_18 - b_16..i_18 + b_16) {
                    val i_23 = i_21 - sceneY
                    val i_24 = i_22 - i_11
                    if (i_23 >= 0 && i_24 >= 0 && i_23 < 128 && i_24 < 128 && distances[i_23][i_24] < 100) {
                        var i_25 = 0
                        if (i_21 < i_17) {
                            i_25 = i_17 - i_21
                        } else if (i_21 > i_19 + i_17 - 1) {
                            i_25 = i_21 - (i_17 + i_19 - 1)
                        }
                        var i_26 = 0
                        if (i_22 < i_18) {
                            i_26 = i_18 - i_22
                        } else if (i_22 > i_20 + i_18 - 1) {
                            i_26 = i_22 - (i_18 + i_20 - 1)
                        }
                        val i_27 = i_25 * i_25 + i_26 * i_26
                        if (i_27 < i_14 || i_27 == i_14 && distances[i_23][i_24] < tempI15) {
                            tempI15 = distances[i_23][i_24]
                            pathExitX = i_21
                            pathExitY = i_22
                        }
                    }
                }
            }
            if (i_14 == Int.MAX_VALUE) {
                return -1
            }
            i_15 = tempI15
        } else {
            i_15 = 0
        }

        if (srcX == pathExitX && pathExitY == srcY) {
            return 0
        } else {
            val b_29: Byte = 0
            bufferX[b_29.toInt()] = pathExitX
            var i_14_2 = b_29 + 1
            bufferY[b_29.toInt()] = pathExitY
            var i_15_2 = directions[pathExitX - sceneY][pathExitY - i_11]
            var i_30 = i_15_2

            while (srcX != pathExitX || pathExitY != srcY) {
                if (i_15_2 != i_30) {
                    i_15_2 = i_30
                    bufferX[i_14_2] = pathExitX
                    bufferY[i_14_2++] = pathExitY
                }
                if ((i_30 and 0x2) != 0) {
                    ++pathExitX
                } else if ((i_30 and 0x8) != 0) {
                    --pathExitX
                }
                if ((i_30 and 0x1) != 0) {
                    ++pathExitY
                } else if ((i_30 and 0x4) != 0) {
                    --pathExitY
                }
                i_30 = directions[pathExitX - sceneY][pathExitY - i_11]
            }

            var i_17 = 0
            while (i_14_2-- > 0) {
                ints_6[i_17] = bufferX[i_14_2]
                ints_7[i_17++] = bufferY[i_14_2]
                if (i_17 >= ints_6.size) {
                    break
                }
            }
            return i_17
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
    fun performCalculationS2(i_0: Int, i_1: Int, routestrategy2: RouteStrategy, clipmap3: CollisionMap): Boolean {
        var i_5 = i_0
        var i_6 = i_1
        val b_7: Byte = 64
        val b_8: Byte = 64
        val i_9 = i_0 - b_7
        val i_10 = i_1 - b_8
        directions[b_7.toInt()][b_8.toInt()] = 99
        distances[b_7.toInt()][b_8.toInt()] = 0
        val b_11: Byte = 0
        var i_12 = 0
        bufferX[b_11.toInt()] = i_0
        val b_10001 = b_11
        var i_19 = b_11 + 1
        bufferY[b_10001.toInt()] = i_1
        val ints_13 = clipmap3.collisionFlags

        while (i_12 != i_19) {
            i_5 = bufferX[i_12]
            i_6 = bufferY[i_12]
            i_12 = i_12 + 1 and QUEUE_SIZE
            val i_17 = i_5 - i_9
            val i_18 = i_6 - i_10
            val i_14 = i_5 - clipmap3.offsetX
            val i_15 = i_6 - clipmap3.offsetY

            if (routestrategy2.canExit(2, i_5, i_6, clipmap3)) {
                exitX = i_5
                exitY = i_6
                return true
            }

            val i_16 = distances[i_17][i_18] + 1

            if (i_17 > 0 && directions[i_17 - 1][i_18] == 0 && CollisionMap.notFlagged(
                    ints_13[i_14 - 1][i_15],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 - 1][i_15 + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[i_19] = i_5 - 1
                bufferY[i_19] = i_6
                i_19 = i_19 + 1 and QUEUE_SIZE
                directions[i_17 - 1][i_18] = 2
                distances[i_17 - 1][i_18] = i_16
            }

            if (i_17 < 126 && directions[i_17 + 1][i_18] == 0 && CollisionMap.notFlagged(
                    ints_13[i_14 + 2][i_15],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 + 2][i_15 + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[i_19] = i_5 + 1
                bufferY[i_19] = i_6
                i_19 = i_19 + 1 and QUEUE_SIZE
                directions[i_17 + 1][i_18] = 8
                distances[i_17 + 1][i_18] = i_16
            }

            if (i_18 > 0 && directions[i_17][i_18 - 1] == 0 && CollisionMap.notFlagged(
                    ints_13[i_14][i_15 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 + 1][i_15 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[i_19] = i_5
                bufferY[i_19] = i_6 - 1
                i_19 = i_19 + 1 and QUEUE_SIZE
                directions[i_17][i_18 - 1] = 1
                distances[i_17][i_18 - 1] = i_16
            }

            if (i_18 < 126 && directions[i_17][i_18 + 1] == 0 && CollisionMap.notFlagged(
                    ints_13[i_14][i_15 + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 + 1][i_15 + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                bufferX[i_19] = i_5
                bufferY[i_19] = i_6 + 1
                i_19 = i_19 + 1 and QUEUE_SIZE
                directions[i_17][i_18 + 1] = 4
                distances[i_17][i_18 + 1] = i_16
            }

            if (i_17 > 0 && i_18 > 0 && directions[i_17 - 1][i_18 - 1] == 0 && CollisionMap.notFlagged(
                    ints_13[i_14 - 1][i_15],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 - 1][i_15 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14][i_15 - 1],
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
                bufferX[i_19] = i_5 - 1
                bufferY[i_19] = i_6 - 1
                i_19 = i_19 + 1 and QUEUE_SIZE
                directions[i_17 - 1][i_18 - 1] = 3
                distances[i_17 - 1][i_18 - 1] = i_16
            }

            if (i_17 < 126 && i_18 > 0 && directions[i_17 + 1][i_18 - 1] == 0 && CollisionMap.notFlagged(
                    ints_13[i_14 + 1][i_15 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 + 2][i_15 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 + 2][i_15],
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
                bufferX[i_19] = i_5 + 1
                bufferY[i_19] = i_6 - 1
                i_19 = i_19 + 1 and QUEUE_SIZE
                directions[i_17 + 1][i_18 - 1] = 9
                distances[i_17 + 1][i_18 - 1] = i_16
            }

            if (i_17 > 0 && i_18 < 126 && directions[i_17 - 1][i_18 + 1] == 0 && CollisionMap.notFlagged(
                    ints_13[i_14 - 1][i_15 + 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 - 1][i_15 + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14][i_15 + 2],
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
                bufferX[i_19] = i_5 - 1
                bufferY[i_19] = i_6 + 1
                i_19 = i_19 + 1 and QUEUE_SIZE
                directions[i_17 - 1][i_18 + 1] = 6
                distances[i_17 - 1][i_18 + 1] = i_16
            }

            if (i_17 < 126 && i_18 < 126 && directions[i_17 + 1][i_18 + 1] == 0 && CollisionMap.notFlagged(
                    ints_13[i_14 + 1][i_15 + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 + 2][i_15 + 2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_13[i_14 + 2][i_15 + 1],
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
                bufferX[i_19] = i_5 + 1
                bufferY[i_19] = i_6 + 1
                i_19 = i_19 + 1 and QUEUE_SIZE
                directions[i_17 + 1][i_18 + 1] = 12
                distances[i_17 + 1][i_18 + 1] = i_16
            }
        }

        exitX = i_5
        exitY = i_6
        return false
    }

    @JvmStatic
    fun performCalculationSX(
        i_0: Int,
        i_1: Int,
        i_2: Int,
        routestrategy3: RouteStrategy,
        clipmap4: CollisionMap
    ): Boolean {
        var i_6 = i_0
        var i_7 = i_1
        val b_8: Byte = 64
        val b_9: Byte = 64
        val i_10 = i_0 - b_8
        val i_11 = i_1 - b_9
        directions[b_8.toInt()][b_9.toInt()] = 99
        distances[b_8.toInt()][b_9.toInt()] = 0
        val b_12: Byte = 0
        var i_13 = 0
        bufferX[b_12.toInt()] = i_0
        var i_21 = b_12 + 1
        bufferY[b_12.toInt()] = i_1
        val ints_14 = clipmap4.collisionFlags

        label208@ while (i_21 != i_13) {
            i_6 = bufferX[i_13]
            i_7 = bufferY[i_13]
            i_13 = i_13 + 1 and QUEUE_SIZE
            val i_19 = i_6 - i_10
            val i_20 = i_7 - i_11
            val i_15 = i_6 - clipmap4.offsetX
            val i_16 = i_7 - clipmap4.offsetY

            if (routestrategy3.canExit(i_2, i_6, i_7, clipmap4)) {
                exitX = i_6
                exitY = i_7
                return true
            }

            val i_17 = distances[i_19][i_20] + 1
            var i_18: Int

            if (i_19 > 0 && directions[i_19 - 1][i_20] == 0 && CollisionMap.notFlagged(
                    ints_14[i_15 - 1][i_16],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_14[i_15 - 1][i_16 + i_2 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i_18 = 1
                while (true) {
                    if (i_18 >= i_2 - 1) {
                        bufferX[i_21] = i_6 - 1
                        bufferY[i_21] = i_7
                        i_21 = i_21 + 1 and QUEUE_SIZE
                        directions[i_19 - 1][i_20] = 2
                        distances[i_19 - 1][i_20] = i_17
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            ints_14[i_15 - 1][i_18 + i_16],
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
                    ++i_18
                }
            }

            if (i_19 < 128 - i_2 && directions[i_19 + 1][i_20] == 0 && CollisionMap.notFlagged(
                    ints_14[i_15 + i_2][i_16],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_14[i_15 + i_2][i_16 + i_2 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i_18 = 1
                while (true) {
                    if (i_18 >= i_2 - 1) {
                        bufferX[i_21] = i_6 + 1
                        bufferY[i_21] = i_7
                        i_21 = i_21 + 1 and QUEUE_SIZE
                        directions[i_19 + 1][i_20] = 8
                        distances[i_19 + 1][i_20] = i_17
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            ints_14[i_15 + i_2][i_18 + i_16],
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
                    ++i_18
                }
            }

            if (i_20 > 0 && directions[i_19][i_20 - 1] == 0 && CollisionMap.notFlagged(
                    ints_14[i_15][i_16 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_14[i_15 + i_2 - 1][i_16 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i_18 = 1
                while (true) {
                    if (i_18 >= i_2 - 1) {
                        bufferX[i_21] = i_6
                        bufferY[i_21] = i_7 - 1
                        i_21 = i_21 + 1 and QUEUE_SIZE
                        directions[i_19][i_20 - 1] = 1
                        distances[i_19][i_20 - 1] = i_17
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            ints_14[i_18 + i_15][i_16 - 1],
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
                    ++i_18
                }
            }

            if (i_20 < 128 - i_2 && directions[i_19][i_20 + 1] == 0 && CollisionMap.notFlagged(
                    ints_14[i_15][i_16 + i_2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                ) && CollisionMap.notFlagged(
                    ints_14[i_15 + i_2 - 1][i_16 + i_2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i_18 = 1
                while (true) {
                    if (i_18 >= i_2 - 1) {
                        bufferX[i_21] = i_6
                        bufferY[i_21] = i_7 + 1
                        i_21 = i_21 + 1 and QUEUE_SIZE
                        directions[i_19][i_20 + 1] = 4
                        distances[i_19][i_20 + 1] = i_17
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            ints_14[i_15 + i_18][i_16 + i_2],
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
                    ++i_18
                }
            }

            if (i_19 > 0 && i_20 > 0 && directions[i_19 - 1][i_20 - 1] == 0 && CollisionMap.notFlagged(
                    ints_14[i_15 - 1][i_16 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.NE_ALT_OBJ,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i_18 = 1
                while (true) {
                    if (i_18 >= i_2) {
                        bufferX[i_21] = i_6 - 1
                        bufferY[i_21] = i_7 - 1
                        i_21 = i_21 + 1 and QUEUE_SIZE
                        directions[i_19 - 1][i_20 - 1] = 3
                        distances[i_19 - 1][i_20 - 1] = i_17
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            ints_14[i_15 - 1][i_18 + (i_16 - 1)],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.NE_ALT_OBJ,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        ) || !CollisionMap.notFlagged(
                            ints_14[i_18 + (i_15 - 1)][i_16 - 1],
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
                    ++i_18
                }
            }

            if (i_19 < 128 - i_2 && i_20 > 0 && directions[i_19 + 1][i_20 - 1] == 0 && CollisionMap.notFlagged(
                    ints_14[i_15 + i_2][i_16 - 1],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.NW_ALT_OBJ,
                    CollisionFlag.N_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i_18 = 1
                while (true) {
                    if (i_18 >= i_2) {
                        bufferX[i_21] = i_6 + 1
                        bufferY[i_21] = i_7 - 1
                        i_21 = i_21 + 1 and QUEUE_SIZE
                        directions[i_19 + 1][i_20 - 1] = 9
                        distances[i_19 + 1][i_20 - 1] = i_17
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            ints_14[i_15 + i_2][i_18 + (i_16 - 1)],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.NW_ALT_OBJ,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.SW_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        ) || !CollisionMap.notFlagged(
                            ints_14[i_18 + i_15][i_16 - 1],
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
                    ++i_18
                }
            }

            if (i_19 > 0 && i_20 < 128 - i_2 && directions[i_19 - 1][i_20 + 1] == 0 && CollisionMap.notFlagged(
                    ints_14[i_15 - 1][i_16 + i_2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.E_ALT_OBJ,
                    CollisionFlag.SE_ALT_OBJ,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i_18 = 1
                while (true) {
                    if (i_18 >= i_2) {
                        bufferX[i_21] = i_6 - 1
                        bufferY[i_21] = i_7 + 1
                        i_21 = i_21 + 1 and QUEUE_SIZE
                        directions[i_19 - 1][i_20 + 1] = 6
                        distances[i_19 - 1][i_20 + 1] = i_17
                        break
                    }
                    if (!CollisionMap.notFlagged(
                            ints_14[i_15 - 1][i_18 + i_16],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.N_ALT_OBJ,
                            CollisionFlag.NE_ALT_OBJ,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        ) || !CollisionMap.notFlagged(
                            ints_14[i_18 + (i_15 - 1)][i_16 + i_2],
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
                    ++i_18
                }
            }

            if (i_19 < 128 - i_2 && i_20 < 128 - i_2 && directions[i_19 + 1][i_20 + 1] == 0 && CollisionMap.notFlagged(
                    ints_14[i_15 + i_2][i_16 + i_2],
                    CollisionFlag.BLOCKED_DECO,
                    CollisionFlag.BLOCKED,
                    CollisionFlag.S_ALT_OBJ,
                    CollisionFlag.SW_ALT_OBJ,
                    CollisionFlag.W_ALT_OBJ,
                    CollisionFlag.ALT_OBJ
                )
            ) {
                i_18 = 1
                while (i_18 < i_2) {
                    if (!CollisionMap.notFlagged(
                            ints_14[i_15 + i_18][i_16 + i_2],
                            CollisionFlag.BLOCKED_DECO,
                            CollisionFlag.BLOCKED,
                            CollisionFlag.E_ALT_OBJ,
                            CollisionFlag.SE_ALT_OBJ,
                            CollisionFlag.S_ALT_OBJ,
                            CollisionFlag.SW_ALT_OBJ,
                            CollisionFlag.W_ALT_OBJ,
                            CollisionFlag.ALT_OBJ
                        ) || !CollisionMap.notFlagged(
                            ints_14[i_15 + i_2][i_18 + i_16],
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
                        continue@label208
                    }
                    i_18++
                }
                bufferX[i_21] = i_6 + 1
                bufferY[i_21] = i_7 + 1
                i_21 = i_21 + 1 and QUEUE_SIZE
                directions[i_19 + 1][i_20 + 1] = 12
                distances[i_19 + 1][i_20 + 1] = i_17
            }
        }

        exitX = i_6
        exitY = i_7
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
    fun calculateRoute(destinationX: Int, destinationY: Int, bool2: Boolean, strategy: RouteStrategy): Boolean {
        val baseX = PlayerUpdate.MY_PLAYER!!.movementBaseX[0]
        val baseY = PlayerUpdate.MY_PLAYER!!.movementBaseY[0]
        if (baseX >= 0 && baseX < Resource.mapRegion!!.getSceneSizeX() && baseY >= 0 && baseY < Resource.mapRegion!!.getSceneSizeY()) {
            if (destinationX >= 0 && destinationX < Resource.mapRegion!!.getSceneSizeX() && destinationY >= 0 && destinationY < Resource.mapRegion!!.getSceneSizeY()) {
                val executedStep = findRoute(
                    baseX,
                    baseY,
                    PlayerUpdate.MY_PLAYER!!.getSize(),
                    strategy,
                    Resource.mapRegion!!.getCollisionMap(PlayerUpdate.MY_PLAYER!!.plane.toInt()),
                    bool2,
                    client.WALKING_STEPS_X,
                    client.WALKING_STEPS_Y
                )
                if (executedStep < 1) {
                    return false
                }
                ComponentMinimap.minimapFlagX = client.WALKING_STEPS_X[executedStep - 1]
                ComponentMinimap.minimapFlagY = client.WALKING_STEPS_Y[executedStep - 1]
                ComponentMinimap.minimapContainsFlag = false
                PulseEventQueue.delayedClearMinimapFlagUpdate()
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun routeToGrounditem(x: Int, y: Int) {
        if (Game.STELLAR_DAWN != client.CURRENT_GAME) {
            if (!calculateRoute(x, y, false, RouteStrategies.forGroundItem(x, y))) {
                calculateRoute(x, y, false, RouteStrategies.forPlayer(x, y, 1, 1))
            }
        } else if (!calculateRoute(x, y, false, RouteStrategies.forPlayer(x, y, 1, 1))) {
            calculateRoute(x, y, false, RouteStrategies.forGroundItem(x, y))
        }
    }
}
