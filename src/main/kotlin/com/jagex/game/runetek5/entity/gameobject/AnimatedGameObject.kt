package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.config.sequence.EntitySequence
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.vardomain.VarProviders

class AnimatedGameObject(
    renderer: AbstractToolkit,
    val loctypelist: ObjectTypeList,
    loctype: ObjectType,
    val type: Int,
    val rotation: Int,
    logicHeight: Int,
    plane: Int,
    val entityNode: SceneGraphEntity,
    val overlayHidden: Boolean,
    sequenceId: Int
) {

    var hasSequenceId: Boolean = false
    var locCustomisation: ObjectTypeCustomisation? = null
    var pairModel: Model? = null
    var castShadowAtPlane: BooleanArray? = null
    var shadow: Shadow? = null
    var particleSystem: ParticleSystem? = null
    var currentCycle: Int = 0
    var aBool1552: Boolean = false
    var gameobjectId: Int = -1
    var modelMinY: Int = 0

    @JvmField
    val locId: Int
    val logicHeight: Byte
    val heightLevel: Byte
    val renderShadow: Boolean
    val sequence: Sequence

    init {
        this.locId = loctype.id
        this.hasSequenceId = sequenceId != -1
        this.logicHeight = logicHeight.toByte()
        this.heightLevel = plane.toByte()
        this.renderShadow = renderer.hasShadows() && loctype.dynamicShadow && !this.overlayHidden
        this.sequence = EntitySequence(entityNode)
        this.animate(false, sequenceId, 1)
    }

    fun animate(sequenceId: Int) {
        hasSequenceId = true
        animate(false, sequenceId, 1)
    }

    fun getMinY(): Int = modelMinY

    fun addCustomisation(customisation: ObjectTypeCustomisation?) {
        locCustomisation = customisation
        pairModel = null
    }

    fun hasRenderingShadow(): Boolean {
        return renderShadow
    }

    fun removeShadow() {
        val shadowLocal = shadow
        if (shadowLocal != null) {
            val abs = entityNode.getAbsolutePosition().coords
            Resource.mapRegion!!.getScene()!!.removeShadow(shadowLocal, heightLevel.toInt(), abs.x.toInt(), abs.z.toInt(), castShadowAtPlane)
            castShadowAtPlane = null
            shadow = null
        }
    }

    fun animateParticles(renderer: AbstractToolkit, model: Model, projectionMatrix: Matrix43, performCycle: Boolean) {
        val emitters = model.getEmissiveTriangles()
        val effectiveVertices = model.getEffectiveVertices()
        if ((particleSystem == null || particleSystem!!.isDisabled) && (emitters != null || effectiveVertices != null)) {
            var locType: ObjectType? = loctypelist.list(locId)
            if (locType!!.alternativeIds != null) {
                locType = locType.getMultiLoc(if (Client.MAP_LOADING_STAGE == 4) VarProviders.CUTSCENE_ACTION_VAR_PROVIDER else VarProviders.PLAYER_VAR_PROVIDER)
            }
            if (locType != null) {
                particleSystem = ParticleSystem.get(Client.cycles, true)
            }
        }

        if (particleSystem != null) {
            model.updateParticlePosition(projectionMatrix)
            if (performCycle) {
                particleSystem!!.performCycle(renderer, Client.cycles.toLong(), emitters, effectiveVertices)
            } else {
                particleSystem!!.setCurrentCycle(Client.cycles.toLong())
            }
            particleSystem!!.setLogicHeight(logicHeight.toInt())
        }
    }

    fun getNegativeMinY(): Int = -getMinY()

    fun animate(isTransformed: Boolean, seqId: Int, animFlags: Int) {
        var additionalAnimationFlag = animFlags
        var randomSequenceId = seqId
        var replay = false
        if (seqId == -1) {
            var computedLoc: ObjectType? = loctypelist.list(locId)
            var originalLoc: ObjectType? = computedLoc
            if (computedLoc!!.alternativeIds != null) {
                computedLoc = computedLoc.getMultiLoc(if (Client.MAP_LOADING_STAGE == 4) VarProviders.CUTSCENE_ACTION_VAR_PROVIDER else VarProviders.PLAYER_VAR_PROVIDER)
            }
            if (computedLoc == null) {
                return
            }

            if (originalLoc === computedLoc) {
                originalLoc = null
            }
            if (computedLoc.isAnimated()) {
                if (isTransformed && sequence.hasType() && computedLoc.hasSequence(sequence.getId())) {
                    return
                }
                if (computedLoc.id != gameobjectId) {
                    replay = computedLoc.replaySequence
                }
                randomSequenceId = computedLoc.getRandomAnimation()
                if (computedLoc.hasAdditionalAnimations()) {
                    additionalAnimationFlag = 0
                } else {
                    additionalAnimationFlag = 1
                }
            } else if (originalLoc != null && originalLoc.isAnimated()) {
                if (isTransformed && sequence.hasType() && originalLoc.hasSequence(sequence.getId())) {
                    return
                }
                if (gameobjectId != computedLoc.id) {
                    replay = originalLoc.replaySequence
                }
                randomSequenceId = originalLoc.getRandomAnimation()
                if (originalLoc.hasAdditionalAnimations()) {
                    additionalAnimationFlag = 0
                } else {
                    additionalAnimationFlag = 1
                }
            }
        }
        if (randomSequenceId == -1) {
            sequence.resetSequence()
        } else {
            sequence.animateSequence(randomSequenceId, 0, additionalAnimationFlag, replay)
            currentCycle = Client.cycles
            aBool1552 = false
            pairModel = null
        }
    }

    fun getAnimatedModel(renderer: AbstractToolkit, functionMask: Int, bool3: Boolean, hasShadow: Boolean): Model? {
        var functionMask = functionMask
        var hasShadow = hasShadow
        var loctype: ObjectType? = loctypelist.list(locId)
        if (loctype!!.alternativeIds != null) {
            loctype = loctype.getMultiLoc(if (Client.MAP_LOADING_STAGE == 4) VarProviders.CUTSCENE_ACTION_VAR_PROVIDER else VarProviders.PLAYER_VAR_PROVIDER)
        }
        if (loctype == null) {
            removeShadow()
            gameobjectId = -1
            return null
        }

        if (!hasSequenceId && gameobjectId != loctype.id) {
            animate(true, -1, 0)
            aBool1552 = false
            pairModel = null
        }
        method2133()
        if (hasShadow) {
            hasShadow = hasShadow && renderShadow && !aBool1552 && (PreferenceManager.preferences!!.sceneryShadows!!.getValue() != 0)
        }
        if (bool3 && !hasShadow) {
            gameobjectId = loctype.id
            return null
        }
        val scenePosition = entityNode.getAbsolutePosition().coords
        val scene = Resource.mapRegion!!.getScene()!!
        val shadowLocal = shadow
        if (hasShadow && shadowLocal != null) {
            scene.removeShadow(shadowLocal, heightLevel.toInt(), scenePosition.x.toInt(), scenePosition.z.toInt(), castShadowAtPlane)
            aBool1552 = false
        }
        val currentHeight = scene.localHeightmap!![heightLevel.toInt()]!!
        val nextHeight: Ground? = if (overlayHidden) {
            scene.surfaceHeightmap!![0]
        } else {
            if (heightLevel < 3) scene.localHeightmap!![heightLevel + 1] else null
        }
        var model: Model? = null
        if (sequence.hasType()) {
            if (hasShadow) {
                functionMask = functionMask or ModelFunctionMask.SHADOWED
            }
            model = loctype.getAnimatedModel(
                renderer,
                functionMask,
                if (type != 11) type else 10,
                if (type == 11) 4 + rotation else rotation,
                currentHeight,
                nextHeight,
                scenePosition.x.toInt(),
                currentHeight.getAverageHeight(scenePosition.x.toInt(), scenePosition.z.toInt()),
                scenePosition.z.toInt(),
                sequence,
                locCustomisation
            )
            if (model != null) {
                if (hasShadow) {
                    if (castShadowAtPlane == null) {
                        castShadowAtPlane = BooleanArray(4)
                    }
                    shadow = model.createShadowSprite(shadow)
                    val shadowLocal = shadow
                    if (shadowLocal != null) {
                        scene.renderShadows(shadowLocal, heightLevel.toInt(), scenePosition.x.toInt(), scenePosition.z.toInt(), castShadowAtPlane)
                        aBool1552 = true
                    }
                }
                modelMinY = model.getMinY()
                model.getSize3d()
            } else {
                castShadowAtPlane = null
                shadow = null
                modelMinY = 0
            }
            pairModel = null
        } else if (pairModel != null && pairModel!!.getFunctionMask() and functionMask == functionMask && gameobjectId == loctype.id) {
            model = pairModel
        } else {
            if (pairModel != null) {
                functionMask = functionMask or pairModel!!.getFunctionMask()
            }
            val pair = loctype.getPair(
                renderer,
                functionMask,
                if (type != 11) type else 10,
                if (type == 11) 4 + rotation else rotation,
                currentHeight,
                nextHeight,
                scenePosition.x.toInt(),
                currentHeight.getAverageHeight(scenePosition.x.toInt(), scenePosition.z.toInt()),
                scenePosition.z.toInt(),
                hasShadow,
                locCustomisation
            )
            if (pair != null) {
                pairModel = pair.mainObject as Model?
                model = pairModel
                if (hasShadow) {
                    shadow = pair.companionObject as Shadow?
                    val shadowLocal = shadow
                    if (shadowLocal != null) {
                        castShadowAtPlane = null
                        scene.renderShadows(shadowLocal, heightLevel.toInt(), scenePosition.x.toInt(), scenePosition.z.toInt(), null)
                        aBool1552 = true
                    }
                }
                if (model != null) {
                    modelMinY = model.getMinY()
                    model.getSize3d()
                }
            } else {
                castShadowAtPlane = null
                shadow = null
                pairModel = null
                modelMinY = 0
            }
        }
        gameobjectId = loctype.id
        return model
    }

    fun method2133() {
        if (sequence.hasType()) {
            if (sequence.progressCycle(Client.cycles - currentCycle)) {
                if (PreferenceManager.preferences!!.sceneryShadows!!.getValue() == 2) {
                    aBool1552 = false
                }
                if (sequence.needsReset()) {
                    sequence.update(-1)
                    hasSequenceId = false
                    animate(false, -1, 0)
                }
            }
        } else {
            animate(false, -1, 0)
        }
        currentCycle = Client.cycles
    }

    fun getAnimatedShadowedModel(toolkit: AbstractToolkit) {
        getAnimatedModel(toolkit, ModelFunctionMask.SHADOWED, true, true)
    }
}
