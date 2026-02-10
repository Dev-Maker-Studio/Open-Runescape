package com.jagex.game.runetek5.config.item

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RETEXTURE
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RECOLOR
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_X
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_Y
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_Z
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.HAS_TEXTURE_COORDS
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.FULL_DISPLACEMENT
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISABLE_TEXTURES

/**
 *
 * @author Steven
 *
 * [NXT]
	[X] jag::game::ObjType::DecodeType(jag::Packet &,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  0000000000458610 00001783 000001C8 00000000 R . . . . T .
	jag::game::ObjType::GetAppearance(bool,jag::shared_ptr<jag::game::ObjTypeCustomisation> const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  00000000000F1160 0000009A                   R . . . . . .
	jag::game::ObjType::GetModel(jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::shared_ptr<jag::game::PlayerModel> const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  0000000000516590 000005B1 000000E8 00000080 R . . . . . .
	[X]jag::game::ObjType::GetStackSizeAlt(int)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              .text  00000000000F1040 0000011A                   R . . . . T .
	[-]jag::game::ObjType::ObjType(jag::game::ConfigTypeListPreloadPackedContainer<jag::game::ObjType> *,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  00000000003953B0 00000523 00000038 00000000 R . . . . . .
	[X]jag::game::ObjType::PostDecode(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .text  00000000000A1DE0 00000002                   R . . . . T .
	jag::game::ObjType::SetupDerivedObject(jag::game::DerivedObjType,jag::shared_ptr<jag::game::ObjType> const&,jag::shared_ptr<jag::game::ObjType> const&,char const*)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   .text  00000000003912A0 00000507 00000048 00000000 R . . . . . .

 *
 */
open class ItemType {

    @JvmField
    var value: Int = 1
    @JvmField
    var modelZoom: Int = 2000
    @JvmField
    var modelRotationX: Int = 0
    @JvmField
    var modelRotationY: Int = 0
    @JvmField
    var modelRotationZ: Int = 0
    @JvmField
    var modelOffsetX: Int = 0
    @JvmField
    var modelOffsetY: Int = 0
    @JvmField
    var name: String = "null"
    @JvmField
    var membersOnly: Boolean = false
    @JvmField
    var wearPos: Int = -1
    @JvmField
    var wearPos2: Int = -1
    @JvmField
    var wearPos3: Int = -1
    @JvmField
    var teamId: Int = 0
    @JvmField
    var groundActions: Array<String?>? = null
    @JvmField
    var stackable: Int = 0
    @JvmField
    var inventoryActions: Array<String?>? = null
    @JvmField
    var multiStackSize: Int = -1
    @JvmField
    var tooltipColor: Int = 0
    @JvmField
    var hasTooltipColor: Boolean = false
    @JvmField
    var tradeable: Boolean = false
    @JvmField
    var unknownInt6: Int = 0
    @JvmField
    var certId: Int = -1
    @JvmField
    var certTemplateId: Int = -1
    @JvmField
    var lendTemplateId: Int = -1
    @JvmField
    var primaryCursorActionIndex: Int = -1
    @JvmField
    var primaryCursor: Int = -1
    @JvmField
    var secondaryCursorActionIndex: Int = -1
    @JvmField
    var secondaryCursor: Int = -1
    @JvmField
    var customCursorOp1: Int = -1
    @JvmField
    var customCursorId1: Int = -1
    @JvmField
    var customCursorOp2: Int = -1
    @JvmField
    var customCursorId2: Int = -1
    @JvmField
    var quests: IntArray? = null
    @JvmField
    var pickSizeShift: Int = 0
    @JvmField
    var bindTemplateId: Int = -1
    @JvmField
    var typeList: ItemTypeList? = null
    @JvmField
    var id: Int = 0
    @JvmField
    var modelId: Int = 0
    @JvmField
    var recolorSrc: ShortArray? = null
    @JvmField
    var recolorDst: ShortArray? = null
    @JvmField
    var recolorDPalette: ByteArray? = null
    @JvmField
    var retexSrc: ShortArray? = null
    @JvmField
    var retexDst: ShortArray? = null
    @JvmField
    var maleEquip1: Int = -1
    @JvmField
    var maleEquip2: Int = -1
    @JvmField
    var maleEquip3: Int = -1
    @JvmField
    var femaleEquip1: Int = -1
    @JvmField
    var femaleEquip2: Int = -1
    @JvmField
    var femaleEquip3: Int = -1
    @JvmField
    var maleEquipmentOffsetX: Int = 0
    @JvmField
    var femaleEquipmentOffsetX: Int = 0
    @JvmField
    var maleEquipmentOffsetY: Int = 0
    @JvmField
    var femaleEquipmentOffsetY: Int = 0
    @JvmField
    var maleEquipmentOffsetZ: Int = 0
    @JvmField
    var femaleEquipmentOffsetZ: Int = 0
    @JvmField
    var maleHead1: Int = -1
    @JvmField
    var maleHead2: Int = -1
    @JvmField
    var femaleHead1: Int = -1
    @JvmField
    var femaleHead2: Int = -1
    @JvmField
    var parameters: IterableNodeMap<Node<*>>? = null
    @JvmField
    var resizeX: Int = 128
    @JvmField
    var stackIds: IntArray? = null
    @JvmField
    var stackTriggerAmount: IntArray? = null
    @JvmField
    var resizeY: Int = 128
    @JvmField
    var resizeZ: Int = 128
    @JvmField
    var ambient: Int = 0
    @JvmField
    var contrast: Int = 0
    @JvmField
    var lendId: Int = -1
    @JvmField
    var bindId: Int = -1

    /**
     * ***********************************************************|
     * 							Decode
     * ***********************************************************|
     */

    /**
     * [J]
     */
    fun decodeType(stream: Packet) {
        while (true) {
            val opcode = stream.readUByte()
            if (opcode == 0) {
                return
            }
            decodeOpcode(stream, opcode)
        }
    }

    fun decodeOpcode(stream: Packet, opcode: Int) {
        when {
            opcode == 1 -> this.modelId = stream.readBigSmart()
            opcode == 2 -> this.name = stream.readString()
            opcode == 4 -> this.modelZoom = stream.readUShort()
            opcode == 5 -> this.modelRotationX = stream.readUShort()
            opcode == 6 -> this.modelRotationY = stream.readUShort()
            opcode == 7 -> {
                this.modelOffsetX = stream.readUShort()
                if (this.modelOffsetX > 32767) {
                    this.modelOffsetX -= 65536
                }
            }
            opcode == 8 -> {
                this.modelOffsetY = stream.readUShort()
                if (this.modelOffsetY > 32767) {
                    this.modelOffsetY -= 65536
                }
            }
            opcode == 11 -> this.stackable = 1
            opcode == 12 -> this.value = stream.readInt()
            opcode == 13 -> this.wearPos = stream.readUByte()
            opcode == 14 -> this.wearPos2 = stream.readUByte()
            opcode == 16 -> this.membersOnly = true
            opcode == 18 -> this.multiStackSize = stream.readUShort()
            opcode == 23 -> this.maleEquip1 = stream.readBigSmart()
            opcode == 24 -> this.maleEquip2 = stream.readBigSmart()
            opcode == 25 -> this.femaleEquip1 = stream.readBigSmart()
            opcode == 26 -> this.femaleEquip2 = stream.readBigSmart()
            opcode == 27 -> this.wearPos3 = stream.readUByte()
            opcode in 30..34 -> this.groundActions!![opcode - 30] = stream.readString()
            opcode in 35..39 -> this.inventoryActions!![opcode - 35] = stream.readString()
            opcode == 40 -> {
                val count = stream.readUByte()
                this.recolorSrc = ShortArray(count)
                this.recolorDst = ShortArray(count)
                for (index in 0 until count) {
                    this.recolorSrc!![index] = stream.readUShort().toShort()
                    this.recolorDst!![index] = stream.readUShort().toShort()
                }
            }
            opcode == 41 -> {
                val count = stream.readUByte()
                this.retexSrc = ShortArray(count)
                this.retexDst = ShortArray(count)
                for (index in 0 until count) {
                    this.retexSrc!![index] = stream.readUShort().toShort()
                    this.retexDst!![index] = stream.readUShort().toShort()
                }
            }
            opcode == 42 -> {
                val count = stream.readUByte()
                this.recolorDPalette = ByteArray(count)
                for (index in 0 until count) {
                    this.recolorDPalette!![index] = stream.readByte()
                }
            }
            opcode == 43 -> {
                this.tooltipColor = stream.readInt()
                this.hasTooltipColor = true
            }
            opcode == 65 -> this.tradeable = true
            opcode == 78 -> this.maleEquip3 = stream.readBigSmart()
            opcode == 79 -> this.femaleEquip3 = stream.readBigSmart()
            opcode == 90 -> this.maleHead1 = stream.readBigSmart()
            opcode == 91 -> this.femaleHead1 = stream.readBigSmart()
            opcode == 92 -> this.maleHead2 = stream.readBigSmart()
            opcode == 93 -> this.femaleHead2 = stream.readBigSmart()
            opcode == 95 -> this.modelRotationZ = stream.readUShort()
            opcode == 96 -> this.unknownInt6 = stream.readUByte()
            opcode == 97 -> this.certId = stream.readUShort()
            opcode == 98 -> this.certTemplateId = stream.readUShort()
            opcode in 100..109 -> {
                if (this.stackIds == null) {
                    this.stackIds = IntArray(10)
                    this.stackTriggerAmount = IntArray(10)
                }
                this.stackIds!![opcode - 100] = stream.readUShort()
                this.stackTriggerAmount!![opcode - 100] = stream.readUShort()
            }
            opcode == 110 -> this.resizeX = stream.readUShort()
            opcode == 111 -> this.resizeY = stream.readUShort()
            opcode == 112 -> this.resizeZ = stream.readUShort()
            opcode == 113 -> this.ambient = stream.readByte().toInt()
            opcode == 114 -> this.contrast = stream.readByte().toInt()
            opcode == 115 -> this.teamId = stream.readUByte()
            opcode == 121 -> this.lendId = stream.readUShort()
            opcode == 122 -> this.lendTemplateId = stream.readUShort()
            opcode == 125 -> {
                this.maleEquipmentOffsetX = stream.readByte().toInt() shl 2
                this.maleEquipmentOffsetY = stream.readByte().toInt() shl 2
                this.maleEquipmentOffsetZ = stream.readByte().toInt() shl 2
            }
            opcode == 126 -> {
                this.femaleEquipmentOffsetX = stream.readByte().toInt() shl 2
                this.femaleEquipmentOffsetY = stream.readByte().toInt() shl 2
                this.femaleEquipmentOffsetZ = stream.readByte().toInt() shl 2
            }
            opcode == 127 -> {
                this.primaryCursorActionIndex = stream.readUByte()
                this.primaryCursor = stream.readUShort()
            }
            opcode == 128 -> {
                this.secondaryCursorActionIndex = stream.readUByte()
                this.secondaryCursor = stream.readUShort()
            }
            opcode == 129 -> {
                this.customCursorOp1 = stream.readUByte()
                this.customCursorId1 = stream.readUShort()
            }
            opcode == 130 -> {
                this.customCursorOp2 = stream.readUByte()
                this.customCursorId2 = stream.readUShort()
            }
            opcode == 132 -> {
                val count = stream.readUByte()
                this.quests = IntArray(count)
                for (index in 0 until count) {
                    this.quests!![index] = stream.readUShort()
                }
            }
            opcode == 134 -> this.pickSizeShift = stream.readUByte()
            opcode == 139 -> this.bindId = stream.readUShort()
            opcode == 140 -> this.bindTemplateId = stream.readUShort()
            opcode == 249 -> {
                val count = stream.readUByte()
                if (this.parameters == null) {
                    val index = MathUtils.nextPowerOfTwo(count)
                    this.parameters = IterableNodeMap<Node<*>>(index)
                }
                for (index in 0 until count) {
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
    }

    fun postDecode() {
        // NOOP
    }

    fun getAnimatedModel(toolkit: AbstractToolkit, initialFunctionMask: Int, stackSize: Int, playerAppearance: PlayerAppearance?, animatingSequence: Sequence?, tintHue: Int, tintSaturation: Int, tintLightness: Int, tintOpacity: Int): Model? {
        if (this.stackIds != null && stackSize > 1) {
            var itemId = -1
            for (stackIndex in 0 until 10) {
                if (stackSize >= this.stackTriggerAmount!![stackIndex] && this.stackTriggerAmount!![stackIndex] != 0) {
                    itemId = this.stackIds!![stackIndex]
                }
            }
            if (itemId != -1) {
                return this.typeList!!.list(itemId).getAnimatedModel(toolkit, initialFunctionMask, 1, playerAppearance, animatingSequence, tintHue, tintSaturation, tintLightness, tintOpacity)
            }
        }
        var copiedMask = initialFunctionMask
        if (animatingSequence != null) {
            copiedMask = initialFunctionMask or animatingSequence.getFunctionMask()
        }
        var model: Model?
        synchronized(this.typeList!!.modelCache) {
            model = this.typeList!!.modelCache.get(this.id.toLong() or (toolkit.renderId.toLong() shl 29)) as Model?
        }
        if (model == null || toolkit.mismatchingFunctionMask(model!!.getFunctionMask(), copiedMask) != 0) {
            if (model != null) {
                copiedMask = toolkit.mergeFunctionMasks(copiedMask, model!!.getFunctionMask())
            }
            var adjustmentFunctionMask = copiedMask
            if (this.retexSrc != null) {
                adjustmentFunctionMask = copiedMask or RETEXTURE
            }
            if (this.recolorSrc != null || playerAppearance != null) {
                adjustmentFunctionMask = adjustmentFunctionMask or RECOLOR
            }
            if (this.resizeX != 128) {
                adjustmentFunctionMask = adjustmentFunctionMask or DISPLACE_X
            }
            if (this.resizeX != 128) {
                adjustmentFunctionMask = adjustmentFunctionMask or DISPLACE_Y
            }
            if (this.resizeX != 128) {
                adjustmentFunctionMask = adjustmentFunctionMask or DISPLACE_Z
            }
            val mesh = Mesh.fromResource(this.typeList!!.meshResource, this.modelId)
            if (mesh == null) {
                return null
            }
            if (mesh.version < 13) {
                mesh.upscale()
            }
            model = toolkit.createModel(mesh, adjustmentFunctionMask, this.typeList!!.preferenceFlags, this.ambient + 64, this.contrast * 5 + 850)
            if (this.resizeX != 128 || this.resizeY != 128 || this.resizeZ != 128) {
                model!!.scale(this.resizeX, this.resizeY, this.resizeZ)
            }
            if (this.recolorSrc != null) {
                for (recolorIndex in this.recolorSrc!!.indices) {
                    if (this.recolorDPalette != null && recolorIndex < this.recolorDPalette!!.size) {
                        model!!.recolor(this.recolorSrc!![recolorIndex], recolorPalette[this.recolorDPalette!![recolorIndex].toInt() and 0xff])
                    } else {
                        model!!.recolor(this.recolorSrc!![recolorIndex], this.recolorDst!![recolorIndex])
                    }
                }
            }
            if (this.retexSrc != null) {
                for (rextureIndex in this.retexSrc!!.indices) {
                    model!!.rematerial(this.retexSrc!![rextureIndex], this.retexDst!![rextureIndex])
                }
            }
            if (playerAppearance != null) {
                for (paletteIndex in 0 until 10) {
                    for (colorIndex in 0 until PlayerAppearance.recolorPaletteSrc!![paletteIndex]!!.size) {
                        if (playerAppearance.recolorDstIndices!![paletteIndex] < PlayerAppearance.recolorPaletteDst!![paletteIndex]!![colorIndex].size) {
                            model!!.recolor(PlayerAppearance.recolorPaletteSrc!![paletteIndex]!![colorIndex], PlayerAppearance.recolorPaletteDst!![paletteIndex]!![colorIndex][playerAppearance.recolorDstIndices!![paletteIndex]])
                        }
                    }
                }
            }
            model!!.applyShading(copiedMask)
            synchronized(this.typeList!!.modelCache) {
                this.typeList!!.modelCache.put(model, this.id.toLong() or (toolkit.renderId.toLong() shl 29))
            }
        }
        if (animatingSequence != null || tintOpacity != 0) {
            model = model!!.copy(1.toByte(), copiedMask, true)
            if (animatingSequence != null) {
                animatingSequence.animateRotatedModel(model!!, 0)
            }
            if (tintOpacity != 0) {
                model!!.tint(tintHue, tintSaturation, tintLightness, tintOpacity)
            }
        }
        model!!.applyShading(initialFunctionMask)
        return model
    }

    fun getSpritePixels(mainToolkit: AbstractToolkit, itemToolkit: AbstractToolkit, amount: Int, outlineSize: Int, shadowColor: Int, zoomedIn: Boolean, renderAmounts: Component.StackMode, fontRenderer: AbstractFontRenderer, model: PlayerAppearance?): IntArray? {
        val inventoryMesh = Mesh.fromResource(this.typeList!!.meshResource, this.modelId)
        if (inventoryMesh == null) {
            return null
        }
        if (inventoryMesh.version < 13) {
            inventoryMesh.upscale()
        }
        if (this.recolorSrc != null) {
            for (recolorIndex in this.recolorSrc!!.indices) {
                if (this.recolorDPalette != null && recolorIndex < this.recolorDPalette!!.size) {
                    inventoryMesh.recolor(this.recolorSrc!![recolorIndex], recolorPalette[this.recolorDPalette!![recolorIndex].toInt() and 0xff])
                } else {
                    inventoryMesh.recolor(this.recolorSrc!![recolorIndex], this.recolorDst!![recolorIndex])
                }
            }
        }
        if (this.retexSrc != null) {
            for (rextureIndex in this.retexSrc!!.indices) {
                inventoryMesh.retexture(this.retexSrc!![rextureIndex], this.retexDst!![rextureIndex])
            }
        }
        if (model != null) {
            for (paletteIndex in 0 until 10) {
                for (colorIndex in 0 until PlayerAppearance.recolorPaletteSrc!![paletteIndex]!!.size) {
                    if (model.recolorDstIndices!![paletteIndex] < PlayerAppearance.recolorPaletteDst!![paletteIndex]!![colorIndex].size) {
                        inventoryMesh.recolor(PlayerAppearance.recolorPaletteSrc!![paletteIndex]!![colorIndex], PlayerAppearance.recolorPaletteDst!![paletteIndex]!![colorIndex][model.recolorDstIndices!![paletteIndex]])
                    }
                }
            }
        }
        var adjustmentFunctionMask = HAS_TEXTURE_COORDS
        var hasScaling = false
        if (this.resizeX != 128 || this.resizeY != 128 || this.resizeZ != 128) {
            hasScaling = true
            adjustmentFunctionMask = adjustmentFunctionMask or FULL_DISPLACEMENT
        }
        val inventoryModel = mainToolkit.createModel(inventoryMesh, adjustmentFunctionMask, DISABLE_TEXTURES, this.ambient + 64, this.contrast * 5 + 768)
        if (!inventoryModel.areTexturesCached()) {
            return null
        }
        if (hasScaling) {
            inventoryModel.scale(this.resizeX, this.resizeY, this.resizeZ)
        }
        var sprite: NativeSprite? = null
        if (this.certTemplateId != -1) {
            sprite = this.typeList!!.getSprite(mainToolkit, itemToolkit, this.certId, 10, 1, 0, true, true, Component.StackMode.NONUM, fontRenderer, model)
            if (sprite == null) {
                return null
            }
        } else if (this.lendTemplateId != -1) {
            sprite = this.typeList!!.getSprite(mainToolkit, itemToolkit, this.lendId, amount, outlineSize, shadowColor, false, true, Component.StackMode.NONUM, fontRenderer, model)
            if (sprite == null) {
                return null
            }
        } else if (this.bindTemplateId != -1) {
            sprite = this.typeList!!.getSprite(mainToolkit, itemToolkit, this.bindId, amount, outlineSize, shadowColor, false, true, Component.StackMode.NONUM, fontRenderer, model)
            if (sprite == null) {
                return null
            }
        }
        val zoom: Int = when {
            zoomedIn -> (1.5 * this.modelZoom).toInt() shl 2
            outlineSize == 2 -> (this.modelZoom * 1.04).toInt() shl 2
            else -> this.modelZoom shl 2
        }
        val modelMatrix = mainToolkit.copyModelMatrix()
        val matrix4418 = mainToolkit.getScratchMatrix()
        matrix4418.method6531(16.0f, 16.0f, 512.0f, 512.0f, 50.0f, 2.14748365e9f, mainToolkit.getDisplay()!!.getWidth().toFloat(), mainToolkit.getDisplay()!!.getHeight().toFloat())
        mainToolkit.setModelMatrix(matrix4418)
        mainToolkit.setDrawableArea(0, 0, mainToolkit.getDisplay()!!.getWidth(), mainToolkit.getDisplay()!!.getHeight())
        val sunPosition = Matrix43()
        mainToolkit.setSunPosition(sunPosition)
        mainToolkit.updateSun(0.95f + (Math.random() / 10.0).toFloat())
        mainToolkit.applyLighting(16777215, 0.95f + (Math.random() / 10.0).toFloat(), 0.95f + (Math.random() / 10.0).toFloat(), -50.0f, -10.0f, -50.0f)
        val itemMatrix = mainToolkit.getRenderingMatrix()
        itemMatrix.rotateMatrix(0.0f, 0.0f, 1.0f, MathUtils.getTheta(-this.modelRotationZ shl 3))
        itemMatrix.rotate(0.0f, 1.0f, 0.0f, MathUtils.getTheta(this.modelRotationY shl 3))
        itemMatrix.translate(
            (this.modelOffsetX shl 2).toFloat(),
            ((zoom * MathUtils.SINE[this.modelRotationX shl 3] shr 14) - inventoryModel.getMinY() / 2 + (this.modelOffsetY shl 2)).toFloat(),
            ((zoom * MathUtils.COSINE[this.modelRotationX shl 3] shr 14) + (this.modelOffsetY shl 2)).toFloat()
        )
        itemMatrix.rotate(1.0f, 0.0f, 0.0f, MathUtils.getTheta(this.modelRotationX shl 3))
        mainToolkit.setClipping(0, 0, 36, 32)
        mainToolkit.applyClearFlags(2, 0)
        mainToolkit.drawFilledRectangle(0, 0, 36, 32, 0, 0)
        mainToolkit.calculateFog(0, -1, 0)
        inventoryModel.drawMatrixThreaded(itemMatrix, null, ModelFunctionMaskGL.SMOOTH_EDGES)
        mainToolkit.setModelMatrix(modelMatrix)
        var pixels = mainToolkit.copyPixelRectangle(0, 0, 36, 32)
        if (outlineSize >= 1) {
            pixels = outline(pixels, -16777214)
            if (outlineSize >= 2) {
                pixels = outline(pixels, -1)
            }
        }
        if (shadowColor != 0) {
            shadow(pixels, shadowColor)
        }
        if (this.lendTemplateId != -1) {
            sprite!!.draw(0, 0)
        } else if (this.bindTemplateId != -1) {
            sprite!!.draw(0, 0)
        }
        mainToolkit.createNativeSprite(pixels, 36, 36, 32).draw(0, 0)
        if (this.certTemplateId != -1) {
            sprite!!.draw(0, 0)
        }
        if (renderAmounts == Component.StackMode.ALWAYS_NUM || renderAmounts == Component.StackMode.WHEN_MULTIPLE && (this.stackable == 1 || amount != 1) && amount != -1) {
            fontRenderer.renderPlain(TextUtils.formatAmount(amount, this.typeList!!.language), 0, 9, -256, -16777215)
        }
        pixels = mainToolkit.copyPixelRectangle(0, 0, 36, 32)
        for (pixelIndex in pixels.indices) {
            if ((pixels[pixelIndex] and 0xffffff) == 0) {
                pixels[pixelIndex] = 0
            } else {
                pixels[pixelIndex] = pixels[pixelIndex] or (-0xffffff - 1)
            }
        }
        return pixels
    }

    fun isBodyCached(female: Boolean, customisation: ItemTypeCustomisation?): Boolean {
        val primaryModel: Int
        val secondaryModel: Int
        val tertiaryModel: Int
        if (female) {
            if (customisation != null) {
                primaryModel = customisation.femaleBody[0]
                secondaryModel = customisation.femaleBody[1]
                tertiaryModel = customisation.femaleBody[2]
            } else {
                primaryModel = this.femaleEquip1
                secondaryModel = this.femaleEquip2
                tertiaryModel = this.femaleEquip3
            }
        } else if (customisation != null) {
            primaryModel = customisation.maleBody[0]
            secondaryModel = customisation.maleBody[1]
            tertiaryModel = customisation.maleBody[2]
        } else {
            primaryModel = this.maleEquip1
            secondaryModel = this.maleEquip2
            tertiaryModel = this.maleEquip3
        }
        if (primaryModel == -1) {
            return true
        }
        var fullyCached = this.typeList!!.meshResource.isCached(primaryModel, 0)
        if (secondaryModel != -1 && !this.typeList!!.meshResource.isCached(secondaryModel, 0)) {
            fullyCached = false
        }
        if (tertiaryModel != -1 && !this.typeList!!.meshResource.isCached(tertiaryModel, 0)) {
            fullyCached = false
        }
        return fullyCached
    }

    fun getBodyMesh(female: Boolean, meshModifier: ItemTypeCustomisation?): Mesh? {
        val primaryModel: Int
        val secondaryModel: Int
        val tertiaryModel: Int
        if (female) {
            if (meshModifier != null) {
                primaryModel = meshModifier.femaleBody[0]
                secondaryModel = meshModifier.femaleBody[1]
                tertiaryModel = meshModifier.femaleBody[2]
            } else {
                primaryModel = this.femaleEquip1
                secondaryModel = this.femaleEquip2
                tertiaryModel = this.femaleEquip3
            }
        } else if (meshModifier != null) {
            primaryModel = meshModifier.maleBody[0]
            secondaryModel = meshModifier.maleBody[1]
            tertiaryModel = meshModifier.maleBody[2]
        } else {
            primaryModel = this.maleEquip1
            secondaryModel = this.maleEquip2
            tertiaryModel = this.maleEquip3
        }
        if (primaryModel == -1) {
            return null
        }
        var primaryMesh = Mesh.fromResource(this.typeList!!.meshResource, primaryModel)
        if (primaryMesh == null) {
            return null
        }
        if (primaryMesh.version < 13) {
            primaryMesh.upscale()
        }
        if (secondaryModel != -1) {
            val secondaryMesh = Mesh.fromResource(this.typeList!!.meshResource, secondaryModel)
            if (secondaryMesh!!.version < 13) {
                secondaryMesh.upscale()
            }
            if (tertiaryModel != -1) {
                val tertiaryMesh = Mesh.fromResource(this.typeList!!.meshResource, tertiaryModel)
                if (tertiaryMesh!!.version < 13) {
                    tertiaryMesh.upscale()
                }
                val meshesToCombine = arrayOf<Mesh?>(primaryMesh, secondaryMesh, tertiaryMesh)
                primaryMesh = Mesh(meshesToCombine, 3)
            } else {
                val meshesToCombine = arrayOf<Mesh?>(primaryMesh, secondaryMesh)
                primaryMesh = Mesh(meshesToCombine, 2)
            }
        }
        if (!female && (this.maleEquipmentOffsetX != 0 || this.maleEquipmentOffsetY != 0 || this.maleEquipmentOffsetZ != 0)) {
            primaryMesh.translate(this.maleEquipmentOffsetX, this.maleEquipmentOffsetY, this.maleEquipmentOffsetZ)
        }
        if (female && (this.femaleEquipmentOffsetX != 0 || this.femaleEquipmentOffsetY != 0 || this.femaleEquipmentOffsetZ != 0)) {
            primaryMesh.translate(this.femaleEquipmentOffsetX, this.femaleEquipmentOffsetY, this.femaleEquipmentOffsetZ)
        }
        val rextureDestination: ShortArray?
        if (this.recolorSrc != null) {
            if (meshModifier != null && meshModifier.recolDst != null) {
                rextureDestination = meshModifier.recolDst
            } else {
                rextureDestination = this.recolorDst
            }
            for (recolorIndex in this.recolorSrc!!.indices) {
                primaryMesh.recolor(this.recolorSrc!![recolorIndex], rextureDestination!![recolorIndex])
            }
        }
        if (this.retexSrc != null) {
            val rextureDestination2: ShortArray?
            if (meshModifier != null && meshModifier.retexDst != null) {
                rextureDestination2 = meshModifier.retexDst
            } else {
                rextureDestination2 = this.retexDst
            }
            for (retextureIndex in this.retexSrc!!.indices) {
                primaryMesh.retexture(this.retexSrc!![retextureIndex], rextureDestination2!![retextureIndex])
            }
        }
        return primaryMesh
    }

    fun isHeadCached(bool1: Boolean, meshModifier: ItemTypeCustomisation?): Boolean {
        val primaryHeadModel: Int
        val secondaryHeadModel: Int
        if (bool1) {
            if (meshModifier != null) {
                primaryHeadModel = meshModifier.femaleHead[0]
                secondaryHeadModel = meshModifier.femaleHead[1]
            } else {
                primaryHeadModel = this.femaleHead1
                secondaryHeadModel = this.femaleHead2
            }
        } else if (meshModifier != null) {
            primaryHeadModel = meshModifier.maleHead[0]
            secondaryHeadModel = meshModifier.maleHead[1]
        } else {
            primaryHeadModel = this.maleHead1
            secondaryHeadModel = this.maleHead2
        }
        if (primaryHeadModel == -1) {
            return true
        }
        var fullyCached = this.typeList!!.meshResource.isCached(primaryHeadModel, 0)
        if (secondaryHeadModel != -1 && !this.typeList!!.meshResource.isCached(secondaryHeadModel, 0)) {
            fullyCached = false
        }
        return fullyCached
    }

    fun getHeadMesh(isFemale: Boolean, meshModifier: ItemTypeCustomisation?): Mesh? {
        val primaryHeadModel: Int
        val secondaryHealModel: Int
        if (isFemale) {
            if (meshModifier != null) {
                primaryHeadModel = meshModifier.femaleHead[0]
                secondaryHealModel = meshModifier.femaleHead[1]
            } else {
                primaryHeadModel = this.femaleHead1
                secondaryHealModel = this.femaleHead2
            }
        } else if (meshModifier != null) {
            primaryHeadModel = meshModifier.maleHead[0]
            secondaryHealModel = meshModifier.maleHead[1]
        } else {
            primaryHeadModel = this.maleHead1
            secondaryHealModel = this.maleHead2
        }
        if (primaryHeadModel == -1) {
            return null
        }
        var primaryHeadMesh = Mesh.fromResource(this.typeList!!.meshResource, primaryHeadModel)
        if (primaryHeadMesh!!.version < 13) {
            primaryHeadMesh.upscale()
        }
        if (secondaryHealModel != -1) {
            val secondaryHeadMesh = Mesh.fromResource(this.typeList!!.meshResource, secondaryHealModel)
            if (secondaryHeadMesh!!.version < 13) {
                secondaryHeadMesh.upscale()
            }
            val meshesToCombine = arrayOf<Mesh?>(primaryHeadMesh, secondaryHeadMesh)
            primaryHeadMesh = Mesh(meshesToCombine, 2)
        }
        if (this.recolorSrc != null) {
            val recolorDestination: ShortArray?
            if (meshModifier != null && meshModifier.recolDst != null) {
                recolorDestination = meshModifier.recolDst
            } else {
                recolorDestination = this.recolorDst
            }
            for (recolorIndex in this.recolorSrc!!.indices) {
                primaryHeadMesh.recolor(this.recolorSrc!![recolorIndex], recolorDestination!![recolorIndex])
            }
        }
        if (this.retexSrc != null) {
            val rextureDestination: ShortArray?
            if (meshModifier != null && meshModifier.retexDst != null) {
                rextureDestination = meshModifier.retexDst
            } else {
                rextureDestination = this.retexDst
            }
            for (rextureIndex in this.retexSrc!!.indices) {
                primaryHeadMesh.retexture(this.retexSrc!![rextureIndex], rextureDestination!![rextureIndex])
            }
        }
        return primaryHeadMesh
    }

    /**
     * [J]
     */
    fun getStackSizeAlt(stackSize: Int): ItemType {
        if (this.stackIds != null && stackSize > 1) {
            var stackId = -1
            for (stackIndex in 0 until 10) {
                if (stackSize >= this.stackTriggerAmount!![stackIndex] && this.stackTriggerAmount!![stackIndex] != 0) {
                    stackId = this.stackIds!![stackIndex]
                }
            }
            if (stackId != -1) {
                return this.typeList!!.list(stackId)
            }
        }
        return this
    }

    fun generateLend(template: ItemType, original: ItemType) {
        this.value = 0
        this.modelId = template.modelId
        this.modelZoom = template.modelZoom
        this.modelRotationX = template.modelRotationX
        this.modelRotationY = template.modelRotationY
        this.modelRotationZ = template.modelRotationZ
        this.modelOffsetX = template.modelOffsetX
        this.modelOffsetY = template.modelOffsetY
        this.recolorSrc = original.recolorSrc
        this.recolorDst = original.recolorDst
        this.recolorDPalette = original.recolorDPalette
        this.retexSrc = original.retexSrc
        this.retexDst = original.retexDst
        this.name = original.name
        this.membersOnly = original.membersOnly
        this.wearPos = original.wearPos
        this.wearPos2 = original.wearPos2
        this.wearPos3 = original.wearPos3
        this.maleEquip1 = original.maleEquip1
        this.maleEquip2 = original.maleEquip2
        this.maleEquip3 = original.maleEquip3
        this.femaleEquip1 = original.femaleEquip1
        this.femaleEquip2 = original.femaleEquip2
        this.femaleEquip3 = original.femaleEquip3
        this.maleEquipmentOffsetX = original.maleEquipmentOffsetX
        this.femaleEquipmentOffsetX = original.femaleEquipmentOffsetX
        this.maleEquipmentOffsetY = original.maleEquipmentOffsetY
        this.femaleEquipmentOffsetY = original.femaleEquipmentOffsetY
        this.maleEquipmentOffsetZ = original.maleEquipmentOffsetZ
        this.femaleEquipmentOffsetZ = original.femaleEquipmentOffsetZ
        this.maleHead1 = original.maleHead1
        this.maleHead2 = original.maleHead2
        this.femaleHead1 = original.femaleHead1
        this.femaleHead2 = original.femaleHead2
        this.teamId = original.teamId
        this.groundActions = original.groundActions
        this.parameters = original.parameters
        this.inventoryActions = Array(5) { null }
        if (original.inventoryActions != null) {
            System.arraycopy(original.inventoryActions, 0, this.inventoryActions, 0, 4)
        }
        this.inventoryActions!![4] = LocalizedText.DISCARD.translate(this.typeList!!.language)
    }

    fun generateBind(template: ItemType, original: ItemType) {
        this.value = 0
        this.modelId = template.modelId
        this.modelZoom = template.modelZoom
        this.modelRotationX = template.modelRotationX
        this.modelRotationY = template.modelRotationY
        this.modelRotationZ = template.modelRotationZ
        this.modelOffsetX = template.modelOffsetX
        this.modelOffsetY = template.modelOffsetY
        this.recolorSrc = original.recolorSrc
        this.recolorDst = original.recolorDst
        this.recolorDPalette = original.recolorDPalette
        this.retexSrc = original.retexSrc
        this.retexDst = original.retexDst
        this.name = original.name
        this.membersOnly = original.membersOnly
        this.wearPos = original.wearPos
        this.wearPos2 = original.wearPos2
        this.wearPos3 = original.wearPos3
        this.maleEquip1 = original.maleEquip1
        this.maleEquip2 = original.maleEquip2
        this.maleEquip3 = original.maleEquip3
        this.femaleEquip1 = original.femaleEquip1
        this.femaleEquip2 = original.femaleEquip2
        this.femaleEquip3 = original.femaleEquip3
        this.maleEquipmentOffsetX = original.maleEquipmentOffsetX
        this.femaleEquipmentOffsetX = original.femaleEquipmentOffsetX
        this.maleEquipmentOffsetY = original.maleEquipmentOffsetY
        this.femaleEquipmentOffsetY = original.femaleEquipmentOffsetY
        this.maleEquipmentOffsetZ = original.maleEquipmentOffsetZ
        this.femaleEquipmentOffsetZ = original.femaleEquipmentOffsetZ
        this.maleHead1 = original.maleHead1
        this.maleHead2 = original.maleHead2
        this.femaleHead1 = original.femaleHead1
        this.femaleHead2 = original.femaleHead2
        this.teamId = original.teamId
        this.groundActions = original.groundActions
        this.parameters = original.parameters
        this.stackable = original.stackable
        this.inventoryActions = Array(5) { null }
        if (original.inventoryActions != null) {
            System.arraycopy(original.inventoryActions, 0, this.inventoryActions, 0, 4)
        }
        this.inventoryActions!![4] = LocalizedText.DISCARD_2.translate(this.typeList!!.language)
    }

    fun generateNote(template: ItemType, original: ItemType) {
        this.modelId = template.modelId
        this.modelZoom = template.modelZoom
        this.modelRotationX = template.modelRotationX
        this.modelRotationY = template.modelRotationY
        this.modelRotationZ = template.modelRotationZ
        this.modelOffsetX = template.modelOffsetX
        this.modelOffsetY = template.modelOffsetY
        this.recolorSrc = template.recolorSrc
        this.recolorDst = template.recolorDst
        this.recolorDPalette = template.recolorDPalette
        this.retexSrc = template.retexSrc
        this.retexDst = template.retexDst
        this.name = original.name
        this.membersOnly = original.membersOnly
        this.value = original.value
        this.stackable = 1
    }

    fun getCs2Integer(key: Int, defaultValue: Int): Int {
        if (this.parameters == null) {
            return defaultValue
        }
        val integerNode = this.parameters!!.get(key.toLong()) as IntNode?
        return integerNode?.value ?: defaultValue
    }

    fun getCs2String(key: Int, defaultValue: String): String {
        if (this.parameters == null) {
            return defaultValue
        }
        val objectNode = this.parameters!!.get(key.toLong()) as ObjectNode?
        return objectNode?.value as? String ?: defaultValue
    }

    fun shadow(pixels: IntArray, color: Int) {
        for (descendingX in 31 downTo 1) {
            val pixelIndexStart = descendingX * 36
            for (descendingY in 35 downTo 1) {
                if (pixels[descendingY + pixelIndexStart] == 0 && pixels[descendingY + pixelIndexStart - 1 - 36] != 0) {
                    pixels[pixelIndexStart + descendingY] = color
                }
            }
        }
    }

    companion object {
        @JvmField
        var recolorPalette: ShortArray = ShortArray(256)

        @JvmStatic
        fun outline(pixels: IntArray, outlineColor: Int): IntArray {
            val outlinedPixels = IntArray(1152)
            var pixelIndex = 0
            for (x in 0 until 32) {
                for (y in 0 until 36) {
                    var pixel = pixels[pixelIndex]
                    if (pixel == 0) {
                        if (y > 0 && pixels[pixelIndex - 1] != 0) {
                            pixel = outlineColor
                        } else if (x > 0 && pixels[pixelIndex - 36] != 0) {
                            pixel = outlineColor
                        } else if (y < 35 && pixels[pixelIndex + 1] != 0) {
                            pixel = outlineColor
                        } else if (x < 31 && pixels[pixelIndex + 36] != 0) {
                            pixel = outlineColor
                        }
                    }
                    outlinedPixels[pixelIndex++] = pixel
                }
            }
            return outlinedPixels
        }
    }
}
