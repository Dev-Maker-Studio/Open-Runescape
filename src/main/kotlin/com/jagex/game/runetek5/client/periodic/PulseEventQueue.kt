package com.jagex.game.runetek5.client.periodic

object PulseEventQueue {

    @JvmStatic
    fun delayedClientvarcUpdate(varId: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.CLIENT_SETVARC, varId.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityClientvarcUpdate(varId: Int, value: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.CLIENT_SETVARC, varId.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = value
    }

    @JvmStatic
    fun delayedVarcstringUpdate(varId: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.CLIENT_SETVARCSTR, varId.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityVarcstringUpdate(varId: Int, value: String?) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.CLIENT_SETVARCSTR, varId.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.string = value
    }

    @JvmStatic
    fun delayedIftextUpdate(componentIdHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETTEXT, componentIdHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIftextUpdate(componentIdHash: Int, text: String?) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETTEXT, componentIdHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.string = text
    }

    @JvmStatic
    fun delayIfcontentUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETCONTENT, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfcontentUpdate(componentHash: Int, modelType: Int, primaryModelId: Int, secondaryModelId: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETCONTENT, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = modelType
        pulseEvent.secondary = primaryModelId
        pulseEvent.tertiary = secondaryModelId
    }

    @JvmStatic
    fun delayedIfanimUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETANIM, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfanimUpdate(componentHash: Int, animId: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETANIM, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = animId
    }

    @JvmStatic
    fun delayedIfcolorUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETCOLOR, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfcolorUpdate(componentHash: Int, color: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETCOLOR, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = color
    }

    @JvmStatic
    fun delayedIfhide(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETHIDE, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfhide(componentHash: Int, hidden: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETHIDE, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = hidden
    }

    @JvmStatic
    fun delayedIfangleUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETANGLE, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfangleUpdate(interfaceHash: Int, pitch: Int, roll: Int, scale: Int) {
        val event = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETANGLE, interfaceHash.toLong())
        event.queueHighPriority()
        event.primary = pitch
        event.secondary = roll
        event.tertiary = scale
    }

    @JvmStatic
    fun delayedIfitemUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETITEM, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfitemUpdate(componentHash: Int, itemId: Int, amount: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETITEM, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = itemId
        pulseEvent.secondary = amount
    }

    @JvmStatic
    fun delayedIfoffsetUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETOFFSET, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfoffsetUpdate(componentHash: Int, x: Int, y: Int, z: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETOFFSET, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = x
        pulseEvent.secondary = y
        pulseEvent.tertiary = z
    }

    @JvmStatic
    fun delayedIfpositionUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETPOSITION, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfpositionUpdate(component: Int, x: Int, y: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETPOSITION, component.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = x
        pulseEvent.secondary = y
    }

    @JvmStatic
    fun delayedIfscrollposUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETSCROLLPOS, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfscrollposUpdate(componentHash: Int, scrollPos: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETSCROLLPOS, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = scrollPos
    }

    // Outdated?
    @JvmStatic
    fun priorityT13Update(componentHash: Int, value: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.T13, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = value
    }

    @JvmStatic
    fun delayedIfgraphicUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETGRAPHIC, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfgraphicUpdate(componentHash: Int, graphicId: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETGRAPHIC, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = graphicId
    }

    @JvmStatic
    fun delayedClearMinimapFlagUpdate() {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.MINIMAP_FLAG, 0L)
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityMinimapFlagUpdate(flagX: Int, flagY: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.MINIMAP_FLAG, 0L)
        pulseEvent.queueHighPriority()
        pulseEvent.primary = flagX
        pulseEvent.secondary = flagY
    }

    @JvmStatic
    fun delayedIffontUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETFONT, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIffontUpdate(componentHash: Int, fontId: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETFONT, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = fontId
    }

    @JvmStatic
    fun method5581(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.T17, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    // Outdated
    @JvmStatic
    fun priorityT17Update(componentHash: Int, value: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.T17, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = value
    }

    @JvmStatic
    fun delayedIfrecol(componentHash: Int, recolorIndex: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETRECOL, recolorIndex.toLong() shl 32 or componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfrecol(componentHash: Int, recolorIndex: Int, srcColor: Int, dstColor: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETRECOL, recolorIndex.toLong() shl 32 or componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = srcColor
        pulseEvent.secondary = dstColor
    }

    @JvmStatic
    fun delayedIfretex(componentHash: Int, retexIndex: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETRETEX, retexIndex.toLong() shl 32 or componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfretex(componentHash: Int, retexIndex: Int, srcTexture: Int, dstTexture: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETRETEX, retexIndex.toLong() shl 32 or componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = srcTexture
        pulseEvent.secondary = dstTexture
    }

    @JvmStatic
    fun delayedIfmonospacedUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETMONOSPACED, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun delayedIfclickmaskUpdate(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETCLICKMASK, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun priorityIfclickmaskUpdate(clickMask: Int, enabled: Boolean) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETCLICKMASK, clickMask.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = if (enabled) 1 else 0
    }

    @JvmStatic
    fun delayedClearIftextantimacro(componentHash: Int) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETTEXTANTIMACRO, componentHash.toLong())
        pulseEvent.queueLowPriority()
    }

    @JvmStatic
    fun prioritySetIftextantimacro(componentHash: Int, enabled: Boolean) {
        val pulseEvent = PulseEventHandler.createPulseEvent(PulseEventType.IF_SETTEXTANTIMACRO, componentHash.toLong())
        pulseEvent.queueHighPriority()
        pulseEvent.primary = if (enabled) 1 else 0
    }
}
