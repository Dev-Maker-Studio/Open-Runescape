package com.jagex.game.runetek5.config.rsinterface

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.bas.BasTypeList
import com.jagex.game.runetek5.config.identitykit.IdkTypeList
import com.jagex.game.runetek5.config.npc.NpcTypeList
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.item.ItemTypeList
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.sequence.InterfaceSequence
import com.jagex.game.runetek5.config.sequence.MovingSequence
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.config.skybox.SkyboxTypeList
import com.jagex.game.runetek5.config.sun.SunTypeList
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCTypeCustomisation
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontCombo
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.font.FontProvider
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.light.Skybox
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.MediaType
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.Hashing
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.VarDomain

import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RECOLOR
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RETEXTURE

/**
 * @author Steven
 *
 */
class Component {

    enum class StackMode {
        NONUM,
        ALWAYS_NUM,
        WHEN_MULTIPLE
    }

    enum class UnkSetting1(@JvmField val value: Int) {
        UNK_0(0),
        UNK_1(1),
        UNK_2(2);

        companion object {
            @JvmStatic
            fun forId(id: Int): UnkSetting1? {
                return values().firstOrNull { it.value == id }
            }
        }
    }

    companion object {
        @JvmField
        val NATIVE_SPRITE_CACHE = LRUCache(3000000, 200)

        @JvmField
        val MODEL_CACHE = LRUCache(50)

        @JvmStatic
        var missingMedia = false

        @JvmField
        val INTERACTIVE_SPRITES = LRUCache(8)

        @JvmField
        val COMPONENT_LIGHTING = LRUCache(4)

        @JvmField
        val DEFAULT_SETTINGS = IFEvents(0, -1)

        @JvmField
        val ITEMS = NodeCollection()

        @JvmStatic
        fun fromHash(hash: Int): Component? {
            val interfaceId = hash shr 16
            if (Interface.INTERFACES!![interfaceId] == null || Interface.INTERFACES!![interfaceId]!!.getComponent(hash) == null) {
                val loaded = Interface.interfaceLoaded(interfaceId, null)
                if (!loaded) {
                    return null
                }
            }
            return Interface.INTERFACES!![interfaceId]!!.getComponent(hash)
        }

        @JvmStatic
        fun listIcomponent(interfaceId: Int, slotId: Int): Component? {
            val component = fromHash(interfaceId)
            if (slotId == -1)
                return component
            if (component != null && component.slotChildren != null && slotId < component.slotChildren!!.size)
                return component.slotChildren!![slotId]
            return null
        }

        @JvmStatic
        fun prepareSequenceOnInterface(component: Component) {
            if (component.contentType == ComponentContentType.CONTENT_TYPE_328) {
                if (PlayerUpdate.MY_PLAYER.displayName == null) {
                    component.primaryMediaId = 0
                    component.secondaryMediaId = 0
                } else {
                    component.spritePitch = 150
                    component.spriteRoll = (Math.sin(client.cycles / 40.0) * 256.0).toInt() and 0x7ff
                    component.mediaType = MediaType.PLAYER_MODEL
                    component.primaryMediaId = PlayerUpdate.myPlayerIndex
                    val displayName = PlayerUpdate.MY_PLAYER.displayName
                    component.secondaryMediaId = Utils.stringToInt(displayName!!)
                    val movingSequence = PlayerUpdate.MY_PLAYER.movingSequence
                    if (movingSequence != null) {
                        if (component.animatingSequence == null) {
                            component.animatingSequence = InterfaceSequence()
                        }
                        component.animation = movingSequence.getId()
                        component.animatingSequence!!.copy(movingSequence)
                    } else {
                        component.animatingSequence = null
                    }
                }
            }
        }

        @JvmStatic
        fun processCacheCycles() {
            NATIVE_SPRITE_CACHE.clean(50)
            MODEL_CACHE.clean(50)
            INTERACTIVE_SPRITES.clean(50)
            COMPONENT_LIGHTING.clean(50)
        }

        @JvmStatic
        fun update(components: Array<Component?>, parent: Int) {
            for (index in components.indices) {
                val component = components[index]
                if (component != null && component.parentId == parent && !InterfaceRenderer.isComponentHidden(component)) {
                    if (component.type == ComponentType.CONTAINER) {
                        update(components, component.idHash)
                        if (component.itemSlots != null) {
                            update(component.itemSlots!!, component.idHash)
                        }
                        val subinterface = client.OPEN_INTERFACES[component.idHash.toLong()]
                        if (subinterface != null) {
                            update(subinterface.subInterfaceId)
                        }
                    }
                    if (component.type == ComponentType.MODEL && component.animation != -1) {
                        if (component.animatingSequence == null) {
                            component.animatingSequence = InterfaceSequence()
                            component.animatingSequence!!.update(component.animation)
                        }
                        if (component.animatingSequence!!.progressCycle(client.animatingStep) && component.animatingSequence!!.needsReset()) {
                            component.animatingSequence!!.reset()
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun resetCaches() {
            NATIVE_SPRITE_CACHE.reset()
            MODEL_CACHE.reset()
            INTERACTIVE_SPRITES.reset()
            COMPONENT_LIGHTING.reset()
        }

        @JvmStatic
        fun getCursor(component: Component, option: Int): Int {
            return if (!client.getComponentSettings(component).clickOptionEnabled(option) && component.params == null)
                -1
            else
                if (component.opCursors != null && component.opCursors!!.size > option) component.opCursors!![option] else -1
        }

        @JvmStatic
        fun drawComponentObjSprite(component: Component) {
            if (component.type == ComponentType.SPRITE && component.containerItemId != -1) {
                InterfaceComponentRenderer.drawObjSprite(Renderers.activeToolkit!!, component)
            }
        }

        @JvmStatic
        fun clearQueues() {
            NATIVE_SPRITE_CACHE.resize()
            MODEL_CACHE.resize()
            INTERACTIVE_SPRITES.resize()
            COMPONENT_LIGHTING.resize()
        }

        @JvmStatic
        fun componentMenuOption(component: Component, option: Int): String? {
            return if (!client.getComponentSettings(component).clickOptionEnabled(option) && component.params == null)
                null
            else
                if (component.options != null && component.options!!.size > option && component.options!![option] != null && component.options!![option]!!.trim().isNotEmpty())
                    component.options!![option]
                else
                    if (client.DISPLAY_HIDDEN_MENU_OPTIONS) "Hidden-$option" else null
        }

        @JvmStatic
        fun getDepthChild(icomponentdefinitions0: Component): Component? {
            var icomponentdefinitions2 = client.method11634(icomponentdefinitions0)
            if (icomponentdefinitions2 == null) {
                icomponentdefinitions2 = icomponentdefinitions0.depthChildOverride
            }
            return icomponentdefinitions2
        }

        @JvmStatic
        fun getUseOptionFlags(settings: Int): Int {
            return settings shr 11 and 0x7f
        }

        @JvmStatic
        fun runScriptsIfLoaded(interfaceId: Int, xteas: IntArray?) {
            if (interfaceId != -1 && Interface.interfaceLoaded(interfaceId, xteas)) {
                val components = Interface.INTERFACES!![interfaceId]!!.components
                runComponentOnloadScripts(components!!)
            }
        }

        @JvmStatic
        fun update(interfaceId: Int) {
            if (Interface.interfaceLoaded(interfaceId, null)) {
                update(Interface.INTERFACES!![interfaceId]!!.components!!, -1)
            }
        }

        @JvmStatic
        fun method3629(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
            if (!Interface.interfaceLoaded(i_0, null)) {
                if (i_7 != -1) {
                    client.IS_COMPONENT_ON_TOP[i_7] = true
                } else {
                    for (i_9 in 0 until 107) {
                        client.IS_COMPONENT_ON_TOP[i_9] = true
                    }
                }
            } else {
                InterfaceComponentRenderer.drawComponents(Interface.INTERFACES!![i_0]!!.getComponents(), -1, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_7 < 0)
            }
        }

        @JvmStatic
        fun method483(interface0: Interface, icomponentdefinitions1: Component, bool2: Boolean) {
            InterfaceComponentRenderer.updateEntityOnInterface(interface0.components!!, icomponentdefinitions1, bool2)
        }

        @JvmStatic
        fun runComponentOnloadScripts(components: Array<Component?>) {
            for (index in components.indices) {
                val component = components[index]
                if (component != null && component.onLoadScript != null) {
                    val hookRequests = HookRequest()
                    hookRequests.source = component
                    hookRequests.params = component.onLoadScript
                    CS2Executor.execute_hook_inner(hookRequests, 2000000)
                }
            }
        }

        @JvmStatic
        fun resetCache(i_0: Int) {
            Interface.preferenceMask = i_0
            MODEL_CACHE.reset()
        }

        @JvmStatic
        fun redrawComponent(component: Component) {
            if (component.lastRenderedCycle == client.anInt7408) {
                client.IS_COMPONENT_ON_TOP[component.cacheReference] = true
            }
        }
    }

    @JvmField
    var modelTintBlue: Int = 0

    @JvmField
    var npcCustomisation: NPCTypeCustomisation? = null

    @JvmField
    var modelTintRed: Int = 0

    @JvmField
    var modelTintGreen: Int = 0

    @JvmField
    var modelTintScalar: Int = 0

    @JvmField
    var anObjectArray1407: Array<Any?>? = null

    @JvmField
    var anIntArray1408: IntArray? = null

    @JvmField
    var serverTriggers: Int = 0

    @JvmField
    var anObjectArray1418: Array<Any?>? = null

    @JvmField
    var cycles: IntArray? = null

    @JvmField
    var sunRotationX: Int = 0

    @JvmField
    var sunRotationY: Int = 0

    @JvmField
    var lightPositionX: Int = 0

    @JvmField
    var lightPositionY: Int = 0

    @JvmField
    var lightPositionZ: Int = 0

    @JvmField
    var animatingSequence: Sequence? = null

    @JvmField
    var slotChildren: Array<Component>? = null

    @JvmField
    var itemSlots: Array<Component?>? = null

    @JvmField
    var type: ComponentType? = null

    @JvmField
    var name: String? = null

    @JvmField
    var contentType: ComponentContentType? = null

    @JvmField
    var basePositionX: Int = 0

    @JvmField
    var basePositionY: Int = 0

    @JvmField
    var baseWidth: Int = 0

    @JvmField
    var baseHeight: Int = 0

    @JvmField
    var aspectWidthType: Byte = 0

    @JvmField
    var aspectHeightType: Byte = 0

    @JvmField
    var aspectXType: Byte = 0

    @JvmField
    var aspectYType: Byte = 0

    @JvmField
    var parentId: Int = -1

    @JvmField
    var hidden: Boolean = false

    @JvmField
    var scrollWidth: Int = 0

    @JvmField
    var scrollHeight: Int = 0

    @JvmField
    var preventClickThrough: Boolean = false

    @JvmField
    var spriteId: Int = -1

    @JvmField
    var angle2d: Int = 0

    @JvmField
    var mediaType: MediaType = MediaType.RAW_MODEL

    @JvmField
    var primaryMediaId: Int = 0

    @JvmField
    var tiling: Boolean = false

    @JvmField
    var fontId: Int = -1

    @JvmField
    var text: String = ""

    @JvmField
    var color: Int = 0

    @JvmField
    var alpha: Boolean = false

    @JvmField
    var transparency: Int = 0

    @JvmField
    var borderThickness: Int = 0

    @JvmField
    var spriteShadow: Int = 0

    @JvmField
    var anInt1358: Int = 0

    @JvmField
    var textHorizontalAli: Int = 0

    @JvmField
    var textVerticalAli: Int = 0

    @JvmField
    var lineWidth: Int = 1

    @JvmField
    var hasOrigin: Boolean = false

    @JvmField
    var monospaced: Boolean = true

    @JvmField
    var filled: Boolean = false

    @JvmField
    var keyTriggeringAction: Array<ByteArray>? = null

    @JvmField
    var keyTriggerParams: Array<ByteArray>? = null

    @JvmField
    var cycleSteps: IntArray? = null

    @JvmField
    var anIntArray1267: IntArray? = null

    @JvmField
    var opBase: String? = null

    @JvmField
    var flipVertical: Boolean = false

    @JvmField
    var shadow: Boolean = false

    @JvmField
    var lineDirection: Boolean = false

    @JvmField
    var options: Array<String?>? = null

    @JvmField
    var usesOrthogonal: Boolean = false

    @JvmField
    var maxTextLines: Int = 0

    @JvmField
    var opCursors: IntArray? = null

    @JvmField
    var flipHorizontal: Boolean = false

    @JvmField
    var resumeText: String? = null

    @JvmField
    var priorityRender: Boolean = false

    @JvmField
    var hasInteraction: Boolean = false

    @JvmField
    var anInt1380: Int = 0

    @JvmField
    var anInt1381: Int = 0

    @JvmField
    var anUnkSetting11: UnkSetting1? = null

    @JvmField
    var targetVerb: String? = null

    @JvmField
    var originX: Int = 0

    @JvmField
    var originY: Int = 0

    @JvmField
    var spritePitch: Int = 0

    @JvmField
    var spriteRoll: Int = 0

    @JvmField
    var spriteYaw: Int = 0

    @JvmField
    var spriteScale: Int = 100

    @JvmField
    var clickMask: Boolean = true

    @JvmField
    var originZ: Int = 0

    @JvmField
    var animation: Int = 0

    @JvmField
    var targetOverCursor: Int = -1

    @JvmField
    var moveOverCursor: Int = -1

    @JvmField
    var targetParams: IFEvents? = null

    @JvmField
    var aspectWidth: Int = 0

    @JvmField
    var targetLeaveCursor: Int = -1

    @JvmField
    var onLoadScript: Array<Any?>? = null

    @JvmField
    var onMouseOver: Array<Any?>? = null

    @JvmField
    var onMouseLeaveScript: Array<Any?>? = null

    @JvmField
    var hookParams: Array<Any?>? = null

    @JvmField
    var anObjectArray1400: Array<Any?>? = null

    @JvmField
    var anObjectArray1397: Array<Any?>? = null

    @JvmField
    var mouseLeaveScript: Array<Any?>? = null

    @JvmField
    var anObjectArray1387: Array<Any?>? = null

    @JvmField
    var anObjectArray1409: Array<Any?>? = null

    @JvmField
    var params: Array<Any?>? = null

    @JvmField
    var aspectHeight: Int = 0

    @JvmField
    var anObjectArray1393: Array<Any?>? = null

    @JvmField
    var popupScript: Array<Any?>? = null

    @JvmField
    var onClick: Array<Any?>? = null

    @JvmField
    var anObjectArray1319: Array<Any?>? = null

    @JvmField
    var onRelease: Array<Any?>? = null

    @JvmField
    var onHold: Array<Any?>? = null

    @JvmField
    var anObjectArray1451: Array<Any?>? = null

    @JvmField
    var anObjectArray1394: Array<Any?>? = null

    @JvmField
    var onMouseMove: Array<Any?>? = null

    @JvmField
    var onKey: Array<Any?>? = null

    @JvmField
    var onScrollWheel: Array<Any?>? = null

    @JvmField
    var varps: IntArray? = null

    @JvmField
    var mouseLeaveArrayParams: IntArray? = null

    @JvmField
    var anIntArray1402: IntArray? = null

    @JvmField
    var keyPressArray: IntArray? = null

    @JvmField
    var mouseWheelArray: IntArray? = null

    @JvmField
    var anObjectArray1413: Array<Any?>? = null

    @JvmField
    var anObjectArray1292: Array<Any?>? = null

    @JvmField
    var anObjectArray1415: Array<Any?>? = null

    @JvmField
    var anObjectArray1416: Array<Any?>? = null

    @JvmField
    var anObjectArray1383: Array<Any?>? = null

    @JvmField
    var anObjectArray1419: Array<Any?>? = null

    @JvmField
    var anObjectArray1361: Array<Any?>? = null

    @JvmField
    var hookrequestType0Params: Array<Any?>? = null

    @JvmField
    var hookrequestType1Params: Array<Any?>? = null

    @JvmField
    var anObjectArray1353: Array<Any?>? = null

    @JvmField
    var anObjectArray1271: Array<Any?>? = null

    @JvmField
    var hasScript: Boolean = false

    @JvmField
    var particleSystem: ParticleSystem? = null

    @JvmField
    var idHash: Int = -1

    @JvmField
    var slotId: Int = -1

    @JvmField
    var x: Int = 0

    @JvmField
    var y: Int = 0

    @JvmField
    var width: Int = 0

    @JvmField
    var height: Int = 0

    @JvmField
    var aspectX: Int = 1

    @JvmField
    var aspectY: Int = 1

    @JvmField
    var scrollX: Int = 0

    @JvmField
    var scrollY: Int = 0

    @JvmField
    var secondaryMediaId: Int = -1

    @JvmField
    var offsetX: Int = 0

    @JvmField
    var offsetY: Int = 0

    @JvmField
    var renderStack: StackMode = StackMode.WHEN_MULTIPLE

    @JvmField
    var textAntiMacro: Boolean = false

    @JvmField
    var targetOpCursor: Int = 0

    @JvmField
    var depthChildOverride: Component? = null

    @JvmField
    var containerItemId: Int = 0

    @JvmField
    var itemStackSize: Int = 0

    @JvmField
    var wearCol: Boolean = false

    @JvmField
    var skyboxId: Int = 0

    @JvmField
    var anInt1435: Int = 0

    @JvmField
    var isHooked: Boolean = false

    @JvmField
    var isPressed: Boolean = false

    @JvmField
    var anInt1442: Int = 0

    @JvmField
    var anInt1443: Int = 0

    @JvmField
    var anInt1444: Int = 0

    @JvmField
    var anInt1445: Int = 0

    @JvmField
    var lastKeyCycle: Int = 0

    @JvmField
    var lastMouseWheelCycle: Int = 0

    @JvmField
    var anInt1448: Int = 0

    @JvmField
    var cacheReference: Int = 0

    @JvmField
    var lastRenderedCycle: Int = 0

    @JvmField
    var aClass4651365: IterableNodeMap<Node<*>>? = null

    @JvmField
    var recolorSrc: ShortArray? = null

    @JvmField
    var recolorDst: ShortArray? = null

    @JvmField
    var retextureSrc: ShortArray? = null

    @JvmField
    var retextureDst: ShortArray? = null

    init {
        this.targetParams = DEFAULT_SETTINGS
        this.hasInteraction = false
        this.opBase = ""
        this.targetOpCursor = -1
        this.depthChildOverride = null
        this.anInt1380 = 0
        this.anInt1381 = 0
        this.anUnkSetting11 = UnkSetting1.UNK_0
        this.targetVerb = ""
        this.hasScript = false
        this.containerItemId = -1
        this.itemStackSize = 0
        this.wearCol = false
        this.skyboxId = -1
        this.anInt1435 = -1
        this.animation = -1
        this.isHooked = false
        this.isPressed = false
        this.anInt1442 = -1
        this.anInt1443 = 0
        this.anInt1444 = 0
        this.anInt1445 = 0
        this.lastKeyCycle = 0
        this.lastMouseWheelCycle = 0
        this.anInt1448 = 0
        this.cacheReference = -1
        this.lastRenderedCycle = -1
    }

    fun getAnimatedMediaModel(
        toolkit: AbstractToolkit,
        functionMask: Int,
        bastypelist: BasTypeList,
        idktypelist: IdkTypeList,
        npctypelist: NpcTypeList,
        objtypelist: ItemTypeList,
        varprovider: VarDomain,
        sequence: Sequence?,
        playerappearance: PlayerAppearance?
    ): Model? {
        var functionMaskVar = functionMask
        missingMedia = false
        if (this.mediaType == MediaType.UNDEFINED) {
            return null
        }
        if (this.mediaType == MediaType.RAW_MODEL && this.primaryMediaId == -1) {
            return null
        }
        if (this.mediaType == MediaType.RAW_MODEL) {
            if (sequence != null) {
                functionMaskVar = functionMaskVar or sequence.getFunctionMask()
            }
            var crc64 = -1L
            val crcTable = Hashing.CRC64_TABLE
            if (this.recolorSrc != null) {
                for (index in this.recolorSrc!!.indices) {
                    crc64 = crc64 ushr 8 xor crcTable[((crc64 xor (this.recolorSrc!![index].toInt() shr 8).toLong()) and 0xffL).toInt()]
                    crc64 = crc64 ushr 8 xor crcTable[((crc64 xor this.recolorSrc!![index].toLong()) and 0xffL).toInt()]
                    crc64 = crc64 ushr 8 xor crcTable[((crc64 xor (this.recolorDst!![index].toInt() shr 8).toLong()) and 0xffL).toInt()]
                    crc64 = crc64 ushr 8 xor crcTable[((crc64 xor this.recolorDst!![index].toLong()) and 0xffL).toInt()]
                }
                functionMaskVar = functionMaskVar or RECOLOR
            }
            if (this.retextureSrc != null) {
                for (index in this.retextureSrc!!.indices) {
                    crc64 = crc64 ushr 8 xor crcTable[((crc64 xor (this.retextureSrc!![index].toInt() shr 8).toLong()) and 0xffL).toInt()]
                    crc64 = crc64 ushr 8 xor crcTable[((crc64 xor this.retextureSrc!![index].toLong()) and 0xffL).toInt()]
                    crc64 = crc64 ushr 8 xor crcTable[((crc64 xor (this.retextureDst!![index].toInt() shr 8).toLong()) and 0xffL).toInt()]
                    crc64 = crc64 ushr 8 xor crcTable[((crc64 xor this.retextureDst!![index].toLong()) and 0xffL).toInt()]
                }
                functionMaskVar = functionMaskVar or RETEXTURE
            }
            val uid = toolkit.renderId.toLong() shl 59 or (this.mediaType.getId().toLong() shl 54) or (this.primaryMediaId.toLong() shl 38) or (crc64 and 0x3fffffffffL)
            var model = MODEL_CACHE.get(uid) as? Model
            if (model == null || toolkit.mismatchingFunctionMask(model!!.getFunctionMask(), functionMaskVar) != 0) {
                if (model != null) {
                    functionMaskVar = toolkit.mergeFunctionMasks(functionMaskVar, model.getFunctionMask())
                }
                val mesh = Mesh.fromResource(Resource.MESH, this.primaryMediaId)
                if (mesh == null) {
                    missingMedia = true
                    return null
                }
                if (mesh.version < 13) {
                    mesh.upscale()
                }
                model = toolkit.createModel(mesh, functionMaskVar, Interface.preferenceMask, 64, 768)
                if (this.recolorSrc != null) {
                    for (index in this.recolorSrc!!.indices) {
                        model.recolor(this.recolorSrc!![index], this.recolorDst!![index])
                    }
                }
                if (this.retextureSrc != null) {
                    for (index in this.retextureSrc!!.indices) {
                        model.rematerial(this.retextureSrc!![index], this.retextureDst!![index])
                    }
                }
                MODEL_CACHE.put(model, uid)
            }
            if (sequence != null) {
                model = model!!.copy(1.toByte(), functionMaskVar, true)
                sequence.animateRotatedModel(model!!, 0)
            }
            model!!.applyShading(functionMaskVar)
            return model
        } else {
            if (this.mediaType == MediaType.NPC_HEAD) {
                val npcHeadModel = npctypelist.list(this.primaryMediaId).getAnimatedHeadmodel(toolkit, functionMaskVar, varprovider, sequence, this.npcCustomisation)
                if (npcHeadModel == null) {
                    missingMedia = true
                    return null
                }
                return npcHeadModel
            } else if (this.mediaType == MediaType.PLAYER_HEAD) {
                if (playerappearance == null) {
                    return null
                }
                val decoratedHeadModel = playerappearance.getAnimatedHeadModel(toolkit, functionMaskVar, idktypelist, npctypelist, objtypelist, varprovider, sequence)
                if (decoratedHeadModel == null) {
                    missingMedia = true
                    return null
                }
                return decoratedHeadModel
            } else if (this.mediaType == MediaType.ITEM) {
                val objtype = objtypelist.list(this.primaryMediaId)
                val objModel = objtype.getAnimatedModel(toolkit, functionMaskVar, 10, playerappearance, sequence, 0, 0, 0, 0)
                if (objModel == null) {
                    missingMedia = true
                    return null
                }
                return objModel
            } else if (this.mediaType == MediaType.NPC_MODEL) {
                val npcBodyModel = npctypelist.list(this.primaryMediaId).getAnimatedBodymodel(toolkit, functionMaskVar, bastypelist, varprovider, sequence, null, null, null, 0, this.npcCustomisation)
                if (npcBodyModel == null) {
                    missingMedia = true
                    return null
                }
                return npcBodyModel
            } else if (this.mediaType == MediaType.PLAYER_HEAD_IGNOREWORN) {
                if (playerappearance == null) {
                    return null
                }
                val primaryHead = this.primaryMediaId ushr 16
                val secondaryHead = this.primaryMediaId and 0xffff
                val tertiaryHead = this.secondaryMediaId
                val animatedHeadModel = playerappearance.getPlainAnimatedHeadModel(toolkit, functionMaskVar, idktypelist, sequence, primaryHead, secondaryHead, tertiaryHead)
                if (animatedHeadModel == null) {
                    missingMedia = true
                    return null
                }
                return animatedHeadModel
            }
            return null
        }
    }

    fun draw(toolkit: AbstractToolkit): NativeSprite? {
        missingMedia = false
        val spriteHash = ((if (this.flipHorizontal) 1L else 0L) shl 39) + ((if (this.alpha) 1L else 0L) shl 35) + this.spriteId + (this.borderThickness.toLong() shl 36) + ((if (this.flipVertical) 1L else 0L) shl 38) + (this.spriteShadow.toLong() shl 40)
        var nativeSprite = NATIVE_SPRITE_CACHE.get(spriteHash) as? NativeSprite
        if (nativeSprite != null) {
            return nativeSprite
        }
        var spriteType = SpriteType.listSingle(StaticMethods.SPRITES_INDEX, this.spriteId, 0)
        if (spriteType == null) {
            missingMedia = true
            return null
        }
        if (this.flipVertical) {
            spriteType.flipVertical()
        }
        if (this.flipHorizontal) {
            spriteType.flipHorizontal()
        }
        if (this.borderThickness > 0) {
            spriteType.drawSmallBorder(this.borderThickness)
        } else if (this.spriteShadow != 0) {
            spriteType.drawSmallBorder(1)
        }
        if (this.borderThickness >= 1) {
            spriteType.drawThickBorder(1)
        }
        if (this.borderThickness >= 2) {
            spriteType.drawThickBorder(16777215)
        }
        if (this.spriteShadow != 0) {
            spriteType.outline(0xffffff.inv() or this.spriteShadow)
        }
        nativeSprite = toolkit.generateSprite(spriteType, true)
        NATIVE_SPRITE_CACHE.put(nativeSprite, spriteHash, nativeSprite.getWidth() * nativeSprite.getHeight() * 4)
        return nativeSprite
    }

    fun recolor(index: Int, srcColor: Short, dstColor: Short) {
        if (index < 5) {
            if (this.recolorSrc == null) {
                this.recolorSrc = ShortArray(5)
                this.recolorDst = ShortArray(5)
            }
            this.recolorSrc!![index] = srcColor
            this.recolorDst!![index] = dstColor
        }
    }

    fun retexture(index: Int, srcTexture: Short, dstTexture: Short) {
        if (index < 5) {
            if (this.retextureSrc == null) {
                this.retextureSrc = ShortArray(5)
                this.retextureDst = ShortArray(5)
            }
            this.retextureSrc!![index] = srcTexture
            this.retextureDst!![index] = dstTexture
        }
    }

    fun decode(buffer: Packet) {
        var i_3 = buffer.readUByte()
        if (i_3 == 255) {
            i_3 = -1
        }
        var typeId = buffer.readUByte()
        if ((typeId and 0x80) != 0) {
            typeId = typeId and 0x7f
            this.name = buffer.readString()
        }
        this.type = ComponentType.forId(typeId)
        this.contentType = ComponentContentType.forId(buffer.readUShort())
        this.basePositionX = buffer.readShort()
        this.basePositionY = buffer.readShort()
        this.baseWidth = buffer.readUShort()
        this.baseHeight = buffer.readUShort()
        this.aspectWidthType = buffer.readByte()
        this.aspectHeightType = buffer.readByte()
        this.aspectXType = buffer.readByte()
        this.aspectYType = buffer.readByte()
        this.parentId = buffer.readUShort()
        if (this.parentId == 65535) {
            this.parentId = -1
        } else {
            this.parentId += this.idHash and -65536
        }
        val interationMask = buffer.readUByte()
        this.hidden = (interationMask and 0x1) != 0
        if (i_3 >= 0) {
            this.preventClickThrough = (interationMask and 0x2) != 0
        }
        if (this.type == ComponentType.CONTAINER) {
            this.scrollWidth = buffer.readUShort()
            this.scrollHeight = buffer.readUShort()
            if (i_3 < 0) {
                this.preventClickThrough = buffer.readUByte() == 1
            }
        }
        if (this.type == ComponentType.SPRITE) {
            this.spriteId = buffer.readInt()
            this.angle2d = buffer.readUShort()
            val displayMask = buffer.readUByte()
            this.tiling = (displayMask and 0x1) != 0
            this.alpha = (displayMask and 0x2) != 0
            this.transparency = buffer.readUByte()
            this.borderThickness = buffer.readUByte()
            this.spriteShadow = buffer.readInt()
            this.flipVertical = buffer.readUByte() == 1
            this.flipHorizontal = buffer.readUByte() == 1
            this.color = buffer.readInt()
            if (i_3 >= 3) {
                this.clickMask = buffer.readUByte() == 1
            }
        }
        if (this.type == ComponentType.MODEL) {
            this.mediaType = MediaType.RAW_MODEL
            this.primaryMediaId = buffer.readBigSmart()
            val flag2 = buffer.readUByte()
            val bool6 = (flag2 and 0x1) == 1
            this.hasOrigin = (flag2 and 0x2) == 2
            this.usesOrthogonal = (flag2 and 0x4) == 4
            this.priorityRender = (flag2 and 0x8) == 8
            if (bool6) {
                this.originX = buffer.readShort()
                this.originY = buffer.readShort()
                this.spritePitch = buffer.readUShort()
                this.spriteRoll = buffer.readUShort()
                this.spriteYaw = buffer.readUShort()
                this.spriteScale = buffer.readUShort()
            } else if (this.hasOrigin) {
                this.originX = buffer.readShort()
                this.originY = buffer.readShort()
                this.originZ = buffer.readShort()
                this.spritePitch = buffer.readUShort()
                this.spriteRoll = buffer.readUShort()
                this.spriteYaw = buffer.readUShort()
                this.spriteScale = buffer.readShort()
            }
            this.animation = buffer.readBigSmart()
            if (this.aspectWidthType.toInt() != 0) {
                this.aspectWidth = buffer.readUShort()
            }
            if (this.aspectHeightType.toInt() != 0) {
                this.aspectHeight = buffer.readUShort()
            }
        }
        if (this.type == ComponentType.TEXT) {
            this.fontId = buffer.readBigSmart()
            if (i_3 >= 2) {
                this.monospaced = buffer.readUByte() == 1
            }
            this.text = buffer.readString()
            if (this.text.lowercase().contains("runescape")) {
                this.text = this.text.replace("runescape", "Darkan", true)
                this.text = this.text.replace("RuneScape", "Darkan", true)
                this.text = this.text.replace("Runescape", "Darkan", true)
            }
            this.anInt1358 = buffer.readUByte()
            this.textHorizontalAli = buffer.readUByte()
            this.textVerticalAli = buffer.readUByte()
            this.shadow = buffer.readUByte() == 1
            this.color = buffer.readInt()
            this.transparency = buffer.readUByte()
            if (i_3 >= 0) {
                this.maxTextLines = buffer.readUByte()
            }
        }
        if (this.type == ComponentType.FIGURE) {
            this.color = buffer.readInt()
            this.filled = buffer.readUByte() == 1
            this.transparency = buffer.readUByte()
        }
        if (this.type == ComponentType.LINE) {
            this.lineWidth = buffer.readUByte()
            this.color = buffer.readInt()
            this.lineDirection = buffer.readUByte() == 1
        }
        val optionMask = buffer.readUMedium()
        var i_16 = buffer.readUByte()
        var i_7: Int
        if (i_16 != 0) {
            this.keyTriggeringAction = Array(11) { ByteArray(0) }
            this.keyTriggerParams = Array(11) { ByteArray(0) }
            this.cycleSteps = IntArray(11)
            this.anIntArray1267 = IntArray(11)
            while (i_16 != 0) {
                i_7 = (i_16 shr 4) - 1
                i_16 = i_16 shl 8 or buffer.readUByte()
                i_16 = i_16 and 0xfff
                if (i_16 == 4095) {
                    i_16 = -1
                }
                val b_8 = buffer.readByte()
                if (b_8.toInt() != 0) {
                    this.hasInteraction = true
                }
                val b_9 = buffer.readByte()
                this.cycleSteps!![i_7] = i_16
                this.keyTriggeringAction!![i_7] = byteArrayOf(b_8)
                this.keyTriggerParams!![i_7] = byteArrayOf(b_9)
                i_16 = buffer.readUByte()
            }
        }
        this.opBase = buffer.readString()
        if (Loader.DEBUG_IF_OPS)
            this.opBase += "(opBase)"
        i_7 = buffer.readUByte()
        val i_17 = i_7 and 0xf
        val i_18 = i_7 shr 4
        var i_10: Int
        if (i_17 > 0) {
            this.options = Array(i_17) { "" }
            i_10 = 0
            while (i_10 < i_17) {
                this.options!![i_10] = buffer.readString()
                i_10++
            }
        }
        var i_11: Int
        if (i_18 > 0) {
            i_10 = buffer.readUByte()
            this.opCursors = IntArray(i_10 + 1)
            i_11 = 0
            while (i_11 < this.opCursors!!.size) {
                this.opCursors!![i_11] = -1
                i_11++
            }
            this.opCursors!![i_10] = buffer.readUShort()
        }
        if (i_18 > 1) {
            i_10 = buffer.readUByte()
            this.opCursors!![i_10] = buffer.readUShort()
        }
        this.resumeText = buffer.readString()
        if (Loader.DEBUG_IF_OPS)
            this.resumeText += "(resumeText)"
        if (this.resumeText!!.isEmpty()) {
            this.resumeText = null
        }
        this.anInt1380 = buffer.readUByte()
        this.anInt1381 = buffer.readUByte()
        this.anUnkSetting11 = UnkSetting1.forId(buffer.readUByte())
        this.targetVerb = buffer.readString()
        if (Loader.DEBUG_IF_OPS)
            this.targetVerb += "(targetVerb)"
        i_10 = -1
        if (getUseOptionFlags(optionMask) != 0) {
            i_10 = buffer.readUShort()
            if (i_10 == 65535) {
                i_10 = -1
            }
            this.targetOverCursor = buffer.readUShort()
            if (this.targetOverCursor == 65535) {
                this.targetOverCursor = -1
            }
            this.targetLeaveCursor = buffer.readUShort()
            if (this.targetLeaveCursor == 65535) {
                this.targetLeaveCursor = -1
            }
        }
        if (i_3 >= 0) {
            this.moveOverCursor = buffer.readUShort()
            if (this.moveOverCursor == 65535) {
                this.moveOverCursor = -1
            }
        }
        this.targetParams = IFEvents(optionMask, i_10)
        if (i_3 >= 0) {
            i_11 = buffer.readUByte()
            var i_12: Int
            var i_13: Int
            var i_14: Int
            i_12 = 0
            while (i_12 < i_11) {
                i_13 = buffer.readUMedium()
                i_14 = buffer.readInt()
                this.aClass4651365!!.put(IntNode(i_14), i_13.toLong())
                i_12++
            }
            i_12 = buffer.readUByte()
            i_13 = 0
            while (i_13 < i_12) {
                i_14 = buffer.readUMedium()
                val string_15 = buffer.readJagString()
                this.aClass4651365!!.put(ObjectNode(string_15), i_14.toLong())
                i_13++
            }
        }
        this.onLoadScript = decodeScript(buffer)
        this.onMouseOver = decodeScript(buffer)
        this.onMouseLeaveScript = decodeScript(buffer)
        this.hookParams = decodeScript(buffer)
        this.anObjectArray1400 = decodeScript(buffer)
        this.anObjectArray1397 = decodeScript(buffer)
        this.mouseLeaveScript = decodeScript(buffer)
        this.anObjectArray1387 = decodeScript(buffer)
        this.anObjectArray1409 = decodeScript(buffer)
        this.params = decodeScript(buffer)
        if (i_3 >= 0) {
            this.anObjectArray1393 = decodeScript(buffer)
        }
        this.popupScript = decodeScript(buffer)
        this.onClick = decodeScript(buffer)
        this.anObjectArray1319 = decodeScript(buffer)
        this.onRelease = decodeScript(buffer)
        this.onHold = decodeScript(buffer)
        this.anObjectArray1451 = decodeScript(buffer)
        this.anObjectArray1394 = decodeScript(buffer)
        this.onMouseMove = decodeScript(buffer)
        this.onKey = decodeScript(buffer)
        this.onScrollWheel = decodeScript(buffer)
        this.varps = decodeScript3(buffer)
        this.mouseLeaveArrayParams = decodeScript3(buffer)
        this.anIntArray1402 = decodeScript3(buffer)
        this.keyPressArray = decodeScript3(buffer)
        this.mouseWheelArray = decodeScript3(buffer)
    }

    fun reset() {
        this.onLoadScript = null
        this.onClick = null
        this.anObjectArray1319 = null
        this.onRelease = null
        this.onHold = null
        this.onMouseOver = null
        this.popupScript = null
        this.onMouseLeaveScript = null
        this.anObjectArray1451 = null
        this.anObjectArray1394 = null
        this.anObjectArray1400 = null
        this.hookParams = null
        this.anObjectArray1397 = null
        this.varps = null
        this.mouseLeaveScript = null
        this.mouseLeaveArrayParams = null
        this.anObjectArray1387 = null
        this.anIntArray1402 = null
        this.onKey = null
        this.keyPressArray = null
        this.onScrollWheel = null
        this.mouseWheelArray = null
        this.anObjectArray1409 = null
        this.params = null
        this.anObjectArray1393 = null
        this.onMouseMove = null
        this.anObjectArray1413 = null
        this.anObjectArray1292 = null
        this.anObjectArray1415 = null
        this.anObjectArray1416 = null
        this.anObjectArray1383 = null
        this.anObjectArray1419 = null
        this.anObjectArray1361 = null
        this.hookrequestType0Params = null
        this.hookrequestType1Params = null
        this.anObjectArray1353 = null
        this.anObjectArray1271 = null
    }

    fun getFontRenderer(fontCombo: FontCombo, fontProvider: FontProvider): AbstractFontRenderer? {
        val fontRenderer = fontCombo.list(fontProvider, this.fontId, false, this.monospaced) as? AbstractFontRenderer
        missingMedia = fontRenderer == null
        return fontRenderer
    }

    fun getFontMetrics(fontCombo: FontCombo, fontProvider: FontProvider): FontMetrics? {
        val font = fontCombo.list(fontProvider, this.fontId)
        missingMedia = font == null
        return font
    }

    fun decodeScript(buffer: Packet): Array<Any?>? {
        val scriptCount = buffer.readUByte()
        if (scriptCount == 0) {
            return null
        }
        val components = Array<Any?>(scriptCount) { null }
        for (index in 0 until scriptCount) {
            val type = buffer.readUByte()
            if (type == 0) {
                components[index] = buffer.readInt()
            } else if (type == 1) {
                components[index] = buffer.readString()
            }
        }
        this.hasScript = true
        return components
    }

    fun decodeScript3(buffer: Packet): IntArray? {
        val count = buffer.readUByte()
        if (count == 0) {
            return null
        }
        val ints_4 = IntArray(count)
        for (i_5 in 0 until count) {
            ints_4[i_5] = buffer.readInt()
        }
        return ints_4
    }

    fun cycleParticles(toolkit: AbstractToolkit, model: Model, matrix4x4: Matrix43, cycle: Int) {
        model.updateParticlePosition(matrix4x4)
        val emitters = model.getEmissiveTriangles()
        val effectiveVertices = model.getEffectiveVertices()
        if ((this.particleSystem == null || this.particleSystem!!.isDisabled) && (emitters != null || effectiveVertices != null)) {
            this.particleSystem = ParticleSystem.get(cycle, false)
        }
        if (this.particleSystem != null) {
            this.particleSystem!!.performCycle(toolkit!!, cycle.toLong(), emitters, effectiveVertices)
        }
    }

    fun addOptionName(index: Int, optionName: String) {
        if (this.options == null || this.options!!.size <= index) {
            val replacementOptionNames: Array<String?> = Array(index + 1) { null }
            if (this.options != null) {
                System.arraycopy(this.options, 0, replacementOptionNames, 0, this.options!!.size)
            }
            this.options = replacementOptionNames
        }
        this.options!![index] = optionName
    }

    fun method1995(i_1: Int) {
        if (this.aClass4651365 != null) {
            val node3 = this.aClass4651365!!.get(i_1.toLong())
            node3?.unlink()
        }
    }

    fun method1997(i_1: Int, i_2: Int): Int {
        return if (this.aClass4651365 == null) {
            i_2
        } else {
            val class282Sub384 = this.aClass4651365!!.get(i_1.toLong()) as? IntNode
            if (class282Sub384 == null) i_2 else class282Sub384.value
        }
    }

    fun method1998(i_1: Int, string_2: String): String {
        if (this.aClass4651365 == null) {
            return string_2
        }
        val class282Sub474 = this.aClass4651365!!.get(i_1.toLong()) as? ObjectNode
        return if (class282Sub474 == null) string_2 else class282Sub474.value as String
    }

    fun method1999(i_1: Int, i_2: Int) {
        if (this.aClass4651365 == null) {
            this.aClass4651365 = IterableNodeMap(16)
            this.aClass4651365!!.put(IntNode(i_2), i_1.toLong())
        } else {
            val class282Sub384 = this.aClass4651365!!.get(i_1.toLong()) as? IntNode
            if (class282Sub384 == null) {
                this.aClass4651365!!.put(IntNode(i_2), i_1.toLong())
            } else {
                class282Sub384.value = i_2
            }
        }
    }

    fun method2000(i_1: Int, string_2: String) {
        if (this.aClass4651365 == null) {
            this.aClass4651365 = IterableNodeMap(16)
        } else {
            val class282Sub474 = this.aClass4651365!!.get(i_1.toLong()) as? ObjectNode
            if (class282Sub474 != null) {
                class282Sub474.unlink()
            }
        }
        this.aClass4651365!!.put(ObjectNode(string_2), i_1.toLong())
    }

    fun calculateLighting(skyboxtypelist: SkyboxTypeList, suntypelist: SunTypeList): Skybox? {
        if (this.skyboxId == -1) {
            return null
        }
        val uid = (this.lightPositionX and 0xffff).toLong() shl 48 or
                ((this.lightPositionY and 0xffff).toLong() shl 32) or
                ((this.lightPositionZ and 0xffff).toLong() shl 16) or
                (this.skyboxId and 0xffff).toLong()
        var lighting = COMPONENT_LIGHTING.get(uid) as? Skybox
        if (lighting == null) {
            lighting = skyboxtypelist.generateSkybox(this.skyboxId, this.lightPositionX, this.lightPositionY, this.lightPositionZ, suntypelist)
            COMPONENT_LIGHTING.put(lighting, uid)
        }
        return lighting
    }

    fun addCursor(cursorCount: Int, cursor: Int) {
        if (this.opCursors == null || this.opCursors!!.size <= cursorCount) {
            val cursors = IntArray(cursorCount + 1)
            if (this.opCursors != null) {
                System.arraycopy(this.opCursors, 0, cursors, 0, this.opCursors!!.size)
                for (index in this.opCursors!!.size until cursorCount) {
                    cursors[index] = -1
                }
            }
            this.opCursors = cursors
        }
        this.opCursors!![cursorCount] = cursor
    }

    fun addInteractiveSprite(toolkit: AbstractToolkit): InteractiveComponentSprite? {
        val uid = this.idHash.toLong() shl 32 or (this.slotId.toLong() and 0xffffffffL)
        var sprite = INTERACTIVE_SPRITES.get(uid) as? InteractiveComponentSprite
        if (sprite != null) {
            if (sprite.spriteId != this.spriteId) {
                INTERACTIVE_SPRITES.removeByUid(uid)
                sprite = null
            }
            if (sprite != null) {
                return sprite
            }
        }
        val spriteType = SpriteType.listSingle(StaticMethods.SPRITES_INDEX, this.spriteId, 0)
        if (spriteType == null) {
            return null
        }
        val sizeX = spriteType.width + spriteType.minX + spriteType.borderX
        val sizeY = spriteType.height + spriteType.minY + spriteType.borderY
        val rowStartX = IntArray(sizeY)
        val rowWidth = IntArray(sizeY)
        for (y in 0 until spriteType.height) {
            var clipLeft = 0
            var clipRight = spriteType.width
            for (x in 0 until spriteType.width) {
                if (spriteType.pixels[x + y * spriteType.width].toInt() != 0) {
                    clipLeft = x
                    break
                }
            }
            var x = spriteType.width - 1
            while (x >= clipLeft) {
                if (spriteType.pixels[x + y * spriteType.width].toInt() != 0) {
                    clipRight = x + 1
                    break
                }
                --x
            }
            rowStartX[y + spriteType.minY] = clipLeft + spriteType.minX
            rowWidth[y + spriteType.minY] = clipRight - clipLeft
        }
        val surface = toolkit.createComponentSurface(sizeX, sizeY, rowStartX, rowWidth)
        if (surface == null) {
            return null
        }
        sprite = InteractiveComponentSprite(sizeX, sizeY, rowWidth, rowStartX, surface, this.spriteId)
        INTERACTIVE_SPRITES.put(sprite, uid)
        return sprite
    }

    fun isIgnoredContentType(): Boolean {
        return contentType == ComponentContentType.MINIMAP ||
                contentType == ComponentContentType.CONTENT_TYPE_1406 ||
                contentType == ComponentContentType.GAME_WINDOW ||
                contentType == ComponentContentType.CONTENT_TYPE_1403
    }
}
