package com.jagex.game.runetek5.entity.pathingentity

import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.hitsplat.HitSplatType
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.entity.EntityNodeList
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.hint.HintRenderer
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.math.QuaternionPool
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.vardomain.VarProviders

object EntityUpdating {

    @JvmField
    var currentMovementType: Int = 0

    @JvmField
    val entityOverheadPool: EntityNodeList<EntityOverhead> = EntityNodeList()

    @JvmField
    var pooledOverheadCount: Int = 0

    @JvmField
    var turnDirectionIndex: Int = 0

    @JvmStatic
    fun drawOverheadInterfaces(componentReference: Int, startX: Int, startY: Int, endX: Int, endY: Int, drawX: Int, drawY: Int) {
        if (Client.MAP_LOADING_STAGE == 1) {
            val playerIndiceCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES
            val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES
            for (index in 0 until playerIndiceCount) {
                val player = PlayerUpdate.players[playerIndices[index]]
                player?.drawOverheadInterface(componentReference, startX, startY, endX, endY, drawX, drawY)
            }
            for (index in 0 until Client.NPC_UPDATE_INDEX) {
                val npcKey = Client.NPC_UPDATE_INDICES[index]
                val npcObjectNode = Client.NPC_MAP.get(npcKey.toLong())
                if (npcObjectNode != null) {
                    (npcObjectNode.value as PathingEntity).drawOverheadInterface(componentReference, startX, startY, endX, endY, drawX, drawY)
                }
            }
        }
    }

    @JvmStatic
    fun move(pathingEntity: PathingEntity, cutscene: Boolean) {
        var moveType = MovementType.STATIONARY.id.toInt()
        var turnDirection = 0

        if (pathingEntity.forceMovementT1Delay > Client.cycles) {
            PathingEntity.forceMovementType1(pathingEntity)
        } else if (pathingEntity.forceMovementT2Delay >= Client.cycles) {
            PathingEntity.forceMovementType2(pathingEntity)
        } else {
            updatePosition(pathingEntity, cutscene)
            moveType = currentMovementType
            turnDirection = turnDirectionIndex
        }

        val abs = pathingEntity.getAbsolutePosition().coords
        if (abs.x.toInt() < 512 || abs.z.toInt() < 512 || abs.x.toInt() >= (Resource.mapRegion!!.sceneSizeX - 1) * 512 || abs.z.toInt() >= (Resource.mapRegion!!.sceneSizeY - 1) * 512) {
            pathingEntity.currentSequence.update(-1)
            for (index in pathingEntity.spotAnimations.indices) {
                pathingEntity.spotAnimations[index].spotAnimationId = -1
                pathingEntity.spotAnimations[index].sequence.update(-1.toByte())
            }
            pathingEntity.currentAnimations = null
            pathingEntity.forceMovementT1Delay = 0
            pathingEntity.forceMovementT2Delay = 0
            moveType = MovementType.STATIONARY.id.toInt()
            turnDirection = 0
            pathingEntity.setBasePosition((512 * pathingEntity.movementBaseX[0] + pathingEntity.getSize() * 256).toFloat(), abs.y, (pathingEntity.movementBaseY[0] * 512 + pathingEntity.getSize() * 256).toFloat())
            pathingEntity.resetXyIndex()
        }

        if (PlayerUpdate.MY_PLAYER!! == pathingEntity && (abs.x.toInt() < 6144 || abs.z.toInt() < 6144 || abs.x.toInt() >= (Resource.mapRegion!!.sceneSizeX - 12) * 512 || abs.z.toInt() >= (Resource.mapRegion!!.sceneSizeY - 12) * 512)) {
            pathingEntity.currentSequence.update(-1)
            for (index in pathingEntity.spotAnimations.indices) {
                pathingEntity.spotAnimations[index].spotAnimationId = -1
                pathingEntity.spotAnimations[index].sequence.update(-1.toByte())
            }
            pathingEntity.currentAnimations = null
            pathingEntity.forceMovementT1Delay = 0
            pathingEntity.forceMovementT2Delay = 0
            moveType = MovementType.STATIONARY.id.toInt()
            turnDirection = 0
            pathingEntity.setBasePosition(
                (512 * pathingEntity.movementBaseX[0] + pathingEntity.getSize() * 256).toFloat(),
                abs.y,
                (512 * pathingEntity.movementBaseY[0] + pathingEntity.getSize() * 256).toFloat()
            )
            pathingEntity.resetXyIndex()
        }

        val rotation = turnTo(pathingEntity)
        rotateEquipment(pathingEntity)
        PathingEntity.animateMovement(pathingEntity, moveType, turnDirection, rotation)
        PlayerUpdate.animateMovement(pathingEntity, moveType.toByte())
        PathingEntity.animateMovement(pathingEntity)

        val quaternion = QuaternionPool.newQuaternion()
        quaternion.rotateEuler(MathUtils.getTheta(pathingEntity.yaw.getRotation()), MathUtils.getTheta(pathingEntity.pitch.getRotation()), MathUtils.getTheta(pathingEntity.roll.getRotation()))
        pathingEntity.rotationUpdate(quaternion)
        quaternion.store()
    }

    @JvmStatic
    fun drawEntitites(clipLeft: Int, clipTop: Int, width: Int, height: Int) {
        val playerIndiceCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES
        val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES
        Client.currentHintPtr = 0

        val maxIndex: Int = if (Client.MAP_LOADING_STAGE == 4) {
            CutsceneTypeList.CUTSCENE_ENTITIES!!.size
        } else {
            playerIndiceCount + Client.NPC_UPDATE_INDEX
        }

        var entity: Any?
        for (id in 0 until maxIndex) {
            var npctype: NPCType? = null

            if (Client.MAP_LOADING_STAGE == 4) {
                val cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![id]
                if (!cutsceneEntity.initialized) {
                    continue
                }
                entity = cutsceneEntity.getEntity()
                if ((entity as PathingEntity).drawCycle != Client.timesSceneRendered) {
                    continue
                }
                if (cutsceneEntity.id >= 0) {
                    npctype = (entity as NpcEntity).definitions
                    if (npctype!!.alternativeIds != null) {
                        npctype = npctype!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
                        if (npctype == null) {
                            continue
                        }
                    }
                }
            } else {
                if (id < playerIndiceCount) {
                    entity = PlayerUpdate.players[playerIndices[id]]
                } else {
                    entity = (Client.NPC_MAP.get(Client.NPC_UPDATE_INDICES[id - playerIndiceCount].toLong()) as ObjectNode).value
                    npctype = (entity as NpcEntity).definitions
                    if (npctype!!.alternativeIds != null) {
                        npctype = npctype!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
                        if (npctype == null) {
                            continue
                        }
                    }
                }

                val pathingEntity = entity as PathingEntity
                if (pathingEntity.worldPriority < 0 || pathingEntity.drawCycle != Client.timesSceneRendered && pathingEntity.plane != PlayerUpdate.MY_PLAYER!!.plane) {
                    continue
                }
            }

            val pathingEntity = entity as PathingEntity
            HintRenderer.calculateHintForSceneobject(pathingEntity as SceneGraphEntity, pathingEntity.getIconHeight())

            if (Client.HINT_POSITION[0] >= 0.0F) {
                if (pathingEntity.privateMessagesEnabled()) {
                    val details = pathingEntity.getHintDetails()
                    if (details != null && Client.currentHintPtr < Client.MAX_HINTS) {
                        Client.HINT_WIDTH[Client.currentHintPtr] = FontMetrics.defaultFontMetrics!!.getWidthNoSprites(details.getText()!!) / 2
                        Client.HINT_X[Client.currentHintPtr] = Client.HINT_POSITION[0].toInt()
                        Client.HINT_Y[Client.currentHintPtr] = Client.HINT_POSITION[1].toInt()
                        Client.HINT_DETAILS[Client.currentHintPtr] = details
                        Client.currentHintPtr++
                    }
                }

                var drawY = (clipTop + Client.HINT_POSITION[1]).toInt()
                drawY -= FontMetrics.defaultFontMetrics!!.topPadding
                var hasDrawnHitbar = false

                if (!pathingEntity.isHidden && !pathingEntity.entityHitbars.isEmpty()) {
                    var entityHitbar = pathingEntity.entityHitbars.getFirst() as EntityHitbar?
                    while (entityHitbar != null) {
                        val displayDetails = entityHitbar.getActiveDisplay(Client.cycles)
                        if (displayDetails == null) {
                            if (entityHitbar.isEmpty()) {
                                entityHitbar.unlinkNode()
                            }
                        } else {
                            val hitbarType = entityHitbar.hitbarType
                            val playerEntity = PlayerUpdate.players[playerIndices[id]]
                            val redBar: NativeSprite?
                            val greenBar: NativeSprite?

                            if (playerEntity != null && playerEntity.pMapdot) {
                                redBar = hitbarType.getPRedBar(Renderers.activeToolkit!!)
                                greenBar = hitbarType.getPGreenBar(Renderers.activeToolkit!!)
                            } else {
                                redBar = hitbarType.getRedBar(Renderers.activeToolkit!!)
                                greenBar = hitbarType.getGreenBar(Renderers.activeToolkit!!)
                            }

                            if (redBar != null && greenBar != null) {
                                var alpha = 255
                                val i_23 = Client.cycles - displayDetails.discardAtCycle
                                val i_24 = greenBar.getWidth() * displayDetails.toPercentage / 255
                                val i_116: Int

                                if (displayDetails.lifetimeCycles > i_23) {
                                    val i_25 = if (hitbarType.animationStepSize == 0) 0 else hitbarType.animationStepSize * (i_23 / hitbarType.animationStepSize)
                                    val drawX = greenBar.getWidth() * displayDetails.fromPercentage / 255
                                    i_116 = i_25 * (i_24 - drawX) / displayDetails.lifetimeCycles + drawX
                                } else {
                                    i_116 = i_24
                                    val i_25 = displayDetails.lifetimeCycles + hitbarType.fadeOutDuration - i_23
                                    if (hitbarType.fadeStartOffset >= 0) {
                                        alpha = (i_25 shl 8) / (hitbarType.fadeOutDuration - hitbarType.fadeStartOffset)
                                    }
                                }

                                var finalI_116 = i_116
                                if (displayDetails.toPercentage > 0 && finalI_116 < 2) {
                                    finalI_116 = 2
                                }

                                val i_25 = redBar.getHeight()
                                val drawX = (Client.HINT_POSITION[0] + clipLeft - (redBar.getWidth() shr 1)).toInt()
                                drawY -= i_25

                                if (alpha >= 0 && alpha < 255) {
                                    val packedAlpha = alpha shl 24
                                    val colorArgb = packedAlpha or 0xffffff
                                    redBar.draw(drawX, drawY, 0, colorArgb, 1)
                                    Renderers.activeToolkit!!.expandClipping(drawX, drawY, drawX + finalI_116, drawY + i_25)
                                    greenBar.draw(drawX, drawY, 0, colorArgb, 1)
                                } else {
                                    redBar.draw(drawX, drawY)
                                    Renderers.activeToolkit!!.expandClipping(drawX, drawY, finalI_116 + drawX, drawY + i_25)
                                    greenBar.draw(drawX, drawY)
                                }

                                Renderers.activeToolkit!!.setClipping(clipLeft, clipTop, clipLeft + width, height + clipTop)
                                drawY -= 2
                                hasDrawnHitbar = true
                            }
                        }
                        entityHitbar = pathingEntity.entityHitbars.getNext() as EntityHitbar?
                    }
                }

                if (!hasDrawnHitbar) {
                    drawY -= Client.ENTITY_DEFAULTS!!.defaultHitbarHeight + 2
                }

                if (!pathingEntity.isHidden) {
                    if (npctype == null) {
                        val playerEntity = pathingEntity as PlayerEntity
                        if (playerEntity.skullId != -1) {
                            val skullSprite = StaticMedia.headiconPkSprites!![playerEntity.skullId]
                            drawY -= skullSprite.getHeight()
                            skullSprite.draw((Client.HINT_POSITION[0] + clipLeft - 12.0F).toInt(), drawY)
                            drawY -= 2
                        }
                        if (playerEntity.prayerHeadIconId != -1) {
                            val prayerSprite = StaticMedia.headiconPrayerSprites!![playerEntity.prayerHeadIconId]
                            drawY -= prayerSprite.getHeight()
                            prayerSprite.draw((Client.HINT_POSITION[0] + clipLeft - 12.0F).toInt(), drawY)
                            drawY -= 2
                        }
                    } else if (npctype.headIcons >= 0 && npctype.headIcons < StaticMedia.headiconPrayerSprites!!.size) {
                        val nativesprite120 = StaticMedia.headiconPrayerSprites!![npctype.headIcons]
                        drawY -= nativesprite120.getHeight()
                        nativesprite120.draw((clipLeft + Client.HINT_POSITION[0] - (nativesprite120.getWidth() shr 1)).toInt(), drawY)
                        drawY -= 2
                    }
                }

                if (entity is PlayerEntity) {
                    if (id >= 0) {
                        var i_15 = 0
                        for (index in PlayerUpdate.HINT_ARROWS.indices) {
                            val arrow = PlayerUpdate.HINT_ARROWS[index]
                            if (arrow != null && arrow.targetType == 10 && arrow.targetIndex == playerIndices[id]) {
                                val playerArrow = StaticMedia.hintHeadiconSprites!![arrow.arrowSprite]
                                if (playerArrow.getHeight() > i_15) {
                                    i_15 = playerArrow.getHeight()
                                }
                                playerArrow.draw((clipLeft + Client.HINT_POSITION[0] - 12.0F).toInt(), drawY - playerArrow.getHeight())
                            }
                        }
                        if (i_15 > 0) {
                            @Suppress("UNUSED_VALUE")
                            val i_10000 = drawY - (i_15 + 2)
                        }
                    }
                } else {
                    var i_15 = 0
                    for (index in PlayerUpdate.HINT_ARROWS.indices) {
                        val arrow = PlayerUpdate.HINT_ARROWS[index]
                        if (arrow != null && arrow.targetType == 1 && Client.NPC_UPDATE_INDICES[id - playerIndiceCount] == arrow.targetIndex) {
                            val npcArrow = StaticMedia.hintHeadiconSprites!![arrow.arrowSprite]
                            if (npcArrow.getHeight() > i_15) {
                                i_15 = npcArrow.getHeight()
                            }
                            val shouldDraw: Boolean = if (arrow.denominator == 0) {
                                true
                            } else {
                                val i_21 = GameShell.gameCyclesPerSecond() * 1000 / arrow.denominator / 2
                                Client.cycles % (i_21 * 2) < i_21
                            }
                            if (shouldDraw) {
                                npcArrow.draw((clipLeft + Client.HINT_POSITION[0] - 12.0F).toInt(), drawY - npcArrow.getHeight())
                            }
                        }
                    }
                    if (i_15 > 0) {
                        @Suppress("UNUSED_VALUE")
                        val i_10000 = drawY - (i_15 + 2)
                    }
                }

                // Hitsplat rendering
                for (hitsplatIndex in 0 until Client.ENTITY_DEFAULTS!!.maximumHits) {
                    val cycle = pathingEntity.hitsplatCycles[hitsplatIndex]
                    val hitplatId = pathingEntity.activeHitsplatIds[hitsplatIndex]
                    var hitsplatType: HitSplatType? = null
                    var cyclesVisible = 0

                    if (hitplatId >= 0) {
                        if (cycle <= Client.cycles) {
                            continue
                        }
                        hitsplatType = TypeLists.HITSPLAT!!.list(pathingEntity.activeHitsplatIds[hitsplatIndex])
                        cyclesVisible = hitsplatType.cyclesVisible
                    } else if (cycle < 0) {
                        continue
                    }

                    val soakHitsplatId = pathingEntity.soakHitsplatIds[hitsplatIndex]
                    var soakHitsplat: HitSplatType? = null
                    if (soakHitsplatId >= 0) {
                        soakHitsplat = TypeLists.HITSPLAT!!.list(soakHitsplatId)
                    }

                    if (cycle - cyclesVisible <= Client.cycles) {
                        if (hitsplatType == null) {
                            pathingEntity.hitsplatCycles[hitsplatIndex] = -1
                        } else {
                            val i_116 = pathingEntity.getIconHeight() / 2
                            HintRenderer.calculateHintForSceneobject(pathingEntity as SceneGraphEntity, i_116)

                            if (Client.HINT_POSITION[0] > -1.0f) {
                                Client.HINT_POSITION[0] += Client.ENTITY_DEFAULTS!!.hitOffsetsX!![hitsplatIndex]
                                Client.HINT_POSITION[1] += Client.ENTITY_DEFAULTS!!.hitOffsetsY!![hitsplatIndex]

                                var i_27 = 0
                                var i_28 = 0
                                var i_29 = 0
                                var i_30 = 0
                                var i_31 = 0
                                var i_32 = 0
                                var i_33 = 0
                                var i_34 = 0
                                var soakLeftCapSprite: NativeSprite? = null
                                var soakMiddleFillSprite: NativeSprite? = null
                                var soakInnerLeftSprite: NativeSprite? = null
                                var soakRightCapSprite: NativeSprite? = null
                                var i_39 = 0
                                var i_40 = 0
                                var i_41 = 0
                                var i_42 = 0
                                var i_43 = 0
                                var i_44 = 0
                                var i_45 = 0
                                var i_46 = 0
                                var i_47 = 0

                                val leftCapSprite = hitsplatType.getLeftCapSprite(Renderers.activeToolkit!!)
                                if (leftCapSprite != null) {
                                    i_27 = leftCapSprite.getWidth()
                                    val i_49 = leftCapSprite.getHeight()
                                    if (i_49 > i_47) {
                                        i_47 = i_49
                                    }
                                    leftCapSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                    i_31 = Client.HITSPLAT_SIZE_CACHE[0]
                                }

                                val middleFillSprite = hitsplatType.getMiddleFillSprite(Renderers.activeToolkit!!)
                                if (middleFillSprite != null) {
                                    i_28 = middleFillSprite.getWidth()
                                    val i_50 = middleFillSprite.getHeight()
                                    if (i_50 > i_47) {
                                        i_47 = i_50
                                    }
                                    middleFillSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                    i_32 = Client.HITSPLAT_SIZE_CACHE[0]
                                }

                                val innerLeftSprite = hitsplatType.getInnerLeftSprite(Renderers.activeToolkit!!)
                                if (innerLeftSprite != null) {
                                    i_29 = innerLeftSprite.getWidth()
                                    val i_51 = innerLeftSprite.getHeight()
                                    if (i_51 > i_47) {
                                        i_47 = i_51
                                    }
                                    innerLeftSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                    i_33 = Client.HITSPLAT_SIZE_CACHE[0]
                                }

                                val rightCapSprite = hitsplatType.getRightCapSprite(Renderers.activeToolkit!!)
                                var i_52: Int
                                if (rightCapSprite != null) {
                                    i_30 = rightCapSprite.getWidth()
                                    i_52 = rightCapSprite.getHeight()
                                    if (i_52 > i_47) {
                                        i_47 = i_52
                                    }
                                    rightCapSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                    i_34 = Client.HITSPLAT_SIZE_CACHE[0]
                                }

                                if (soakHitsplat != null) {
                                    soakLeftCapSprite = soakHitsplat.getLeftCapSprite(Renderers.activeToolkit!!)
                                    if (soakLeftCapSprite != null) {
                                        i_39 = soakLeftCapSprite.getWidth()
                                        i_52 = soakLeftCapSprite.getHeight()
                                        if (i_52 > i_47) {
                                            i_47 = i_52
                                        }
                                        soakLeftCapSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                        i_43 = Client.HITSPLAT_SIZE_CACHE[0]
                                    }
                                    soakMiddleFillSprite = soakHitsplat.getMiddleFillSprite(Renderers.activeToolkit!!)
                                    if (soakMiddleFillSprite != null) {
                                        i_40 = soakMiddleFillSprite.getWidth()
                                        i_52 = soakMiddleFillSprite.getHeight()
                                        if (i_52 > i_47) {
                                            i_47 = i_52
                                        }
                                        soakMiddleFillSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                        i_44 = Client.HITSPLAT_SIZE_CACHE[0]
                                    }
                                    soakInnerLeftSprite = soakHitsplat.getInnerLeftSprite(Renderers.activeToolkit!!)
                                    if (soakInnerLeftSprite != null) {
                                        i_41 = soakInnerLeftSprite.getWidth()
                                        i_52 = soakInnerLeftSprite.getHeight()
                                        if (i_52 > i_47) {
                                            i_47 = i_52
                                        }
                                        soakInnerLeftSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                        i_45 = Client.HITSPLAT_SIZE_CACHE[0]
                                    }
                                    soakRightCapSprite = soakHitsplat.getRightCapSprite(Renderers.activeToolkit!!)
                                    if (soakRightCapSprite != null) {
                                        i_42 = soakRightCapSprite.getWidth()
                                        i_52 = soakRightCapSprite.getHeight()
                                        if (i_52 > i_47) {
                                            i_47 = i_52
                                        }
                                        soakRightCapSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                        i_46 = Client.HITSPLAT_SIZE_CACHE[0]
                                    }
                                }

                                var hitsplatFontRenderer = Renderers.FONT_RENDERER
                                var soakFont = Renderers.FONT_RENDERER
                                var hitsplatFontMetrics = FontManager.worldMapAreaFont
                                var soakFontMetrics = FontManager.worldMapAreaFont
                                var fontId = hitsplatType.fontId
                                var resolvedFontRenderer: AbstractFontRenderer?
                                var resolvedFontMetrics: FontMetrics?

                                if (fontId >= 0) {
                                    resolvedFontRenderer = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, fontId, true, hitsplatType.hasColor) as AbstractFontRenderer?
                                    resolvedFontMetrics = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, fontId)
                                    if (resolvedFontRenderer != null && resolvedFontMetrics != null) {
                                        hitsplatFontRenderer = resolvedFontRenderer
                                        hitsplatFontMetrics = resolvedFontMetrics
                                    }
                                }

                                if (soakHitsplat != null) {
                                    fontId = soakHitsplat.fontId
                                    if (fontId >= 0) {
                                        resolvedFontRenderer = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, fontId, true, soakHitsplat.hasColor) as AbstractFontRenderer?
                                        resolvedFontMetrics = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, fontId)
                                        if (resolvedFontRenderer != null && resolvedFontMetrics != null) {
                                            soakFont = resolvedFontRenderer
                                            soakFontMetrics = resolvedFontMetrics
                                        }
                                    }
                                }

                                var soakText: String? = null
                                val hitsplatText = pathingEntity.hitsplatCustomText[hitsplatIndex] ?: hitsplatType.getCenteredText(pathingEntity.hitsplatText[hitsplatIndex])
                                val regularWidth = hitsplatFontMetrics!!.getWidthNoSprites(hitsplatText)
                                var soakWidth = 0

                                if (soakHitsplat != null) {
                                    soakText = pathingEntity.soakHitsplatCustomText[hitsplatIndex] ?: soakHitsplat.getCenteredText(pathingEntity.soakHitsplatText[hitsplatIndex])
                                    soakWidth = soakFontMetrics!!.getWidthNoSprites(soakText)
                                }

                                var i_63 = 0
                                var i_64 = 0
                                if (i_28 > 0) {
                                    i_63 = regularWidth / i_28 + 1
                                }
                                if (soakHitsplat != null && i_40 > 0) {
                                    i_64 = soakWidth / i_40 + 1
                                }

                                var i_65 = 0
                                val i_66 = i_65
                                if (i_27 > 0) {
                                    i_65 += i_27
                                }
                                i_65 += 2
                                val i_67 = i_65
                                if (i_29 > 0) {
                                    i_65 += i_29
                                }
                                val i_68 = i_65
                                val i_69 = i_65
                                var i_70: Int
                                if (i_28 > 0) {
                                    i_70 = i_28 * i_63
                                    i_65 += i_70
                                    @Suppress("UNUSED_VALUE")
                                    val temp = (i_70 - regularWidth) / 2
                                } else {
                                    i_65 += regularWidth
                                }
                                i_70 = i_65
                                if (i_30 > 0) {
                                    i_65 += i_30
                                }

                                var i_71 = 0
                                var i_72 = 0
                                var i_73 = 0
                                var i_74 = 0
                                var i_75 = 0
                                var i_76: Int

                                if (soakHitsplat != null) {
                                    i_65 += 2
                                    i_71 = i_65
                                    if (i_39 > 0) {
                                        i_65 += i_39
                                    }
                                    i_65 += 2
                                    i_72 = i_65
                                    if (i_41 > 0) {
                                        i_65 += i_41
                                    }
                                    i_73 = i_65
                                    i_75 = i_65
                                    if (i_40 > 0) {
                                        i_76 = i_64 * i_40
                                        i_65 += i_76
                                        @Suppress("UNUSED_VALUE")
                                        val temp = (i_76 - soakWidth) / 2
                                    } else {
                                        i_65 += soakWidth
                                    }
                                    i_74 = i_65
                                    if (i_42 > 0) {
                                        i_65 += i_42
                                    }
                                }

                                i_76 = pathingEntity.hitsplatCycles[hitsplatIndex] - Client.cycles
                                val i_77 = hitsplatType.scrollOffsetX - i_76 * hitsplatType.scrollOffsetX / hitsplatType.cyclesVisible
                                val i_78 = i_76 * hitsplatType.scrollOffsetY / hitsplatType.cyclesVisible + -hitsplatType.scrollOffsetY
                                val i_79 = (clipLeft + Client.HINT_POSITION[0] - (i_65 shr 1) + i_77).toInt()
                                val i_80 = (i_78 + (Client.HINT_POSITION[1] + clipTop - 12.0F)).toInt()
                                var i_81 = i_80
                                var i_82 = i_80 + i_47
                                val i_83 = i_80 + hitsplatType.textOffsetY + 15
                                val i_84 = i_83 - hitsplatFontMetrics!!.topPadding
                                val i_85 = i_83 + hitsplatFontMetrics!!.bottomPadding

                                if (i_84 < i_80) {
                                    i_81 = i_84
                                }
                                if (i_85 > i_82) {
                                    i_82 = i_85
                                }

                                var i_86 = 0
                                var i_87: Int

                                if (soakHitsplat != null) {
                                    i_86 = i_80 + soakHitsplat.textOffsetY + 15
                                    val i_87_temp = i_86 - soakFontMetrics!!.topPadding
                                    val i_88 = i_86 + soakFontMetrics!!.bottomPadding
                                    if (i_87_temp < i_81) {
                                        // i_81 = i_87_temp // Dead code in original
                                    }
                                    if (i_88 > i_82) {
                                        // i_82 = i_88 // Dead code in original
                                    }
                                }

                                i_87 = 255
                                if (hitsplatType.fadeStartCycle >= 0) {
                                    i_87 = (i_76 shl 8) / (hitsplatType.cyclesVisible - hitsplatType.fadeStartCycle)
                                }

                                if (i_87 >= 0 && i_87 < 255) {
                                    val alpha = i_87 shl 24
                                    val i_89 = alpha or 0xffffff
                                    if (leftCapSprite != null) {
                                        leftCapSprite.draw(i_66 + i_79 - i_31, i_80, 0, i_89, 1)
                                    }
                                    if (innerLeftSprite != null) {
                                        innerLeftSprite.draw(i_67 + i_79 - i_33, i_80, 0, i_89, 1)
                                    }
                                    var i_90: Int
                                    if (middleFillSprite != null) {
                                        for (i_90 in 0 until i_63) {
                                            middleFillSprite.draw(i_90 * i_28 + (i_68 + i_79 - i_32), i_80, 0, i_89, 1)
                                        }
                                    }
                                    if (rightCapSprite != null) {
                                        rightCapSprite.draw(i_70 + i_79 - i_34, i_80, 0, i_89, 1)
                                    }
                                    hitsplatFontRenderer!!.renderPlain(hitsplatText, i_79 + i_69, i_83, hitsplatType.color or alpha, 0)
                                    if (soakHitsplat != null) {
                                        if (soakLeftCapSprite != null) {
                                            soakLeftCapSprite.draw(i_79 + i_71 - i_43, i_80, 0, i_89, 1)
                                        }
                                        if (soakInnerLeftSprite != null) {
                                            soakInnerLeftSprite.draw(i_79 + i_72 - i_45, i_80, 0, i_89, 1)
                                        }
                                        if (soakMiddleFillSprite != null) {
                                            for (i_90 in 0 until i_64) {
                                                soakMiddleFillSprite.draw(i_90 * i_40 + (i_73 + i_79 - i_44), i_80, 0, i_89, 1)
                                            }
                                        }
                                        if (soakRightCapSprite != null) {
                                            soakRightCapSprite.draw(i_79 + i_74 - i_46, i_80, 0, i_89, 1)
                                        }
                                        soakFont!!.renderPlain(soakText!!, i_75 + i_79, i_86, soakHitsplat.color or alpha, 0)
                                    }
                                } else {
                                    if (leftCapSprite != null) {
                                        leftCapSprite.draw(i_79 + i_66 - i_31, i_80)
                                    }
                                    if (innerLeftSprite != null) {
                                        innerLeftSprite.draw(i_67 + i_79 - i_33, i_80)
                                    }
                                    if (middleFillSprite != null) {
                                        for (i in 0 until i_63) {
                                            middleFillSprite.draw(i * i_28 + (i_68 + i_79 - i_32), i_80)
                                        }
                                    }
                                    if (rightCapSprite != null) {
                                        rightCapSprite.draw(i_70 + i_79 - i_34, i_80)
                                    }
                                    hitsplatFontRenderer!!.renderPlain(hitsplatText, i_79 + i_69, i_83, hitsplatType.color or 0xFF000000.toInt(), 0)
                                    if (soakHitsplat != null) {
                                        if (soakLeftCapSprite != null) {
                                            soakLeftCapSprite.draw(i_79 + i_71 - i_43, i_80)
                                        }
                                        if (soakInnerLeftSprite != null) {
                                            soakInnerLeftSprite.draw(i_79 + i_72 - i_45, i_80)
                                        }
                                        if (soakMiddleFillSprite != null) {
                                            for (i in 0 until i_64) {
                                                soakMiddleFillSprite.draw(i_40 * i + (i_79 + i_73 - i_44), i_80)
                                            }
                                        }
                                        if (soakRightCapSprite != null) {
                                            soakRightCapSprite.draw(i_74 + i_79 - i_46, i_80)
                                        }
                                        soakFont!!.renderPlain(soakText!!, i_79 + i_75, i_86, soakHitsplat.color or 0xFF000000.toInt(), 0)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (index in 0 until Client.overlappingEntityCount) {
            val ptr = Client.ENTITY_INDEX_REFERENCES[index]
            entity = if (ptr < 2048) {
                PlayerUpdate.players[ptr]
            } else {
                (Client.NPC_MAP.get((ptr - 2048).toLong()) as ObjectNode).value
            }
            val i_92 = Client.NPC_INDEX_REFERENCES[index]
            val obj14: Any? = if (i_92 < 2048) {
                PlayerUpdate.players[i_92]
            } else {
                (Client.NPC_MAP.get((i_92 - 2048).toLong()) as ObjectNode).value
            }
            drawOverheadSprite(entity as PathingEntity, obj14 as PathingEntity, --((entity as PathingEntity).overheadSpriteCount), clipLeft, clipTop)
        }

        val padding = FontMetrics.defaultFontMetrics!!.bottomPadding + FontMetrics.defaultFontMetrics!!.topPadding + 2
        for (hintIndex in 0 until Client.currentHintPtr) {
            val hintX = Client.HINT_X[hintIndex]
            var hintY = Client.HINT_Y[hintIndex]
            val hintWidth = Client.HINT_WIDTH[hintIndex]
            var loop = true

            while (loop) {
                loop = false
                for (index in 0 until hintIndex) {
                    if (hintY + 2 > Client.HINT_Y[index] - padding && hintY - padding < Client.HINT_Y[index] + 2 && hintX - hintWidth < Client.HINT_WIDTH[index] + Client.HINT_X[index] && hintX + hintWidth > Client.HINT_X[index] - Client.HINT_WIDTH[index] && Client.HINT_Y[index] - padding < hintY) {
                        hintY = Client.HINT_Y[index] - padding
                        loop = true
                    }
                }
            }

            Client.HINT_Y[hintIndex] = hintY
            val drawnText = Client.HINT_DETAILS[hintIndex]!!.getText()

            if (Client.overheadChatFilter == 0) {
                var color = 16776960
                val i_18 = Client.HINT_DETAILS[hintIndex]!!.getColorType()

                if (i_18 < 6) {
                    color = Client.OVERHEAD_TEXT_COLORS[i_18]
                }
                if (i_18 == 6) {
                    color = if (Client.timesSceneRendered % 20 < 10) 16711680 else 16776960
                }
                if (i_18 == 7) {
                    color = if (Client.timesSceneRendered % 20 < 10) 255 else 65535
                }
                if (i_18 == 8) {
                    color = if (Client.timesSceneRendered % 20 < 10) 45056 else 8454016
                }
                if (i_18 == 9) {
                    val i_19 = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    if (i_19 < 50) {
                        color = i_19 * 1280 + 16711680
                    } else if (i_19 < 100) {
                        color = 16776960 - (i_19 - 50) * 327680
                    } else if (i_19 < 150) {
                        color = (i_19 - 100) * 5 + 65280
                    }
                }
                if (i_18 == 10) {
                    val i_19 = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    if (i_19 < 50) {
                        color = i_19 * 5 + 16711680
                    } else if (i_19 < 100) {
                        color = 16711935 - (i_19 - 50) * 327680
                    } else if (i_19 < 150) {
                        color = (i_19 - 100) * 327680 + 255 - (i_19 - 100) * 5
                    }
                }
                if (i_18 == 11) {
                    val i_19 = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    if (i_19 < 50) {
                        color = 16777215 - i_19 * 327685
                    } else if (i_19 < 100) {
                        color = (i_19 - 50) * 327685 + 65280
                    } else if (i_19 < 150) {
                        color = 16777215 - (i_19 - 100) * 327680
                    }
                }

                val drawColor = color or 0xFF000000.toInt()
                val drawType = Client.HINT_DETAILS[hintIndex]!!.getDrawType()

                if (drawType == 0) {
                    // All regular text: no effects / plain color effects (red:) / cyan / flash1 / flash2 / flash3 / glow1 / glow2 / glow3
                    MiniMenu.minimenuHeader!!.renderCentered(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, 0xFF000000.toInt())
                }
                if (drawType == 1) {
                    // Wave effect (wave:texthere)
                    MiniMenu.minimenuHeader!!.renderCenteredWave(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, Client.timesSceneRendered)
                }
                if (drawType == 2) {
                    // Wave2 effect (wave2:texthere)
                    MiniMenu.minimenuHeader!!.renderCenteredWave2(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, Client.timesSceneRendered)
                }
                if (drawType == 3) {
                    // Shake effect (shake:texthere)
                    val i_21 = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    MiniMenu.minimenuHeader!!.renderCenteredShake(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, Client.timesSceneRendered, i_21)
                }
                if (drawType == 4) {
                    // Scroll effect (scroll:texthere)
                    val i_21 = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    val i_116 = i_21 * (FontMetrics.defaultFontMetrics!!.getWidthNoSprites(drawnText!!) + 100) / 150
                    Renderers.activeToolkit!!.expandClipping(clipLeft + hintX - 50, clipTop, clipLeft + hintX + 50, height + clipTop)
                    MiniMenu.minimenuHeader!!.renderPlain(drawnText, clipLeft + hintX + 50 - i_116, hintY + clipTop, drawColor, 0xFF000000.toInt())
                    Renderers.activeToolkit!!.setClipping(clipLeft, clipTop, clipLeft + width, height + clipTop)
                }
                if (drawType == 5) {
                    // Slide effect (slide:texthere)
                    val i_21 = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    var i_116 = 0
                    if (i_21 < 25) {
                        i_116 = i_21 - 25
                    } else if (i_21 > 125) {
                        i_116 = i_21 - 125
                    }
                    val paddingY = FontMetrics.defaultFontMetrics!!.topPadding + FontMetrics.defaultFontMetrics!!.bottomPadding
                    Renderers.activeToolkit!!.expandClipping(clipLeft, hintY + clipTop - paddingY - 1, clipLeft + width, hintY + clipTop + 5)
                    MiniMenu.minimenuHeader!!.renderCentered(drawnText, clipLeft + hintX, i_116 + hintY + clipTop, drawColor, 0xFF000000.toInt())
                    Renderers.activeToolkit!!.setClipping(clipLeft, clipTop, clipLeft + width, height + clipTop)
                }
            } else {
                MiniMenu.minimenuHeader!!.renderCentered(drawnText, clipLeft + hintX, hintY + clipTop, -256, 0xFF000000.toInt())
            }
        }
    }

    @JvmStatic
    fun turnTo(current: PathingEntity): Int {
        if (current.turnDirection == 0) {
            return 0
        }

        if (current.faceEntity != -1) {
            var facingEntity: Any? = null
            if (current.faceEntity < 32768) {
                val facingNpc = Client.NPC_MAP.get(current.faceEntity.toLong())
                if (facingNpc != null) {
                    facingEntity = facingNpc.value
                }
            } else {
                facingEntity = PlayerUpdate.players[current.faceEntity - 32768]
            }

            if (facingEntity != null) {
                val delta = Vector3Pool.delta(current.getAbsolutePosition().coords, (facingEntity as PathingEntity).getAbsolutePosition().coords)
                val deltaX = delta.x.toInt()
                val deltaZ = delta.z.toInt()
                if (deltaX != 0 || deltaZ != 0) {
                    current.turn((Math.atan2(deltaX.toDouble(), deltaZ.toDouble()) * 2607.5945876176133).toInt() and 0x3fff)
                }
            }
        }

        if (current is PlayerEntity) {
            if (current.faceDirection != -1 && (current.movementStepsLeft == 0 || current.stalledStepCount > 0)) {
                current.turn(current.faceDirection)
                current.faceDirection = -1
            }
        } else if (current is NpcEntity) {
            if (current.faceX != -1 && (current.movementStepsLeft == 0 || current.stalledStepCount > 0)) {
                val abs = current.getAbsolutePosition().coords
                val base = Resource.mapRegion!!.getBaseCoordinates()
                val deltaX = abs.x.toInt() - (current.faceX * 256 - base.x * 256 - base.x * 256)
                val deltaZ = abs.z.toInt() - (current.faceY * 256 - base.y * 256 - base.y * 256)
                if (deltaX != 0 || deltaZ != 0) {
                    current.turn((Math.atan2(deltaX.toDouble(), deltaZ.toDouble()) * 2607.5945876176133).toInt() and 0x3fff)
                }
                current.faceX = -1
            }
        }

        return current.setFacingGradual()
    }

    @JvmStatic
    fun drawOverheadSprite(pathingEntity: PathingEntity, pathingNpc: PathingEntity, spriteGridIndex: Int, clipLeft: Int, clipTop: Int) {
        val overheadSpriteId = pathingNpc.getOverheadSprite()
        if (overheadSpriteId != -1) {
            var overheadSprites = Client.OVERHEAD_SPRITES.get(overheadSpriteId.toLong()) as NativeSprite?
            if (overheadSprites == null) {
                val overheadSpriteTypes = SpriteType.decodeGroup(Resource.SPRITES, overheadSpriteId)
                if (overheadSpriteTypes == null) {
                    return
                }
                overheadSprites = Renderers.activeToolkit!!.generateSprite(overheadSpriteTypes[0], true)
                Client.OVERHEAD_SPRITES.put(overheadSprites, overheadSpriteId.toLong())
            }

            val abs = pathingEntity.getAbsolutePosition().coords
            HintRenderer.calculateHintArrowPlacement(pathingEntity.plane.toInt(), abs.x.toInt(), abs.z.toInt(), pathingEntity.getSize() * 256, 0, false)
            var drawX = (clipLeft + Client.HINT_POSITION[0] - 18.0F).toInt()
            var drawY = (clipTop + Client.HINT_POSITION[1] - 16.0F - 54.0F).toInt()
            drawX += spriteGridIndex / 4 * 18
            drawY += spriteGridIndex % 4 * 18
            overheadSprites.draw(drawX, drawY)

            if (pathingNpc == pathingEntity) {
                Renderers.activeToolkit!!.drawOpaqueRectangle(drawX - 1, drawY - 1, 18, 18, -256)
            }

            val entityOverhead = createEntityOverhead()
            entityOverhead.pathingEntity = pathingNpc
            entityOverhead.startX = drawX
            entityOverhead.startY = drawY
            entityOverhead.endX = drawX + 16
            entityOverhead.endY = drawY + 16
            Client.ENTITY_OVERHEADS.offer(entityOverhead)
        }
    }

    @JvmStatic
    fun createEntityOverhead(): EntityOverhead {
        val overhead = entityOverheadPool.get() as EntityOverhead?
        if (overhead != null) {
            --pooledOverheadCount
            return overhead
        }
        return EntityOverhead()
    }

    @JvmStatic
    fun recycleOverhead(overhead: EntityOverhead) {
        overhead.pathingEntity = null
        if (pooledOverheadCount < 20) {
            entityOverheadPool.offer(overhead)
            ++pooledOverheadCount
        }
    }

    @JvmStatic
    fun positionAllEntities() {
        var overhead = Client.ENTITY_OVERHEADS.get() as EntityOverhead?
        while (overhead != null) {
            recycleOverhead(overhead)
            overhead = Client.ENTITY_OVERHEADS.get() as EntityOverhead?
        }

        val fromPlane: Byte = 0
        val toPlane: Byte = 3
        var plane: Int

        if (Client.MAP_LOADING_STAGE == 4) {
            for (plane in fromPlane.toInt()..toPlane.toInt()) {
                PlayerUpdate.positionEntitiesOnPlane(plane)
            }
        } else {
            PlayerUpdate.calculateEntityPriorities()
            for (plane in fromPlane.toInt()..toPlane.toInt()) {
                MapRegion.resetDrawPriorities()
                PlayerUpdate.updateDrawPriority(plane)
                PlayerUpdate.positionEntitiesOnPlane(plane)
            }
            Client.buildOverlappingEntityList()
        }
        PlayerUpdate.positionMisalignedEntities()
    }

    @JvmStatic
    fun updatePosition(entity: PathingEntity, cutscene: Boolean) {
        val bas = entity.getBastype()

        if (entity.movementStepsLeft == 0) {
            entity.stalledStepCount = 0
            currentMovementType = MovementType.STATIONARY.id.toInt()
            turnDirectionIndex = 0
        } else {
            if (entity.currentSequence.hasType() && !entity.currentSequence.hasSpeed()) {
                val animationdefinitions4 = entity.currentSequence.getType()
                if (entity.previousMovementStepsLeft > 0 && animationdefinitions4!!.interruptType == 0) {
                    ++entity.stalledStepCount
                    currentMovementType = MovementType.STATIONARY.id.toInt()
                    turnDirectionIndex = 0
                    return
                }
                if (entity.previousMovementStepsLeft <= 0 && animationdefinitions4!!.moveType == 0) {
                    ++entity.stalledStepCount
                    currentMovementType = MovementType.STATIONARY.id.toInt()
                    turnDirectionIndex = 0
                    return
                }
            }

            for (index in entity.spotAnimations.indices) {
                if (entity.spotAnimations[index].spotAnimationId != -1 && entity.spotAnimations[index].sequence.hasSpeed()) {
                    val spotanimtype = TypeLists.SPOT_ANIM!!.list(entity.spotAnimations[index].spotAnimationId)
                    if (spotanimtype.replay && spotanimtype.sequenceId != -1) {
                        val seqtype = TypeLists.SEQ!!.list(spotanimtype.sequenceId)
                        if (entity.previousMovementStepsLeft > 0 && seqtype!!.interruptType == 0) {
                            entity.stalledStepCount++
                            currentMovementType = MovementType.STATIONARY.id.toInt()
                            turnDirectionIndex = 0
                            return
                        }
                        if (entity.previousMovementStepsLeft <= 0 && seqtype!!.moveType == 0) {
                            entity.stalledStepCount++
                            currentMovementType = MovementType.STATIONARY.id.toInt()
                            turnDirectionIndex = 0
                            return
                        }
                    }
                }
            }

            val abs = Vector3Pool.vector3From(entity.getAbsolutePosition().coords)
            val startX = abs.x.toInt()
            val startZ = abs.z.toInt()
            val endX = 512 * entity.movementBaseX[entity.movementStepsLeft - 1] + entity.getSize() * 256
            val endZ = 512 * entity.movementBaseY[entity.movementStepsLeft - 1] + entity.getSize() * 256

            if (startX < endX) {
                if (startZ < endZ) {
                    entity.turn(10240)
                } else if (startZ > endZ) {
                    entity.turn(14336)
                } else {
                    entity.turn(12288)
                }
            } else if (startX > endX) {
                if (startZ < endZ) {
                    entity.turn(6144)
                } else if (startZ > endZ) {
                    entity.turn(2048)
                } else {
                    entity.turn(4096)
                }
            } else if (startZ < endZ) {
                entity.turn(8192)
            } else if (startZ > endZ) {
                entity.turn(0)
            }

            val moveType = entity.movementTypes[entity.movementStepsLeft - 1]

            if (cutscene || endX - startX <= 1024 && endX - startX >= -1024 && endZ - startZ <= 1024 && endZ - startZ >= -1024) {
                var positionDelta = 16
                var delayMovement = true

                if (entity is NpcEntity) {
                    delayMovement = entity.definitions!!.delayMovement
                }

                if (delayMovement) {
                    val yawRotationDelta = entity.lastYawRotation - entity.yaw.rotation
                    if (yawRotationDelta != 0 && entity.faceEntity == -1 && entity.turnDirection != 0) {
                        positionDelta = 8
                    }
                    if (!cutscene && entity.movementStepsLeft > 2) {
                        positionDelta = 24
                    }
                    if (!cutscene && entity.movementStepsLeft > 3) {
                        positionDelta = 32
                    }
                } else {
                    if (!cutscene && entity.movementStepsLeft > 1) {
                        positionDelta = 24
                    }
                    if (!cutscene && entity.movementStepsLeft > 2) {
                        positionDelta = 32
                    }
                }

                if (entity.stalledStepCount > 0 && entity.movementStepsLeft > 1) {
                    positionDelta = 32
                    entity.stalledStepCount--
                }

                if (moveType == MovementType.RUNNING.id) {
                    positionDelta = positionDelta shl 1
                } else if (moveType == MovementType.HALF_WALK.id) {
                    positionDelta = positionDelta shr 1
                }

                // Is always -1
                if (bas.unused != -1) {
                    positionDelta = positionDelta shl 9
                    if (entity.movementStepsLeft == 1) {
                        val i_15 = entity.walkSpeedAccumulator * entity.walkSpeedAccumulator
                        val sizeX = (if (abs.x.toInt() > endX) abs.x.toInt() - endX else endX - abs.x.toInt()) shl 9
                        val sizeZ = (if (abs.z.toInt() > endZ) abs.z.toInt() - endZ else endZ - abs.z.toInt()) shl 9
                        val maxSize = Math.max(sizeX, sizeZ)
                        val i_16 = maxSize * bas.unused * 2
                        if (i_15 > i_16) {
                            entity.walkSpeedAccumulator /= 2
                        } else if (i_15 / 2 > maxSize) {
                            entity.walkSpeedAccumulator -= bas.unused
                            if (entity.walkSpeedAccumulator < 0) {
                                entity.walkSpeedAccumulator = 0
                            }
                        } else if (entity.walkSpeedAccumulator < positionDelta) {
                            entity.walkSpeedAccumulator += bas.unused
                            if (entity.walkSpeedAccumulator > positionDelta) {
                                entity.walkSpeedAccumulator = positionDelta
                            }
                        }
                    } else if (entity.walkSpeedAccumulator < positionDelta) {
                        entity.walkSpeedAccumulator += bas.unused
                        if (entity.walkSpeedAccumulator > positionDelta) {
                            entity.walkSpeedAccumulator = positionDelta
                        }
                    } else {
                        entity.walkSpeedAccumulator -= bas.unused
                        if (entity.walkSpeedAccumulator < 0) {
                            entity.walkSpeedAccumulator = 0
                        }
                    }
                    positionDelta = entity.walkSpeedAccumulator shr 9
                    if (positionDelta < 1) {
                        positionDelta = 1
                    }
                }

                turnDirectionIndex = 0

                if (endX == startX && endZ == startZ) {
                    currentMovementType = MovementType.STATIONARY.id.toInt()
                } else {
                    if (startX < endX) {
                        abs.x += positionDelta
                        turnDirectionIndex = turnDirectionIndex or 0x4
                        if (abs.x > endX) {
                            abs.x = endX.toFloat()
                        }
                    } else if (startX > endX) {
                        abs.x -= positionDelta
                        turnDirectionIndex = turnDirectionIndex or 0x8
                        if (abs.x < endX) {
                            abs.x = endX.toFloat()
                        }
                    }

                    if (startZ < endZ) {
                        abs.z += positionDelta
                        turnDirectionIndex = turnDirectionIndex or 0x1
                        if (abs.z > endZ) {
                            abs.z = endZ.toFloat()
                        }
                    } else if (startZ > endZ) {
                        abs.z -= positionDelta
                        turnDirectionIndex = turnDirectionIndex or 0x2
                        if (abs.z < endZ) {
                            abs.z = endZ.toFloat()
                        }
                    }

                    entity.setBasePosition(abs)

                    if (positionDelta >= 32) {
                        currentMovementType = MovementType.RUNNING.id.toInt()
                    } else {
                        currentMovementType = moveType.toInt()
                    }
                }

                if (endX == abs.x.toInt() && endZ == abs.z.toInt()) {
                    entity.movementStepsLeft--
                    if (entity.previousMovementStepsLeft > 0) {
                        entity.previousMovementStepsLeft--
                    }
                }
            } else {
                entity.setBasePosition(endX.toFloat(), abs.y, endZ.toFloat())
                entity.turn(entity.lastYawRotation, false)
                entity.movementStepsLeft--
                if (entity.previousMovementStepsLeft > 0) {
                    entity.previousMovementStepsLeft--
                }
                currentMovementType = MovementType.STATIONARY.id.toInt()
                turnDirectionIndex = 0
            }
            abs.pushVectorStack()
        }
    }

    @JvmStatic
    fun rotateEquipment(current: PathingEntity) {
        if (current.equipmentUpdateDetails != null || current.equipmentRotations != null) {
            var reset = true
            val base = Resource.mapRegion!!.getBaseCoordinates()

            for (equipIndex in current.equipmentUpdateDetails!!.indices) {
                var bitpacked = -1
                bitpacked = current.equipmentUpdateDetails!![equipIndex]

                if (bitpacked == -1) {
                    if (!current.rotateEquipment(equipIndex, -1)) {
                        reset = false
                    }
                } else {
                    reset = false
                    val currentAbsolutePosition = current.getAbsolutePosition().coords
                    val deltaX: Int
                    val deltaZ: Int

                    if ((bitpacked and -1073741824) == -1073741824) {
                        val position = bitpacked and 0xfffffff
                        val absX = position shr 14
                        val absY = position and 0x3fff
                        deltaX = currentAbsolutePosition.x.toInt() - ((absX - base.x) * 512 + 256)
                        deltaZ = currentAbsolutePosition.z.toInt() - ((absY - base.y) * 512 + 256)
                    } else {
                        val lastAbsolutePosition: Vector3
                        if ((bitpacked and 0x8000) != 0) {
                            val playerIndex = bitpacked and 0x7fff
                            val player = PlayerUpdate.players[playerIndex]
                            if (player == null) {
                                current.rotateEquipment(equipIndex, -1)
                                continue
                            }
                            lastAbsolutePosition = player.getAbsolutePosition().coords
                        } else {
                            val npcReference = Client.NPC_MAP.get(bitpacked.toLong())
                            if (npcReference == null) {
                                current.rotateEquipment(equipIndex, -1)
                                continue
                            }
                            val npcEntity = npcReference.value as NpcEntity
                            lastAbsolutePosition = npcEntity.getAbsolutePosition().coords
                        }
                        deltaX = currentAbsolutePosition.x.toInt() - lastAbsolutePosition.x.toInt()
                        deltaZ = currentAbsolutePosition.z.toInt() - lastAbsolutePosition.z.toInt()
                    }

                    if (deltaX != 0 || deltaZ != 0) {
                        current.rotateEquipment(equipIndex, (Math.atan2(deltaX.toDouble(), deltaZ.toDouble()) * 2607.5945876176133).toInt() and 0x3fff)
                    }
                }
            }

            if (reset) {
                current.equipmentUpdateDetails = null
                current.equipmentRotations = null
            }
        }
    }
}
