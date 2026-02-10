package com.jagex.game.runetek5.client.periodic

import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.event.Varc
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.sequence.InterfaceSequence
import com.jagex.game.runetek5.config.varc.VarcTypeList
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.model.MediaType
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.VarProviders

import com.jagex.game.runetek5.client.periodic.PulseEventType.CLIENT_SETVARC
import com.jagex.game.runetek5.client.periodic.PulseEventType.CLIENT_SETVARCSTR
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETTEXT
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETCONTENT
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETANIM
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETCOLOR
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETHIDE
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETANGLE
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETITEM
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETOFFSET
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETPOSITION
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETSCROLLPOS
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETGRAPHIC
import com.jagex.game.runetek5.client.periodic.PulseEventType.MINIMAP_FLAG
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETFONT
import com.jagex.game.runetek5.client.periodic.PulseEventType.T17
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETRECOL
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETRETEX
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETMONOSPACED
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETCLICKMASK
import com.jagex.game.runetek5.client.periodic.PulseEventType.IF_SETTEXTANTIMACRO

/**
 *
 * @author Brainpower - 22/01/2023
 *
 */
object PulseEventHandler {

    /**
     * Cached with the different type of pulse events to
     * prevent them being constantly recreated
     */
    @JvmField val PULSE_EVENT_CACHE: IterableNodeMap<PulseEvent> = IterableNodeMap<PulseEvent>(16)

    /**
     * Pulse events that should be executed asap
     */
    @JvmField val HIGH_PRIORITY: Queue<PulseEvent> = Queue<PulseEvent>()

    /**
     * Pulse events with a delay
     */
    @JvmField val LOW_PRIORITY: Queue<PulseEvent> = Queue<PulseEvent>()

    @JvmStatic
    fun createPulseEvent(type: Int, uid: Long): PulseEvent {
        var pulseEvent = PULSE_EVENT_CACHE.get(type.toLong() shl 56 or uid)
        if (pulseEvent == null) {
            pulseEvent = PulseEvent(type, uid)
            PULSE_EVENT_CACHE.put(pulseEvent, pulseEvent.pointer)
        }
        return pulseEvent
    }

    @JvmStatic
    fun getNextExecutedEvent(): PulseEvent? {
        var pulseEvent = HIGH_PRIORITY.getNext() as PulseEvent?
        if (pulseEvent != null) {
            pulseEvent.unlink()
            pulseEvent.unlinkSub()
        } else {
            do {
                pulseEvent = LOW_PRIORITY.getNext() as PulseEvent?
                if (pulseEvent == null) {
                    return null
                }
                if (pulseEvent.executeAfter() > Utils.time()) {
                    return null
                }
                pulseEvent.unlink()
                pulseEvent.unlinkSub()
            } while ((pulseEvent.indexReference and Long.MIN_VALUE) == 0L)
        }
        return pulseEvent
    }

    @JvmStatic
    fun clearAllPulseEvents() {
        PULSE_EVENT_CACHE.clear()
        LOW_PRIORITY.reset()
        HIGH_PRIORITY.reset()
    }

    @JvmStatic
    fun processPulseEvents() {
        var configIndex = VarProviders.PLAYER_VAR_PROVIDER.service(true)
        while (configIndex != -1) {
            PlayerUpdate.onVarChange(configIndex)
            Client.PULSE_EVENT_CONFIGS_CACHE[++Client.pulseEventConfigsPtr - 1 and 0x1f] = configIndex
            configIndex = VarProviders.PLAYER_VAR_PROVIDER.service(false)
        }
        var event = getNextExecutedEvent()
        while (event != null) {
            val type = event.getType()
            val value = event.getValue()
            when (type) {
                CLIENT_SETVARC -> {
                    ScriptExecutor.pushEvent(Varc(value.toInt(), VarcTypeList.VARC_INT!![value.toInt()], event.primary))
                    VarcTypeList.VARC_INT!![value.toInt()] = event.primary
                    Client.clientIsActive = Client.clientIsActive or VarcTypeList.VARC_BOOL!![value.toInt()]
                    Client.recentKeyUpdates!![++Client.keyCycle - 1 and 0x1f] = value.toInt()
                }
                CLIENT_SETVARCSTR -> {
                    VarcTypeList.VARC_STRING!![value.toInt()] = event.string
                    Client.recentMouseWheels!![++Client.mouseWheelCycle - 1 and 0x1f] = value.toInt()
                }
                IF_SETTEXT -> {
                    val component = Component.fromHash(value.toInt())
                    if (event.string != component!!.text) {
                        component.text = event.string!!
                        Component.redrawComponent(component)
                    }
                }
                IF_SETCONTENT -> {
                    val component = Component.fromHash(value.toInt())
                    val mediaType = MediaType.forId(event.primary)
                    val primaryModel = event.secondary
                    val secondaryModel = event.tertiary
                    if (mediaType != component!!.mediaType || primaryModel != component.primaryMediaId || secondaryModel != component.secondaryMediaId) {
                        component.mediaType = mediaType!!
                        component.primaryMediaId = primaryModel
                        component.secondaryMediaId = secondaryModel
                        component.npcCustomisation = null
                        Component.redrawComponent(component)
                    }
                }
                IF_SETANIM -> {
                    val component = Component.fromHash(value.toInt())
                    if (event.primary != component!!.animation) {
                        if (event.primary != -1) {
                            if (component.animatingSequence == null) {
                                component.animatingSequence = InterfaceSequence()
                            }
                            component.animatingSequence!!.update(event.primary)
                        } else {
                            component.animatingSequence = null
                        }
                        component.animation = event.primary
                        Component.redrawComponent(component)
                    }
                }
                IF_SETCOLOR -> {
                    val bitpacked = event.primary
                    val red = bitpacked shr 10 and 0x1f
                    val green = bitpacked shr 5 and 0x1f
                    val blue = bitpacked and 0x1f
                    val color = (green shl 11) + (red shl 19) + (blue shl 3)
                    val component = Component.fromHash(value.toInt())
                    if (color != component!!.color) {
                        component.color = color
                        Component.redrawComponent(component)
                    }
                }
                IF_SETHIDE -> {
                    val component = Component.fromHash(value.toInt())
                    val hidden = event.primary == 1
                    if (hidden != component!!.hidden) {
                        component.hidden = hidden
                        Component.redrawComponent(component)
                    }
                }
                IF_SETANGLE -> {
                    val component = Component.fromHash(value.toInt())
                    if (event.primary != component!!.spritePitch || event.secondary != component.spriteRoll || component.spriteScale != event.tertiary) {
                        component.spritePitch = event.primary
                        component.spriteRoll = event.secondary
                        component.spriteScale = event.tertiary
                        if (component.containerItemId != -1) {
                            if (component.aspectWidth > 0) {
                                component.spriteScale = component.spriteScale * 32 / component.aspectWidth
                            } else if (component.baseWidth > 0) {
                                component.spriteScale = component.spriteScale * 32 / component.baseWidth
                            }
                        }
                        Component.redrawComponent(component)
                    }
                }
                IF_SETITEM -> {
                    val component = Component.fromHash(value.toInt())
                    if (component!!.containerItemId != event.primary || event.secondary != component.itemStackSize) {
                        component.containerItemId = event.primary
                        component.itemStackSize = event.secondary
                        Component.redrawComponent(component)
                    }
                }
                IF_SETOFFSET -> {
                    val component = Component.fromHash(value.toInt())
                    if (event.primary != component!!.offsetX || event.secondary != component.offsetY || event.tertiary != component.spriteYaw) {
                        component.offsetX = event.primary
                        component.offsetY = event.secondary
                        component.spriteYaw = event.tertiary
                        Component.redrawComponent(component)
                    }
                }
                IF_SETPOSITION -> {
                    val component = Component.fromHash(value.toInt())
                    component!!.aspectXType = 0
                    component.x = event.primary
                    component.basePositionX = event.primary
                    component.aspectYType = 0
                    component.y = event.secondary
                    component.basePositionY = event.secondary
                    Component.redrawComponent(component)
                }
                IF_SETSCROLLPOS -> {
                    val component = Component.fromHash(value.toInt())
                    var scrollHeight = event.primary
                    if (component != null && component.type == ComponentType.CONTAINER) {
                        if (scrollHeight > component.scrollHeight - component.height) {
                            scrollHeight = component.scrollHeight - component.height
                        }
                        if (scrollHeight < 0) {
                            scrollHeight = 0
                        }
                        if (scrollHeight != component.scrollY) {
                            component.scrollY = scrollHeight
                            Component.redrawComponent(component)
                        }
                    }
                }
                IF_SETGRAPHIC -> {
                    val component = Component.fromHash(value.toInt())
                    component!!.spriteId = event.primary
                }
                MINIMAP_FLAG -> {
                    ComponentMinimap.minimapContainsFlag = true
                    ComponentMinimap.minimapFlagX = event.primary
                    ComponentMinimap.minimapFlagY = event.secondary
                }
                IF_SETFONT -> {
                    val icomponentdefinitions5 = Component.fromHash(value.toInt())
                    icomponentdefinitions5!!.fontId = event.primary
                }
                T17 -> {
                    val component = Component.fromHash(value.toInt())
                    component!!.videoFileId = event.primary
                }
                IF_SETRECOL -> {
                    val component = Component.fromHash(value.toInt())
                    val colorIndex = (value shr 32).toInt()
                    component!!.recolor(colorIndex, event.primary.toShort(), event.secondary.toShort())
                }
                IF_SETRETEX -> {
                    val component = Component.fromHash(value.toInt())
                    val textureIndex = (value shr 32).toInt()
                    component!!.retexture(textureIndex, event.primary.toShort(), event.secondary.toShort())
                }
                IF_SETMONOSPACED -> {
                    val component = Component.fromHash(value.toInt())
                    component!!.monospaced = event.primary == 1
                }
                IF_SETCLICKMASK -> {
                    val component = Component.fromHash(value.toInt())
                    component!!.clickMask = event.primary == 1
                }
                IF_SETTEXTANTIMACRO -> {
                    val component = Component.fromHash(value.toInt())
                    if (event.primary == 1 != component!!.textAntiMacro) {
                        component.textAntiMacro = event.primary == 1
                        Component.redrawComponent(component)
                    }
                }
            }
            event = getNextExecutedEvent()
        }
    }
}
