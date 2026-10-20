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
    }

    override fun getActiveLights(cycledLightNodes: Array<PointLight?>): Int {
        var lightCount = 0
        LoopLights@ for (x in startX.toInt()..endX.toInt()) {
            for (y in startZ.toInt()..endZ.toInt()) {
                val currentLightUid = scene!!.tileLightUids!![plane.toInt()][x][y]
                var uidStep = 0L

                while (uidStep <= 48L) {
                    val slot = (currentLightUid ushr uidStep.toInt() and 0xffffL).toInt()
                    if (slot <= 0) break

                    val positionedLight = scene!!.lightSlotOccupant!![ slot - 1]

                    var isDuplicate = false
                    for (loopPlane in 0 until lightCount) {
                        if (cycledLightNodes[loopPlane]!! == positionedLight!!.pointLight) {
                            isDuplicate = true
                            break
                        }
                    }

                    if (!isDuplicate) {
                        cycledLightNodes[lightCount++] = positionedLight!!.pointLight
                        if (lightCount == 4) break@LoopLights
                    }

                    uidStep += 16L
                }
            }
        }

        for (lightIndex in lightCount until 4) {
            cycledLightNodes[lightIndex] = null
        }

        if (rotation.toInt() != 0) {
            val cameraOffsetX = startX - scene!!.cameraGridPositionX
            val cameraOffsetY = startZ - scene!!.cameraGridPositionY

            val (startX_adj, startY_adj, endX_adj, endY_adj) = if (rotation.toInt() == 1) {
                if (cameraOffsetY > cameraOffsetX) {
                    arrayOf(startX.toInt(), startZ - 1, startX + 1, startZ.toInt())
                } else {
                    arrayOf(startX.toInt(), startZ + 1, startX - 1, startZ.toInt())
                }
            } else {
                if (cameraOffsetY > -cameraOffsetX) {
                    arrayOf(startX.toInt(), startZ - 1, startX - 1, startZ.toInt())
                } else {
                    arrayOf(startX.toInt(), startZ + 1, startX + 1, startZ.toInt())
                }
            }

            var planeIndex = 0
            while (planeIndex < lightCount) {
                var foundInAdjacent = false
                var lightUid = scene!!.tileLightUids!![plane.toInt()][startX_adj][startY_adj]
                while (lightUid != 0L) {
                    val positionedLight = scene!!.lightSlotOccupant!![ ((lightUid and 0xffffL) - 1L).toInt()]
                    if (cycledLightNodes[planeIndex]!! == positionedLight!!.pointLight) {
                        foundInAdjacent = true
                        break
                    }
                    lightUid = lightUid ushr 16
                }

                if (!foundInAdjacent) {
                    lightUid = scene!!.tileLightUids!![plane.toInt()][endX_adj][endY_adj]
                    while (lightUid != 0L) {
                        val positionedLight = scene!!.lightSlotOccupant!![ ((lightUid and 0xffffL) - 1L).toInt()]
                        if (cycledLightNodes[planeIndex]!! == positionedLight!!.pointLight) {
                            foundInAdjacent = true
                            break
                        }
                        lightUid = lightUid ushr 16
                    }
                }

                if (!foundInAdjacent) {
                    for (i in planeIndex until lightCount - 1)
                        cycledLightNodes[i] = cycledLightNodes[i + 1]
                    lightCount--
                }
                planeIndex++
            }
        }

        return lightCount
    }

    override fun isSoftHidden(toolkit: AbstractToolkit): Boolean {
        return scene!!.culling.hideTile(collisionPlane.toInt(), startX.toInt(), endX.toInt(), startZ.toInt(), endZ.toInt(), getAxisAlignedBoundingBox(toolkit))
    }

    override fun canRender(): Boolean {
        for (x in startX.toInt()..endX.toInt()) {
            for (y in startZ.toInt()..endZ.toInt()) {
                val tileX = scene!!.visibleTiles + (x - scene!!.cameraGridPositionX)
                if (tileX >= 0 && tileX < scene!!.tileInFrustum.size) {
                    val tileY = scene!!.visibleTiles + (y - scene!!.cameraGridPositionY)
                    if (tileY >= 0 && tileY < scene!!.tileInFrustum.size && scene!!.tileInFrustum[tileX][tileY]) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
