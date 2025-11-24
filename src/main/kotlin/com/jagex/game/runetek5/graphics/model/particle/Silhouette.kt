package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.graphics.model.Model

/**
 * Axis-aligned bounding box (AABB)
 * @author Steven - 17/10/2023
 */
class Silhouette(
    absX: Int,
    absY: Int,
    absZ: Int,
    size2d: Int,
    minX: Int,
    maxX: Int,
    minY: Int,
    maxY: Int,
    minZ: Int,
    maxZ: Int
) {

    @JvmField var absX: Int = 0
    @JvmField var absY: Int = 0
    @JvmField var absZ: Int = 0
    @JvmField var size3d: Int = 0
    @JvmField var minX: Int = 0
    @JvmField var maxX: Int = 0
    @JvmField var minY: Int = 0
    @JvmField var maxY: Int = 0
    @JvmField var minZ: Int = 0
    @JvmField var maxZ: Int = 0

    init {
        this.absX = absX
        this.absY = absY
        this.absZ = absZ
        this.size3d = size2d * size2d
        this.minX = this.absX + minX
        this.maxX = this.absX + maxX
        this.minY = this.absY + minY
        this.maxY = this.absY + maxY
        this.minZ = this.absZ + minZ
        this.maxZ = this.absZ + maxZ
    }

    fun set(absX: Int, absY: Int, absZ: Int, size2d: Int, minX: Int, maxX: Int, minY: Int, maxY: Int, minZ: Int, maxZ: Int) {
        this.absX = absX
        this.absY = absY
        this.absZ = absZ
        this.size3d = size2d * size2d
        this.minX = this.absX + minX
        this.maxX = this.absX + maxX
        this.minY = this.absY + minY
        this.maxY = this.absY + maxY
        this.minZ = this.absZ + minZ
        this.maxZ = this.absZ + maxZ
    }

    fun testIntersection(posX: Int, posY: Int, posZ: Int): Boolean {
        if (posX < minX || posX > maxX) {
            return false
        }
        if (posY < minY || posY > maxY) {
            return false
        }
        if (posZ < minZ || posZ > maxZ) {
            return false
        }
        val sizex = posX - absX
        val sizez = posZ - absZ
        return sizex * sizex + sizez * sizez < size3d
    }

    companion object {
        @JvmStatic
        fun set(silhouette: Silhouette, x: Int, y: Int, z: Int, model: Model?) {
            if (model != null) {
                silhouette.set(x, y, z, model.getSize2d(), model.getMinX(), model.getMaxX(), model.getMinY(), model.getMaxY(), model.getMinZ(), model.getMaxZ())
            }
        }

        @JvmStatic
        fun generate(x: Int, y: Int, z: Int, model: Model?): Silhouette? {
            if (model == null) {
                return null
            }
            return Silhouette(
                x, y, z, model.getSize2d(), model.getMinX(), model.getMaxX(),
                model.getMinY(), model.getMaxY(), model.getMinZ(), model.getMaxZ()
            )
        }
    }
}
