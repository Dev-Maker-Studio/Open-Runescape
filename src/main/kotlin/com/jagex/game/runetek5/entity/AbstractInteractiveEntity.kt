package com.jagex.game.runetek5.entity

import com.jagex.game.runetek5.graphics.light.FlickeringEffect
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

abstract class AbstractInteractiveEntity(
    scene: Scene?,
    plane: Int,
    collisionPlane: Int,
    x: Int,
    y: Int,
    z: Int,
    startX: Int,
    endX: Int,
    startY: Int,
    endY: Int,
    @JvmField val supportsItems: Boolean,
    @JvmField val rotation: Byte
) : SceneGraphEntity(scene) {

    @JvmField var startX: Short
    @JvmField var endX: Short
    @JvmField var startZ: Short
    @JvmField var endZ: Short

    init {
        this.plane = plane.toByte()
        this.collisionPlane = collisionPlane.toByte()
        setBasePosition(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
        this.startX = startX.toShort()
        this.endX = endX.toShort()
        this.startZ = startY.toShort()
        this.endZ = endY.toShort()
    }

    open fun calculatePosition() {
        // NOOP
    }

    override fun getActiveLights(cycledLightNodes: Array<PointLight?>): Int {
        var lightCount = 0
        LoopLights@ for (x in startX.toInt()..endX.toInt()) {
            for (y in startZ.toInt()..endZ.toInt()) {
                var currentLightUid = scene!!.tileLightUids[plane.toInt()][x][y]
                var uidStep = 0L
                FindLightSlot@ while (uidStep <= 48L) {
                    val slot = (currentLightUid ushr uidStep.toInt() and 0xffffL).toInt()
                    if (slot <= 0)
                        break
                    val positionedLight = scene!!.lightSlotOccupant[slot - 1]
                    for (loopPlane in 0 until lightCount) {
                        if (cycledLightNodes[loopPlane] == positionedLight.pointLight) {
                            uidStep += 16L
                            continue@FindLightSlot
                        }
                    }
                    cycledLightNodes[lightCount++] = positionedLight.pointLight
                    if (lightCount == 4)
                        break@LoopLights
                    uidStep += 16L
                }
            }
        }
        for (lightIndex in lightCount until 4)
            cycledLightNodes[lightIndex] = null
        if (rotation.toInt() != 0) {
            val cameraOffsetX = startX - scene!!.cameraGridPositionX
            val cameraOffsetY = startZ - scene!!.cameraGridPositionY
            val StartX: Short
            val StartY: Int
            val EndX: Int
            val EndY: Short
            if (rotation.toInt() == 1) {
                if (cameraOffsetY > cameraOffsetX) {
                    StartX = startX
                    StartY = startZ - 1
                    EndX = 1 + startX
                    EndY = startZ
                } else {
                    StartX = startX
                    StartY = 1 + startZ
                    EndX = startX - 1
                    EndY = startZ
                }
            } else if (cameraOffsetY > -cameraOffsetX) {
                StartX = startX
                StartY = startZ - 1
                EndX = startX - 1
                EndY = startZ
            } else {
                StartX = startX
                StartY = startZ + 1
                EndX = 1 + startX
                EndY = startZ
            }
            var plane = 0
            FindLight@ while (plane < lightCount) {
                var lightUid = scene!!.tileLightUids[plane][StartX.toInt()][StartY]
                while (0L != lightUid) {
                    val positionedLight = scene!!.lightSlotOccupant[((lightUid and 0xffffL) - 1L).toInt()]
                    lightUid = lightUid ushr 16
                    if (cycledLightNodes[plane] == positionedLight.pointLight)
                        continue@FindLight
                }
                lightUid = scene!!.tileLightUids[plane][EndX][EndY.toInt()]
                while (lightUid != 0L) {
                    val positionedLight = scene!!.lightSlotOccupant[((lightUid and 0xffffL) - 1L).toInt()]
                    lightUid = lightUid ushr 16
                    if (cycledLightNodes[plane] == positionedLight.pointLight)
                        continue@FindLight
                }
                for (ascendingLightPlane in plane until lightCount - 1)
                    cycledLightNodes[ascendingLightPlane] = cycledLightNodes[1 + ascendingLightPlane]
                lightCount--
                plane++
            }
        }
        return lightCount
    }


    /**
     * [J] Tile the entity is located on is hidden
     */
    override fun isSoftHidden(toolkit: AbstractToolkit): Boolean {
        return scene!!.culling.hideTile(collisionPlane.toInt(), startX.toInt(), endX.toInt(), startZ.toInt(), endZ.toInt(), getAxisAlignedBoundingBox(toolkit))
    }

    /**
     * [J] Is the entity in view distance
     */
    override fun canRender(): Boolean {
        for (x in startX.toInt()..endX.toInt()) {
            for (y in startZ.toInt()..endZ.toInt()) {
                val tileX = scene!!.visibleTiles + (x - scene!!.cameraGridPositionX)
                if (tileX >= 0 && tileX < scene!!.adjacent_tile_on_screen.size) {
                    val tileY = scene!!.visibleTiles + (y - scene!!.cameraGridPositionY)
                    if (tileY >= 0 && tileY < scene!!.adjacent_tile_on_screen.size && scene!!.adjacent_tile_on_screen[tileX][tileY]) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
