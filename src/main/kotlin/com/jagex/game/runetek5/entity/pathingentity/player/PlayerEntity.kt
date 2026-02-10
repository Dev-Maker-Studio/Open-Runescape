package com.jagex.game.runetek5.entity.pathingentity.player

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.item.ItemTypeCustomisation
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.graphics.hint.HintArrow
import com.jagex.game.runetek5.graphics.hint.HintDetails
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_X
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_Y
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_Z
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.TINT
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.interaction.FriendList
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.scene.tile.Tile
import com.jagex.game.runetek5.sound.player.area.AmbientSound
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.vardomain.VarProviders
import java.util.*

open class PlayerEntity(scene: Scene?) : PathingEntity(scene) {

    @JvmField
    var basePositionX: Int = 0

    @JvmField
    var basePositionY: Int = 0

    @JvmField
    var male: Byte = 0

    @JvmField
    var skullId: Int = -1

    @JvmField
    var prayerHeadIconId: Int = -1

    @JvmField
    var combatLevel: Int = 0

    @JvmField
    var cbLevelWithSumm: Int = 0

    @JvmField
    var cbLevelRelated: Int = -1

    @JvmField
    var elo: Int = 0

    @JvmField
    var faceDirection: Int = -1

    @JvmField
    var shouldSuppressMovingAnimation: Boolean = false

    @JvmField
    var teamId: Int = 0

    @JvmField
    var walkingSoundEffectId: Int = -1

    @JvmField
    var teleportingSoundEffectId: Int = -1

    @JvmField
    var defaultSoundEffectId: Int = -1

    @JvmField
    var runningSoundEffectId: Int = -1

    @JvmField
    var instrumentSoundEffect: Boolean = false

    @JvmField
    var isNpc: Int = 0

    @JvmField
    var ambientSoundVolume: Int = 255

    @JvmField
    var hasPendingPositionUpdate: Boolean = false

    /**
     * Instead of a dot a 'P' is drawn
     * on the minimap
     * (presumably for player moderators
     * although I'm not 100% sure)
     */
    @JvmField
    var pMapdot: Boolean = false

    @JvmField
    var hidden: Boolean = false

    @JvmField
    var reduceAttackPriority: Boolean = false

    @JvmField
    val playerConfigs: ConfigNodeMap = ConfigNodeMap()

    @JvmField
    var username: String? = null

    @JvmField
    var displayName: String? = null

    @JvmField
    var playerAppearance: PlayerAppearance? = null

    @JvmField
    var prefixTitle: String? = null

    @JvmField
    var postfixTitle: String? = null

    @JvmField
    var renderEmote: Int = 0

    override fun getAxisAlignedBoundingBox(graphicalrenderer1: AbstractToolkit): Silhouette? {
        return null
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
        if (playerAppearance != null && (particlesInitialized || method16133(toolkit, 0))) {
            val renderingMatrix = toolkit.getRenderingMatrix()
            renderingMatrix.translateFromCoordspace(getAbsolutePosition())
            renderingMatrix.translate(0.0f, -5.0f, 0.0f)
            updateParticles(toolkit, models, renderingMatrix, particlesInitialized)
            Arrays.fill(models, null)
        }
    }

    fun hasAppearance(): Boolean {
        return playerAppearance != null
    }

    fun getUsernameWithTitle(): String {
        var fullUsername = ""
        if (prefixTitle != null) {
            fullUsername = prefixTitle!!
        }

        fullUsername += username
        if (postfixTitle != null) {
            fullUsername += postfixTitle
        }

        return fullUsername
    }

    fun getDisplayName(): String? {
        return displayName
    }

    fun move(baseX: Int, baseY: Int, movementType: Byte) {
        if (currentSequence.hasType() && currentSequence.type!!.moveType == 1) {
            currentAnimations = null
            currentSequence.update(-1)
        }

        for (index in spotAnimations.indices) {
            if (spotAnimations[index].spotAnimationId != -1) {
                val gfx = TypeLists.SPOT_ANIM!!.list(spotAnimations[index].spotAnimationId)
                if (gfx.replay && gfx.sequenceId != -1 && TypeLists.SEQ!!.list(gfx.sequenceId).moveType == 1) {
                    spotAnimations[index].sequence.update(-1)
                    spotAnimations[index].spotAnimationId = -1
                }
            }
        }

        faceDirection = -1
        if (baseX >= 0 && baseX < Resource.mapRegion!!.sceneSizeX && baseY >= 0 && baseY < Resource.mapRegion!!.sceneSizeY) {
            if (movementBaseX[0] >= 0 && movementBaseX[0] < Resource.mapRegion!!.sceneSizeX && movementBaseY[0] >= 0 && movementBaseY[0] < Resource.mapRegion!!.sceneSizeY) {
                if (movementType == MovementType.RUNNING.id) {
                    PlayerUpdate.findRoute(this, baseX, baseY, MovementType.RUNNING.id)
                }

                addMovementStep(baseX, baseY, movementType)
            } else {
                move(baseX, baseY)
            }
        } else {
            move(baseX, baseY)
        }
    }

    fun move(baseX: Int, baseY: Int) {
        movementStepsLeft = 0
        previousMovementStepsLeft = 0
        stalledStepCount = 0
        movementBaseX[0] = baseX
        movementBaseY[0] = baseY
        val size = getSize()
        val abs = Vector3Pool.vector3From(getAbsolutePosition().coords)
        abs.x = (size * 256 + movementBaseX[0] * 512).toFloat()
        abs.z = (size * 256 + movementBaseY[0] * 512).toFloat()
        setBasePosition(abs)
        abs.pushVectorStack()
        if (this == PlayerUpdate.MY_PLAYER!!) {
            Resource.mapRegion!!.environmentManager!!.requestDirectEnvironmentTransition()
        }

        if (particleSystem != null) {
            particleSystem!!.onEntityUpdate()
        }
    }

    override fun privateMessagesEnabled(): Boolean {
        return Client.ENTITY_DEFAULTS!!.enablePlayerMessages
    }

    override fun getSize(): Int {
        return if (playerAppearance != null && playerAppearance!!.morphism != -1)
            TypeLists.NPC!!.list(playerAppearance!!.morphism).size
        else
            super.getSize()
    }

    override fun getCurrentRenderAnimation(): Int {
        return renderEmote
    }

    override fun getOverheadSprite(): Int {
        return -1
    }

    override fun calculateProjection(toolkit: AbstractToolkit, i_2: Int, i_3: Int): Boolean {
        if (playerAppearance != null && method16133(toolkit, 131072)) {
            val projectionMatrix = getProjectionMatrix()
            var bool6 = false

            for (index in models.indices) {
                if (models[index] != null && models[index]!!.calculateProjection(i_2, i_3, projectionMatrix, true, 0)) {
                    bool6 = true
                    break
                }
            }

            for (index in models.indices) {
                models[index] = null
            }

            return bool6
        } else {
            return false
        }
    }

    override fun castShadow(class521Sub12: SceneGraphEntity, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean) {
        throw IllegalStateException()
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    fun addMovementStep(baseX: Int, baseY: Int, movementType: Byte) {
        if (movementStepsLeft < movementBaseX.size - 1) {
            movementStepsLeft++
        }

        for (index in movementStepsLeft downTo 1) {
            movementBaseX[index] = movementBaseX[index - 1]
            movementBaseY[index] = movementBaseY[index - 1]
            movementTypes[index] = movementTypes[index - 1]
        }

        movementBaseX[0] = baseX
        movementBaseY[0] = baseY
        movementTypes[0] = movementType
    }

    fun method16133(toolkit: AbstractToolkit, initialFunctionMask: Int): Boolean {
        var functionMask = initialFunctionMask
        val bastype = getBastype()
        val currSeq = if (currentSequence.hasType() && !currentSequence.hasSpeed()) currentSequence else null
        val moveAnimation = if (!movingSequence.hasType() || shouldSuppressMovingAnimation || movingSequence.hasType && currSeq != null) null else movingSequence
        val x = getBastype().modelWidth
        val z = getBastype().modelLength
        if (x != 0 || z != 0 || getBastype().rollTargetAngle != 0 || getBastype().pitchTargetAngle != 0) {
            functionMask = functionMask or DISPLACE_X or DISPLACE_Y or DISPLACE_Z
        }

        val rotation = yaw.getRotation()
        val tintModel = opacity.toInt() != 0 && Client.cycles >= tintStartCycle && Client.cycles < tintEndCycle
        if (tintModel) {
            functionMask = functionMask or TINT
        }

        @Suppress("UNCHECKED_CAST")
        val model = playerAppearance!!.getBodyModel(
            toolkit, functionMask, TypeLists.BAS!!, TypeLists.IDK!!, TypeLists.NPC!!, TypeLists.OBJ!!,
            VarProviders.PLAYER_VAR_PROVIDER, currSeq, moveAnimation, sequences as Array<Sequence?>?,
            equipmentRotations, rotation, TypeLists.EQUIPMENT_DEFAULTS!!
        ).also { models[0] = it }

        val bodyModelCount = PlayerAppearance.countBodyModels()
        if (GameShell.maxMemory < 96 && bodyModelCount > 50) {
            uploadScene()
        }

        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE && bodyModelCount < 50) {
            var unusedSlots: Int
            unusedSlots = 50 - bodyModelCount
            while (unusedSlots > Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES) {
                Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA!![Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES] = ByteArray(102400)
                Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES++
            }

            while (unusedSlots < Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES) {
                Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES--
                Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA!![Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES] = null
            }
        } else if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
            Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA = arrayOfNulls(50)
            Client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES = 0
        }

        if (model == null) {
            return false
        }
        modelMinY = model.getMinY()
        model.getSize3d()
        rotate(model)
        if (x == 0 && z == 0) {
            hillChange(rotation, getSize() shl 9, getSize() shl 9, 0, 0)
        } else {
            hillChange(rotation, x, z, getBastype().hillRotateX, getBastype().hillRotateZ)
            if (rotateX != 0) {
                model.xaxisRotateWithoutNormals(rotateX)
            }

            if (rotateZ != 0) {
                model.zaxisRotateWithoutNormals(rotateZ)
            }

            if (translateY != 0) {
                model.translate(0, translateY, 0)
            }
        }

        if (tintModel) {
            model.tint(hue.toInt(), saturation.toInt(), lightness.toInt(), opacity.toInt() and 0xff)
        }

        if (!shouldSuppressMovingAnimation) {
            displace(toolkit, getBastype(), functionMask, x, z, rotation)
        }

        return true
    }

    fun sendChat(string_1: String, i_2: Int, i_3: Int) {
        setHintDetails(string_1, i_2, i_3, GameShell.gameCyclesPerSecond() * Client.ENTITY_DEFAULTS!!.playerMessageDuration)
    }

    override fun getCS2Index(): Int {
        return -playerIndex - 1
    }

    fun drawHint(toolkit: AbstractToolkit, hintMatrix: Matrix43, x: Long, y: Long, fileId: Int, hintDimension: Long) {
        val dimension = x * x + y * y
        if (dimension >= 262144L && dimension <= hintDimension) {
            val rotationY = (Math.atan2(x.toDouble(), y.toDouble()) * 2607.5945876176133 - yaw.rotation).toInt() and 0x3fff
            val model = createPlayerHint(toolkit, rotationY, rotateX, rotateZ, translateY, fileId)
            if (model != null) {
                toolkit.setDepthBuffering(false)
                model.drawMatrixThreaded(hintMatrix, null, ModelFunctionMaskGL.EMPTY)
                toolkit.setDepthBuffering(true)
            }
        }
    }

    fun decodeAppearance(buffer: Packet) {
        buffer.currentPosition = 0
        val flags = buffer.readUByte()
        male = (flags and 0x1).toByte()
        val volumeUnadaptableBefore = instrumentSoundEffect
        instrumentSoundEffect = (flags and 0x2) != 0
        val showElo = (flags and 0x4) != 0
        val size = getSize()
        setSize((flags shr 3 and 0x7) + 1)
        val titleBefore = (flags and 0x40) != 0
        val titleAfter = (flags and 0x80) != 0
        val absolutePos = Vector3Pool.vector3From(getAbsolutePosition().coords)
        absolutePos.x += ((getSize() - size) shl 8).toFloat()
        absolutePos.z += ((getSize() - size) shl 8).toFloat()
        setBasePosition(absolutePos)
        absolutePos.pushVectorStack()
        if (titleBefore) {
            prefixTitle = buffer.readJagString()
        } else {
            prefixTitle = null
        }

        if (titleAfter) {
            postfixTitle = buffer.readJagString()
        } else {
            postfixTitle = null
        }

        skullId = buffer.readByte().toInt()
        prayerHeadIconId = buffer.readByte().toInt()
        hidden = buffer.readByte().toInt() == 1
        if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LIVE && Client.PLAYER_RIGHTS >= 2) {
            hidden = false
        }

        var currentMorphId = -1
        teamId = 0
        val bodyAppearances = IntArray(TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.size)
        val meshModifiers = arrayOfNulls<ItemTypeCustomisation>(TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.size)
        val wornItems = arrayOfNulls<ItemType>(TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.size)

        for (slot in TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.indices) {
            if (TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!![slot] != 1) {
                val i_15 = buffer.readUByte()
                if (i_15 == 0) {
                    bodyAppearances[slot] = 0
                } else {
                    val i_16 = buffer.readUByte()
                    var itemId = i_16 + (i_15 shl 8)
                    if (slot == 0 && itemId == 65535) {
                        currentMorphId = buffer.readBigSmart()
                        teamId = buffer.readUByte()
                        break
                    }

                    if (itemId >= 16384) {
                        itemId -= 16384
                        bodyAppearances[slot] = itemId or 0x40000000.toInt()
                        wornItems[slot] = TypeLists.OBJ!!.list(itemId)
                        val team = wornItems[slot]!!.teamId
                        if (team != 0) {
                            teamId = team
                        }
                    } else {
                        bodyAppearances[slot] = itemId - 256 or -2147483648
                    }
                }
            }
        }

        if (currentMorphId == -1) {
            val functionMask = buffer.readUShort()
            var count = 0
            for (slot in TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.indices) {
                if (TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!![slot] == 0) {
                    if ((functionMask and (1 shl count)) != 0) {
                        meshModifiers[slot] = ItemTypeCustomisation.decodeItemEffects(wornItems[slot]!!, buffer)
                    }
                    count++
                }
            }
        }

        val staticRecolorDst = IntArray(10)
        for (slot in 0 until 10) {
            var color = buffer.readUByte()
            if (PlayerAppearance.recolorPaletteDst!!.isEmpty() || color < 0 || color >= PlayerAppearance.recolorPaletteDst!![slot][0].size) {
                color = 0
            }
            staticRecolorDst[slot] = color
        }

        renderEmote = buffer.readUShort()
        username = buffer.readString()
        displayName = username
        if (this == PlayerUpdate.MY_PLAYER!!) {
            PlayerUpdate.MY_PLAYER_USERNAME = username
        }

        combatLevel = buffer.readUByte()
        if (showElo) {
            elo = buffer.readUShort()
            if (elo == 65535) {
                elo = -1
            }

            cbLevelWithSumm = combatLevel
            cbLevelRelated = -1
        } else {
            elo = 0
            cbLevelWithSumm = buffer.readUByte()
            cbLevelRelated = buffer.readUByte()
            if (cbLevelRelated == 255) {
                cbLevelRelated = -1
            }
        }

        val i_15 = isNpc
        isNpc = buffer.readUByte()
        if (isNpc != 0) {
            val walk = walkingSoundEffectId
            val teleport = teleportingSoundEffectId
            val def = defaultSoundEffectId
            val run = runningSoundEffectId
            val ambient = ambientSoundVolume
            walkingSoundEffectId = buffer.readUShort()
            teleportingSoundEffectId = buffer.readUShort()
            defaultSoundEffectId = buffer.readUShort()
            runningSoundEffectId = buffer.readUShort()
            ambientSoundVolume = buffer.readUByte()
            if (volumeUnadaptableBefore != instrumentSoundEffect || i_15 != isNpc || walk != walkingSoundEffectId || teleport != teleportingSoundEffectId || def != defaultSoundEffectId || run != runningSoundEffectId || ambient != ambientSoundVolume) {
                createPlayerAmbientSound(this)
            }
        } else {
            AmbientSound.unlinkPlayerSound(this)
        }

        if (playerAppearance == null) {
            playerAppearance = PlayerAppearance()
        }

        val morphedIdLastCycle = playerAppearance!!.morphism
        val typeRecolorDst = playerAppearance!!.recolorDstIndices
        @Suppress("UNCHECKED_CAST")
        playerAppearance!!.setAppearance(getCurrentRenderAnimation(), bodyAppearances, meshModifiers as Array<ItemTypeCustomisation>?, staticRecolorDst, male == 1.toByte(), currentMorphId)
        if (currentMorphId != morphedIdLastCycle) {
            val absolutePosition2 = Vector3Pool.vector3From(getAbsolutePosition().coords)
            absolutePosition2.x = ((movementBaseX[0] shl 9) + (getSize() shl 8)).toFloat()
            absolutePosition2.z = ((movementBaseY[0] shl 9) + (getSize() shl 8)).toFloat()
            setBasePosition(absolutePosition2)
            absolutePosition2.pushVectorStack()
        }

        if (playerIndex == PlayerUpdate.myPlayerIndex && typeRecolorDst != null) {
            for (i in staticRecolorDst.indices) {
                if (staticRecolorDst[i] != typeRecolorDst[i]) {
                    TypeLists.OBJ!!.resetRecolorCache()
                    break
                }
            }
        }

        if (particleSystem != null) {
            particleSystem!!.onEntityUpdate()
        }

        if (movingSequence.hasType() && movingSequence.hasType) {
            val basType = getBastype()
            if (!basType.isStandAnimation(movingSequence.getId())) {
                movingSequence.update(-1)
                movingSequence.hasType = false
            }
        }
    }

    override fun hasTileShadow(): Boolean {
        return false
    }

    override fun draw(activeToolkit: AbstractToolkit): RenderedEntity? {
        if (playerAppearance != null && method16133(activeToolkit, 2048)) {
            val renderingMatrix = activeToolkit.getRenderingMatrix()
            val projectionMatrix = getProjectionMatrix()
            val absolute = getAbsolutePosition()
            val rotation = yaw.getRotation()
            val occupiedTile = scene!!.tiles[plane.toInt()][absolute.coords.x.toInt() shr 9][absolute.coords.z.toInt() shr 9]
            if (occupiedTile != null && occupiedTile.groundDecoration != null) {
                val groundDec = occupiedTile.groundDecoration!!
                val i_8 = groundContouredOffsetY - groundDec.floorOffsetY
                groundContouredOffsetY = (groundContouredOffsetY - i_8 / 10.0f).toInt()
            } else {
                groundContouredOffsetY = (groundContouredOffsetY - groundContouredOffsetY / 10.0f).toInt()
            }

            renderingMatrix.copyFrom(projectionMatrix)
            renderingMatrix.translate(0.0f, (-20 - groundContouredOffsetY).toFloat(), 0.0f)
            var renderedEntity: RenderedEntity? = null
            hasPreferedPriorities = false
            if (PreferenceManager.preferences!!.characterShadows!!.getValue() == 1) {
                val bastype = getBastype()
                if (bastype.rendersShadow && (playerAppearance!!.morphism == -1 || TypeLists.NPC!!.list(playerAppearance!!.morphism).shadowed)) {
                    val shadowSequence = if (currentSequence.hasType() && currentSequence.hasSpeed()) currentSequence else null
                    val shadowMovement = if (movingSequence.hasType() && (!movingSequence.hasType || shadowSequence == null)) movingSequence else null
                    val shadowModel = getAnimatedShadowModel(activeToolkit, rotateX, rotateZ, translateY, 1, models[0], 0, 0, 160, 240, shadowMovement ?: shadowSequence)
                    if (shadowModel != null) {
                        if (boundingBoxPolygons == null || boundingBoxPolygons!!.size < models.size + 1) {
                            setupBoundingBox(models.size + 1)
                        }

                        renderedEntity = RenderedEntityCache.generate(true)
                        hasPreferedPriorities = true
                        activeToolkit.setDepthBuffering(false)
                        shadowModel.drawMatrixThreaded(renderingMatrix, boundingBoxPolygons!![models.size], ModelFunctionMaskGL.EMPTY)
                        activeToolkit.setDepthBuffering(true)
                    }
                }
            }

            if (this == PlayerUpdate.MY_PLAYER!!) {
                for (index in PlayerUpdate.HINT_ARROWS.indices.reversed()) {
                    val hintArrow = PlayerUpdate.HINT_ARROWS[index]
                    if (hintArrow != null && hintArrow.modelId != -1) {
                        if (hintArrow.targetType == 1) {
                            val objectNode = Client.NPC_MAP.get(hintArrow.targetIndex.toLong())
                            if (objectNode != null) {
                                val npcEntity = objectNode.value as NpcEntity
                                val abs = Vector3Pool.delta(npcEntity.getAbsolutePosition().coords, PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords)
                                val absX = abs.x.toInt()
                                val absY = abs.z.toInt()
                                drawHint(activeToolkit, renderingMatrix, absX.toLong(), absY.toLong(), hintArrow.modelId, 92160000L)
                            }
                        }

                        if (hintArrow.targetType == 2) {
                            val abs = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                            val hintDrawX = hintArrow.x - abs.x.toInt()
                            val hintDrawY = hintArrow.y - abs.z.toInt()
                            var hintDimension = (hintArrow.distance shl 9).toLong()
                            hintDimension *= hintDimension
                            drawHint(activeToolkit, renderingMatrix, hintDrawX.toLong(), hintDrawY.toLong(), hintArrow.modelId, hintDimension)
                        }

                        if (hintArrow.targetType == 10 && hintArrow.targetIndex >= 0 && hintArrow.targetIndex < PlayerUpdate.players.size) {
                            val player = PlayerUpdate.players[hintArrow.targetIndex]
                            if (player != null) {
                                val delta = Vector3Pool.delta(player.getAbsolutePosition().coords, PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords)
                                val x = delta.x.toInt()
                                val y = delta.z.toInt()
                                drawHint(activeToolkit, renderingMatrix, x.toLong(), y.toLong(), hintArrow.modelId, 92160000L)
                            }
                        }
                    }
                }
            }

            renderingMatrix.copyFrom(projectionMatrix)
            renderingMatrix.translate(0.0f, (-5 - groundContouredOffsetY).toFloat(), 0.0f)
            if (boundingBoxPolygons == null || boundingBoxPolygons!!.size < models.size) {
                setupBoundingBox(models.size)
            }

            if (renderedEntity == null) {
                renderedEntity = RenderedEntityCache.generate(true)
            }

            updateParticles(activeToolkit, models, renderingMatrix, false)

            for (index in models.indices) {
                if (models[index] != null) {
                    models[index]!!.drawMatrixThreaded(renderingMatrix, boundingBoxPolygons!![index], if (this == PlayerUpdate.MY_PLAYER!!) ModelFunctionMaskGL.SMOOTH_EDGES else ModelFunctionMaskGL.EMPTY)
                }
            }

            if (particleSystem != null) {
                val particles = particleSystem!!.getAllParticles()
                activeToolkit.renderParticles(particles)
            }

            for (index in models.indices) {
                if (models[index] != null) {
                    hasPreferedPriorities = hasPreferedPriorities or models[index]!!.accountForLayers()
                }

                models[index] = null
            }

            drawCycle = Client.timesSceneRendered
            return renderedEntity
        }
        return null
    }

    override fun getHintDetails(): HintDetails? {
        if (hintDetails != null) {
            if (hintDetails!!.text == null) {
                return null
            }
            if (ChatBox.PUBLIC_FILTER == 0 || ChatBox.PUBLIC_FILTER == 3 || ChatBox.PUBLIC_FILTER == 1 && FriendList.contains(displayName)) {
                return hintDetails
            }
        }
        return null
    }

    companion object {
        @JvmField
        var sceneUploaded: Boolean = false

        @JvmStatic
        fun createPlayerHint(toolkit: AbstractToolkit, rotationY: Int, rotationX: Int, rotationZ: Int, translateY: Int, fileId: Int): Model? {
            var model = HintArrow.hintCache.get(fileId.toLong()) as Model?
            val modelFlags: Short = 2055
            if (model == null) {
                val mesh = Mesh.fromResource(Resource.MODEL!!, fileId) ?: return null
                if (mesh.version < 13) {
                    mesh.upscale()
                }
                model = toolkit.createModel(mesh, modelFlags.toInt(), HintArrow.hintPreferenceFlags, 64, 768)
                HintArrow.hintCache.put(model, fileId.toLong())
            }
            model = model.copy(6.toByte(), modelFlags.toInt(), true)
            if (rotationY != 0) {
                model!!.yaxisRotateWithoutNormals(rotationY)
            }
            if (rotationX != 0) {
                model!!.xaxisRotateWithoutNormals(rotationX)
            }
            if (rotationZ != 0) {
                model!!.zaxisRotateWithoutNormals(rotationZ)
            }
            if (translateY != 0) {
                model!!.translate(0, translateY, 0)
            }
            return model
        }

        @JvmStatic
        fun createPlayerAmbientSound(player: PlayerEntity) {
            val ambientSound = AmbientSound.PLAYER_AMBIENT_SOUNDS.get(player.playerIndex.toLong()) as AmbientSound?
            if (ambientSound == null) {
                AmbientSound.createAmbientSoundEffect(player.plane.toInt(), player.movementBaseX[0], player.movementBaseY[0], 0, null, null, player)
            } else {
                ambientSound.load()
            }
        }

        @JvmStatic
        fun uploadScene() {
            if (!sceneUploaded) {
                MapRegion.upload(Resource.mapRegion!!.scene!!.surfaceTiles as Array<Array<Array<Tile?>?>?>)
                if (Resource.mapRegion!!.scene!!.waterTiles != null) {
                    MapRegion.upload(Resource.mapRegion!!.scene!!.waterTiles as Array<Array<Array<Tile?>?>?>)
                }
                sceneUploaded = true
            }
        }
    }
}
