package com.jagex.game.runetek5.client.periodic;

import static com.jagex.game.runetek5.client.periodic.PulseEventType.*;

public class PulseEventQueue {

	public static void delayedClientvarcUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(CLIENT_SETVARC, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityClientvarcUpdate(int i_0, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(CLIENT_SETVARC, i_0);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_1;
	}
	
	public static void delayedVarcstringUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(CLIENT_SETVARCSTR, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityVarcstringUpdate(int i_0, String string_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(CLIENT_SETVARCSTR, i_0);
	    pulseEvent.queueHighPriority();
	    pulseEvent.string = string_1;
	}
	
	public static void delayedIftextUpdate(int componentIdHash) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETTEXT, componentIdHash);
	    pulseEvent.queueLowPriority();
	}

	public static void priorityIftextUpdate(int componentIdHash, String text) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETTEXT, componentIdHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.string = text;
	}
	
	public static void delayIfcontentUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETCONTENT, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfcontentUpdate(int componentHash, int modelType, int primaryModelId, int secondaryModelId) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETCONTENT, componentHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = modelType;
	    pulseEvent.secondary = primaryModelId;
	    pulseEvent.tertiary = secondaryModelId;
	}
	
	public static void delayedIfanimUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETANIM, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfanimUpdate(int i_0, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETANIM, i_0);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_1;
	}
	
	public static void delayedIfcolorUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETCOLOR, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfcolorUpdate(int componentHash, int color) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETCOLOR, componentHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = color;
	}
	
	public static void delayedIfhide(int componentHash) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETHIDE, componentHash);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfhide(int componentHash, int hidden) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETHIDE, componentHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = hidden;
	}
	
	public static void delayedIfangleUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETANGLE, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfangleUpdate(int interfaceHash, int pitch, int roll, int scale) {
	    PulseEvent event = PulseEventHandler.createPulseEvent(IF_SETANGLE, interfaceHash);
	    event.queueHighPriority();
	    event.primary = pitch;
	    event.secondary = roll;
	    event.tertiary = scale;
	}
	
	public static void delayedIfitemUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETITEM, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfitemUpdate(int componentHash, int itemId, int amount) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETITEM, componentHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = itemId;
	    pulseEvent.secondary = amount;
	}
	
	public static void delayedIfoffsetUpdate(int componentHash) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETOFFSET, componentHash);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfoffsetUpdate(int componentHash, int i_1, int i_2, int i_3) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETOFFSET, componentHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_1;
	    pulseEvent.secondary = i_2;
	    pulseEvent.tertiary = i_3;
	}
	
	public static void delayedIfpositionUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETPOSITION, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfpositionUpdate(int component, int x, int y) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETPOSITION, component);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = x;
	    pulseEvent.secondary = y;
	}
	
	public static void delayedIfscrollposUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETSCROLLPOS, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfscrollposUpdate(int i_0, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETSCROLLPOS, i_0);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_1;
	}
	
	// Outdated?
	public static void priorityT13Update(int i_0, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(T13, i_0);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_1;
	}
	
	public static void delayedIfgraphicUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETGRAPHIC, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfgraphicUpdate(int i_0, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETGRAPHIC, i_0);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_1;
	}
	
	public static void delayedClearMinimapFlagUpdate() {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(MINIMAP_FLAG, 0L);
	    pulseEvent.queueLowPriority();
	}

	public static void priorityMinimapFlagUpdate(int flagX, int flagY) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(MINIMAP_FLAG, 0L);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = flagX;
	    pulseEvent.secondary = flagY;
	}
	
	public static void delayedIffontUpdate(int componentHash) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETFONT, componentHash);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIffontUpdate(int componentHash, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETFONT, componentHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_1;
	}
	
	public static void method5581(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(T17, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	// Outdated
	public static void priorityT17Update(int i_0, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(T17, i_0);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_1;
	}
	
	public static void delayedIfrecol(int i_0, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETRECOL, (long) i_1 << 32 | i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfrecol(int componentHash, int recolorIndex, int srcColor, int dstColor) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETRECOL, (long) recolorIndex << 32 | componentHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = srcColor;
	    pulseEvent.secondary = dstColor;
	}
	
	public static void delayedIfretex(int i_0, int i_1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETRETEX, (long) i_1 << 32 | i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfretex(int i_0, int i_1, int i_2, int i_3) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETRETEX, (long) i_1 << 32 | i_0);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = i_2;
	    pulseEvent.secondary = i_3;
	}
	
	public static void delayedIfmonospacedUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETMONOSPACED, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void delayedIfclickmaskUpdate(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETCLICKMASK, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void priorityIfclickmaskUpdate(int clickMask, boolean bool1) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETCLICKMASK, clickMask);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = bool1 ? 1 : 0;
	}
	
	public static void delayedClearIftextantimacro(int i_0) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETTEXTANTIMACRO, i_0);
	    pulseEvent.queueLowPriority();
	}
	
	public static void prioritySetIftextantimacro(int componentHash, boolean enabled) {
	    PulseEvent pulseEvent = PulseEventHandler.createPulseEvent(IF_SETTEXTANTIMACRO, componentHash);
	    pulseEvent.queueHighPriority();
	    pulseEvent.primary = enabled ? 1 : 0;
	}
}
