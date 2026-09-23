package com.jagex.game.runetek5.config.`object`

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.config.TypeResult
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.ALTERS_AMBIENT
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.graphics.model.PoseCopyPair
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.ALTERS_CONTRAST
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_X
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_Y
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.DISPLACE_Z
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.INVERT
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RECOLOR
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RETEXTURE
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.ROTATE_WITH_NORMALS
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.SHADOWED
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.TINT
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.vardomain.VarDomain

/**
 * [J]
 * @author Steven
 *
 * [NXT]
	[-]jag::game::LocType::DataSerialiseWrite<jag::Packet>(jag::DataSerialise<jag::Packet> &) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  000000000015C7C0 00000C44 00000148 00000000 R . . . . . .
	[X] jag::game::LocType::DecodeType(jag::Packet &,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000004688E0 00001245 00000058 00000000 R . . . . T .
	[-]jag::game::LocType::GetHillData(jag::Array<jag::Array<int>> &,jag::Array<jag::Array<int>> &,jag::game::LocType const&,jag::game::HeightMap const&,int,uint,uint,jag::Rotation90,bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 .text  00000000003E79D0 000003DD 000000B8 00000019 R . . . . . .
	[-]jag::game::LocType::GetLocationHeight(jag::game::LocType const&,jag::game::HeightMap const&,int,uint,uint,jag::Rotation90,bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  0000000000161A60 000002FA 00000098 0000000C R . . . . . .
	[-]jag::game::LocType::GetLocationOnFlatGround(jag::game::LocType const&,int,jag::game::HeightMap const&,int,uint,uint,jag::Rotation90,bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  0000000000161D60 00000123 00000060 00000011 R . . . . . .
	[-]jag::game::LocType::GetMaxRemoveRoofLevel(jag::game::LinkMap const&,int,uint,uint,jag::Rotation90)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000C1880 000000E4 00000038 00000000 R . . . . . .
	[~]jag::game::LocType::GetModels(jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::game::LocShape,jag::Rotation90,bool,jag::game::LocTypeCustomisation const*) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   .text  0000000000515BE0 00000991 00000118 000000B0 R . . . . . .
	[X] jag::game::LocType::GetMultiLoc(jag::shared_ptr<jag::game::LocType> &,jag::game::PlayerVarDomain const*,jag::game::ConfigProvider *)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .text  00000000001112F0 000002AA 00000028 00000000 R . . . . . .
	[-]jag::game::LocType::LocType(jag::game::ConfigTypeListPreloadPackedContainer<jag::game::LocType> *,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  0000000000420040 0000038F 00000018 00000000 R . . . . . .
	[-]jag::game::LocType::MakeLocationSoundData(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .text  00000000000441C8 0000021F 00000038 00000001 R . . . . . .
	[X] jag::game::LocType::PostDecode(void)
 *
 */
open class ObjectType {

	@JvmField
	var id: Int = 0

	/**
	 * The right click / mini menu actions
	 * to interact with this object
	 */
	@JvmField
	var actions: Array<String?>? = null

	@JvmField
	var shapes: ByteArray? = null
	@JvmField
	var objectModelIds: Array<IntArray>? = null

	/**
	 * Hover / minimenu name
	 * No actions if 'null'
	 */
	@JvmField
	var name: String = "null"

	/**
	 * Tiles in width taken up by the model
	 */
	@JvmField
	var sizeX: Int = 1

	/**
	 * Tiles in length taken up by the model
	 */
	@JvmField
	var sizeY: Int = 1

	/**
	 * Is an entity blocked from passing through
	 * 0 = no
	 * 1 = yes (explicit)
	 * 2 = yes (default)
	 */
	@JvmField
	var blocksMovement: Int = 2

	/**
	 * Does an entity have to path around the object
	 * when using projectiles (I.E. range/mage)
	 */
	@JvmField
	var blocksProjectiles: Boolean = true

	/**
	 * Does the object have menu interactions
	 * -1 = no (default)
	 * 0 = no (explicit)
	 * 1 = yes
	 */
	@JvmField
	var hasActions: Int = -1

	@JvmField
	var delayShading: Boolean = false
	@JvmField
	var occlusionMode: Int = -1
	@JvmField
	var decorDisplacement: Int = 64
	@JvmField
	var inverted: Boolean = false
	/**
	 * Draw low detail (any mode) shadows under objects
	 * (shadow only applies on tile below)
	 * Shadow is static - always placed the same way
	 */
	@JvmField
	var staticShadow: Boolean = true
	@JvmField
	var forceDisplayDecoration: Boolean = false
	@JvmField
	var ignoreClipOnAltRoute: Boolean = false
	@JvmField
	var supportsItems: Int = -1
	@JvmField
	var soundId: Int = -1
	@JvmField
	var soundRadius: Int = 0
	@JvmField
	var soundMinInterval: Int = 0
	@JvmField
	var soundMaxInterval: Int = 0
	@JvmField
	var soundGroupIds: IntArray? = null
	@JvmField
	var alternativeIds: IntArray? = null
	/**
	 * Only rendered if textures are enabled
	 */
	@JvmField
	var requiresTextures: Boolean = false
	/**
	 * Draw high detail (gpu mode) shadows under objects
	 * (shadow only applies on tile below)
	 * Shadow is dynamic - based of the position of sun
	 */
	@JvmField
	var dynamicShadow: Boolean = true

	@JvmField
	var replaySequence: Boolean = true

	/**
	 * Does a player have to be a member to interact
	 * with this object
	 */
	@JvmField
	var members: Boolean = false

	@JvmField
	var mapIconRotates: Boolean = false
	@JvmField
	var hasAnimation: Boolean = false
	@JvmField
	var primaryCursorActionIndex: Int = -1
	@JvmField
	var primaryCursor: Int = -1
	@JvmField
	var secondaryCursorActionIndex: Int = -1
	@JvmField
	var secondaryCursor: Int = -1
	@JvmField
	var mapIconRotation: Int = 0
	@JvmField
	var mapIconId: Int = -1
	@JvmField
	var soundVolume: Int = 255
	@JvmField
	var mapIconFlipped: Boolean = false
	@JvmField
	var mapCategoryid: Int = -1
	@JvmField
	var quests: IntArray? = null
	@JvmField
	var groundDecorationHeight: Int = 0
	@JvmField
	var instrumentSoundEffect: Boolean = false
	@JvmField
	var instrumentAmbientSound: Boolean = false
	@JvmField
	var cullY: Int = 960
	@JvmField
	var cullXZ: Int = 0
	@JvmField
	var ambientSoundMaxDelay: Int = 256
	@JvmField
	var ambientSoundMinDelay: Int = 256
	@JvmField
	var transforms: Boolean = false

	/**
	 * Not sure if this is correct
	 */
	@JvmField
	var ambientSoundMaxHearDistance: Int = 0

	/**
	 * Tints the object based on the tint
	 * set for the current scene
	 */
	@JvmField
	var dynamicTint: Boolean = false

	/**
	 * The TypeList used for loading LocTypes,
	 * used for fetching if player is member
	 * and usage of caches stored there
	 */
	@JvmField
	var locTypeList: ObjectTypeList? = null

	/**
	 * The type of ground adjustment that has to
	 * be performed on the model to display as intended
	 */
	@JvmField
	var groundContourType: Byte = 0

	/**
	 * The animations the object performs by default.
	 * One in the case of static animating,
	 * multiple in the case of randomized animation display
	 */
	@JvmField
	var animations: IntArray? = null

	@JvmField
	var ambient: Int = 0
	@JvmField
	var contrast: Int = 0

	/**
	 * Model face colors to find and replace
	 * with the same index within [.recolorDst]
	 */
	@JvmField
	var recolorSrc: ShortArray? = null

	/**
	 * Model face colors that are replacements for colors
	 * with the same index within [.recolorSrc]
	 */
	@JvmField
	var recolorDst: ShortArray? = null

	/**
	 * Model face materials to find and replace
	 * with the same index within [.retextureDst]
	 */
	@JvmField
	var retextureSrc: ShortArray? = null

	/**
	 * Model face materials that are replacements for materials
	 * with the same index within [.retextureSrc]
	 */
	@JvmField
	var retextureDst: ShortArray? = null


	@JvmField
	var recolorDPalette: ByteArray? = null

	/**
	 * Scaling factor for vertices x applied to [.objectModelIds]
	 * Only scales when value != 128. Default usage of 128 to make minor
	 * scaling easy (Example 129 --> (129 >> 7) * 100 --> 0.78125% scaled
	 */
	@JvmField
	var resizeX: Int = 128

	/**
	 * Scaling factor for vertices y applied to [.objectModelIds]
	 * Only scales when value != 128. Default usage of 128 to make minor
	 * scaling easy (Example 129 --> (129 >> 7) * 100 --> 0.78125% scaled
	 */
	@JvmField
	var resizeY: Int = 128

	/**
	 * Scaling factor for vertices z applied to [.objectModelIds]
	 * Only scales when value != 128. Default usage of 128 to make minor
	 * scaling easy (Example 129 --> (129 >> 7) * 100 --> 0.78125% scaled
	 */
	@JvmField
	var resizeZ: Int = 128
	@JvmField
	var offsetX: Int = 0
	@JvmField
	var offsetY: Int = 0
	@JvmField
	var offsetZ: Int = 0
	@JvmField
	var varpbitId: Int = -1
	@JvmField
	var varpId: Int = -1
	@JvmField
	var goundContourModifier: Int = -1
	@JvmField
	var animationOdds: IntArray? = null
	@JvmField
	var tintHue: Byte = 0
	@JvmField
	var tintSaturation: Byte = 0
	@JvmField
	var tintLightness: Byte = 0
	@JvmField
	var tintOpacity: Byte = 0
	@JvmField
	var shadowOffsetX: Int = 0
	@JvmField
	var shadowOffsetY: Int = 0
	@JvmField
	var shadowOffsetZ: Int = 0
	@JvmField
	var parameters: IterableNodeMap<Node<*>>? = null

	/**
	 * [J]
	 */
	internal fun decodeType(buffer: Packet) {
		while (true) {
			val opcode = buffer.readUByte()
			if (opcode == 0) {
				return
			}
			decodeOpcode(buffer, opcode)
		}
	}

	fun decodeOpcode(buffer: Packet, opcode: Int) {
		when {
			opcode == 1 -> {
				val count = buffer.readUByte()
				this.shapes = ByteArray(count)
				this.objectModelIds = Array(count) { IntArray(0) }
				for (shape in 0 until count) {
					this.shapes!![shape] = buffer.readByte()
					val models = buffer.readUByte()
					this.objectModelIds!![shape] = IntArray(models)
					for (model in 0 until models) {
						this.objectModelIds!![shape][model] = buffer.readBigSmart()
					}
				}
			}
			opcode == 2 -> this.name = buffer.readString()
			opcode == 14 -> this.sizeX = buffer.readUByte()
			opcode == 15 -> this.sizeY = buffer.readUByte()
			opcode == 17 -> {
				this.blocksMovement = 0
				this.blocksProjectiles = false
			}
			opcode == 18 -> this.blocksProjectiles = false
			opcode == 19 -> this.hasActions = buffer.readUByte()
			opcode == 21 -> this.groundContourType = 1
			opcode == 22 -> this.delayShading = true
			opcode == 23 -> this.occlusionMode = 1
			opcode == 24 -> {
				val animation = buffer.readBigSmart()
				if (animation != -1) {
					this.animations = intArrayOf(animation)
				}
			}
			opcode == 27 -> this.blocksMovement = 1
			opcode == 28 -> this.decorDisplacement = buffer.readUByte() shl 2
			opcode == 29 -> this.ambient = buffer.readByte().toInt()
			opcode == 39 -> this.contrast = buffer.readByte().toInt() * 5
			opcode in 30..34 -> this.actions!![opcode - 30] = buffer.readString()
			opcode == 40 -> {
				val count = buffer.readUByte()
				this.recolorSrc = ShortArray(count)
				this.recolorDst = ShortArray(count)
				for (index in 0 until count) {
					this.recolorSrc!![index] = buffer.readUShort().toShort()
					this.recolorDst!![index] = buffer.readUShort().toShort()
				}
			}
			opcode == 41 -> {
				val count = buffer.readUByte()
				this.retextureSrc = ShortArray(count)
				this.retextureDst = ShortArray(count)
				for (index in 0 until count) {
					this.retextureSrc!![index] = buffer.readUShort().toShort()
					this.retextureDst!![index] = buffer.readUShort().toShort()
				}
			}
			opcode == 42 -> {
				val count = buffer.readUByte()
				this.recolorDPalette = ByteArray(count)
				for (index in 0 until count) {
					this.recolorDPalette!![index] = buffer.readByte()
				}
			}
			opcode == 62 -> this.inverted = true
			opcode == 64 -> this.staticShadow = false
			opcode == 65 -> this.resizeX = buffer.readUShort()
			opcode == 66 -> this.resizeY = buffer.readUShort()
			opcode == 67 -> this.resizeZ = buffer.readUShort()
			opcode == 69 -> buffer.readUByte()
			opcode == 70 -> this.offsetX = buffer.readShort().toInt() shl 2
			opcode == 71 -> this.offsetY = buffer.readShort().toInt() shl 2
			opcode == 72 -> this.offsetZ = buffer.readShort().toInt() shl 2
			opcode == 73 -> this.forceDisplayDecoration = true
			opcode == 74 -> this.ignoreClipOnAltRoute = true
			opcode == 75 -> this.supportsItems = buffer.readUByte()
			opcode == 77 || opcode == 92 -> {
				this.varpbitId = buffer.readUShort()
				if (this.varpbitId == 65535) {
					this.varpbitId = -1
				}
				this.varpId = buffer.readUShort()
				if (this.varpId == 65535) {
					this.varpId = -1
				}
				var lastAlt = -1
				if (opcode == 92) {
					lastAlt = buffer.readBigSmart()
				}
				val count = buffer.readUByte()
				this.alternativeIds = IntArray(count + 2)
				for (index in 0..count) {
					this.alternativeIds!![index] = buffer.readBigSmart()
				}
				this.alternativeIds!![count + 1] = lastAlt
			}
			opcode == 78 -> {
				this.soundId = buffer.readUShort()
				this.soundRadius = buffer.readUByte()
			}
			opcode == 79 -> {
				this.soundMinInterval = buffer.readUShort()
				this.soundMaxInterval = buffer.readUShort()
				this.soundRadius = buffer.readUByte()
				val count = buffer.readUByte()
				this.soundGroupIds = IntArray(count)
				for (index in 0 until count) {
					this.soundGroupIds!![index] = buffer.readUShort()
				}
			}
			opcode == 81 -> {
				this.groundContourType = 2
				this.goundContourModifier = buffer.readUByte() * 256
			}
			opcode == 82 -> this.requiresTextures = true
			opcode == 88 -> this.dynamicShadow = false
			opcode == 89 -> this.replaySequence = false
			opcode == 91 -> this.members = true
			opcode == 93 -> {
				this.groundContourType = 3
				this.goundContourModifier = buffer.readUShort()
			}
			opcode == 94 -> this.groundContourType = 4
			opcode == 95 -> {
				this.groundContourType = 5
				this.goundContourModifier = buffer.readShort().toInt()
			}
			opcode == 97 -> this.mapIconRotates = true
			opcode == 98 -> this.hasAnimation = true
			opcode == 99 -> {
				this.primaryCursorActionIndex = buffer.readUByte()
				this.primaryCursor = buffer.readUShort()
			}
			opcode == 100 -> {
				this.secondaryCursorActionIndex = buffer.readUByte()
				this.secondaryCursor = buffer.readUShort()
			}
			opcode == 101 -> this.mapIconRotation = buffer.readUByte()
			opcode == 102 -> this.mapIconId = buffer.readUShort()
			opcode == 103 -> this.occlusionMode = 0
			opcode == 104 -> this.soundVolume = buffer.readUByte()
			opcode == 105 -> this.mapIconFlipped = true
			opcode == 106 -> {
				val numAnimations = buffer.readUByte()
				var sum = 0
				this.animations = IntArray(numAnimations)
				this.animationOdds = IntArray(numAnimations)
				for (index in 0 until numAnimations) {
					this.animations!![index] = buffer.readBigSmart()
					sum += buffer.readUByte().also { this.animationOdds!![index] = it }
				}
				for (index in 0 until numAnimations) {
					this.animationOdds!![index] = this.animationOdds!![index] * 65535 / sum
				}
			}
			opcode == 107 -> this.mapCategoryid = buffer.readUShort()
			opcode in 150..154 -> {
				this.actions!![opcode - 150] = buffer.readString()
				if (!this.locTypeList!!.playerIsMember) {
					this.actions!![opcode - 150] = null
				}
			}
			opcode == 160 -> {
				val count = buffer.readUByte()
				this.quests = IntArray(count)
				for (index in 0 until count) {
					this.quests!![index] = buffer.readUShort()
				}
			}
			opcode == 162 -> {
				this.groundContourType = 3
				this.goundContourModifier = buffer.readInt()
			}
			opcode == 163 -> {
				this.tintHue = buffer.readByte()
				this.tintSaturation = buffer.readByte()
				this.tintLightness = buffer.readByte()
				this.tintOpacity = buffer.readByte()
			}
			opcode == 164 -> this.shadowOffsetX = buffer.readShort().toInt()
			opcode == 165 -> this.shadowOffsetY = buffer.readShort().toInt()
			opcode == 166 -> this.shadowOffsetZ = buffer.readShort().toInt()
			opcode == 167 -> this.groundDecorationHeight = buffer.readUShort()
			opcode == 168 -> this.instrumentSoundEffect = true // ambient sound
			opcode == 169 -> this.instrumentAmbientSound = true
			opcode == 170 -> this.cullY = buffer.readUSmart()
			opcode == 171 -> this.cullXZ = buffer.readUSmart()
			opcode == 173 -> {
				this.ambientSoundMaxDelay = buffer.readUShort()
				this.ambientSoundMinDelay = buffer.readUShort()
			}
			opcode == 177 -> this.transforms = true
			opcode == 178 -> this.ambientSoundMaxHearDistance = buffer.readUByte()
			opcode == 189 -> this.dynamicTint = true
			opcode == 249 -> {
				val count = buffer.readUByte()
				if (this.parameters == null) {
					val capacity = MathUtils.nextPowerOfTwo(count)
					this.parameters = IterableNodeMap(capacity)
				}
				for (index in 0 until count) {
					val isString = buffer.readUByte() == 1
					val key = buffer.readUMedium().toLong()
					val value: Node<*>
					if (isString) {
						value = ObjectNode(buffer.readString())
					} else {
						value = IntNode(buffer.readInt())
					}
					this.parameters!!.put(value, key)
				}
			}
		}
	}

	/**
	 * [J]
	 */
	internal fun postDecode() {
		if (this.hasActions == -1) {
			this.hasActions = 0
			if (this.shapes != null && this.shapes!!.size == 1 && this.shapes!![0].toInt() == ObjectShapes.SCENERY_INTERACT.shape) {
				this.hasActions = 1
			}
			for (index in 0..4) {
				if (this.actions!![index] != null) {
					this.hasActions = 1
					break
				}
			}
		}
		if (this.supportsItems == -1) {
			this.supportsItems = if (this.blocksMovement != 0) 1 else 0
		}
		if (isAnimated() || this.hasAnimation || this.alternativeIds != null) {
			this.transforms = true
		}
	}

	@Synchronized
	fun getPair(toolkit: AbstractToolkit, initialFunctionMask: Int, shape: Int, rotation: Int, currentLevel: Ground?, nextLevel: Ground?, sceneX: Int, averageHeight: Int, sceneY: Int, hasShadow: Boolean, locCustomisation: ObjectTypeCustomisation?): TypeResult? {
		val typeList = this.locTypeList
			?: throw IllegalStateException("ObjectType.locTypeList is null for id=$id. This ObjectType was not properly initialized via ObjectTypeList.list()")
		var shapeVar = shape
		if (ObjectShapes.isWallDecoration(shapeVar)) {
			shapeVar = ObjectShapes.STRAIGHT_INSIDE_WALL_DEC.shape
		}
		var uid = ((shapeVar.toLong() shl 3) + rotation.toLong() + (this.id.toLong() shl 10))
		uid = uid or (toolkit.renderId.toLong() shl 29)
		if (locCustomisation != null) {
			uid = uid or (locCustomisation.id shl 32)
		}
		var adjustedFunctionMask = initialFunctionMask
		if (this.groundContourType == 3.toByte()) {
			adjustedFunctionMask = initialFunctionMask or DISPLACE_X or DISPLACE_Y or DISPLACE_Z
		} else {
			if (this.groundContourType.toInt() != 0 || this.shadowOffsetY != 0) {
				adjustedFunctionMask = initialFunctionMask or DISPLACE_Y
			}
			if (this.shadowOffsetX != 0) {
				adjustedFunctionMask = adjustedFunctionMask or DISPLACE_X
			}
			if (this.shadowOffsetZ != 0) {
				adjustedFunctionMask = adjustedFunctionMask or DISPLACE_Z
			}
		}
		if (hasShadow) {
			adjustedFunctionMask = adjustedFunctionMask or SHADOWED
		}
		var pair: TypeResult?
		synchronized(typeList.typeresultCache) {
			pair = typeList.typeresultCache.get(uid) as TypeResult?
		}
		var model: Model? = pair?.mainObject as? Model?
		var shadow: Any? = null
		if (model != null && toolkit.mismatchingFunctionMask(model.getFunctionMask(), adjustedFunctionMask) == 0) {
			shadow = pair!!.companionObject
			if (hasShadow && shadow == null) {
				@Suppress("UNCHECKED_CAST")
				shadow = model.createShadowSprite(null)
				pair.companionObject = shadow
			}
		} else {
			if (model != null) {
				adjustedFunctionMask = toolkit.mergeFunctionMasks(adjustedFunctionMask, model.getFunctionMask())
			}
			var rotatingFunctionMask = adjustedFunctionMask
			if (shapeVar == ObjectShapes.SCENERY_INTERACT.shape && rotation > 3) {
				rotatingFunctionMask = adjustedFunctionMask or DISPLACE_X or DISPLACE_Z
			}
			model = getStationaryModel(toolkit, rotatingFunctionMask, shapeVar, rotation, locCustomisation)
			if (model == null) {
				return null
			}
			if (shapeVar == ObjectShapes.SCENERY_INTERACT.shape && rotation > 3) {
				model.yaxisRotateWithoutNormals(2048)
			}
			if (hasShadow) {
				@Suppress("UNCHECKED_CAST")
				shadow = model.createShadowSprite(null)
			}
			model.applyShading(adjustedFunctionMask)
			pair = TypeResult(model, shadow)
			synchronized(typeList.typeresultCache) {
				typeList.typeresultCache.put(pair, uid)
			}
		}
		val hasGroundContour = this.groundContourType.toInt() != 0 && (currentLevel != null || nextLevel != null)
		val hasTranslation = this.shadowOffsetX != 0 || this.shadowOffsetY != 0 || this.shadowOffsetZ != 0
		if (hasGroundContour || hasTranslation) {
			model = model.copy(0.toByte(), adjustedFunctionMask, true)
			if (hasGroundContour && this.goundContourModifier < 16384) {
				model?.contourToGround(this.groundContourType.toInt(), this.goundContourModifier, currentLevel, nextLevel, sceneX, averageHeight, sceneY)
			}
			if (hasTranslation) {
				model!!.translate(this.shadowOffsetX, this.shadowOffsetY, this.shadowOffsetZ)
			}
			model!!.applyShading(initialFunctionMask)
		} else {
			model = model.copy(0.toByte(), initialFunctionMask, true)
		}
		typeList.locresult.mainObject = model
		typeList.locresult.companionObject = shadow
		return typeList.locresult
	}

	fun getStationaryModel(toolkit: AbstractToolkit, functionMask: Int, type: Int, rotation: Int, locCustomisation: ObjectTypeCustomisation?): Model? {
		var functionMaskVar = functionMask
		val adjustedAmbient = 64 + this.ambient
		val adjustedContrast = 850 + this.contrast
		val initialFunctionMask = functionMaskVar
		val invert = this.inverted || type == ObjectShapes.WALL_WHOLE_CORNER.shape && rotation > 3
		if (invert) {
			functionMaskVar = functionMaskVar or INVERT
		}
		if (rotation == 0) {
			if (this.resizeX != 128 || this.offsetX != 0) {
				functionMaskVar = functionMaskVar or DISPLACE_X
			}
			if (this.resizeZ != 128 || this.offsetZ != 0) {
				functionMaskVar = functionMaskVar or DISPLACE_Z
			}
		} else {
			functionMaskVar = functionMaskVar or ROTATE_WITH_NORMALS
		}
		if (this.resizeY != 128 || this.offsetY != 0) {
			functionMaskVar = functionMaskVar or DISPLACE_Y
		}
		if (this.recolorSrc != null) {
			functionMaskVar = functionMaskVar or RECOLOR
		}
		if (this.retextureSrc != null) {
			functionMaskVar = functionMaskVar or RETEXTURE
		}
		if (this.tintOpacity != 0.toByte()) {
			functionMaskVar = functionMaskVar or TINT
		}
		var createdModel: Model? = null
		if (this.shapes != null) {
			var matchingShape = -1
			for (shapeIndex in this.shapes!!.indices) {
				if (type == this.shapes!![shapeIndex].toInt()) {
					matchingShape = shapeIndex
					break
				}
			}
			if (matchingShape == -1) {
				return null
			}
			val modelsByShape = if (locCustomisation != null && locCustomisation.objectModelIds != null)
				locCustomisation.objectModelIds else this.objectModelIds!![matchingShape]
			val shapeModelCount = modelsByShape.size
			if (shapeModelCount > 0) {
				var uid = toolkit.renderId.toLong()
				for (modelIndex in 0 until shapeModelCount) {
					uid = modelsByShape[modelIndex] + 67783L * uid
				}
				synchronized(this.locTypeList!!.modelsByShapeCache) {
					createdModel = this.locTypeList!!.modelsByShapeCache.get(uid) as Model?
				}
				if (createdModel != null) {
					if (adjustedAmbient != createdModel.getAmbient()) {
						functionMaskVar = functionMaskVar or ALTERS_AMBIENT
					}
					if (adjustedContrast != createdModel.getContrast()) {
						functionMaskVar = functionMaskVar or ALTERS_CONTRAST
					}
				}
				if (createdModel == null || toolkit.mismatchingFunctionMask(createdModel.getFunctionMask(), functionMaskVar) != 0) {
					var adjustedFunctionMask = functionMaskVar or 0x1f01f
					if (createdModel != null) {
						adjustedFunctionMask = toolkit.mergeFunctionMasks(adjustedFunctionMask, createdModel.getFunctionMask())
					}
					var mesh: Mesh? = null
					synchronized(this.locTypeList!!.meshCombineCache) {
						for (shapeIndex in 0 until shapeModelCount) {
							synchronized(this.locTypeList!!.meshResource) {
								mesh = Mesh.fromResource(this.locTypeList!!.meshResource, modelsByShape[shapeIndex])
							}
							if (mesh == null) {
								return null
							}
							if (mesh!!.version < 13) {
								mesh!!.upscale()
							}
							if (shapeModelCount > 1) {
								this.locTypeList!!.meshCombineCache[shapeIndex] = mesh
							}
						}
						if (shapeModelCount > 1) {
							mesh = Mesh(this.locTypeList!!.meshCombineCache, shapeModelCount)
						}
					}
					createdModel = toolkit.createModel(mesh!!, adjustedFunctionMask, this.locTypeList!!.preferenceMask, adjustedAmbient, adjustedContrast)
					if (LOC_DIAG.contains(id)) {
						System.err.println("[LocDiag] loc=$id shape=$type rot=$rotation models=${modelsByShape.joinToString()} mesh=${mesh!!.numVertices}v/${mesh!!.numFaces}f model=${createdModel.javaClass.simpleName} x[${createdModel.getMinX()},${createdModel.getMaxX()}] y[${createdModel.getMinY()},${createdModel.getMaxY()}] z[${createdModel.getMinZ()},${createdModel.getMaxZ()}] mask=${Integer.toHexString(adjustedFunctionMask)} prefs=${Integer.toHexString(this.locTypeList!!.preferenceMask)} ambient=$adjustedAmbient contrast=$adjustedContrast")
					}
					synchronized(this.locTypeList!!.modelsByShapeCache) {
						this.locTypeList!!.modelsByShapeCache.put(createdModel, uid)
					}
				}
			}
		}
		if (createdModel == null) {
			return null
		}
		val model = createdModel.copy(0.toByte(), functionMaskVar, true)
		if (adjustedAmbient != createdModel.getAmbient()) {
			model!!.setAmbient(adjustedAmbient)
		}
		if (adjustedContrast != createdModel.getContrast()) {
			model!!.setContrast(adjustedContrast)
		}
		if (invert) {
			model!!.mirror()
		}
		if (ObjectShapes.STRAIGHT_INSIDE_WALL_DEC.shape == type && rotation > 3) {
			model!!.rotateWithNormals(2048)
			model.translate(180, 0, -180)
		}
		val rotationVar = rotation and 0x3
		if (rotationVar == 1) {
			model!!.rotateWithNormals(4096)
		} else if (rotationVar == 2) {
			model!!.rotateWithNormals(8192)
		} else if (rotationVar == 3) {
			model!!.rotateWithNormals(12288)
		}
		if (this.recolorSrc != null) {
			val destinationColor: ShortArray = if (locCustomisation != null && locCustomisation.recolorDst != null)
				locCustomisation.recolorDst else this.recolorDst!!
			for (recolorIndex in this.recolorSrc!!.indices) {
				if (this.recolorDPalette != null && recolorIndex < this.recolorDPalette!!.size) {
					model!!.recolor(this.recolorSrc!![recolorIndex], locRecolorPalette[this.recolorDPalette!![recolorIndex].toInt() and 0xff])
				} else {
					model!!.recolor(this.recolorSrc!![recolorIndex], destinationColor[recolorIndex])
				}
			}
		}
		if (this.retextureSrc != null) {
			val retextureDestination: ShortArray = if (locCustomisation != null && locCustomisation.retexDst != null)
				locCustomisation.retexDst else this.retextureDst!!
			for (retextureIndex in this.retextureSrc!!.indices) {
				model!!.rematerial(this.retextureSrc!![retextureIndex], retextureDestination[retextureIndex])
			}
		}
		if (this.tintOpacity != 0.toByte()) {
			model!!.tint(this.tintHue.toInt(), this.tintSaturation.toInt(), this.tintLightness.toInt(), this.tintOpacity.toInt() and 0xff)
		}
		if (this.resizeX != 128 || this.resizeY != 128 || this.resizeZ != 128) {
			model!!.scale(this.resizeX, this.resizeY, this.resizeZ)
		}
		if (this.offsetX != 0 || this.offsetY != 0 || this.offsetZ != 0) {
			model!!.translate(this.offsetX, this.offsetY, this.offsetZ)
		}
		model!!.applyShading(initialFunctionMask)
		return model
	}

	@Synchronized
	fun getAnimatedModel(toolkit: AbstractToolkit, functionMask: Int, type: Int, rotation: Int, currentLvl: Ground?, nextLvl: Ground?, sceneX: Int, averageHeight: Int, sceneY: Int, sequence: Sequence?, locCustomisation: ObjectTypeCustomisation?, privatePool: PoseCopyPair? = null): Model? {
		val locTypeList = this.locTypeList ?: return null
		var typeVar = type
		if (ObjectShapes.isWallDecoration(typeVar)) {
			typeVar = ObjectShapes.STRAIGHT_INSIDE_WALL_DEC.shape
		}
		var uid = ((typeVar.toLong() shl 3) + rotation.toLong() + (this.id.toLong() shl 10))
		uid = uid or (toolkit.renderId.toLong() shl 29)
		if (locCustomisation != null) {
			uid = uid or (locCustomisation.id shl 32)
		}
		var functionMaskVar = functionMask
		if (sequence != null) {
			functionMaskVar = functionMaskVar or sequence.getFunctionMask()
		}
		if (this.groundContourType == 3.toByte()) {
			functionMaskVar = functionMaskVar or DISPLACE_X or DISPLACE_Y or DISPLACE_Z
		} else {
			if (this.groundContourType.toInt() != 0 || this.shadowOffsetY != 0) {
				functionMaskVar = functionMaskVar or DISPLACE_Y
			}
			if (this.shadowOffsetX != 0) {
				functionMaskVar = functionMaskVar or DISPLACE_X
			}
			if (this.shadowOffsetZ != 0) {
				functionMaskVar = functionMaskVar or DISPLACE_Z
			}
		}
		if (typeVar == ObjectShapes.SCENERY_INTERACT.shape && rotation > 3) {
			functionMaskVar = functionMaskVar or DISPLACE_X or DISPLACE_Z
		}
		var model: Model?
		synchronized(locTypeList.uidToModelCache) {
			model = locTypeList.uidToModelCache[uid] as Model?
		}
		if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), functionMaskVar) != 0) {
			if (model != null) {
				functionMaskVar = toolkit.mergeFunctionMasks(functionMaskVar, model.getFunctionMask())
			}
			model = getStationaryModel(toolkit, functionMaskVar, typeVar, rotation, locCustomisation)
			if (model == null) {
				return null
			}
			synchronized(locTypeList.uidToModelCache) {
				locTypeList.uidToModelCache.put(model, uid)
			}
		}
		val baseModel: Model = model
		val animatedCacheKey = if (sequence != null && privatePool == null) createAnimatedModelCacheKey(uid, functionMaskVar, sequence) else null
		if (animatedCacheKey != null) {
			val cached = lookupCachedAnimatedModel(animatedCacheKey, baseModel)
			if (cached != null) {
				return cached
			}
		}
		var applyShading = false
		if (sequence != null) {
			// Cacheable animation frames are built into a dedicated copy (copy type 0) so
			// the result — including its GL buffers — survives across render frames and is
			// shared by identical locs on the same frame. Uncacheable ones keep using the
			// shared toolkit copy slot, which the next animated entity overwrites and which
			// therefore has to be rebuilt every render frame.
			// privatePose: the caller keeps the returned pose across frames (see
			// AnimatedGameObject's pose memo), so it must not live in the shared slot.
			val privatePose = privatePool != null && baseModel is ModelGL
			model = if (privatePose) {
				privatePool!!.copy(baseModel as ModelGL, functionMaskVar)
			} else {
				val copyType: Byte = if (animatedCacheKey != null) 0 else 1
				baseModel.copy(copyType, functionMaskVar, true)
			}
			// A private pose copy must own its normals: copyModelData shares the base's
			// arrays unless the mask alters light, and neighbour normal merging
			// (castShadow) on a copy that shares them drifts the BASE model's lighting —
			// and every wall merged with it.
			if (privatePose && model is ModelGL && baseModel is ModelGL) model.detachSharedNormals(baseModel)
			applyShading = true
			sequence.animateRotatedModel(model!!, rotation and 0x3)
		}
		if (typeVar == ObjectShapes.SCENERY_INTERACT.shape && rotation > 3) {
			if (!applyShading) {
				model = model.copy(3.toByte(), functionMaskVar, true)
				applyShading = true
			}
			model!!.yaxisRotateWithoutNormals(2048)
		}
		if (this.groundContourType.toInt() != 0) {
			if (!applyShading) {
				model = model.copy(3.toByte(), functionMaskVar, true)
				applyShading = true
			}
			model!!.contourToGround(this.groundContourType.toInt(), this.goundContourModifier, currentLvl, nextLvl, sceneX, averageHeight, sceneY)
		}
		if (this.shadowOffsetX != 0 || this.shadowOffsetY != 0 || this.shadowOffsetZ != 0) {
			if (!applyShading) {
				model = model.copy(3.toByte(), functionMaskVar, true)
				applyShading = true
			}
			model!!.translate(this.shadowOffsetX, this.shadowOffsetY, this.shadowOffsetZ)
		}
		if (applyShading) {
			model.applyShading(functionMaskVar)
		}
		if (animatedCacheKey != null) {
			storeCachedAnimatedModel(animatedCacheKey, baseModel, model)
		}
		return model
	}

	/**
	 * Decides whether the animated model produced by [getAnimatedModel] for [sequence]'s
	 * current state is a pure function of the returned key, and can therefore be cached
	 * and reused across render frames (and across identical locs, e.g. rows of torches).
	 * Returns null when the result depends on state the key cannot capture; those calls
	 * fall back to the per-frame shared-copy-slot rebuild.
	 */
	private fun createAnimatedModelCacheKey(uid: Long, functionMask: Int, sequence: Sequence): AnimatedModelCacheKey? {
		if (this.groundContourType.toInt() != 0) {
			// Ground-contoured geometry depends on the surrounding heightmap, which is not
			// captured by the key and changes whenever the scene is rebuilt.
			return null
		}
		val seqType = sequence.type ?: return null
		if (seqType.frames == null || seqType.tweened) {
			// Tweened sequences interpolate towards the next frame by elapsed time, so the
			// output changes every client cycle; caching per-cycle results would only churn
			// GL buffers instead of saving work.
			return null
		}
		if (sequence.tweening && seqType.interfaceFrames != null) {
			return null
		}
		if (!sequence.isMaskCalculated()) {
			// Framesets are not loaded yet: animateRotatedModel would be a no-op, and caching
			// the unanimated result would keep this frame frozen after they load.
			return null
		}
		return AnimatedModelCacheKey(uid, functionMask, seqType.id, sequence.currentFrameId)
	}

	/**
	 * [J]
	 */
	fun getMultiLoc(vars: VarDomain): ObjectType? {
		var index = -1
		if (this.varpbitId != -1) {
			index = vars.getVarBitValue(this.varpbitId)
		} else if (this.varpId != -1) {
			index = vars.getVarValue(this.varpId)
		}
		if (index >= 0 && index < this.alternativeIds!!.size - 1 && this.alternativeIds!![index] != -1) {
			return this.locTypeList!!.list(this.alternativeIds!![index])
		}
		val id = this.alternativeIds!![this.alternativeIds!!.size - 1]
		return if (id != -1) this.locTypeList!!.list(id) else null
	}

	fun fullyCached(): Boolean {
		if (this.objectModelIds == null) {
			return true
		}
		var cached = true
		synchronized(this.locTypeList!!.meshResource) {
			for (shape in this.objectModelIds!!.indices) {
				for (model in this.objectModelIds!![shape].indices) {
					cached = cached and this.locTypeList!!.meshResource.isCached(this.objectModelIds!![shape][model], 0)
				}
			}
			return cached
		}
	}

	fun isTypeCached(shape: Int): Boolean {
		if (this.objectModelIds == null) {
			return true
		}
		var cached = true
		synchronized(this.locTypeList!!.meshResource) {
			for (shapeIndex in this.shapes!!.indices) {
				if (this.shapes!![shapeIndex].toInt() == shape) {
					for (model in this.objectModelIds!![shapeIndex].indices) {
						if (!this.locTypeList!!.meshResource.isCached(this.objectModelIds!![shapeIndex][model], 0)) {
							cached = false
						}
					}
				}
			}
			return cached
		}
	}

	fun hasSound(): Boolean {
		if (this.alternativeIds == null) {
			return this.soundId != -1 || this.soundGroupIds != null
		}
		for (alternativeIndex in this.alternativeIds!!.indices) {
			if (this.alternativeIds!![alternativeIndex] != -1) {
				val loctype = this.locTypeList!!.list(this.alternativeIds!![alternativeIndex])
				if (loctype.soundId != -1 || loctype.soundGroupIds != null) {
					return true
				}
			}
		}
		return false
	}

	fun getParamsInt(key: Int, defaultValue: Int): Int {
		if (this.parameters == null) {
			return defaultValue
		}
		val intnode = this.parameters!!.get(key.toLong()) as IntNode?
		return intnode?.value ?: defaultValue
	}

	fun getParamsString(paramsKey: Int, defaultString: String): String {
		if (this.parameters == null) {
			return defaultString
		}
		val objectNode = this.parameters!!.get(paramsKey.toLong()) as ObjectNode?
		return objectNode?.value as? String ?: defaultString
	}

	fun getRandomAnimation(): Int {
		if (this.animations != null) {
			if (this.animations!!.size <= 1) {
				return this.animations!![0]
			}
			val randomCache = (Math.random() * 65535.0).toInt()
			var randomCacheVar = randomCache
			for (animationIndex in this.animations!!.indices) {
				if (randomCacheVar <= this.animationOdds!![animationIndex]) {
					return this.animations!![animationIndex]
				}
				randomCacheVar -= this.animationOdds!![animationIndex]
			}
		}
		return -1
	}

	fun hasSequence(animationToFind: Int): Boolean {
		if (this.animations != null && animationToFind != -1) {
			for (animationIndex in this.animations!!.indices) {
				if (this.animations!![animationIndex] == animationToFind) {
					return true
				}
			}
		}
		return false
	}

	fun hasAdditionalAnimations(): Boolean {
		return this.animations != null && this.animations!!.size > 1
	}

	fun isAnimated(): Boolean {
		return this.animations != null
	}

	fun getAnimations(): IntArray? {
		return this.animations
	}

	/**
	 * Identifies one cacheable animated-loc build: [uid] already encodes loc id, shape,
	 * rotation, customisation and renderer; the sequence id and frame index pin the pose.
	 * Only non-tweened sequences are cached (see [createAnimatedModelCacheKey]), so the
	 * frame index fully determines the vertex data.
	 */
	private data class AnimatedModelCacheKey(
		val uid: Long,
		val functionMask: Int,
		val sequenceId: Int,
		val frameId: Int
	)

	/**
	 * A cached animated build together with the stationary model it was derived from.
	 * The base reference doubles as the validity check: when the stationary-model cache
	 * is reset or the entry is rebuilt (mask merge, toolkit change, customisation), the
	 * base instance changes and everything derived from the old instance is stale.
	 */
	private class CachedAnimatedModel(val base: Model, val model: Model)

	companion object {
		/** -Drender.locdiag=1,2,3 logs the mesh and model bounds built for those loc ids. */
		private val LOC_DIAG: Set<Int> = System.getProperty("render.locdiag")?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
		@JvmField
		var locRecolorPalette: ShortArray = ShortArray(256)

		// Sized for extended render distance: entries ~ (distinct animated loc
		// uids visible) x (frames of their sequences held hot). A 3x-distance map
		// shows ~9x the area of the legacy 104 map; too-small capacity thrashes
		// and degrades to a full rebuild per render frame, worse than no cache.
		private const val ANIMATED_MODEL_CACHE_CAPACITY = 2048

		/**
		 * LRU cache of fully built animated-loc models. Animated locs (torches, fires,
		 * fountains, ...) previously re-ran copy + skeletal transform + shading and
		 * re-uploaded their geometry on every render frame even though the animation
		 * only advances a few times per second; entries here are rebuilt once per
		 * animation frame and drawn from their own retained GL buffers in between.
		 * Access is guarded by synchronizing on the map itself.
		 */
		private val animatedModelCache =
			object : LinkedHashMap<AnimatedModelCacheKey, CachedAnimatedModel>(64, 0.75f, true) {
				override fun removeEldestEntry(eldest: MutableMap.MutableEntry<AnimatedModelCacheKey, CachedAnimatedModel>): Boolean {
					return size > ANIMATED_MODEL_CACHE_CAPACITY
				}
			}

		private fun lookupCachedAnimatedModel(key: AnimatedModelCacheKey, base: Model): Model? {
			synchronized(animatedModelCache) {
				val entry = animatedModelCache[key] ?: return null
				if (entry.base !== base) {
					animatedModelCache.remove(key)
					return null
				}
				return entry.model
			}
		}

		private fun storeCachedAnimatedModel(key: AnimatedModelCacheKey, base: Model, model: Model) {
			synchronized(animatedModelCache) {
				animatedModelCache[key] = CachedAnimatedModel(base, model)
			}
		}

		@JvmStatic
		fun isTypeCached(loctypelist: ObjectTypeList, id: Int, type: Int): Boolean {
			var typeVar = type
			val loctype = loctypelist.list(id)
			if (typeVar == 11) {
				typeVar = 10
			}
			if (typeVar in 5..8) {
				typeVar = 4
			}
			return loctype.isTypeCached(typeVar)
		}
	}
}
