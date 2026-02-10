package com.jagex.game.runetek5.entity.pathingentity.npc

import java.util.Arrays
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration
import com.jagex.game.runetek5.graphics.hint.HintDetails
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.vardomain.VarProviders
import com.jagex.game.runetek5.vardomain.VarnCache
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask

class NpcEntity(scene: Scene?) : PathingEntity(scene, 25) {

    @JvmField var customisation: NPCTypeCustomisation? = null
    @JvmField var faceX: Int = -1
    @JvmField var faceY: Int = -1
    @JvmField var meshModifierCount: Int = 1
    @JvmField var headMeshModifierCount: Int = 1
    @JvmField var currRenderAnim: Int = -1
    @JvmField val varns: VarnCache = VarnCache()
    @JvmField var definitions: NPCType? = null
    @JvmField var modifiedName: String? = null
    @JvmField var modifiedCbLvl: Int = 0
    val maxStats: IntArray = IntArray(6)
    val currentStats: IntArray = IntArray(6)
    var tintHue: Int = 0
    var tintSaturation: Int = 0
    var tintLightness: Int = 0
    var tintOpacity: Int = 0

    init {
        createDynamicTint()
    }

    override fun getMaxSizeShift(): Int {
        return if (this.definitions == null) 0 else this.definitions!!.sizeShift
    }

    /**
     * Generates random tints for the entity's model
     * presumably one of the purposes of this is to
     * deal with color bots
     */
    fun createDynamicTint() {
        this.tintHue = 32 + (Math.random() * 4.0).toInt()
        this.tintSaturation = 3 + (Math.random() * 2.0).toInt()
        this.tintLightness = 16 + (Math.random() * 3.0).toInt()
        if (PreferenceManager.preferences!!.textures!!.getValue() == 1) {
            this.tintOpacity = (Math.random() * 6.0).toInt()
        } else {
            this.tintOpacity = (Math.random() * 12.0).toInt()
        }
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        return null
    }

    fun getAnimatedBodymodel(toolkit: AbstractToolkit, functionMask: Int): Boolean {
        var functionMask = functionMask
        val initialFunctionMask = functionMask
        val bastype = getBastype()
        val seq = if (super.currentSequence.hasType() && !super.currentSequence.hasSpeed()) super.currentSequence else null
        val moveAnimation = if (super.movingSequence.hasType() && (!super.movingSequence.hasType || seq == null)) super.movingSequence else null
        val x = bastype.modelWidth
        val z = bastype.modelLength
        if (x != 0 || z != 0 || bastype.rollTargetAngle != 0 || bastype.pitchTargetAngle != 0) {
            functionMask = functionMask or (ModelFunctionMask.DISPLACE_X or ModelFunctionMask.DISPLACE_Y or ModelFunctionMask.DISPLACE_Z)
        }

        val hasTint = super.opacity.toInt() != 0 && Client.cycles >= super.tintStartCycle && Client.cycles < super.tintEndCycle
        if (hasTint) {
            functionMask = functionMask or ModelFunctionMask.TINT
        }

        val rotation = super.yaw.getRotation()
        val model = this.definitions!!.getAnimatedBodymodel(toolkit, functionMask, TypeLists.BAS!!, VarProviders.PLAYER_VAR_PROVIDER, seq, moveAnimation, super.sequences as Array<Sequence?>?, super.equipmentRotations, rotation, this.customisation, getCurrentRenderAnimation(), false)
        super.models[0] = model
        if (model == null) {
            return false
        }
        super.modelMinY = model.getMinY()
        model.getSize3d()
        rotate(model)
        if (x == 0 && z == 0) {
            hillChange(rotation, getSize() shl 9, getSize() shl 9, 0, 0)
        } else {
            hillChange(rotation, x, z, bastype.hillRotateX, bastype.hillRotateZ)
            if (super.rotateX != 0) {
                super.models[0]!!.xaxisRotateWithoutNormals(super.rotateX)
            }

            if (super.rotateZ != 0) {
                super.models[0]!!.zaxisRotateWithoutNormals(super.rotateZ)
            }

            if (super.translateY != 0) {
                super.models[0]!!.translate(0, super.translateY, 0)
            }
        }

        if (hasTint) {
            model.tint(super.hue.toInt(), super.saturation.toInt(), super.lightness.toInt(), super.opacity.toInt() and 0xff)
        }

        displace(toolkit, bastype, initialFunctionMask, x, z, rotation)
        return true
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
        if (this.definitions != null && (super.particlesInitialized || getAnimatedBodymodel(toolkit, 0))) {
            val renderingMatrix = toolkit.getRenderingMatrix()
            renderingMatrix.copyFrom(getProjectionMatrix())
            renderingMatrix.translate(0.0F, -5.0f, 0.0F)
            updateParticles(toolkit, super.models, renderingMatrix, super.particlesInitialized)
            Arrays.fill(super.models, null)
        }
    }

    override fun getCS2Index(): Int {
        return super.playerIndex + 1
    }

    fun move(plane: Int, baseX: Int, baseY: Int, stationary: Boolean, size: Int) {
        super.collisionPlane = plane.toByte()
        this.plane = super.collisionPlane
        if (Resource.mapRegion!!.settingsBits.areRoofsHidden(baseX, baseY)) {
            super.collisionPlane = (super.collisionPlane + 1).toByte()
        }

        if (super.currentSequence.hasType() && super.currentSequence.getType()!!.moveType == 1) {
            super.currentAnimations = null
            super.currentSequence.update(-1)
        }
        for (index in super.spotAnimations.indices) {
            if (super.spotAnimations[index].spotAnimationId != -1) {
                val gfxType = TypeLists.SPOT_ANIM!!.list(super.spotAnimations[index].spotAnimationId)
                if (gfxType.replay && gfxType.sequenceId != -1 && TypeLists.SEQ!!.list(gfxType.sequenceId)!!.moveType == 1) {
                    super.spotAnimations[index].sequence.update(-1)
                    super.spotAnimations[index].spotAnimationId = -1
                }
            }
        }

        if (!stationary) {
            val offsetX = baseX - super.movementBaseX[0]
            val offsetY = baseY - super.movementBaseY[0]
            if (offsetX >= -8 && offsetX <= 8 && offsetY >= -8 && offsetY <= 8) {
                if (super.movementStepsLeft < super.movementBaseX.size - 1) {
                    super.movementStepsLeft++
                }

                for (index in super.movementStepsLeft downTo 1) {
                    super.movementBaseX[index] = super.movementBaseX[index - 1]
                    super.movementBaseY[index] = super.movementBaseY[index - 1]
                    super.movementTypes[index] = super.movementTypes[index - 1]
                }

                super.movementBaseX[0] = baseX
                super.movementBaseY[0] = baseY
                super.movementTypes[0] = MovementType.WALKING.id.toByte()
                return
            }
        }

        super.movementStepsLeft = 0
        super.previousMovementStepsLeft = 0
        super.stalledStepCount = 0
        super.movementBaseX[0] = baseX
        super.movementBaseY[0] = baseY
        val abs = Vector3Pool.vector3From(getAbsolutePosition().coords)
        abs.x = ((size shl 8) + (super.movementBaseX[0] shl 9)).toFloat()
        abs.z = ((size shl 8) + (super.movementBaseY[0] shl 9)).toFloat()
        setBasePosition(abs)
        abs.pushVectorStack()
        if (super.particleSystem != null) {
            super.particleSystem!!.onEntityUpdate()
        }
    }

    fun hasDefinition(): Boolean {
        return this.definitions != null
    }

    override fun getCurrentRenderAnimation(): Int {
        if (this.currRenderAnim != -1) {
            return this.currRenderAnim
        }
        if (this.definitions!!.alternativeIds != null) {
            val npcType = this.definitions!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
            if (npcType != null && npcType.npcBasId != -1) {
                return npcType.npcBasId
            }
        }
        return this.definitions!!.npcBasId
    }

    fun move(direction: NPCDirection, movementType: Int) {
        var baseX = super.movementBaseX[0]
        var baseY = super.movementBaseY[0]
        when (direction.value) {
            0 -> ++baseY
            1 -> {
                --baseX
                ++baseY
            }
            2 -> {
                ++baseX
                ++baseY
            }
            3 -> ++baseX
            4 -> {
                --baseX
                --baseY
            }
            5 -> --baseX
            6 -> --baseY
            7 -> {
                ++baseX
                --baseY
            }
        }

        if (super.currentSequence.hasType() && super.currentSequence.getType()!!.moveType == 1) {
            super.currentAnimations = null
            super.currentSequence.update(-1)
        }

        for (index in super.spotAnimations.indices) {
            if (super.spotAnimations[index].spotAnimationId != -1) {
                val gfxtype = TypeLists.SPOT_ANIM!!.list(super.spotAnimations[index].spotAnimationId)
                if (gfxtype.replay && gfxtype.sequenceId != -1 && TypeLists.SEQ!!.list(gfxtype.sequenceId)!!.moveType == 1) {
                    super.spotAnimations[index].sequence.update(-1)
                    super.spotAnimations[index].spotAnimationId = -1
                }
            }
        }

        if (super.movementStepsLeft < super.movementBaseX.size - 1) {
            super.movementStepsLeft++
        }

        for (step in super.movementStepsLeft downTo 1) {
            super.movementBaseX[step] = super.movementBaseX[step - 1]
            super.movementBaseY[step] = super.movementBaseY[step - 1]
            super.movementTypes[step] = super.movementTypes[step - 1]
        }

        super.movementBaseX[0] = baseX
        super.movementBaseY[0] = baseY
        super.movementTypes[0] = movementType.toByte()
    }

    override fun getIconHeight(): Int {
        if (this.definitions!!.alternativeIds != null) {
            val npctype = this.definitions!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
            if (npctype != null && npctype.iconHeight != -1) {
                return npctype.iconHeight
            }
        }

        return if (this.definitions!!.iconHeight != -1) this.definitions!!.iconHeight else super.getIconHeight()
    }

    fun isVisibile(): Boolean {
        return this.definitions!!.visible
    }

    override fun hasTileShadow(): Boolean {
        return false
    }

    override fun castShadow(class521Sub12: SceneGraphEntity, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean) {
        throw IllegalStateException()
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    fun getStat(index: Int): Int {
        return this.maxStats[index]
    }

    override fun calculateProjection(toolkit: AbstractToolkit, cameraX: Int, cameraY: Int): Boolean {
        if (this.definitions != null && this.getAnimatedBodymodel(toolkit, 131072)) {
            val projectionMatrix = this.getProjectionMatrix()
            var projectionCalculated = false
            for (index in this.models.indices) {
                if (this.models[index] != null) {
                    val notRendered = this.definitions!!.sizeShift > 0 || (if (this.definitions!!.shadowSize != -1) this.definitions!!.shadowSize == 1 else this.definitions!!.size == 1)
                    val isProjected = this.models[index]!!.calculateProjection(cameraX, cameraY, projectionMatrix, notRendered, this.definitions!!.sizeShift)
                    if (isProjected) {
                        projectionCalculated = true
                        break
                    }
                }
            }
            for (index in this.models.indices) {
                this.models[index] = null
            }
            return projectionCalculated
        }
        return false
    }

    fun method16164(string_1: String) {
        val i_5 = GameShell.gameCyclesPerSecond() * Client.ENTITY_DEFAULTS!!.npcMessageDuration
        setHintDetails(string_1, 0, 0, i_5)
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        if (this.definitions != null && getAnimatedBodymodel(toolkit, 526336)) {
            val projectionMatrix = getProjectionMatrix()
            val absPos = getAbsolutePosition()
            val renderingMatrix = toolkit.getRenderingMatrix()
            val i_6 = super.yaw.getRotation()
            val class2937 = super.scene!!.tiles[super.plane.toInt()][(absPos.coords.x.toInt() shr 9)][(absPos.coords.z.toInt() shr 9)]
            if (class2937 != null && class2937.groundDecoration != null) {
                val groundDecoration = class2937.groundDecoration as? AbstractGroundDecoration
                val i_8 = super.groundContouredOffsetY - (groundDecoration?.floorOffsetY?.toInt() ?: 0)
                super.groundContouredOffsetY = (super.groundContouredOffsetY - i_8 / 10.0F).toInt()
            } else {
                super.groundContouredOffsetY = (super.groundContouredOffsetY - super.groundContouredOffsetY / 10.0F).toInt()
            }

            renderingMatrix.copyFrom(projectionMatrix)
            renderingMatrix.translate(0.0F, (-20 - super.groundContouredOffsetY).toFloat(), 0.0F)
            val bastype = getBastype()
            val npctype = if (this.definitions!!.alternativeIds != null) this.definitions!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER) else this.definitions
            super.hasPreferedPriorities = false
            var renderedEntity: RenderedEntity? = null
            if (PreferenceManager.preferences!!.characterShadows!!.getValue() == 1 && npctype!!.shadowed && bastype.rendersShadow) {
                val seq = if (super.currentSequence.hasType() && super.currentSequence.hasSpeed()) super.currentSequence else null
                val moveseq = if (!super.movingSequence.hasType() || super.movingSequence.hasType && seq != null) null else super.movingSequence
                val model = PathingEntity.getAnimatedShadowModel(toolkit, super.rotateX, super.rotateZ, super.translateY, this.definitions!!.size, super.models[0],
                        this.definitions!!.shadowColorSrc.toInt() and 0xffff, this.definitions!!.shadowColorDst.toInt() and 0xffff, this.definitions!!.shadowAlphaSrc.toInt() and 0xff, this.definitions!!.shadowAlphaDst.toInt() and 0xff,
                        if (moveseq != null) moveseq else seq)
                if (model != null) {
                    if (super.boundingBoxPolygons == null || super.boundingBoxPolygons!!.size < super.models.size + 1) {
                        setupBoundingBox(super.models.size + 1)
                    }

                    renderedEntity = RenderedEntityCache.generate(isVisibile())
                    super.hasPreferedPriorities = true
                    toolkit.setDepthBuffering(false)
                    model.drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons!![super.models.size], ModelFunctionMaskGL.EMPTY)
                    toolkit.setDepthBuffering(true)
                }
            }

            if (super.boundingBoxPolygons == null || super.boundingBoxPolygons!!.size < super.models.size) {
                setupBoundingBox(super.models.size)
            }

            if (renderedEntity == null) {
                renderedEntity = RenderedEntityCache.generate(isVisibile())
            }

            updateParticles(toolkit, super.models, renderingMatrix, false)

            for (modelIndex in super.models.indices) {
                if (super.models[modelIndex] != null) {
                    if (this.definitions!!.hasTint) {
                        super.models[modelIndex]!!.tint(this.tintHue, this.tintSaturation, this.tintLightness, this.tintOpacity)
                    }

                    super.models[modelIndex]!!.drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons!![modelIndex], ModelFunctionMaskGL.EMPTY)
                }
            }

            if (super.particleSystem != null) {
                toolkit.renderParticles(super.particleSystem!!.getAllParticles())
            }

            for (modelIndex in super.models.indices) {
                if (super.models[modelIndex] != null) {
                    super.hasPreferedPriorities = super.hasPreferedPriorities or super.models[modelIndex]!!.accountForLayers()
                }
                super.models[modelIndex] = null
            }

            super.drawCycle = Client.timesSceneRendered
            return renderedEntity
        }
        return null
    }

    override fun privateMessagesEnabled(): Boolean {
        return Client.ENTITY_DEFAULTS!!.npcMessagesEnabled
    }

    override fun getHintDetails(): HintDetails? {
        return if (super.hintDetails != null && super.hintDetails!!.text == null) null else super.hintDetails
    }

    fun setDirection(npcType: NPCType?) {
        if (npcType != this.definitions && MiniMenu.isOpen && MiniMenu.containsEntryForNpc(super.playerIndex)) {
            MiniMenu.close()
        }

        this.definitions = npcType
        if (this.definitions != null) {
            this.modifiedName = this.definitions!!.name
            this.modifiedCbLvl = this.definitions!!.combatLevel
        }

        if (super.particleSystem != null) {
            super.particleSystem!!.onEntityUpdate()
        }
    }

    fun getCurrentStat(statIndex: Int): Int {
        return this.currentStats[statIndex]
    }

    fun updateStats(statIndex: Int, max: Int, current: Int) {
        this.maxStats[statIndex] = max
        this.currentStats[statIndex] = current
    }

    override fun getOverheadSprite(): Int {
        if (this.definitions!!.alternativeIds != null) {
            val npcType = this.definitions!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
            if (npcType != null && npcType.overheadSprite != -1) {
                return npcType.overheadSprite
            }
        }

        return this.definitions!!.overheadSprite
    }
}
