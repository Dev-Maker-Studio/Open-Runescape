package com.jagex.game.runetek5.scene

class CollisionMap {

    @JvmField
    var collisionFlags: Array<IntArray> = emptyArray()

    @JvmField
    var offsetX: Int = 0

    @JvmField
    var offsetY: Int = 0

    @JvmField
    var sizeX: Int = 0

    @JvmField
    var sizeY: Int = 0

    fun reset() {
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                if (x != 0 && y != 0 && x < sizeX - 5 && y < sizeY - 5) {
                    collisionFlags[x][y] = CollisionFlag.BLOCKED.flag
                } else {
                    collisionFlags[x][y] = CollisionFlag.EMPTY.flag
                }
            }
        }
    }

    fun flagBlockedObject(x: Int, y: Int, sizeX: Int, sizeY: Int, blocked: Boolean, notAlt: Boolean) {
        var flag = CollisionFlag.OBJ.flag
        if (blocked) {
            flag = flag or CollisionFlag.BLOCKED_OBJ.flag
        }
        if (notAlt) {
            flag = flag or CollisionFlag.ALT_OBJ.flag
        }
        val adjustedX = x - offsetX
        val adjustedY = y - offsetY
        for (tileX in adjustedX until adjustedX + sizeX) {
            if (tileX >= 0 && tileX < this.sizeX) {
                for (tileY in adjustedY until adjustedY + sizeY) {
                    if (tileY >= 0 && tileY < this.sizeY) {
                        addFlag(tileX, tileY, flag)
                    }
                }
            }
        }
    }

    fun unflagBlocked(x: Int, y: Int, sizeX: Int, sizeY: Int, rotation: Int, blocked: Boolean, notAlt: Boolean) {
        var sizeX1 = sizeX
        var sizeY1 = sizeY
        var flag = CollisionFlag.OBJ.flag
        if (blocked) {
            flag = flag or CollisionFlag.BLOCKED_OBJ.flag
        }
        if (notAlt) {
            flag = flag or CollisionFlag.ALT_OBJ.flag
        }
        val adjustedX = x - offsetX
        val adjustedY = y - offsetY
        var tileX: Int
        if (rotation == 1 || rotation == 3) {
            tileX = sizeX1
            sizeX1 = sizeY1
            sizeY1 = tileX
        }
        tileX = adjustedX
        while (tileX < sizeX1 + adjustedX) {
            if (tileX >= 0 && tileX < this.sizeX) {
                for (tileY in adjustedY until adjustedY + sizeY1) {
                    if (tileY >= 0 && tileY < this.sizeY) {
                        removeFlag(tileX, tileY, flag)
                    }
                }
            }
            tileX++
        }
    }

    fun flagBoundary(x: Int, y: Int, type: Int, rotation: Int, blocks: Boolean, alt: Boolean) {
        val adjustedX = x - offsetX
        val adjustedY = y - offsetY
        if (type == 0) {
            if (rotation == 0) {
                addFlag(adjustedX, adjustedY, CollisionFlag.W_OBJ)
                addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_OBJ)
            }
            if (rotation == 1) {
                addFlag(adjustedX, adjustedY, CollisionFlag.N_OBJ)
                addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_OBJ)
            }
            if (rotation == 2) {
                addFlag(adjustedX, adjustedY, CollisionFlag.E_OBJ)
                addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_OBJ)
            }
            if (rotation == 3) {
                addFlag(adjustedX, adjustedY, CollisionFlag.S_OBJ)
                addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_OBJ)
            }
        }
        if (type == 1 || type == 3) {
            if (rotation == 0) {
                addFlag(adjustedX, adjustedY, CollisionFlag.NW_OBJ)
                addFlag(adjustedX - 1, adjustedY + 1, CollisionFlag.SE_OBJ)
            }
            if (rotation == 1) {
                addFlag(adjustedX, adjustedY, CollisionFlag.NE_OBJ)
                addFlag(adjustedX + 1, adjustedY + 1, CollisionFlag.SW_OBJ)
            }
            if (rotation == 2) {
                addFlag(adjustedX, adjustedY, CollisionFlag.SE_OBJ)
                addFlag(adjustedX + 1, adjustedY - 1, CollisionFlag.NW_OBJ)
            }
            if (rotation == 3) {
                addFlag(adjustedX, adjustedY, CollisionFlag.SW_OBJ)
                addFlag(adjustedX - 1, adjustedY - 1, CollisionFlag.NE_OBJ)
            }
        }
        if (type == 2) {
            if (rotation == 0) {
                addFlag(adjustedX, adjustedY, CollisionFlag.N_OBJ, CollisionFlag.W_OBJ)
                addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_OBJ)
                addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_OBJ)
            }
            if (rotation == 1) {
                addFlag(adjustedX, adjustedY, CollisionFlag.N_OBJ, CollisionFlag.E_OBJ)
                addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_OBJ)
                addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_OBJ)
            }
            if (rotation == 2) {
                addFlag(adjustedX, adjustedY, CollisionFlag.E_OBJ, CollisionFlag.S_OBJ)
                addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_OBJ)
                addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_OBJ)
            }
            if (rotation == 3) {
                addFlag(adjustedX, adjustedY, CollisionFlag.S_OBJ, CollisionFlag.W_OBJ)
                addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_OBJ)
                addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_OBJ)
            }
        }
        if (blocks) {
            if (type == 0) {
                if (rotation == 0) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.W_BOUND)
                    addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_BOUND)
                }
                if (rotation == 1) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.N_BOUND)
                    addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_BOUND)
                }
                if (rotation == 2) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.E_BOUND)
                    addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_BOUND)
                }
                if (rotation == 3) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.S_BOUND)
                    addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_BOUND)
                }
            }
            if (type == 1 || type == 3) {
                if (rotation == 0) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.NW_BOUND)
                    addFlag(adjustedX - 1, adjustedY + 1, CollisionFlag.SE_BOUND)
                }
                if (rotation == 1) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.NE_BOUND)
                    addFlag(adjustedX + 1, adjustedY + 1, CollisionFlag.SW_BOUND)
                }
                if (rotation == 2) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.SE_BOUND)
                    addFlag(adjustedX + 1, adjustedY - 1, CollisionFlag.NW_BOUND)
                }
                if (rotation == 3) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.SW_BOUND)
                    addFlag(adjustedX - 1, adjustedY - 1, CollisionFlag.NE_BOUND)
                }
            }
            if (type == 2) {
                if (rotation == 0) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.N_BOUND, CollisionFlag.W_BOUND)
                    addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_BOUND)
                    addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_BOUND)
                }
                if (rotation == 1) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.N_BOUND, CollisionFlag.E_BOUND)
                    addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_BOUND)
                    addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_BOUND)
                }
                if (rotation == 2) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.E_BOUND, CollisionFlag.S_BOUND)
                    addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_BOUND)
                    addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_BOUND)
                }
                if (rotation == 3) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.S_BOUND, CollisionFlag.W_BOUND)
                    addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_BOUND)
                    addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_BOUND)
                }
            }
        }
        if (alt) {
            if (type == 0) {
                if (rotation == 0) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.W_ALT_OBJ)
                    addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_ALT_OBJ)
                }
                if (rotation == 1) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.N_ALT_OBJ)
                    addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_ALT_OBJ)
                }
                if (rotation == 2) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.E_ALT_OBJ)
                    addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_ALT_OBJ)
                }
                if (rotation == 3) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.S_ALT_OBJ)
                    addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_ALT_OBJ)
                }
            }
            if (type == 1 || type == 3) {
                if (rotation == 0) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.NW_ALT_OBJ)
                    addFlag(adjustedX - 1, adjustedY + 1, CollisionFlag.SE_ALT_OBJ)
                }
                if (rotation == 1) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.NE_ALT_OBJ)
                    addFlag(adjustedX + 1, adjustedY + 1, CollisionFlag.SW_ALT_OBJ)
                }
                if (rotation == 2) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.SE_ALT_OBJ)
                    addFlag(adjustedX + 1, adjustedY - 1, CollisionFlag.NW_ALT_OBJ)
                }
                if (rotation == 3) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.SW_ALT_OBJ)
                    addFlag(adjustedX - 1, adjustedY - 1, CollisionFlag.NE_ALT_OBJ)
                }
            }
            if (type == 2) {
                if (rotation == 0) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ)
                    addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_ALT_OBJ)
                    addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_ALT_OBJ)
                }
                if (rotation == 1) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.N_ALT_OBJ, CollisionFlag.E_ALT_OBJ)
                    addFlag(adjustedX, adjustedY + 1, CollisionFlag.S_ALT_OBJ)
                    addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_ALT_OBJ)
                }
                if (rotation == 2) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.E_ALT_OBJ, CollisionFlag.S_ALT_OBJ)
                    addFlag(adjustedX + 1, adjustedY, CollisionFlag.W_ALT_OBJ)
                    addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_ALT_OBJ)
                }
                if (rotation == 3) {
                    addFlag(adjustedX, adjustedY, CollisionFlag.S_ALT_OBJ, CollisionFlag.W_ALT_OBJ)
                    addFlag(adjustedX, adjustedY - 1, CollisionFlag.N_ALT_OBJ)
                    addFlag(adjustedX - 1, adjustedY, CollisionFlag.E_ALT_OBJ)
                }
            }
        }
    }

    fun flagBlockedDecoration(tileX: Int, tileY: Int) {
        val x = tileX - offsetX
        val y = tileY - offsetY
        collisionFlags[x][y] = collisionFlags[x][y] or CollisionFlag.BLOCKED_DECO.flag
    }

    fun unflagFloorDecoration(tileX: Int, tileY: Int) {
        val x = tileX - offsetX
        val y = tileY - offsetY
        collisionFlags[x][y] = collisionFlags[x][y] and CollisionFlag.BLOCKED_DECO.flag.inv()
    }

    fun setUnwalkable(tileX: Int, tileY: Int) {
        val x = tileX - offsetX
        val y = tileY - offsetY
        collisionFlags[x][y] = collisionFlags[x][y] or CollisionFlag.BLOCKED.flag
    }

    fun unblock(sceneX: Int, sceneY: Int) {
        val x = sceneX - offsetX
        val y = sceneY - offsetY
        collisionFlags[x][y] = collisionFlags[x][y] and CollisionFlag.BLOCKED.flag.inv()
    }

    fun removeFlag(x: Int, y: Int, flag: Int) {
        collisionFlags[x][y] = collisionFlags[x][y] and flag.inv()
    }

    fun addFlag(x: Int, y: Int, flag: Int) {
        collisionFlags[x][y] = collisionFlags[x][y] or flag
    }

    fun removeFlag(x: Int, y: Int, vararg flags: CollisionFlag) {
        var flag = 0
        for (f in flags) {
            flag = flag or f.flag
        }
        removeFlag(x, y, flag)
    }

    fun addFlag(x: Int, y: Int, vararg flags: CollisionFlag) {
        var flag = 0
        for (f in flags) {
            flag = flag or f.flag
        }
        addFlag(x, y, flag)
    }

    fun walkable(srcX: Int, srcY: Int, srcSizeX: Int, srcSizeY: Int, dstX: Int, dstY: Int, dstSizeX: Int, dstSizeY: Int, flag: Int): Boolean {
        val srcMaxX = srcSizeX + srcX
        val srcMaxY = srcY + srcSizeY
        val dstMaxX = dstX + dstSizeX
        val dstMaxY = dstSizeY + dstY
        val overlapMin: Int
        val overlapMax: Int
        if (dstMaxX == srcX && notFlagged(flag, CollisionFlag.N_OBJ)) {
            overlapMin = Math.max(srcY, dstY)
            overlapMax = Math.min(srcMaxY, dstMaxY)
            var i = overlapMin
            while (i < overlapMax) {
                if (notFlagged(collisionFlags[dstMaxX - 1 - offsetX][i - offsetY], CollisionFlag.E_OBJ)) {
                    return true
                }
                i++
            }
        } else if (dstX == srcMaxX && notFlagged(flag, CollisionFlag.E_OBJ)) {
            overlapMin = Math.max(srcY, dstY)
            overlapMax = Math.min(srcMaxY, dstMaxY)
            var i = overlapMin
            while (i < overlapMax) {
                if (notFlagged(collisionFlags[dstX - offsetX][i - offsetY], CollisionFlag.W_OBJ)) {
                    return true
                }
                i++
            }
        } else if (dstMaxY == srcY && notFlagged(flag, CollisionFlag.NW_OBJ)) {
            overlapMin = Math.max(srcX, dstX)
            overlapMax = Math.min(srcMaxX, dstMaxX)
            var i = overlapMin
            while (i < overlapMax) {
                if (notFlagged(collisionFlags[i - offsetX][dstMaxY - 1 - offsetY], CollisionFlag.N_OBJ)) {
                    return true
                }
                i++
            }
        } else if (srcMaxY == dstY && notFlagged(flag, CollisionFlag.NE_OBJ)) {
            overlapMin = Math.max(srcX, dstX)
            overlapMax = Math.min(srcMaxX, dstMaxX)
            var i = overlapMin
            while (i < overlapMax) {
                if (notFlagged(collisionFlags[i - offsetX][dstY - offsetY], CollisionFlag.S_OBJ)) {
                    return true
                }
                i++
            }
        }
        return false
    }

    fun canReachLoc(srcX: Int, srcY: Int, size: Int, dstX: Int, dstY: Int, shape: Int, rotation: Int): Boolean {
        if (size == 1) {
            if (dstX == srcX && dstY == srcY) {
                return true
            }
        } else if (dstX >= srcX && dstX <= size + srcX - 1 && dstY <= size + dstY - 1) {
            return true
        }
        val adjustedSrcX = srcX - offsetX
        val adjustedSrcY = srcY - offsetY
        val adjustedDstX = dstX - offsetX
        val adjustedDstY = dstY - offsetY
        if (size == 1) {
            if (shape == 0) {
                if (rotation == 0) {
                    if (adjustedDstX - 1 == adjustedSrcX && adjustedDstY == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedDstY + 1 == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY - 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 1) {
                    if (adjustedDstX == adjustedSrcX && adjustedDstY + 1 == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX - 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 2) {
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY + 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY - 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 3) {
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY - 1) {
                        return true
                    }
                    if (adjustedDstX - 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                }
            }
            if (shape == 2) {
                if (rotation == 0) {
                    if (adjustedDstX - 1 == adjustedSrcX && adjustedDstY == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedDstY + 1 == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY - 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 1) {
                    if (adjustedDstX - 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedDstY + 1 == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY - 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 2) {
                    if (adjustedDstX - 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY + 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY - 1) {
                        return true
                    }
                } else if (rotation == 3) {
                    if (adjustedDstX - 1 == adjustedSrcX && adjustedDstY == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY + 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY - 1) {
                        return true
                    }
                }
            }
            if (shape == 9) {
                if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY + 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.S_OBJ)) {
                    return true
                }
                if (adjustedDstX == adjustedSrcX && adjustedSrcY == adjustedDstY - 1 && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.N_OBJ)) {
                    return true
                }
                if (adjustedDstX - 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.E_OBJ)) {
                    return true
                }
                return adjustedDstX + 1 == adjustedSrcX && adjustedDstY == adjustedSrcY && notFlagged(collisionFlags[adjustedSrcX][adjustedSrcY], CollisionFlag.W_OBJ)
            }
        } else {
            val endX = size + adjustedSrcX - 1
            val endY = size + adjustedSrcY - 1
            if (shape == 0) {
                if (rotation == 0) {
                    if (adjustedDstX - size == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedSrcY == adjustedDstY + 1 && notFlagged(collisionFlags[adjustedDstX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY - size == adjustedSrcY && notFlagged(collisionFlags[adjustedDstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 1) {
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY + 1 == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX - size == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[endX][adjustedDstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[adjustedSrcX][adjustedDstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 2) {
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY + 1 == adjustedSrcY && notFlagged(collisionFlags[adjustedDstX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY - size == adjustedSrcY && notFlagged(collisionFlags[adjustedDstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 3) {
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY - size == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX - size == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[endX][adjustedDstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[adjustedSrcX][adjustedDstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                }
            }
            if (shape == 2) {
                if (rotation == 0) {
                    if (adjustedDstX - size == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY + 1 == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[adjustedSrcX][adjustedDstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY - size == adjustedSrcY && notFlagged(collisionFlags[adjustedDstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 1) {
                    if (adjustedDstX - size == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[endX][adjustedDstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY + 1 == adjustedSrcY) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY - size == adjustedSrcY && notFlagged(collisionFlags[adjustedDstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                } else if (rotation == 2) {
                    if (adjustedDstX - size == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[endX][adjustedDstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY + 1 == adjustedSrcY && notFlagged(collisionFlags[adjustedDstX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY - size == adjustedSrcY) {
                        return true
                    }
                } else if (rotation == 3) {
                    if (adjustedDstX - size == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedSrcY == adjustedDstY + 1 && notFlagged(collisionFlags[adjustedDstX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX + 1 == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[adjustedSrcX][adjustedDstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true
                    }
                    if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY - size == adjustedSrcY) {
                        return true
                    }
                }
            }
            if (shape == 9) {
                if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY + 1 == adjustedSrcY && notFlagged(collisionFlags[adjustedDstX][adjustedSrcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                    return true
                }
                if (adjustedDstX >= adjustedSrcX && adjustedDstX <= endX && adjustedDstY - size == adjustedSrcY && notFlagged(collisionFlags[adjustedDstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                    return true
                }
                if (adjustedDstX - size == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[endX][adjustedDstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                    return true
                }
                return adjustedDstX + 1 == adjustedSrcX && adjustedDstY >= adjustedSrcY && adjustedDstY <= endY && notFlagged(collisionFlags[adjustedSrcX][adjustedDstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)
            }
        }
        return false
    }

    fun canReachNpc(srcX: Int, srcY: Int, size: Int, dstX: Int, dstY: Int, dstSizeX: Int, dstSizeY: Int, clippingFlag: Int): Boolean {
        if (size > 1) {
            return canReach(srcX, srcY, size, size, dstX, dstY, dstSizeX, dstSizeY) || walkable(srcX, srcY, size, size, dstX, dstY, dstSizeX, dstSizeY, clippingFlag)
        } else {
            val endX = dstX + dstSizeX - 1
            val endY = dstY + dstSizeY - 1
            return srcX >= dstX && srcX <= endX && srcY >= dstY && srcY <= endY || (dstX - 1 == srcX && srcY >= dstY && srcY <= endY && notFlagged(collisionFlags[srcX - offsetX][srcY - offsetY], CollisionFlag.E_OBJ) && notFlagged(clippingFlag, CollisionFlag.E_OBJ) || (endX + 1 == srcX && srcY >= dstY && srcY <= endY && notFlagged(collisionFlags[srcX - offsetX][srcY - offsetY], CollisionFlag.W_OBJ) && notFlagged(clippingFlag, CollisionFlag.N_OBJ) || (srcY == dstY - 1 && srcX >= dstX && srcX <= endX && notFlagged(collisionFlags[srcX - offsetX][srcY - offsetY], CollisionFlag.N_OBJ) && notFlagged(clippingFlag, CollisionFlag.NE_OBJ) || srcY == endY + 1 && srcX >= dstX && srcX <= endX && notFlagged(collisionFlags[srcX - offsetX][srcY - offsetY], CollisionFlag.S_OBJ) && notFlagged(clippingFlag, CollisionFlag.NW_OBJ))))
        }
    }

    fun unflagBoundary(x: Int, y: Int, type: Int, rotation: Int, blocks: Boolean, notAlternative: Boolean) {
        val adjustedX = x - offsetX
        val adjustedY = y - offsetY
        if (type == 0) {
            if (rotation == 0) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.W_OBJ)
                removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_OBJ)
            }
            if (rotation == 1) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.N_OBJ)
                removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_OBJ)
            }
            if (rotation == 2) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.E_OBJ)
                removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_OBJ)
            }
            if (rotation == 3) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.S_OBJ)
                removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_OBJ)
            }
        }
        if (type == 1 || type == 3) {
            if (rotation == 0) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.NW_OBJ)
                removeFlag(adjustedX - 1, adjustedY + 1, CollisionFlag.SE_OBJ)
            }
            if (rotation == 1) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.NE_OBJ)
                removeFlag(adjustedX + 1, adjustedY + 1, CollisionFlag.SW_OBJ)
            }
            if (rotation == 2) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.SE_OBJ)
                removeFlag(adjustedX + 1, adjustedY - 1, CollisionFlag.NW_OBJ)
            }
            if (rotation == 3) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.SW_OBJ)
                removeFlag(adjustedX - 1, adjustedY - 1, CollisionFlag.NE_OBJ)
            }
        }
        if (type == 2) {
            if (rotation == 0) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.N_OBJ, CollisionFlag.W_OBJ)
                removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_OBJ)
                removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_OBJ)
            }
            if (rotation == 1) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.N_OBJ, CollisionFlag.E_OBJ)
                removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_OBJ)
                removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_OBJ)
            }
            if (rotation == 2) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.E_OBJ, CollisionFlag.S_OBJ)
                removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_OBJ)
                removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_OBJ)
            }
            if (rotation == 3) {
                removeFlag(adjustedX, adjustedY, CollisionFlag.S_OBJ, CollisionFlag.W_OBJ)
                removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_OBJ)
                removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_OBJ)
            }
        }
        if (blocks) {
            if (type == 0) {
                if (rotation == 0) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.W_BOUND)
                    removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_BOUND)
                }
                if (rotation == 1) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.N_BOUND)
                    removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_BOUND)
                }
                if (rotation == 2) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.E_BOUND)
                    removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_BOUND)
                }
                if (rotation == 3) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.S_BOUND)
                    removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_BOUND)
                }
            }
            if (type == 1 || type == 3) {
                if (rotation == 0) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.NW_BOUND)
                    removeFlag(adjustedX - 1, adjustedY + 1, CollisionFlag.SE_BOUND)
                }
                if (rotation == 1) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.NE_BOUND)
                    removeFlag(adjustedX + 1, adjustedY + 1, CollisionFlag.SW_BOUND)
                }
                if (rotation == 2) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.SE_BOUND)
                    removeFlag(adjustedX + 1, adjustedY - 1, CollisionFlag.NW_BOUND)
                }
                if (rotation == 3) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.SW_BOUND)
                    removeFlag(adjustedX - 1, adjustedY - 1, CollisionFlag.NE_BOUND)
                }
            }
            if (type == 2) {
                if (rotation == 0) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.N_BOUND, CollisionFlag.W_BOUND)
                    removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_BOUND)
                    removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_BOUND)
                }
                if (rotation == 1) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.N_BOUND, CollisionFlag.E_BOUND)
                    removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_BOUND)
                    removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_BOUND)
                }
                if (rotation == 2) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.E_BOUND, CollisionFlag.S_BOUND)
                    removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_BOUND)
                    removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_BOUND)
                }
                if (rotation == 3) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.S_BOUND, CollisionFlag.W_BOUND)
                    removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_BOUND)
                    removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_BOUND)
                }
            }
        }
        if (notAlternative) {
            if (type == 0) {
                if (rotation == 0) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.W_ALT_OBJ)
                    removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_ALT_OBJ)
                }
                if (rotation == 1) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.N_ALT_OBJ)
                    removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_ALT_OBJ)
                }
                if (rotation == 2) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.E_ALT_OBJ)
                    removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_ALT_OBJ)
                }
                if (rotation == 3) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.S_ALT_OBJ)
                    removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_ALT_OBJ)
                }
            }
            if (type == 1 || type == 3) {
                if (rotation == 0) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.NW_ALT_OBJ)
                    removeFlag(adjustedX - 1, adjustedY + 1, CollisionFlag.SE_ALT_OBJ)
                }
                if (rotation == 1) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.NE_ALT_OBJ)
                    removeFlag(adjustedX + 1, adjustedY + 1, CollisionFlag.SW_ALT_OBJ)
                }
                if (rotation == 2) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.SE_ALT_OBJ)
                    removeFlag(adjustedX + 1, adjustedY - 1, CollisionFlag.NW_ALT_OBJ)
                }
                if (rotation == 3) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.SW_ALT_OBJ)
                    removeFlag(adjustedX - 1, adjustedY - 1, CollisionFlag.NE_ALT_OBJ)
                }
            }
            if (type == 2) {
                if (rotation == 0) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ)
                    removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_ALT_OBJ)
                    removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_ALT_OBJ)
                }
                if (rotation == 1) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.N_ALT_OBJ, CollisionFlag.E_ALT_OBJ)
                    removeFlag(adjustedX, adjustedY + 1, CollisionFlag.S_ALT_OBJ)
                    removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_ALT_OBJ)
                }
                if (rotation == 2) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.E_ALT_OBJ, CollisionFlag.S_ALT_OBJ)
                    removeFlag(adjustedX + 1, adjustedY, CollisionFlag.W_ALT_OBJ)
                    removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_ALT_OBJ)
                }
                if (rotation == 3) {
                    removeFlag(adjustedX, adjustedY, CollisionFlag.S_ALT_OBJ, CollisionFlag.W_ALT_OBJ)
                    removeFlag(adjustedX, adjustedY - 1, CollisionFlag.N_ALT_OBJ)
                    removeFlag(adjustedX - 1, adjustedY, CollisionFlag.E_ALT_OBJ)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun getFlags(value: Int): ArrayList<CollisionFlag> {
            val flags = ArrayList<CollisionFlag>()
            for (f in CollisionFlag.values()) {
                if ((value and f.flag) != 0 && f != CollisionFlag.EMPTY) {
                    flags.add(f)
                }
            }
            return flags
        }

        @JvmStatic
        fun notFlagged(value: Int, vararg flags: CollisionFlag): Boolean {
            var flag = 0
            for (f in flags) {
                flag = flag or f.flag
            }
            return (value and flag) == 0
        }

        @JvmStatic
        fun createClipMap(sceneSizeX: Int, sceneSizeY: Int): CollisionMap {
            val clipping = CollisionMap()
            clipping.offsetX = -1
            clipping.offsetY = -1
            clipping.sizeX = sceneSizeX + 1 + 5
            clipping.sizeY = sceneSizeY + 1 + 5
            clipping.collisionFlags = Array(clipping.sizeX) { IntArray(clipping.sizeY) }
            clipping.reset()
            return clipping
        }

        @JvmStatic
        fun canReach(srcX: Int, srcY: Int, sizeX: Int, sizeY: Int, dstX: Int, dstY: Int, dstSizeX: Int, dstSizeY: Int): Boolean {
            return (srcX < dstX + dstSizeX && dstX < srcX + sizeX) && (srcY < dstY + dstSizeY && dstY < sizeY + srcY)
        }
    }
}
