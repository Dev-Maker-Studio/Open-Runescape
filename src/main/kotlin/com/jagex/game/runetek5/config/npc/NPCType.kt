package com.jagex.game.runetek5.config.npc

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.bas.BasTypeList
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCDirection
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCTypeCustomisation
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_X
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_Y
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_Z
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RECOLOR
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RETEXTURE
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.TINT
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.vardomain.VarDomain

/**
 * [J]
 * @author Steven - 11/10/2023
 *
 * [NXT]
	[X]jag::game::NPCType::DecodeType(jag::Packet &,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  0000000000469B30 000010DD 00000048 00000000 R . . . . T .
	[~]jag::game::NPCType::GetBodyModels(jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::shared_ptr<jag::graphics::EffectState> const&,jag::game::NPCTypeCustomisation const*,jag::game::PlayerVarDomain const*,jag::game::ConfigProvider *) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000051D630 0000022C 000000A8 00000010 R . . . . . .
	[~]jag::game::NPCType::GetHeadModels(jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::shared_ptr<jag::graphics::EffectState> const&,jag::game::NPCTypeCustomisation const*,jag::game::PlayerVarDomain const*,jag::game::ConfigProvider *) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000051DA30 0000021C 000000A8 00000010 R . . . . . .
	[~]jag::game::NPCType::GetModel(jag::game::NPCType::ModelType,jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::Array<int> const&,jag::game::NPCTypeCustomisation const*) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  000000000051CBF0 00000A20 000000E8 00000081 R . . . . . .
	[X]jag::game::NPCType::GetMultiNPC(bool &,jag::game::PlayerVarDomain const&,jag::game::ConfigProvider &)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 .text  0000000000117380 00000208 00000038 00000000 R . . . . . .
	[-]jag::game::NPCType::NPCType(jag::game::ConfigTypeListPreloadPackedContainer<jag::game::NPCType> *,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  0000000000420F70 000003DF 00000018 00000000 R . . . . . .
	[X]jag::game::NPCType::PostDecode(void)
 *
 */
open class NPCType {

    @JvmField
    var type: Int = 0
    @JvmField
    var actions: Array<String?>? = null
    @JvmField
    var modelIds: IntArray? = null
    @JvmField
    var gameType: Byte = 0
    @JvmField
    var name: String = "null"
    @JvmField
    var size: Int = 1
    @JvmField
    var npcBasId: Int = -1
    @JvmField
    var primaryCursor: Int = -1
    @JvmField
    var secondaryCursor: Int = -1
    @JvmField
    var primaryCursorActionIndex: Int = -1
    @JvmField
    var secondaryCursorActionIndex: Int = -1
    @JvmField
    var attackCursor: Int = -1
    @JvmField
    var displayOnMinimap: Boolean = true
    @JvmField
    var combatLevel: Int = -1
    @JvmField
    var highPriority: Boolean = false
    @JvmField
    var mediumPriority: Boolean = false
    @JvmField
    var lowPriority: Boolean = false
    @JvmField
    var headIcons: Int = -1
    @JvmField
    var overheadSprite: Int = -1
    @JvmField
    var turnDirection: Int = 32
    @JvmField
    var visible: Boolean = true
    @JvmField
    var delayMovement: Boolean = true
    @JvmField
    var shadowed: Boolean = true
    @JvmField
    var shadowColorSrc: Short = 0
    @JvmField
    var shadowColorDst: Short = 0
    @JvmField
    var shadowAlphaSrc: Byte = -96
    @JvmField
    var shadowAlphaDst: Byte = -16
    @JvmField
    var walkingFlags: Byte = 0
    @JvmField
    var walkingSoundEffect: Int = -1
    @JvmField
    var teleportSoundEffect: Int = -1
    @JvmField
    var idleSoundEffect: Int = -1
    @JvmField
    var runningSoundEffect: Int = -1
    @JvmField
    var ambientSoundMinHearDistance: Int = 0
    @JvmField
    var ambientSoundMaxHearDistance: Int = 0
    @JvmField
    var ambientSoundVolume: Int = 255
    @JvmField
    var iconHeight: Int = -1
    @JvmField
    var respawnDirection: NPCDirection? = null
    @JvmField
    var mecId: Int = 0
    @JvmField
    var shadowSize: Int = 0
    @JvmField
    var ambientSoundMaxDelay: Int = 0
    @JvmField
    var ambientSoundMinDelay: Int = 0
    @JvmField
    var sizeShift: Int = 0
    @JvmField
    var hasTint: Boolean = false
    @JvmField
    var recolorDst: ShortArray? = null
    @JvmField
    var retextureDst: ShortArray? = null
    @JvmField
    var headModels: IntArray? = null
    @JvmField
    var alternativeIds: IntArray? = null
    @JvmField
    var quests: IntArray? = null
    @JvmField
    var instrumentSoundEffect: Boolean = false
    @JvmField
    var npcTypeList: NpcTypeList? = null
    @JvmField
    var tintOpacity: Byte = 0
    @JvmField
    var scaleXZ: Int = 128
    @JvmField
    var scaleY: Int = 128
    @JvmField
    var ambient: Int = 0
    @JvmField
    var contrast: Int = 0
    @JvmField
    var varpbit: Int = -1
    @JvmField
    var varp: Int = -1
    @JvmField
    var recolorSrc: ShortArray? = null
    @JvmField
    var retextureSrc: ShortArray? = null
    @JvmField
    var recolorDPalette: ByteArray? = null
    @JvmField
    var modelOffsets: Array<IntArray?>? = null
    @JvmField
    var tintHue: Byte = 0
    @JvmField
    var tintSaturation: Byte = 0
    @JvmField
    var tintLightness: Byte = 0
    @JvmField
    var parameters: IterableNodeMap<Node<*>>? = null

    init {
        this.respawnDirection = NPCDirection.SOUTH
        this.mecId = -1
        this.gameType = -1
        this.shadowSize = -1
        this.ambientSoundMaxDelay = 256
        this.ambientSoundMinDelay = 256
        this.sizeShift = 0
        this.hasTint = true
    }

    internal fun decodeType(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decodeOpcode(buffer, opcode)
        }
    }

    fun getAnimatedBodymodel(
        toolkit: AbstractToolkit,
        functionMask: Int,
        basType: BasTypeList,
        varProvider: VarDomain,
        animation5: Sequence?,
        animation6: Sequence?,
        arr_7: Array<Sequence?>?,
        ints_8: IntArray?,
        i_9: Int,
        customisation: NPCTypeCustomisation?
    ): Model? {
        return getAnimatedBodymodel(
            toolkit,
            functionMask,
            basType,
            varProvider,
            animation5,
            animation6,
            arr_7,
            ints_8,
            i_9,
            customisation,
            this.npcBasId,
            true
        )
    }

    fun getCs2Value(mapKey: Int, defaultValue: Int): Int {
        if (this.parameters == null) {
            return defaultValue
        } else {
            val node = this.parameters!!.get(mapKey.toLong()) as IntNode?
            return node?.value ?: defaultValue
        }
    }

    internal fun setGameType() {
        if (this.modelIds == null) {
            this.modelIds = IntArray(0)
        }
        if (this.gameType == (-1).toByte()) {
            if (this.npcTypeList!!.game == Game.RUNESCAPE) {
                this.gameType = 1
            } else {
                this.gameType = 0
            }
        }
    }

    fun getAnimatedBodymodel(
        toolkit: AbstractToolkit,
        functionMask: Int,
        bastypelist: BasTypeList,
        varProvider: VarDomain,
        animationSequence: Sequence?,
        idleSequence: Sequence?,
        animatingSequence: Array<Sequence?>?,
        submeshRotations: IntArray?,
        submeshRotationOffset: Int,
        npcCustomisation: NPCTypeCustomisation?,
        basId: Int,
        ignoreOverrideBas: Boolean
    ): Model? {
        if (this.alternativeIds != null) {
            val npctype = getMultiNpc(varProvider)
            return npctype?.getAnimatedBodymodel(
                toolkit,
                functionMask,
                bastypelist,
                varProvider,
                animationSequence,
                idleSequence,
                animatingSequence,
                submeshRotations,
                submeshRotationOffset,
                npcCustomisation,
                basId,
                ignoreOverrideBas
            )
        }
        var displacementFunctionMask = functionMask
        if (this.scaleY != 128) {
            displacementFunctionMask = functionMask or DISPLACE_Y
        }
        if (this.scaleXZ != 128) {
            displacementFunctionMask = displacementFunctionMask or DISPLACE_X or DISPLACE_Z
        }
        var requiresAnimating = false
        val sequenceCount = animatingSequence?.size ?: 0
        for (sequenceIndex in 0 until sequenceCount) {
            if (animatingSequence!![sequenceIndex] != null) {
                displacementFunctionMask = displacementFunctionMask or animatingSequence[sequenceIndex]!!.getFunctionMask()
                requiresAnimating = true
            }
        }
        if (animationSequence != null) {
            displacementFunctionMask = displacementFunctionMask or animationSequence.getFunctionMask()
            requiresAnimating = true
        }
        if (idleSequence != null) {
            displacementFunctionMask = displacementFunctionMask or idleSequence.getFunctionMask()
            requiresAnimating = true
        }
        var npcHash = this.type.toLong() or (toolkit.renderId.toLong() shl 16)
        if (npcCustomisation != null) {
            npcHash = npcHash or (npcCustomisation.uid.toLong() shl 24)
        }
        var model: Model?
        synchronized(this.npcTypeList!!.bodymodelCache) {
            model = this.npcTypeList!!.bodymodelCache.get(npcHash) as Model?
        }
        var basType: BasType? = null
        if (!ignoreOverrideBas && basId != -1) {
            basType = bastypelist.list(basId)
        } else if (this.npcBasId != -1) {
            basType = bastypelist.list(this.npcBasId)
        }
        if (model == null || (model!!.getFunctionMask() and displacementFunctionMask) != displacementFunctionMask) {
            if (model != null) {
                displacementFunctionMask = displacementFunctionMask or model!!.getFunctionMask()
            }
            var appearanceFunctionMask = displacementFunctionMask
            if (this.recolorSrc != null) {
                appearanceFunctionMask = displacementFunctionMask or RECOLOR
            }
            if (this.retextureSrc != null) {
                appearanceFunctionMask = appearanceFunctionMask or RETEXTURE
            }
            if (this.tintOpacity != 0.toByte()) {
                appearanceFunctionMask = appearanceFunctionMask or TINT
            }
            val npcModelIds = if (npcCustomisation != null && npcCustomisation.modelIds != null) npcCustomisation.modelIds else this.modelIds
            var missingModels = false
            synchronized(this.npcTypeList!!.modelResource) {
                var index = 0
                while (index < npcModelIds!!.size) {
                    if (npcModelIds[index] != -1 && !this.npcTypeList!!.modelResource.isCached(npcModelIds[index], 0)) {
                        missingModels = true
                    }
                    index++
                }
            }
            if (missingModels) {
                return null
            }
            val meshes = arrayOfNulls<Mesh>(npcModelIds!!.size)
            for (meshIndex in npcModelIds.indices) {
                if (npcModelIds[meshIndex] != -1) {
                    synchronized(this.npcTypeList!!.modelResource) {
                        meshes[meshIndex] = Mesh.fromResource(this.npcTypeList!!.modelResource, npcModelIds[meshIndex])
                    }
                    if (meshes[meshIndex] != null) {
                        if (meshes[meshIndex]!!.version < 13) {
                            meshes[meshIndex]!!.upscale()
                        }
                        if (this.modelOffsets != null && this.modelOffsets!![meshIndex] != null) {
                            meshes[meshIndex]!!.translate(
                                this.modelOffsets!![meshIndex]!![0],
                                this.modelOffsets!![meshIndex]!![1],
                                this.modelOffsets!![meshIndex]!![2]
                            )
                        }
                    }
                }
            }
            if (basType != null && basType.objVerticeTransformations != null) {
                for (meshIndex in basType.objVerticeTransformations!!.indices) {
                    if (meshIndex < meshes.size && meshes[meshIndex] != null) {
                        var translateX = 0
                        var translateY = 0
                        var translateZ = 0
                        var rotateX = 0
                        var rotateY = 0
                        var rotateZ = 0
                        if (basType.objVerticeTransformations!![meshIndex] != null) {
                            translateX = basType.objVerticeTransformations!![meshIndex]!![0]
                            translateY = basType.objVerticeTransformations!![meshIndex]!![1]
                            translateZ = basType.objVerticeTransformations!![meshIndex]!![2]
                            rotateX = basType.objVerticeTransformations!![meshIndex]!![3] shl 3
                            rotateY = basType.objVerticeTransformations!![meshIndex]!![4] shl 3
                            rotateZ = basType.objVerticeTransformations!![meshIndex]!![5] shl 3
                        }
                        if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
                            meshes[meshIndex]!!.rotateWithoutNormals(rotateX, rotateY, rotateZ)
                        }
                        if (translateX != 0 || translateY != 0 || translateZ != 0) {
                            meshes[meshIndex]!!.translate(translateX, translateY, translateZ)
                        }
                    }
                }
            }
            val mesh: Mesh
            if (meshes.size == 1) {
                mesh = meshes[0]!!
            } else {
                mesh = Mesh(meshes, meshes.size)
            }
            model = toolkit.createModel(mesh, appearanceFunctionMask, this.npcTypeList!!.preferenceFlags, this.ambient + 64, this.contrast * 5 + 850)
            if (this.recolorSrc != null) {
                val recolorDestination: ShortArray?
                if (npcCustomisation != null && npcCustomisation.recolorDst != null) {
                    recolorDestination = npcCustomisation.recolorDst
                } else {
                    recolorDestination = this.recolorDst
                }
                for (colorIndex in this.recolorSrc!!.indices) {
                    if (this.recolorDPalette != null && colorIndex < this.recolorDPalette!!.size) {
                        model!!.recolor(this.recolorSrc!![colorIndex], npcRecolorPalette[this.recolorDPalette!![colorIndex].toInt() and 0xff])
                    } else {
                        model!!.recolor(this.recolorSrc!![colorIndex], recolorDestination!![colorIndex])
                    }
                }
            }
            if (this.retextureSrc != null) {
                val retextureDestination: ShortArray?
                if (npcCustomisation != null && npcCustomisation.retextureDst != null) {
                    retextureDestination = npcCustomisation.retextureDst
                } else {
                    retextureDestination = this.retextureDst
                }
                for (textureIndex in this.retextureSrc!!.indices) {
                    model!!.rematerial(this.retextureSrc!![textureIndex], retextureDestination!![textureIndex])
                }
            }
            if (this.tintOpacity != 0.toByte()) {
                model!!.tint(this.tintHue.toInt(), this.tintSaturation.toInt(), this.tintLightness.toInt(), this.tintOpacity.toInt() and 0xff)
            }
            model!!.applyShading(displacementFunctionMask)
            synchronized(this.npcTypeList!!.bodymodelCache) {
                this.npcTypeList!!.bodymodelCache.put(model, npcHash)
            }
        }
        val copiedModel = model!!.copy(4.toByte(), displacementFunctionMask, true)
        var modifiesSubmesh = false
        if (submeshRotations != null) {
            for (index in 0 until 12) {
                if (submeshRotations[index] != -1) {
                    modifiesSubmesh = true
                    break
                }
            }
        }
        if (requiresAnimating || modifiesSubmesh) {
            var submeshMatrices: Array<Matrix43?>? = null
            if (basType != null) {
                submeshMatrices = basType.getSubmeshMatrices()
            }
            if (modifiesSubmesh && submeshMatrices != null) {
                for (index in 0 until 12) {
                    if (submeshMatrices[index] != null) {
                        copiedModel!!.offsetSubmesh(submeshMatrices[index]!!, 1 shl index, true)
                    }
                }
            }
            var seqPtr = 0
            var submesh = 1
            while (seqPtr < sequenceCount) {
                if (animatingSequence!![seqPtr] != null) {
                    animatingSequence[seqPtr]!!.animateUnrotatedModel(copiedModel!!, submesh)
                }
                seqPtr++
                submesh = submesh shl 1
            }
            if (modifiesSubmesh) {
                for (index in 0 until 12) {
                    if (submeshRotations!![index] != -1) {
                        var rotation = submeshRotations[index] - submeshRotationOffset
                        rotation = rotation and 0x3fff
                        val matrix = Matrix43()
                        matrix.rotateMatrix(0.0F, 1.0F, 0.0F, MathUtils.getTheta(rotation))
                        copiedModel!!.offsetSubmesh(matrix, 1 shl index, false)
                    }
                }
            }
            if (modifiesSubmesh && submeshMatrices != null) {
                for (index in 0 until 12) {
                    if (submeshMatrices[index] != null) {
                        copiedModel!!.offsetSubmesh(submeshMatrices[index]!!, 1 shl index, false)
                    }
                }
            }
            if (animationSequence != null && idleSequence != null) {
                Sequence.doubleAnimation(copiedModel!!, animationSequence, idleSequence)
            } else if (animationSequence != null) {
                animationSequence.animateRotatedModel(copiedModel!!, 0)
            } else if (idleSequence != null) {
                idleSequence.animateRotatedModel(copiedModel!!, 0)
            }
            if (this.scaleXZ != 128 || this.scaleY != 128) {
                copiedModel!!.scale(this.scaleXZ, this.scaleY, this.scaleXZ)
            }
            copiedModel!!.applyShading(functionMask)
        }
        return copiedModel
    }

    fun getAnimatedHeadmodel(
        toolkit: AbstractToolkit,
        initialFunctionMask: Int,
        varProvider: VarDomain,
        headSequence: Sequence?,
        npcCustomisation: NPCTypeCustomisation?
    ): Model? {
        if (this.alternativeIds != null) {
            val npctype = getMultiNpc(varProvider)
            return npctype?.getAnimatedHeadmodel(toolkit, initialFunctionMask, varProvider, headSequence, npcCustomisation)
        }
        if (this.headModels == null && (npcCustomisation == null || npcCustomisation.modelIds == null)) {
            return null
        }
        var flagsCopy = initialFunctionMask
        if (headSequence != null) {
            flagsCopy = initialFunctionMask or headSequence.getFunctionMask()
        }
        var npcHash = this.type.toLong() or (toolkit.renderId.toLong() shl 16)
        if (npcCustomisation != null) {
            npcHash = npcHash or (npcCustomisation.uid.toLong() shl 24)
        }
        var model: Model?
        synchronized(this.npcTypeList!!.headmodelCache) {
            model = this.npcTypeList!!.headmodelCache.get(npcHash) as Model?
        }
        if (model == null || (model!!.getFunctionMask() and flagsCopy) != flagsCopy) {
            if (model != null) {
                flagsCopy = flagsCopy or model!!.getFunctionMask()
            }
            var adjustmentFunctionMask = flagsCopy
            if (this.recolorSrc != null) {
                adjustmentFunctionMask = flagsCopy or RECOLOR
            }
            if (this.retextureSrc != null) {
                adjustmentFunctionMask = adjustmentFunctionMask or RETEXTURE
            }
            if (this.tintOpacity != 0.toByte()) {
                adjustmentFunctionMask = adjustmentFunctionMask or TINT
            }
            val npcHeadModelIds = if (npcCustomisation != null && npcCustomisation.modelIds != null) npcCustomisation.modelIds else this.headModels
            var missingModels = false
            synchronized(this.npcTypeList!!.modelResource) {
                var meshIndex = 0
                while (meshIndex < npcHeadModelIds!!.size) {
                    if (!this.npcTypeList!!.modelResource.isCached(npcHeadModelIds[meshIndex], 0)) {
                        missingModels = true
                    }
                    meshIndex++
                }
            }
            if (missingModels) {
                return null
            }
            val meshes = arrayOfNulls<Mesh>(npcHeadModelIds!!.size)
            synchronized(this.npcTypeList!!.modelResource) {
                for (meshIndex in npcHeadModelIds.indices) {
                    meshes[meshIndex] = Mesh.fromResource(this.npcTypeList!!.modelResource, npcHeadModelIds[meshIndex])
                }
            }
            for (meshIndex in npcHeadModelIds.indices) {
                if (meshes[meshIndex] != null && meshes[meshIndex]!!.version < 13) {
                    meshes[meshIndex]!!.upscale()
                }
            }
            val mesh: Mesh
            if (meshes.size == 1) {
                mesh = meshes[0]!!
            } else {
                mesh = Mesh(meshes, meshes.size)
            }
            model = toolkit.createModel(mesh, adjustmentFunctionMask, this.npcTypeList!!.preferenceFlags, 64, 768)
            if (this.recolorSrc != null) {
                val recolorDestination: ShortArray?
                if (npcCustomisation != null && npcCustomisation.recolorDst != null) {
                    recolorDestination = npcCustomisation.recolorDst
                } else {
                    recolorDestination = this.recolorDst
                }
                for (colorIndex in this.recolorSrc!!.indices) {
                    if (this.recolorDPalette != null && colorIndex < this.recolorDPalette!!.size) {
                        model!!.recolor(this.recolorSrc!![colorIndex], npcRecolorPalette[this.recolorDPalette!![colorIndex].toInt() and 0xff])
                    } else {
                        model!!.recolor(this.recolorSrc!![colorIndex], recolorDestination!![colorIndex])
                    }
                }
            }
            if (this.retextureSrc != null) {
                val retextureDestination: ShortArray?
                if (npcCustomisation != null && npcCustomisation.retextureDst != null) {
                    retextureDestination = npcCustomisation.retextureDst
                } else {
                    retextureDestination = this.retextureDst
                }
                for (textureIndex in this.retextureSrc!!.indices) {
                    model!!.rematerial(this.retextureSrc!![textureIndex], retextureDestination!![textureIndex])
                }
            }
            if (this.tintOpacity != 0.toByte()) {
                model!!.tint(this.tintHue.toInt(), this.tintSaturation.toInt(), this.tintLightness.toInt(), this.tintOpacity.toInt() and 0xff)
            }
            model!!.applyShading(flagsCopy)
            synchronized(this.npcTypeList!!.headmodelCache) {
                this.npcTypeList!!.headmodelCache.put(model, npcHash)
            }
        }
        if (headSequence != null) {
            model = model!!.copy(1.toByte(), flagsCopy, true)
            headSequence.animateRotatedModel(model!!, 0)
        }
        model!!.applyShading(initialFunctionMask)
        return model
    }

    fun isBodyCached(): Boolean {
        if (this.modelIds == null) {
            return true
        }
        var fullyCached = true
        val models = this.modelIds
        for (index in models!!.indices) {
            val archiveId = models[index]
            if (!this.npcTypeList!!.modelResource.isCached(archiveId, 0)) {
                fullyCached = false
            }
        }
        return fullyCached
    }

    fun getCS2Param(key: Int, defaultValue: String): String {
        if (this.parameters == null) {
            return defaultValue
        }
        val objectnode = this.parameters!!.get(key.toLong()) as ObjectNode?
        return objectnode?.value as? String ?: defaultValue
    }

    /**
     * [J]
     */
    fun getMultiNpc(vars: VarDomain): NPCType? {
        var index = -1
        if (this.varpbit != -1) {
            index = vars.getVarBitValue(this.varpbit)
        } else if (this.varp != -1) {
            index = vars.getVarValue(this.varp)
        }
        if (index >= 0 && index < this.alternativeIds!!.size - 1 && this.alternativeIds!![index] != -1) {
            return this.npcTypeList!!.list(this.alternativeIds!![index])
        }
        val morpedId = this.alternativeIds!![this.alternativeIds!!.size - 1]
        return if (morpedId != -1) this.npcTypeList!!.list(morpedId) else null
    }

    fun hasTransformations(vars: VarDomain): Boolean {
        if (this.alternativeIds == null) {
            return true
        }
        var index = -1
        if (this.varpbit != -1) {
            index = vars.getVarBitValue(this.varpbit)
        } else if (this.varp != -1) {
            index = vars.getVarValue(this.varp)
        }
        if (index >= 0 && index < this.alternativeIds!!.size - 1 && this.alternativeIds!![index] != -1) {
            return true
        }
        val morpedId = this.alternativeIds!![this.alternativeIds!!.size - 1]
        return morpedId != -1
    }

    fun hasSoundEffects(): Boolean {
        if (this.alternativeIds == null) {
            return this.walkingSoundEffect != -1 || this.idleSoundEffect != -1 || this.runningSoundEffect != -1
        }
        for (index in this.alternativeIds!!.indices) {
            if (this.alternativeIds!![index] != -1) {
                val morped = this.npcTypeList!!.list(this.alternativeIds!![index])
                if (morped.walkingSoundEffect != -1 || morped.idleSoundEffect != -1 || morped.runningSoundEffect != -1) {
                    return true
                }
            }
        }
        return false
    }

    fun decodeOpcode(stream: Packet, opcode: Int) {
        if (opcode == 1) {
            val modelCount = stream.readUByte()
            this.modelIds = IntArray(modelCount)
            for (index in 0 until modelCount) {
                this.modelIds!![index] = stream.readBigSmart()
            }
        } else if (opcode == 2) {
            this.name = stream.readString()
        } else if (opcode == 12) {
            this.size = stream.readUByte()
        } else if (opcode >= 30 && opcode < 35) {
            this.actions!![opcode - 30] = stream.readString()
        } else if (opcode == 40) {
            val recolorCount = stream.readUByte()
            this.recolorSrc = ShortArray(recolorCount)
            this.recolorDst = ShortArray(recolorCount)
            for (index in 0 until recolorCount) {
                this.recolorSrc!![index] = stream.readUShort().toShort()
                this.recolorDst!![index] = stream.readUShort().toShort()
            }
        } else if (opcode == 41) {
            val retextureCount = stream.readUByte()
            this.retextureSrc = ShortArray(retextureCount)
            this.retextureDst = ShortArray(retextureCount)
            for (index in 0 until retextureCount) {
                this.retextureSrc!![index] = stream.readUShort().toShort()
                this.retextureDst!![index] = stream.readUShort().toShort()
            }
        } else if (opcode == 42) {
            val recolorCount = stream.readUByte()
            this.recolorDPalette = ByteArray(recolorCount)
            for (index in 0 until recolorCount) {
                this.recolorDPalette!![index] = stream.readByte()
            }
        } else if (opcode == 60) {
            val headmodelCount = stream.readUByte()
            this.headModels = IntArray(headmodelCount)
            for (index in 0 until headmodelCount) {
                this.headModels!![index] = stream.readBigSmart()
            }
        } else if (opcode == 93) {
            this.displayOnMinimap = false
        } else if (opcode == 95) {
            this.combatLevel = stream.readUShort()
        } else if (opcode == 97) {
            this.scaleXZ = stream.readUShort()
        } else if (opcode == 98) {
            this.scaleY = stream.readUShort()
        } else if (opcode == 99) {
            this.highPriority = true
        } else if (opcode == 100) {
            this.ambient = stream.readByte().toInt()
        } else if (opcode == 101) {
            this.contrast = stream.readByte().toInt()
        } else if (opcode == 102) {
            this.headIcons = stream.readUShort()
        } else if (opcode == 103) {
            this.turnDirection = stream.readUShort()
        } else if (opcode == 106 || opcode == 118) {
            this.varpbit = stream.readUShort()
            if (this.varpbit == 65535) {
                this.varpbit = -1
            }
            this.varp = stream.readUShort()
            if (this.varp == 65535) {
                this.varp = -1
            }
            var lastAlt = -1
            if (opcode == 118) {
                lastAlt = stream.readUShort()
                if (lastAlt == 65535) {
                    lastAlt = -1
                }
            }
            val length = stream.readUByte()
            this.alternativeIds = IntArray(length + 2)
            for (index in 0..length) {
                this.alternativeIds!![index] = stream.readUShort()
                if (this.alternativeIds!![index] == 65535) {
                    this.alternativeIds!![index] = -1
                }
            }
            this.alternativeIds!![length + 1] = lastAlt
        } else if (opcode == 107) {
            this.visible = false
        } else if (opcode == 109) {
            this.delayMovement = false
        } else if (opcode == 111) {
            this.shadowed = false
        } else if (opcode == 113) {
            this.shadowColorSrc = stream.readUShort().toShort()
            this.shadowColorDst = stream.readUShort().toShort()
        } else if (opcode == 114) {
            this.shadowAlphaSrc = stream.readByte()
            this.shadowAlphaDst = stream.readByte()
        } else if (opcode == 119) {
            this.walkingFlags = stream.readByte()
        } else if (opcode == 121) {
            this.modelOffsets = arrayOfNulls<IntArray>(this.modelIds!!.size)
            val numOffsetAdjustments = stream.readUByte()
            for (index in 0 until numOffsetAdjustments) {
                val model = stream.readUByte()
                val translate = IntArray(3)
                this.modelOffsets!![model] = translate
                translate[0] = stream.readByte().toInt()
                translate[1] = stream.readByte().toInt()
                translate[2] = stream.readByte().toInt()
            }
        } else if (opcode == 123) {
            this.iconHeight = stream.readUShort()
        } else if (opcode == 125) {
            this.respawnDirection = Identifiable.identify(NPCDirection.values(), stream.readByte().toInt()) as NPCDirection?
        } else if (opcode == 127) {
            this.npcBasId = stream.readUShort()
        } else if (opcode == 128) {
            Identifiable.identify(MovementType.values(), stream.readUByte())
        } else if (opcode == 134) {
            this.walkingSoundEffect = stream.readUShort()
            if (this.walkingSoundEffect == 65535) {
                this.walkingSoundEffect = -1
            }
            this.teleportSoundEffect = stream.readUShort()
            if (this.teleportSoundEffect == 65535) {
                this.teleportSoundEffect = -1
            }
            this.idleSoundEffect = stream.readUShort()
            if (this.idleSoundEffect == 65535) {
                this.idleSoundEffect = -1
            }
            this.runningSoundEffect = stream.readUShort()
            if (this.runningSoundEffect == 65535) {
                this.runningSoundEffect = -1
            }
            this.ambientSoundMinHearDistance = stream.readUByte()
        } else if (opcode == 135) {
            this.primaryCursorActionIndex = stream.readUByte()
            this.primaryCursor = stream.readUShort()
        } else if (opcode == 136) {
            this.secondaryCursorActionIndex = stream.readUByte()
            this.secondaryCursor = stream.readUShort()
        } else if (opcode == 137) {
            this.attackCursor = stream.readUShort()
        } else if (opcode == 138) {
            this.overheadSprite = stream.readBigSmart()
        } else if (opcode == 140) {
            this.ambientSoundVolume = stream.readUByte()
        } else if (opcode == 141) {
            this.lowPriority = true
        } else if (opcode == 142) {
            this.mecId = stream.readUShort()
        } else if (opcode == 143) {
            this.mediumPriority = true
        } else if (opcode >= 150 && opcode < 155) {
            this.actions!![opcode - 150] = stream.readString()
            if (!this.npcTypeList!!.isMember) {
                this.actions!![opcode - 150] = null
            }
        } else if (opcode == 155) {
            this.tintHue = stream.readByte()
            this.tintSaturation = stream.readByte()
            this.tintLightness = stream.readByte()
            this.tintOpacity = stream.readByte()
        } else if (opcode == 158) {
            this.gameType = 1
        } else if (opcode == 159) {
            this.gameType = 0
        } else if (opcode == 160) {
            val i_4 = stream.readUByte()
            this.quests = IntArray(i_4)
            for (i_5 in 0 until i_4) {
                this.quests!![i_5] = stream.readUShort()
            }
        } else if (opcode == 162) {
            this.instrumentSoundEffect = true
        } else if (opcode == 163) {
            this.shadowSize = stream.readUByte()
        } else if (opcode == 164) {
            this.ambientSoundMaxDelay = stream.readUShort()
            this.ambientSoundMinDelay = stream.readUShort()
        } else if (opcode == 165) {
            this.sizeShift = stream.readUByte()
        } else if (opcode == 168) {
            this.ambientSoundMaxHearDistance = stream.readUByte()
        } else if (opcode == 169) {
            this.hasTint = false
        } else if (opcode == 249) {
            val paramCount = stream.readUByte()
            if (this.parameters == null) {
                val capacity = MathUtils.nextPowerOfTwo(paramCount)
                this.parameters = IterableNodeMap(capacity)
            }
            for (index in 0 until paramCount) {
                val isString = stream.readUByte() == 1
                val key = stream.readUMedium()
                val parameter: Node<*>
                if (isString) {
                    parameter = ObjectNode(stream.readString())
                } else {
                    parameter = IntNode(stream.readInt())
                }
                this.parameters!!.put(parameter, key.toLong())
            }
        }
    }

    companion object {
        @JvmField
        var npcRecolorPalette: ShortArray = ShortArray(256)
    }
}
