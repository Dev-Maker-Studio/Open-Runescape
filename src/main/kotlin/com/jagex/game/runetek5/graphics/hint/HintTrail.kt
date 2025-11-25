package com.jagex.game.runetek5.graphics.hint

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.entity.hint.HintTrailComparable
import com.jagex.game.runetek5.entity.hint.HintTrailGraphNode
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.HAS_TEXTURE_COORDS
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import java.util.Deque
import java.util.LinkedList

class HintTrail(toolkit: AbstractToolkit, buffer: Packet, @JvmField val modelId: Int) {

    @JvmField var hintTrialGraphNodes: Deque<HintTrailGraphNode>? = null

    @JvmField val length: Int

    @JvmField val placementX: IntArray

    @JvmField val placementY: IntArray

    init {
        length = buffer.readSmart()
        placementX = IntArray(length)
        placementY = IntArray(length)
        var offsetX = buffer.readUShort()
        var offsetY = buffer.readUShort()
        for (index in 0 until length) {
            placementX[index] = offsetX + buffer.readByte().toInt()
            placementY[index] = offsetY + buffer.readByte().toInt()
        }
        getStationaryModel(toolkit)
    }

    fun getStationaryModel(toolkit: AbstractToolkit): Model? {
        val mesh = Mesh.fromResource(Resource.MODEL, modelId)
        if (mesh == null) {
            return null
        }
        if (mesh.version < 13) {
            mesh.upscale()
        }
        return toolkit.createModel(mesh, HAS_TEXTURE_COORDS, trailPreferenceFlags, 64, 768)
    }

    fun addTo(scene: Scene?) {
        if (scene != null) {
            if (length > 0) {
                buildList(scene)
                for (hintTrialNode in hintTrialGraphNodes!!) {
                    scene.addInteractive(hintTrialNode, false)
                }
            }
        }
    }

    fun removeFromScene(scene: Scene?) {
        if (scene != null && hintTrialGraphNodes != null) {
            for (trailGraphNode in hintTrialGraphNodes!!) {
                scene.removeInteractiveOnCondition(
                    trailGraphNode.plane.toInt(),
                    trailGraphNode.startX.toInt(),
                    trailGraphNode.startZ.toInt(),
                    HintTrailComparable(trailGraphNode)
                )
            }
        }
    }

    fun buildList(scene: Scene) {
        hintTrialGraphNodes = LinkedList()
        val settingsBits = Resource.mapRegion.getSettingsBits()
        val base = Resource.mapRegion.getBaseCoordinates()
        var previous = CoordGrid(PlayerUpdate.MY_PLAYER_PLANE, placementX[0], placementY[0])
        for (index in 1 until length) {
            val current = CoordGrid(PlayerUpdate.MY_PLAYER_PLANE, placementX[index], placementY[index])
            while (previous.x != current.x || previous.y != current.y) {
                if (previous.x < current.x) {
                    previous.x++
                } else if (previous.x > current.x) {
                    previous.x--
                }
                if (previous.y < current.y) {
                    previous.y++
                } else if (previous.y > current.y) {
                    previous.y--
                }
                var plane = PlayerUpdate.MY_PLAYER_PLANE
                val x = previous.x - base.x
                val y = previous.y - base.y
                if (x >= 0 && x < scene.sceneDimensionX && y >= 0 && y < scene.sceneDimensionY) {
                    val absX = (x shl 9) + 256
                    val absY = (y shl 9) + 256
                    if (settingsBits.areRoofsHidden(x, y)) {
                        plane++
                    }
                    hintTrialGraphNodes!!.add(
                        HintTrailGraphNode(
                            scene,
                            this,
                            PlayerUpdate.MY_PLAYER_PLANE,
                            plane,
                            absX,
                            MapRegion.tileheightForAbsolutePosition(absX, absY, PlayerUpdate.MY_PLAYER_PLANE),
                            absY
                        )
                    )
                }
            }
            previous = current
        }
    }

    companion object {
        @JvmField var trailPreferenceFlags: Int = 0

        @JvmStatic
        fun resetCache(preferenceFlags: Int) {
            trailPreferenceFlags = preferenceFlags
        }
    }
}
