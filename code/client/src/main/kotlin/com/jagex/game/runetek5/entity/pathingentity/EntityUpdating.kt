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

        pathingEntity.captureInterpolationSnapshot()
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
                                val elapsedCycles = Client.cycles - displayDetails.discardAtCycle
                                val targetFillWidth = greenBar.getWidth() * displayDetails.toPercentage / 255
                                val currentFillWidth: Int

                                if (displayDetails.lifetimeCycles > elapsedCycles) {
                                    val steppedCycles = if (hitbarType.animationStepSize == 0) 0 else hitbarType.animationStepSize * (elapsedCycles / hitbarType.animationStepSize)
                                    val drawX = greenBar.getWidth() * displayDetails.fromPercentage / 255
                                    currentFillWidth = steppedCycles * (targetFillWidth - drawX) / displayDetails.lifetimeCycles + drawX
                                } else {
                                    currentFillWidth = targetFillWidth
                                    val remainingFadeCycles = displayDetails.lifetimeCycles + hitbarType.fadeOutDuration - elapsedCycles
                                    if (hitbarType.fadeStartOffset >= 0) {
                                        alpha = (remainingFadeCycles shl 8) / (hitbarType.fadeOutDuration - hitbarType.fadeStartOffset)
                                    }
                                }

                                var clampedFillWidth = currentFillWidth
                                if (displayDetails.toPercentage > 0 && clampedFillWidth < 2) {
                                    clampedFillWidth = 2
                                }

                                val barHeight = redBar.getHeight()
                                val drawX = (Client.HINT_POSITION[0] + clipLeft - (redBar.getWidth() shr 1)).toInt()
                                drawY -= barHeight

                                if (alpha >= 0 && alpha < 255) {
                                    val packedAlpha = alpha shl 24
                                    val colorArgb = packedAlpha or 0xffffff
                                    redBar.draw(drawX, drawY, 0, colorArgb, 1)
                                    Renderers.activeToolkit!!.expandClipping(drawX, drawY, drawX + clampedFillWidth, drawY + barHeight)
                                    greenBar.draw(drawX, drawY, 0, colorArgb, 1)
                                } else {
                                    redBar.draw(drawX, drawY)
                                    Renderers.activeToolkit!!.expandClipping(drawX, drawY, clampedFillWidth + drawX, drawY + barHeight)
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
                        val npcHeadIcon = StaticMedia.headiconPrayerSprites!![npctype.headIcons]
                        drawY -= npcHeadIcon.getHeight()
                        npcHeadIcon.draw((clipLeft + Client.HINT_POSITION[0] - (npcHeadIcon.getWidth() shr 1)).toInt(), drawY)
                        drawY -= 2
                    }
                }

                if (entity is PlayerEntity) {
                    if (id >= 0) {
                        var maxArrowHeight = 0
                        for (index in PlayerUpdate.HINT_ARROWS.indices) {
                            val arrow = PlayerUpdate.HINT_ARROWS[index]
                            if (arrow != null && arrow.targetType == 10 && arrow.targetIndex == playerIndices[id]) {
                                val playerArrow = StaticMedia.hintHeadiconSprites!![arrow.arrowSprite]
                                if (playerArrow.getHeight() > maxArrowHeight) {
                                    maxArrowHeight = playerArrow.getHeight()
                                }
                                playerArrow.draw((clipLeft + Client.HINT_POSITION[0] - 12.0F).toInt(), drawY - playerArrow.getHeight())
                            }
                        }
                        if (maxArrowHeight > 0) {
                            @Suppress("UNUSED_VALUE")
                            val unused = drawY - (maxArrowHeight + 2)
                        }
                    }
                } else {
                    var maxArrowHeight = 0
                    for (index in PlayerUpdate.HINT_ARROWS.indices) {
                        val arrow = PlayerUpdate.HINT_ARROWS[index]
                        if (arrow != null && arrow.targetType == 1 && Client.NPC_UPDATE_INDICES[id - playerIndiceCount] == arrow.targetIndex) {
                            val npcArrow = StaticMedia.hintHeadiconSprites!![arrow.arrowSprite]
                            if (npcArrow.getHeight() > maxArrowHeight) {
                                maxArrowHeight = npcArrow.getHeight()
                            }
                            val shouldDraw: Boolean = if (arrow.denominator == 0) {
                                true
                            } else {
                                val flashHalfPeriod = GameShell.gameCyclesPerSecond() * 1000 / arrow.denominator / 2
                                Client.cycles % (flashHalfPeriod * 2) < flashHalfPeriod
                            }
                            if (shouldDraw) {
                                npcArrow.draw((clipLeft + Client.HINT_POSITION[0] - 12.0F).toInt(), drawY - npcArrow.getHeight())
                            }
                        }
                    }
                    if (maxArrowHeight > 0) {
                        @Suppress("UNUSED_VALUE")
                        val unused = drawY - (maxArrowHeight + 2)
                    }
                }

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
                            val halfIconHeight = pathingEntity.getIconHeight() / 2
                            HintRenderer.calculateHintForSceneobject(pathingEntity as SceneGraphEntity, halfIconHeight)

                            if (Client.HINT_POSITION[0] > -1.0f) {
                                Client.HINT_POSITION[0] += Client.ENTITY_DEFAULTS!!.hitOffsetsX!![hitsplatIndex]
                                Client.HINT_POSITION[1] += Client.ENTITY_DEFAULTS!!.hitOffsetsY!![hitsplatIndex]

                                var leftCapWidth = 0
                                var middleFillWidth = 0
                                var innerLeftWidth = 0
                                var rightCapWidth = 0
                                var leftCapOffsetX = 0
                                var middleFillOffsetX = 0
                                var innerLeftOffsetX = 0
                                var rightCapOffsetX = 0
                                var soakLeftCapSprite: NativeSprite? = null
                                var soakMiddleFillSprite: NativeSprite? = null
                                var soakInnerLeftSprite: NativeSprite? = null
                                var soakRightCapSprite: NativeSprite? = null
                                var soakLeftCapWidth = 0
                                var soakMiddleFillWidth = 0
                                var soakInnerLeftWidth = 0
                                var soakRightCapWidth = 0
                                var soakLeftCapOffsetX = 0
                                var soakMiddleFillOffsetX = 0
                                var soakInnerLeftOffsetX = 0
                                var soakRightCapOffsetX = 0
                                var maxSpriteHeight = 0

                                val leftCapSprite = hitsplatType.getLeftCapSprite(Renderers.activeToolkit!!)
                                if (leftCapSprite != null) {
                                    leftCapWidth = leftCapSprite.getWidth()
                                    val leftCapHeight = leftCapSprite.getHeight()
                                    if (leftCapHeight > maxSpriteHeight) {
                                        maxSpriteHeight = leftCapHeight
                                    }
                                    leftCapSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                    leftCapOffsetX = Client.HITSPLAT_SIZE_CACHE[0]
                                }

                                val middleFillSprite = hitsplatType.getMiddleFillSprite(Renderers.activeToolkit!!)
                                if (middleFillSprite != null) {
                                    middleFillWidth = middleFillSprite.getWidth()
                                    val middleFillHeight = middleFillSprite.getHeight()
                                    if (middleFillHeight > maxSpriteHeight) {
                                        maxSpriteHeight = middleFillHeight
                                    }
                                    middleFillSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                    middleFillOffsetX = Client.HITSPLAT_SIZE_CACHE[0]
                                }

                                val innerLeftSprite = hitsplatType.getInnerLeftSprite(Renderers.activeToolkit!!)
                                if (innerLeftSprite != null) {
                                    innerLeftWidth = innerLeftSprite.getWidth()
                                    val innerLeftHeight = innerLeftSprite.getHeight()
                                    if (innerLeftHeight > maxSpriteHeight) {
                                        maxSpriteHeight = innerLeftHeight
                                    }
                                    innerLeftSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                    innerLeftOffsetX = Client.HITSPLAT_SIZE_CACHE[0]
                                }

                                val rightCapSprite = hitsplatType.getRightCapSprite(Renderers.activeToolkit!!)
                                var spriteHeight: Int
                                if (rightCapSprite != null) {
                                    rightCapWidth = rightCapSprite.getWidth()
                                    spriteHeight = rightCapSprite.getHeight()
                                    if (spriteHeight > maxSpriteHeight) {
                                        maxSpriteHeight = spriteHeight
                                    }
                                    rightCapSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                    rightCapOffsetX = Client.HITSPLAT_SIZE_CACHE[0]
                                }

                                if (soakHitsplat != null) {
                                    soakLeftCapSprite = soakHitsplat.getLeftCapSprite(Renderers.activeToolkit!!)
                                    if (soakLeftCapSprite != null) {
                                        soakLeftCapWidth = soakLeftCapSprite.getWidth()
                                        spriteHeight = soakLeftCapSprite.getHeight()
                                        if (spriteHeight > maxSpriteHeight) {
                                            maxSpriteHeight = spriteHeight
                                        }
                                        soakLeftCapSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                        soakLeftCapOffsetX = Client.HITSPLAT_SIZE_CACHE[0]
                                    }
                                    soakMiddleFillSprite = soakHitsplat.getMiddleFillSprite(Renderers.activeToolkit!!)
                                    if (soakMiddleFillSprite != null) {
                                        soakMiddleFillWidth = soakMiddleFillSprite.getWidth()
                                        spriteHeight = soakMiddleFillSprite.getHeight()
                                        if (spriteHeight > maxSpriteHeight) {
                                            maxSpriteHeight = spriteHeight
                                        }
                                        soakMiddleFillSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                        soakMiddleFillOffsetX = Client.HITSPLAT_SIZE_CACHE[0]
                                    }
                                    soakInnerLeftSprite = soakHitsplat.getInnerLeftSprite(Renderers.activeToolkit!!)
                                    if (soakInnerLeftSprite != null) {
                                        soakInnerLeftWidth = soakInnerLeftSprite.getWidth()
                                        spriteHeight = soakInnerLeftSprite.getHeight()
                                        if (spriteHeight > maxSpriteHeight) {
                                            maxSpriteHeight = spriteHeight
                                        }
                                        soakInnerLeftSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                        soakInnerLeftOffsetX = Client.HITSPLAT_SIZE_CACHE[0]
                                    }
                                    soakRightCapSprite = soakHitsplat.getRightCapSprite(Renderers.activeToolkit!!)
                                    if (soakRightCapSprite != null) {
                                        soakRightCapWidth = soakRightCapSprite.getWidth()
                                        spriteHeight = soakRightCapSprite.getHeight()
                                        if (spriteHeight > maxSpriteHeight) {
                                            maxSpriteHeight = spriteHeight
                                        }
                                        soakRightCapSprite.cacheSizeInto(Client.HITSPLAT_SIZE_CACHE)
                                        soakRightCapOffsetX = Client.HITSPLAT_SIZE_CACHE[0]
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

                                var regularTileCount = 0
                                var soakTileCount = 0
                                if (middleFillWidth > 0) {
                                    regularTileCount = regularWidth / middleFillWidth + 1
                                }
                                if (soakHitsplat != null && soakMiddleFillWidth > 0) {
                                    soakTileCount = soakWidth / soakMiddleFillWidth + 1
                                }

                                var totalWidth = 0
                                val leftCapStartX = totalWidth
                                if (leftCapWidth > 0) {
                                    totalWidth += leftCapWidth
                                }
                                totalWidth += 2
                                val innerLeftStartX = totalWidth
                                if (innerLeftWidth > 0) {
                                    totalWidth += innerLeftWidth
                                }
                                val middleFillStartX = totalWidth
                                var textStartX = totalWidth
                                var rightCapStartX: Int
                                if (middleFillWidth > 0) {
                                    rightCapStartX = middleFillWidth * regularTileCount
                                    totalWidth += rightCapStartX
                                    textStartX += (rightCapStartX - regularWidth) / 2
                                } else {
                                    totalWidth += regularWidth
                                }
                                rightCapStartX = totalWidth
                                if (rightCapWidth > 0) {
                                    totalWidth += rightCapWidth
                                }

                                var soakLeftCapStartX = 0
                                var soakInnerLeftStartX = 0
                                var soakMiddleFillStartX = 0
                                var soakRightCapStartX = 0
                                var soakTextStartX = 0
                                var remainingCycles: Int

                                if (soakHitsplat != null) {
                                    totalWidth += 2
                                    soakLeftCapStartX = totalWidth
                                    if (soakLeftCapWidth > 0) {
                                        totalWidth += soakLeftCapWidth
                                    }
                                    totalWidth += 2
                                    soakInnerLeftStartX = totalWidth
                                    if (soakInnerLeftWidth > 0) {
                                        totalWidth += soakInnerLeftWidth
                                    }
                                    soakMiddleFillStartX = totalWidth
                                    soakTextStartX = totalWidth
                                    if (soakMiddleFillWidth > 0) {
                                        remainingCycles = soakTileCount * soakMiddleFillWidth
                                        totalWidth += remainingCycles
                                        soakTextStartX += (remainingCycles - soakWidth) / 2
                                    } else {
                                        totalWidth += soakWidth
                                    }
                                    soakRightCapStartX = totalWidth
                                    if (soakRightCapWidth > 0) {
                                        totalWidth += soakRightCapWidth
                                    }
                                }

                                remainingCycles = pathingEntity.hitsplatCycles[hitsplatIndex] - Client.cycles
                                val scrollX = hitsplatType.scrollOffsetX - remainingCycles * hitsplatType.scrollOffsetX / hitsplatType.cyclesVisible
                                val scrollY = remainingCycles * hitsplatType.scrollOffsetY / hitsplatType.cyclesVisible + -hitsplatType.scrollOffsetY
                                val drawX = (clipLeft + Client.HINT_POSITION[0] - (totalWidth shr 1) + scrollX).toInt()
                                val drawY = (scrollY + (Client.HINT_POSITION[1] + clipTop - 12.0F)).toInt()
                                var clipMinY = drawY
                                var clipMaxY = drawY + maxSpriteHeight
                                val textDrawY = drawY + hitsplatType.textOffsetY + 15
                                val textTopY = textDrawY - hitsplatFontMetrics!!.topPadding
                                val textBottomY = textDrawY + hitsplatFontMetrics!!.bottomPadding

                                if (textTopY < drawY) {
                                    clipMinY = textTopY
                                }
                                if (textBottomY > clipMaxY) {
                                    clipMaxY = textBottomY
                                }

                                var soakTextDrawY = 0
                                var fadeAlpha: Int

                                if (soakHitsplat != null) {
                                    soakTextDrawY = drawY + soakHitsplat.textOffsetY + 15
                                    val soakTextTopY = soakTextDrawY - soakFontMetrics!!.topPadding
                                    val soakTextBottomY = soakTextDrawY + soakFontMetrics!!.bottomPadding
                                    if (soakTextTopY < clipMinY) {
                                    }
                                    if (soakTextBottomY > clipMaxY) {
                                    }
                                }

                                fadeAlpha = 255
                                if (hitsplatType.fadeStartCycle >= 0) {
                                    fadeAlpha = (remainingCycles shl 8) / (hitsplatType.cyclesVisible - hitsplatType.fadeStartCycle)
                                }

                                if (fadeAlpha >= 0 && fadeAlpha < 255) {
                                    val alpha = fadeAlpha shl 24
                                    val alphaMask = alpha or 0xffffff
                                    if (leftCapSprite != null) {
                                        leftCapSprite.draw(leftCapStartX + drawX - leftCapOffsetX, drawY, 0, alphaMask, 1)
                                    }
                                    if (innerLeftSprite != null) {
                                        innerLeftSprite.draw(innerLeftStartX + drawX - innerLeftOffsetX, drawY, 0, alphaMask, 1)
                                    }
                                    var tileIndex: Int
                                    if (middleFillSprite != null) {
                                        for (tileIndex in 0 until regularTileCount) {
                                            middleFillSprite.draw(tileIndex * middleFillWidth + (middleFillStartX + drawX - middleFillOffsetX), drawY, 0, alphaMask, 1)
                                        }
                                    }
                                    if (rightCapSprite != null) {
                                        rightCapSprite.draw(rightCapStartX + drawX - rightCapOffsetX, drawY, 0, alphaMask, 1)
                                    }
                                    hitsplatFontRenderer!!.renderPlain(hitsplatText, drawX + textStartX, textDrawY, hitsplatType.color or alpha, 0)
                                    if (soakHitsplat != null) {
                                        if (soakLeftCapSprite != null) {
                                            soakLeftCapSprite.draw(drawX + soakLeftCapStartX - soakLeftCapOffsetX, drawY, 0, alphaMask, 1)
                                        }
                                        if (soakInnerLeftSprite != null) {
                                            soakInnerLeftSprite.draw(drawX + soakInnerLeftStartX - soakInnerLeftOffsetX, drawY, 0, alphaMask, 1)
                                        }
                                        if (soakMiddleFillSprite != null) {
                                            for (tileIndex in 0 until soakTileCount) {
                                                soakMiddleFillSprite.draw(tileIndex * soakMiddleFillWidth + (soakMiddleFillStartX + drawX - soakMiddleFillOffsetX), drawY, 0, alphaMask, 1)
                                            }
                                        }
                                        if (soakRightCapSprite != null) {
                                            soakRightCapSprite.draw(drawX + soakRightCapStartX - soakRightCapOffsetX, drawY, 0, alphaMask, 1)
                                        }
                                        soakFont!!.renderPlain(soakText!!, soakTextStartX + drawX, soakTextDrawY, soakHitsplat.color or alpha, 0)
                                    }
                                } else {
                                    if (leftCapSprite != null) {
                                        leftCapSprite.draw(drawX + leftCapStartX - leftCapOffsetX, drawY)
                                    }
                                    if (innerLeftSprite != null) {
                                        innerLeftSprite.draw(innerLeftStartX + drawX - innerLeftOffsetX, drawY)
                                    }
                                    if (middleFillSprite != null) {
                                        for (i in 0 until regularTileCount) {
                                            middleFillSprite.draw(i * middleFillWidth + (middleFillStartX + drawX - middleFillOffsetX), drawY)
                                        }
                                    }
                                    if (rightCapSprite != null) {
                                        rightCapSprite.draw(rightCapStartX + drawX - rightCapOffsetX, drawY)
                                    }
                                    hitsplatFontRenderer!!.renderPlain(hitsplatText, drawX + textStartX, textDrawY, hitsplatType.color or 0xFF000000.toInt(), 0)
                                    if (soakHitsplat != null) {
                                        if (soakLeftCapSprite != null) {
                                            soakLeftCapSprite.draw(drawX + soakLeftCapStartX - soakLeftCapOffsetX, drawY)
                                        }
                                        if (soakInnerLeftSprite != null) {
                                            soakInnerLeftSprite.draw(drawX + soakInnerLeftStartX - soakInnerLeftOffsetX, drawY)
                                        }
                                        if (soakMiddleFillSprite != null) {
                                            for (i in 0 until soakTileCount) {
                                                soakMiddleFillSprite.draw(soakMiddleFillWidth * i + (drawX + soakMiddleFillStartX - soakMiddleFillOffsetX), drawY)
                                            }
                                        }
                                        if (soakRightCapSprite != null) {
                                            soakRightCapSprite.draw(soakRightCapStartX + drawX - soakRightCapOffsetX, drawY)
                                        }
                                        soakFont!!.renderPlain(soakText!!, drawX + soakTextStartX, soakTextDrawY, soakHitsplat.color or 0xFF000000.toInt(), 0)
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
            val targetEntityIndex = Client.NPC_INDEX_REFERENCES[index]
            val targetEntity: Any? = if (targetEntityIndex < 2048) {
                PlayerUpdate.players[targetEntityIndex]
            } else {
                (Client.NPC_MAP.get((targetEntityIndex - 2048).toLong()) as ObjectNode).value
            }
            drawOverheadSprite(entity as PathingEntity, targetEntity as PathingEntity, --((entity as PathingEntity).overheadSpriteCount), clipLeft, clipTop)
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
                val colorType = Client.HINT_DETAILS[hintIndex]!!.getColorType()

                if (colorType < 6) {
                    color = Client.OVERHEAD_TEXT_COLORS[colorType]
                }
                if (colorType == 6) {
                    color = if (Client.cycles % 20 < 10) 16711680 else 16776960
                }
                if (colorType == 7) {
                    color = if (Client.cycles % 20 < 10) 255 else 65535
                }
                if (colorType == 8) {
                    color = if (Client.cycles % 20 < 10) 45056 else 8454016
                }
                if (colorType == 9) {
                    val colorProgress = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    if (colorProgress < 50) {
                        color = colorProgress * 1280 + 16711680
                    } else if (colorProgress < 100) {
                        color = 16776960 - (colorProgress - 50) * 327680
                    } else if (colorProgress < 150) {
                        color = (colorProgress - 100) * 5 + 65280
                    }
                }
                if (colorType == 10) {
                    val colorProgress = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    if (colorProgress < 50) {
                        color = colorProgress * 5 + 16711680
                    } else if (colorProgress < 100) {
                        color = 16711935 - (colorProgress - 50) * 327680
                    } else if (colorProgress < 150) {
                        color = (colorProgress - 100) * 327680 + 255 - (colorProgress - 100) * 5
                    }
                }
                if (colorType == 11) {
                    val colorProgress = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    if (colorProgress < 50) {
                        color = 16777215 - colorProgress * 327685
                    } else if (colorProgress < 100) {
                        color = (colorProgress - 50) * 327685 + 65280
                    } else if (colorProgress < 150) {
                        color = 16777215 - (colorProgress - 100) * 327680
                    }
                }

                val drawColor = color or 0xFF000000.toInt()
                val drawType = Client.HINT_DETAILS[hintIndex]!!.getDrawType()

                if (drawType == 0) {
                    MiniMenu.minimenuHeader!!.renderCentered(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, 0xFF000000.toInt())
                }
                if (drawType == 1) {
                    MiniMenu.minimenuHeader!!.renderCenteredWave(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, Client.cycles)
                }
                if (drawType == 2) {
                    MiniMenu.minimenuHeader!!.renderCenteredWave2(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, Client.cycles)
                }
                if (drawType == 3) {
                    val effectProgress = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    MiniMenu.minimenuHeader!!.renderCenteredShake(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, Client.cycles, effectProgress)
                }
                if (drawType == 4) {
                    val effectProgress = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    val scrollOffset = effectProgress * (FontMetrics.defaultFontMetrics!!.getWidthNoSprites(drawnText!!) + 100) / 150
                    Renderers.activeToolkit!!.expandClipping(clipLeft + hintX - 50, clipTop, clipLeft + hintX + 50, height + clipTop)
                    MiniMenu.minimenuHeader!!.renderPlain(drawnText, clipLeft + hintX + 50 - scrollOffset, hintY + clipTop, drawColor, 0xFF000000.toInt())
                    Renderers.activeToolkit!!.setClipping(clipLeft, clipTop, clipLeft + width, height + clipTop)
                }
                if (drawType == 5) {
                    val effectProgress = 150 - Client.HINT_DETAILS[hintIndex]!!.getTicksLeft() * 150 / Client.HINT_DETAILS[hintIndex]!!.getTotalTicksVisible()
                    var scrollOffset = 0
                    if (effectProgress < 25) {
                        scrollOffset = effectProgress - 25
                    } else if (effectProgress > 125) {
                        scrollOffset = effectProgress - 125
                    }
                    val paddingY = FontMetrics.defaultFontMetrics!!.topPadding + FontMetrics.defaultFontMetrics!!.bottomPadding
                    Renderers.activeToolkit!!.expandClipping(clipLeft, hintY + clipTop - paddingY - 1, clipLeft + width, hintY + clipTop + 5)
                    MiniMenu.minimenuHeader!!.renderCentered(drawnText, clipLeft + hintX, scrollOffset + hintY + clipTop, drawColor, 0xFF000000.toInt())
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
                val deltaX = abs.x.toInt() - (current.faceX * 256 - base.x * 512)
                val deltaZ = abs.z.toInt() - (current.faceY * 256 - base.y * 512)
                if (deltaX != 0 || deltaZ != 0) {
                    current.turn((Math.atan2(deltaX.toDouble(), deltaZ.toDouble()) * 2607.5945876176133).toInt() and 0x3fff)
                }
                current.faceX = -1
                current.faceY = -1
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
                val currentSeqType = entity.currentSequence.getType()
                if (entity.previousMovementStepsLeft > 0 && currentSeqType!!.interruptType == 0) {
                    ++entity.stalledStepCount
                    currentMovementType = MovementType.STATIONARY.id.toInt()
                    turnDirectionIndex = 0
                    return
                }
                if (entity.previousMovementStepsLeft <= 0 && currentSeqType!!.moveType == 0) {
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

                if (bas.unused != -1) {
                    positionDelta = positionDelta shl 9
                    if (entity.movementStepsLeft == 1) {
                        val speedSquared = entity.walkSpeedAccumulator * entity.walkSpeedAccumulator
                        val sizeX = (if (abs.x.toInt() > endX) abs.x.toInt() - endX else endX - abs.x.toInt()) shl 9
                        val sizeZ = (if (abs.z.toInt() > endZ) abs.z.toInt() - endZ else endZ - abs.z.toInt()) shl 9
                        val maxSize = Math.max(sizeX, sizeZ)
                        val decelerationThreshold = maxSize * bas.unused * 2
                        if (speedSquared > decelerationThreshold) {
                            entity.walkSpeedAccumulator /= 2
                        } else if (speedSquared / 2 > maxSize) {
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
