package com.jagex.game.runetek5.graphics.hint

import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 *
 * @author Brainpower - 06/04/2022
 *
 */
object HintRenderer {

    @JvmStatic
    fun drawHintArrow(x: Int, y: Int) {
        val arrows = PlayerUpdate.HINT_ARROWS
        for (index in arrows.indices) {
            val arrow = arrows[index] ?: continue
            if (arrow.targetType == 2) {
                calculateHintArrowPlacement(arrow.plane, arrow.x, arrow.y, arrow.height * 2)
                if (Client.HINT_POSITION[0] > -1.0f && Client.cycles % 20 < 10) {
                    val arrowSprite = StaticMedia.hintHeadiconSprites!![arrow.arrowSprite]
                    val drawX = (Client.HINT_POSITION[0] + x - 12.0f).toInt()
                    val drawY = (Client.HINT_POSITION[1] + y - 28.0f).toInt()
                    arrowSprite.draw(drawX, drawY)
                }
            }
        }
    }

    @JvmStatic
    fun calculateHintArrowPlacement(
        plane: Int,
        absX: Int,
        absY: Int,
        translateSunX: Int,
        floorOffset: Int,
        calculateProjection: Boolean
    ) {
        val mapRegion = Resource.mapRegion
        if (!calculateProjection && (absX < 512 || absY < 512 ||
                    absX > (mapRegion!!.sceneSizeX - 2) * 512 || absY > (mapRegion.sceneSizeY - 2) * 512)
        ) {
            Client.HINT_POSITION[1] = -1.0f
            Client.HINT_POSITION[0] = -1.0f
        } else {
            val drawHeight = MapRegion.tileheightForAbsolutePosition(absX, absY, plane) - floorOffset
            val toolkit = Renderers.activeToolkit!!
            Client.SUN_MATRIX.copyFrom(toolkit.copySunPosition())
            Client.SUN_MATRIX.translate(translateSunX.toFloat(), 0.0f, 0.0f)
            toolkit.setSunPosition(Client.SUN_MATRIX)
            if (calculateProjection) {
                toolkit.calculateProjection(absX.toFloat(), drawHeight.toFloat(), absY.toFloat(), Client.HINT_POSITION)
            } else {
                toolkit.computeWorldToScreen(absX.toFloat(), drawHeight.toFloat(), absY.toFloat(), Client.HINT_POSITION)
            }
            Client.SUN_MATRIX.translate(-translateSunX.toFloat(), 0.0f, 0.0f)
            toolkit.setSunPosition(Client.SUN_MATRIX)
            Client.HINT_POSITION[0] -= Client.hintArrowOffsetX
            Client.HINT_POSITION[1] -= Client.hintArrowOffsetY
        }
    }

    @JvmStatic
    fun calculateHintArrowPlacement(plane: Int, absX: Int, absY: Int, translateSunX: Int) {
        calculateHintArrowPlacement(plane, absX, absY, 0, translateSunX, false)
    }

    @JvmStatic
    fun calculateHintForSceneobject(graphNode: SceneGraphEntity, floorOffset: Int, calculateProjection: Boolean) {
        val absX: Int
        val absY: Int

        if (graphNode is AbstractInteractiveEntity && graphNode is GameObject) {
            val width = (graphNode.endX.toInt() - graphNode.startX.toInt() + 1) shl 9
            val length = (graphNode.endZ.toInt() - graphNode.startZ.toInt() + 1) shl 9
            absX = width / 2 + (graphNode.startX.toInt() shl 9)
            absY = length / 2 + (graphNode.startZ.toInt() shl 9)
        } else {
            val abs = graphNode.getAbsolutePosition().coords
            absX = abs.x.toInt()
            absY = abs.z.toInt()
        }

        calculateHintArrowPlacement(graphNode.plane.toByte().toInt(), absX, absY, 0, floorOffset, calculateProjection)
    }

    @JvmStatic
    fun calculateSceneobjectHintProjection(sceneObject: SceneGraphEntity, floorOffset: Int) {
        calculateHintForSceneobject(sceneObject, floorOffset, true)
    }

    @JvmStatic
    fun calculateHintForSceneobject(graphNode: SceneGraphEntity, floorOffset: Int) {
        calculateHintForSceneobject(graphNode, floorOffset, false)
    }

    @JvmStatic
    fun method3151(
        offsetX: Int,
        offsetY: Int,
        component: Component,
        sprite: InteractiveComponentSprite,
        clipLeft: Int,
        clipTop: Int
    ) {
        val hintArrows = PlayerUpdate.HINT_ARROWS

        for (index in hintArrows.indices) {
            val arrow = hintArrows[index] ?: continue
            if (arrow.targetType != 0 && Client.cycles % 20 < 10) {
                if (arrow.targetType == 1) {
                    val objectNode = Client.NPC_MAP[arrow.targetIndex.toLong()] as? ObjectNode
                    if (objectNode != null) {
                        val npc = objectNode.value as NpcEntity
                        val abs = npc.getAbsolutePosition().coords
                        val x = (abs.x / 128 - offsetX / 128).toInt()
                        val y = (abs.z / 128 - offsetY / 128).toInt()
                        draw(component, sprite, clipLeft, clipTop, x, y, arrow.arrowSprite, 360000L)
                    }
                }

                if (arrow.targetType == 2) {
                    val x = arrow.x / 128 - offsetX / 128
                    val y = arrow.y / 128 - offsetY / 128
                    val long17 = (arrow.distance.toLong() shl 7)
                    val maxDistance = long17 * long17
                    draw(component, sprite, clipLeft, clipTop, x, y, arrow.arrowSprite, maxDistance)
                }

                if (arrow.targetType == 10 && arrow.targetIndex >= 0 && arrow.targetIndex < PlayerUpdate.players.size) {
                    val player = PlayerUpdate.players[arrow.targetIndex]
                    if (player != null) {
                        val abs = player.getAbsolutePosition().coords
                        val x = (abs.x / 128 - offsetX / 128).toInt()
                        val y = (abs.z / 128 - offsetY / 128).toInt()
                        draw(component, sprite, clipLeft, clipTop, x, y, arrow.arrowSprite, 360000L)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun draw(
        component: Component,
        sprite: InteractiveComponentSprite,
        clipLeft: Int,
        clipTop: Int,
        x: Int,
        y: Int,
        arrowSprite: Int,
        maxDistance: Long
    ) {
        val distanceSquared = (y * y + x * x).toLong()
        if (distanceSquared <= maxDistance) {
            val angle = if (Camera.cameraCalculationType == 4) {
                Camera.camAngleY.toInt() and 0x3fff
            } else {
                (Camera.camAngleY.toInt() + Client.minimapRotation) and 0x3fff
            }

            var sinAngle = MathUtils.SINE[angle]
            var cosAngle = MathUtils.COSINE[angle]

            if (Camera.cameraCalculationType != 4) {
                sinAngle = sinAngle * 256 / (Client.minimapZoom + 256)
                cosAngle = cosAngle * 256 / (Client.minimapZoom + 256)
            }

            val dx = (cosAngle * x + y * sinAngle) shr 14
            val dy = (y * cosAngle - sinAngle * x) shr 14
            val hintSprite = StaticMedia.hintMapmarkerSprites!![arrowSprite]
            val width = hintSprite.getSpriteWidth()
            val height = hintSprite.getSpriteHeight()
            val x1 = dx + component.width / 2 - width / 2
            val x2 = width + x1
            val y1 = -dy + component.height / 2 - height
            val y2 = height + y1

            if (sprite.withinBounds(x1, y1) && sprite.withinBounds(x2, y1) &&
                sprite.withinBounds(x1, y2) && sprite.withinBounds(x2, y2)
            ) {
                hintSprite.method2773(x1 + clipLeft, clipTop + y1, sprite.surface, clipLeft, clipTop)
            } else {
                val angleRadians = atan2(dx.toDouble(), dy.toDouble())
                var maxArrowDistance = min(component.width / 2, component.height / 2)
                maxArrowDistance -= 6
                val i25 = (sin(angleRadians) * maxArrowDistance).toInt()
                val i26 = (cos(angleRadians) * maxArrowDistance).toInt()
                StaticMedia.hintMapedgeSprites!![arrowSprite].method2758(
                    clipLeft + component.width / 2.0f + i25,
                    clipTop + component.height / 2.0f - i26,
                    4096,
                    (65535.0 * (-angleRadians / 6.283185307179586)).toInt()
                )
            }
        }
    }
}
