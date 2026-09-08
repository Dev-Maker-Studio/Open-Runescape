package com.jagex.game.runetek5.config.rsinterface

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.bas.BasTypeList
import com.jagex.game.runetek5.config.identitykit.IdkTypeList
import com.jagex.game.runetek5.config.npc.NpcTypeList
import com.jagex.game.runetek5.config.item.ItemTypeList
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.sequence.InterfaceSequence
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

    enum class DragType(@JvmField val value: Int) {
        UNK_0(0),
        UNK_1(1),
        UNK_2(2);

        companion object {
            @JvmStatic
            fun forId(id: Int): DragType? {
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
                if (PlayerUpdate.MY_PLAYER!!.displayName == null) {
                    component.primaryMediaId = 0
                    component.secondaryMediaId = 0
                } else {
                    component.spritePitch = 150
                    component.spriteRoll = (Math.sin(Client.cycles / 40.0) * 256.0).toInt() and 0x7ff
                    component.mediaType = MediaType.PLAYER_MODEL
                    component.primaryMediaId = PlayerUpdate.myPlayerIndex
                    val displayName = PlayerUpdate.MY_PLAYER!!.displayName
                    component.secondaryMediaId = Utils.stringToInt(displayName!!)
                    val movingSequence = PlayerUpdate.MY_PLAYER!!.movingSequence
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
                        val subinterface = Client.OPEN_INTERFACES!![component.idHash.toLong()]
                        if (subinterface != null) {
                            update(subinterface.subInterfaceId)
                        }
                    }
                    if (component.type == ComponentType.MODEL && component.animation != -1) {
                        if (component.animatingSequence == null) {
                            component.animatingSequence = InterfaceSequence()
                            component.animatingSequence!!.update(component.animation)
                        }
                        if (component.animatingSequence!!.progressCycle(Client.animatingStep) && component.animatingSequence!!.needsReset()) {
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
            return if (!Client.getComponentSettings(component)!!.clickOptionEnabled(option) && component.params == null)
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
            return if (!Client.getComponentSettings(component)!!.clickOptionEnabled(option) && component.params == null)
                null
            else
                if (component.options != null && component.options!!.size > option && component.options!![option] != null && component.options!![option]!!.trim().isNotEmpty())
                    component.options!![option]
                else
                    if (Client.DISPLAY_HIDDEN_MENU_OPTIONS) "Hidden-$option" else null
        }

        @JvmStatic
        fun getDepthChild(component: Component): Component? {
            var depthChild = Client.getDragTarget(component)
            if (depthChild == null) {
                depthChild = component.depthChildOverride
            }
            return depthChild
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
        fun drawInterface(interfaceId: Int, startX: Int, startY: Int, endX: Int, endY: Int, offsetX: Int, offsetY: Int, layerIndex: Int) {
            if (!Interface.interfaceLoaded(interfaceId, null)) {
                if (layerIndex != -1) {
                    Client.IS_COMPONENT_ON_TOP[layerIndex] = true
                } else {
                    for (componentIndex in 0 until 107) {
                        Client.IS_COMPONENT_ON_TOP[componentIndex] = true
                    }
                }
            } else {
                InterfaceComponentRenderer.drawComponents(Interface.INTERFACES!![interfaceId]!!.getComponents(), -1, startX, startY, endX, endY, offsetX, offsetY, layerIndex, layerIndex < 0)
            }
        }

        @JvmStatic
        fun updateEntityLayout(rsInterface: Interface, component: Component, updateLayout: Boolean) {
            InterfaceComponentRenderer.updateEntityOnInterface(rsInterface.components!!, component, updateLayout)
        }

        @JvmStatic
        fun runComponentOnloadScripts(components: Array<Component?>) {
            for (index in components.indices) {
                val component = components[index]
                if (component != null && component.onLoadScript != null) {
                    val hookRequests = HookRequest()
                    hookRequests.source = component
                    hookRequests.params = component.onLoadScript
                    CS2Executor.executeHookInternal(hookRequests, 2000000)
                }
            }
        }

        @JvmStatic
        fun resetCache(preferenceMask: Int) {
            Interface.preferenceMask = preferenceMask
            MODEL_CACHE.reset()
        }

        @JvmStatic
        fun redrawComponent(component: Component) {
            if (component.lastRenderedCycle == Client.lastRenderedCycle) {
                Client.IS_COMPONENT_ON_TOP[component.cacheReference] = true
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
    var onClanVarTransmit: Array<Any?>? = null

    @JvmField
    var clanVarFilter: IntArray? = null

    @JvmField
    var serverTriggers: Int = 0

    @JvmField
    var onClanChannelTransmit: Array<Any?>? = null

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
    var lineSpacing: Int = 0

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
    var keyCharCodes: IntArray? = null

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
    var dragDeadzone: Int = 0

    @JvmField
    var dragDeadTime: Int = 0

    @JvmField
    var dragType: DragType? = null

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
    var onTargetEnter: Array<Any?>? = null

    @JvmField
    var onVarpTransmit: Array<Any?>? = null

    @JvmField
    var mouseLeaveScript: Array<Any?>? = null

    @JvmField
    var onStatTransmit: Array<Any?>? = null

    @JvmField
    var onTimer: Array<Any?>? = null

    @JvmField
    var params: Array<Any?>? = null

    @JvmField
    var aspectHeight: Int = 0

    @JvmField
    var onTargetLeave: Array<Any?>? = null

    @JvmField
    var popupScript: Array<Any?>? = null

    @JvmField
    var onClick: Array<Any?>? = null

    @JvmField
    var onClickRepeat: Array<Any?>? = null

    @JvmField
    var onRelease: Array<Any?>? = null

    @JvmField
    var onHold: Array<Any?>? = null

    @JvmField
    var onDrag: Array<Any?>? = null

    @JvmField
    var onDragComplete: Array<Any?>? = null

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
    var statTransmitFilter: IntArray? = null

    @JvmField
    var keyPressArray: IntArray? = null

    @JvmField
    var mouseWheelArray: IntArray? = null

    @JvmField
    var onChatTransmit: Array<Any?>? = null

    @JvmField
    var onKeyType: Array<Any?>? = null

    @JvmField
    var onFriendTransmit: Array<Any?>? = null

    @JvmField
    var onFriendsChatTransmit: Array<Any?>? = null

    @JvmField
    var onClanSettingsTransmit: Array<Any?>? = null

    @JvmField
    var onMiscTransmit: Array<Any?>? = null

    @JvmField
    var onStockTransmit: Array<Any?>? = null

    @JvmField
    var onSubClose: Array<Any?>? = null

    @JvmField
    var onSubChange: Array<Any?>? = null

    @JvmField
    var onCamFinished: Array<Any?>? = null

    @JvmField
    var onResize: Array<Any?>? = null

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
    var videoFileId: Int = 0

    @JvmField
    var isHooked: Boolean = false

    @JvmField
    var isPressed: Boolean = false

    @JvmField
    var lastServerTick: Int = 0

    @JvmField
    var lastVarpTransmitCycle: Int = 0

    @JvmField
    var lastContainerTransmitCycle: Int = 0

    @JvmField
    var lastStatTransmitCycle: Int = 0

    @JvmField
    var lastKeyCycle: Int = 0

    @JvmField
    var lastMouseWheelCycle: Int = 0

    @JvmField
    var lastClanVarTransmitCycle: Int = 0

    @JvmField
    var cacheReference: Int = 0

    @JvmField
    var lastRenderedCycle: Int = 0

    @JvmField
    var clientParams: IterableNodeMap<Node<*>>? = null

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
        this.dragDeadzone = 0
        this.dragDeadTime = 0
        this.dragType = DragType.UNK_0
        this.targetVerb = ""
        this.hasScript = false
        this.containerItemId = -1
        this.itemStackSize = 0
        this.wearCol = false
        this.skyboxId = -1
        this.videoFileId = -1
        this.animation = -1
        this.isHooked = false
        this.isPressed = false
        this.lastServerTick = -1
        this.lastVarpTransmitCycle = 0
        this.lastContainerTransmitCycle = 0
        this.lastStatTransmitCycle = 0
        this.lastKeyCycle = 0
        this.lastMouseWheelCycle = 0
        this.lastClanVarTransmitCycle = 0
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
                val mesh = Mesh.fromResource(Resource.MESH!!, this.primaryMediaId)
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
        var formatVersion = buffer.readUByte()
        if (formatVersion == 255) {
            formatVersion = -1
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
        if (formatVersion >= 0) {
            this.preventClickThrough = (interationMask and 0x2) != 0
        }
        if (this.type == ComponentType.CONTAINER) {
            this.scrollWidth = buffer.readUShort()
            this.scrollHeight = buffer.readUShort()
            if (formatVersion < 0) {
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
            if (formatVersion >= 3) {
                this.clickMask = buffer.readUByte() == 1
            }
        }
        if (this.type == ComponentType.MODEL) {
            this.mediaType = MediaType.RAW_MODEL
            this.primaryMediaId = buffer.readBigSmart()
            val modelFlags = buffer.readUByte()
            val hasTransform = (modelFlags and 0x1) == 1
            this.hasOrigin = (modelFlags and 0x2) == 2
            this.usesOrthogonal = (modelFlags and 0x4) == 4
            this.priorityRender = (modelFlags and 0x8) == 8
            if (hasTransform) {
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
            if (formatVersion >= 2) {
                this.monospaced = buffer.readUByte() == 1
            }
            this.text = buffer.readString()
            if (this.text.lowercase().contains("runescape")) {
                this.text = this.text.replace("runescape", "Darkan", true)
                this.text = this.text.replace("RuneScape", "Darkan", true)
                this.text = this.text.replace("Runescape", "Darkan", true)
            }
            this.lineSpacing = buffer.readUByte()
            this.textHorizontalAli = buffer.readUByte()
            this.textVerticalAli = buffer.readUByte()
            this.shadow = buffer.readUByte() == 1
            this.color = buffer.readInt()
            this.transparency = buffer.readUByte()
            if (formatVersion >= 0) {
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
        var keyTriggerData = buffer.readUByte()
        var decodeTempInt: Int
        if (keyTriggerData != 0) {
            this.keyTriggeringAction = Array(11) { ByteArray(0) }
            this.keyTriggerParams = Array(11) { ByteArray(0) }
            this.cycleSteps = IntArray(11)
            this.keyCharCodes = IntArray(11)
            while (keyTriggerData != 0) {
                decodeTempInt = (keyTriggerData shr 4) - 1
                keyTriggerData = keyTriggerData shl 8 or buffer.readUByte()
                keyTriggerData = keyTriggerData and 0xfff
                if (keyTriggerData == 4095) {
                    keyTriggerData = -1
                }
                val triggerAction = buffer.readByte()
                if (triggerAction.toInt() != 0) {
                    this.hasInteraction = true
                }
                val triggerParam = buffer.readByte()
                this.cycleSteps!![decodeTempInt] = keyTriggerData
                this.keyTriggeringAction!![decodeTempInt] = byteArrayOf(triggerAction)
                this.keyTriggerParams!![decodeTempInt] = byteArrayOf(triggerParam)
                keyTriggerData = buffer.readUByte()
            }
        }
        this.opBase = buffer.readString()
        if (Loader.DEBUG_IF_OPS)
            this.opBase += "(opBase)"
        decodeTempInt = buffer.readUByte()
        val optionCount = decodeTempInt and 0xf
        val cursorCount = decodeTempInt shr 4
        var tempIndex: Int
        if (optionCount > 0) {
            this.options = Array(optionCount) { "" }
            tempIndex = 0
            while (tempIndex < optionCount) {
                this.options!![tempIndex] = buffer.readString()
                tempIndex++
            }
        }
        var paramCount: Int
        if (cursorCount > 0) {
            tempIndex = buffer.readUByte()
            this.opCursors = IntArray(tempIndex + 1)
            paramCount = 0
            while (paramCount < this.opCursors!!.size) {
                this.opCursors!![paramCount] = -1
                paramCount++
            }
            this.opCursors!![tempIndex] = buffer.readUShort()
        }
        if (cursorCount > 1) {
            tempIndex = buffer.readUByte()
            this.opCursors!![tempIndex] = buffer.readUShort()
        }
        this.resumeText = buffer.readString()
        if (Loader.DEBUG_IF_OPS)
            this.resumeText += "(resumeText)"
        if (this.resumeText!!.isEmpty()) {
            this.resumeText = null
        }
        this.dragDeadzone = buffer.readUByte()
        this.dragDeadTime = buffer.readUByte()
        this.dragType = DragType.forId(buffer.readUByte())
        this.targetVerb = buffer.readString()
        if (Loader.DEBUG_IF_OPS)
            this.targetVerb += "(targetVerb)"
        tempIndex = -1
        if (getUseOptionFlags(optionMask) != 0) {
            tempIndex = buffer.readUShort()
            if (tempIndex == 65535) {
                tempIndex = -1
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
        if (formatVersion >= 0) {
            this.moveOverCursor = buffer.readUShort()
            if (this.moveOverCursor == 65535) {
                this.moveOverCursor = -1
            }
        }
        this.targetParams = IFEvents(optionMask, tempIndex)
        if (formatVersion >= 0) {
            paramCount = buffer.readUByte()
            var paramIndex: Int
            var paramKey: Int
            var paramValue: Int
            paramIndex = 0
            while (paramIndex < paramCount) {
                paramKey = buffer.readUMedium()
                paramValue = buffer.readInt()
                this.clientParams!!.put(IntNode(paramValue), paramKey.toLong())
                paramIndex++
            }
            paramIndex = buffer.readUByte()
            paramKey = 0
            while (paramKey < paramIndex) {
                paramValue = buffer.readUMedium()
                val paramString = buffer.readJagString()
                this.clientParams!!.put(ObjectNode(paramString), paramValue.toLong())
                paramKey++
            }
        }
        this.onLoadScript = decodeScript(buffer)
        this.onMouseOver = decodeScript(buffer)
        this.onMouseLeaveScript = decodeScript(buffer)
        this.hookParams = decodeScript(buffer)
        this.onTargetEnter = decodeScript(buffer)
        this.onVarpTransmit = decodeScript(buffer)
        this.mouseLeaveScript = decodeScript(buffer)
        this.onStatTransmit = decodeScript(buffer)
        this.onTimer = decodeScript(buffer)
        this.params = decodeScript(buffer)
        if (formatVersion >= 0) {
            this.onTargetLeave = decodeScript(buffer)
        }
        this.popupScript = decodeScript(buffer)
        this.onClick = decodeScript(buffer)
        this.onClickRepeat = decodeScript(buffer)
        this.onRelease = decodeScript(buffer)
        this.onHold = decodeScript(buffer)
        this.onDrag = decodeScript(buffer)
        this.onDragComplete = decodeScript(buffer)
        this.onMouseMove = decodeScript(buffer)
        this.onKey = decodeScript(buffer)
        this.onScrollWheel = decodeScript(buffer)
        this.varps = decodeScript3(buffer)
        this.mouseLeaveArrayParams = decodeScript3(buffer)
        this.statTransmitFilter = decodeScript3(buffer)
        this.keyPressArray = decodeScript3(buffer)
        this.mouseWheelArray = decodeScript3(buffer)
    }

    fun reset() {
        this.onLoadScript = null
        this.onClick = null
        this.onClickRepeat = null
        this.onRelease = null
        this.onHold = null
        this.onMouseOver = null
        this.popupScript = null
        this.onMouseLeaveScript = null
        this.onDrag = null
        this.onDragComplete = null
        this.onTargetEnter = null
        this.hookParams = null
        this.onVarpTransmit = null
        this.varps = null
        this.mouseLeaveScript = null
        this.mouseLeaveArrayParams = null
        this.onStatTransmit = null
        this.statTransmitFilter = null
        this.onKey = null
        this.keyPressArray = null
        this.onScrollWheel = null
        this.mouseWheelArray = null
        this.onTimer = null
        this.params = null
        this.onTargetLeave = null
        this.onMouseMove = null
        this.onChatTransmit = null
        this.onKeyType = null
        this.onFriendTransmit = null
        this.onFriendsChatTransmit = null
        this.onClanSettingsTransmit = null
        this.onMiscTransmit = null
        this.onStockTransmit = null
        this.onSubClose = null
        this.onSubChange = null
        this.onCamFinished = null
        this.onResize = null
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
        val values = IntArray(count)
        for (valueIndex in 0 until count) {
            values[valueIndex] = buffer.readInt()
        }
        return values
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

    fun addOptionName(index: Int, optionName: String?) {
        if (this.options == null || this.options!!.size <= index) {
            val replacementOptionNames: Array<String?> = Array(index + 1) { null }
            if (this.options != null) {
                System.arraycopy(this.options, 0, replacementOptionNames, 0, this.options!!.size)
            }
            this.options = replacementOptionNames
        }
        this.options!![index] = optionName
    }

    fun removeParam(paramKey: Int) {
        if (this.clientParams != null) {
            val existingNode = this.clientParams!!.get(paramKey.toLong())
            existingNode?.unlink()
        }
    }

    fun getIntParam(paramKey: Int, defaultValue: Int): Int {
        return if (this.clientParams == null) {
            defaultValue
        } else {
            val intNode = this.clientParams!!.get(paramKey.toLong()) as? IntNode
            if (intNode == null) defaultValue else intNode.value
        }
    }

    fun getStringParam(paramKey: Int, defaultValue: String): String {
        if (this.clientParams == null) {
            return defaultValue
        }
        val objectNode = this.clientParams!!.get(paramKey.toLong()) as? ObjectNode
        return if (objectNode == null) defaultValue else objectNode.value as String
    }

    fun setIntParam(paramKey: Int, paramValue: Int) {
        if (this.clientParams == null) {
            this.clientParams = IterableNodeMap(16)
            this.clientParams!!.put(IntNode(paramValue), paramKey.toLong())
        } else {
            val existingNode = this.clientParams!!.get(paramKey.toLong()) as? IntNode
            if (existingNode == null) {
                this.clientParams!!.put(IntNode(paramValue), paramKey.toLong())
            } else {
                existingNode.value = paramValue
            }
        }
    }

    fun setStringParam(paramKey: Int, paramValue: String) {
        if (this.clientParams == null) {
            this.clientParams = IterableNodeMap(16)
        } else {
            val existingNode = this.clientParams!!.get(paramKey.toLong()) as? ObjectNode
            if (existingNode != null) {
                existingNode.unlink()
            }
        }
        this.clientParams!!.put(ObjectNode(paramValue), paramKey.toLong())
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
