package com.jagex.game.runetek5.client.periodic;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Queue;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.ComponentType;
import com.jagex.game.runetek5.config.sequence.InterfaceSequence;
import com.jagex.game.runetek5.config.varc.VarcTypeList;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.model.MediaType;
import com.jagex.game.runetek5.utils.Utils;
import com.jagex.game.runetek5.vardomain.VarProviders;

import static com.jagex.game.runetek5.client.periodic.PulseEventType.*;

/**
 * 
 * @author Brainpower - 22/01/2023
 *
 */
public class PulseEventHandler {
	
	/**
	 * Cached with the different type of pulse events to
	 * prevent them being constantly recreated
	 */
	public static final IterableNodeMap<PulseEvent> PULSE_EVENT_CACHE = new IterableNodeMap<PulseEvent>(16);
	
	/**
	 * Pulse events that should be executed asap
	 */
	public static final Queue<PulseEvent> HIGH_PRIORITY = new Queue<PulseEvent>();

	/**
	 * Pulse events with a delay
	 */
	public static final Queue<PulseEvent> LOW_PRIORITY = new Queue<PulseEvent>();

	public static PulseEvent createPulseEvent(int type, long uid) {
	    PulseEvent pulseEvent = PulseEventHandler.PULSE_EVENT_CACHE.get((long) type << 56 | uid);
	    if (pulseEvent == null) {
	        pulseEvent = new PulseEvent(type, uid);
	        PulseEventHandler.PULSE_EVENT_CACHE.put(pulseEvent, pulseEvent.pointer);
	    }
	    return pulseEvent;
	}
	
	public static PulseEvent getNextExecutedEvent() {
	    PulseEvent pulseEvent = (PulseEvent) PulseEventHandler.HIGH_PRIORITY.getNext();
	    if (pulseEvent != null) {
	        pulseEvent.unlink();
	        pulseEvent.unlinkSub();
	    } else {
	        do {
	            pulseEvent = (PulseEvent) PulseEventHandler.LOW_PRIORITY.getNext();
	            if (pulseEvent == null) {
	                return null;
	            }
	            if (pulseEvent.executeAfter() > Utils.time()) {
	                return null;
	            }
	            pulseEvent.unlink();
	            pulseEvent.unlinkSub();
	        } while ((pulseEvent.indexReference & -9223372036854775808L) == 0L);
	    }
	    return pulseEvent;
	}

	public static void clearAllPulseEvents() {
	    PulseEventHandler.PULSE_EVENT_CACHE.clear();
	    PulseEventHandler.LOW_PRIORITY.reset();
	    PulseEventHandler.HIGH_PRIORITY.reset();
	}
	
	public static void processPulseEvents() {
	    for (int configIndex = VarProviders.PLAYER_VAR_PROVIDER.service(true); configIndex != -1; configIndex = VarProviders.PLAYER_VAR_PROVIDER.service(false)) {
	        PlayerUpdate.method8333(configIndex);
	        client.PULSE_EVENT_CONFIGS_CACHE[++client.pulseEventConfigsPtr - 1 & 0x1f] = configIndex;
	    }
	    for (PulseEvent event = PulseEventHandler.getNextExecutedEvent(); event != null; event = PulseEventHandler.getNextExecutedEvent()) {
	        int type = event.getType();
	        long value = event.getValue();
	        if (type == CLIENT_SETVARC) {
	            VarcTypeList.VARC_INT[(int) value] = event.primary;
	            client.clientIsActive |= VarcTypeList.VARC_BOOL[(int) value];
	            client.anIntArray7150[++client.keyCycle - 1 & 0x1f] = (int) value;
	        } else if (type == CLIENT_SETVARCSTR) {
	            VarcTypeList.VARC_STRING[(int) value] = event.string;
	            client.recentMouseWheels[++client.mouseWheelCycle - 1 & 0x1f] = (int) value;
	        } else if (type == IF_SETTEXT) {
	            Component component = Component.fromHash((int) value);
	            if (!event.string.equals(component.text)) {
	                component.text = event.string;
	                Component.redrawComponent(component);
	            }
	        } else if (type == IF_SETCONTENT) {
	            Component component = Component.fromHash((int) value);
	            MediaType mediaType = MediaType.forId(event.primary);
	            int primaryModel = event.secondary;
	            int secondaryModel = event.tertiary;
	            if (mediaType != component.mediaType || primaryModel != component.primaryMediaId || secondaryModel != component.secondaryMediaId) {
	                component.mediaType = mediaType;
	                component.primaryMediaId = primaryModel;
	                component.secondaryMediaId = secondaryModel;
	                component.npcCustomisation = null;
	                Component.redrawComponent(component);
	            }
	        } else if (type == IF_SETANIM) {
	            Component component = Component.fromHash((int) value);
	            if (event.primary != component.animation) {
	                if (event.primary != -1) {
	                    if (component.animatingSequence == null) {
	                        component.animatingSequence = new InterfaceSequence();
	                    }
	                    component.animatingSequence.update(event.primary);
	                } else {
	                    component.animatingSequence = null;
	                }
	                component.animation = event.primary;
	                Component.redrawComponent(component);
	            }
	        } else if (type == IF_SETCOLOR) {
	            int bitpacked = event.primary; //should probably be hsl?
	            int red = bitpacked >> 10 & 0x1f;
	            int green = bitpacked >> 5 & 0x1f;
	            int blue = bitpacked & 0x1f;
	            int color = (green << 11) + (red << 19) + (blue << 3);
	            Component component = Component.fromHash((int) value);
	            if (color != component.color) {
	                component.color = color;
	                Component.redrawComponent(component);
	            }
	        } else if (type == IF_SETHIDE) {
	            Component component = Component.fromHash((int) value);
	            boolean hidden = event.primary == 1;
	            if (hidden != component.hidden) {
	                component.hidden = hidden;
	                Component.redrawComponent(component);
	            }
	        } else if (type == IF_SETANGLE) {
	            Component component = Component.fromHash((int) value);
	            if (event.primary != component.spritePitch || event.secondary != component.spriteRoll || component.spriteScale != event.tertiary) {
	                component.spritePitch = event.primary;
	                component.spriteRoll = event.secondary;
	                component.spriteScale = event.tertiary;
	                if (component.containerItemId != -1) {
	                    if (component.aspectWidth > 0) {
	                        component.spriteScale = component.spriteScale * 32 / component.aspectWidth;
	                    } else if (component.baseWidth > 0) {
	                        component.spriteScale = component.spriteScale * 32 / component.baseWidth;
	                    }
	                }
	                Component.redrawComponent(component);
	            }
	        } else if (type == IF_SETITEM) {
	            Component component = Component.fromHash((int) value);
	            if (component.containerItemId != event.primary || event.secondary != component.itemStackSize) {
	                component.containerItemId = event.primary;
	                component.itemStackSize = event.secondary;
	                Component.redrawComponent(component);
	            }
	        } else if (type == IF_SETOFFSET) {
	            Component component = Component.fromHash((int) value);
	            if (event.primary != component.offsetX || event.secondary != component.offsetY || event.tertiary != component.spriteYaw) {
	                component.offsetX = event.primary;
	                component.offsetY = event.secondary;
	                component.spriteYaw = event.tertiary;
	                Component.redrawComponent(component);
	            }
	        } else if (type == IF_SETPOSITION) {
	            Component component = Component.fromHash((int) value);
	            component.aspectXType = 0;
	            component.x = component.basePositionX = event.primary;
	            component.aspectYType = 0;
	            component.y = component.basePositionY = event.secondary;
	            Component.redrawComponent(component);
	        } else if (type == IF_SETSCROLLPOS) {
	            Component component = Component.fromHash((int) value);
	            int scrollHeight = event.primary;
	            if (component != null && component.type == ComponentType.CONTAINER) {
	                if (scrollHeight > component.scrollHeight - component.height) {
	                    scrollHeight = component.scrollHeight - component.height;
	                }
	                if (scrollHeight < 0) {
	                    scrollHeight = 0;
	                }
	                if (scrollHeight != component.scrollY) {
	                    component.scrollY = scrollHeight;
	                    Component.redrawComponent(component);
	                }
	            }
	        } else if (type == IF_SETGRAPHIC) {
	            Component component = Component.fromHash((int) value);
	            component.spriteId = event.primary;
	        } else if (type == MINIMAP_FLAG) {
	            ComponentMinimap.minimapContainsFlag = true;
	            ComponentMinimap.minimapFlagX = event.primary;
	            ComponentMinimap.minimapFlagY = event.secondary;
	        } else if (type == IF_SETFONT) {
	            Component icomponentdefinitions5 = Component.fromHash((int) value);
	            icomponentdefinitions5.fontId = event.primary;
	        } else if (type == T17) {
	            Component component = Component.fromHash((int) value);
	            component.anInt1435 = event.primary;
	        } else if (type == IF_SETRECOL) {
	            Component component = Component.fromHash((int) value);
	            int colorIndex = (int) (value >> 32);
	            component.recolor(colorIndex, (short) event.primary, (short) event.secondary);
	        } else if (type == IF_SETRETEX) {
	            Component component = Component.fromHash((int) value);
	            int textureIndex = (int) (value >> 32);
	            component.retexture(textureIndex, (short) event.primary, (short) event.secondary);
	        } else if (type == IF_SETMONOSPACED) {
	            Component component = Component.fromHash((int) value);
	            component.monospaced = event.primary == 1;
	        } else if (type == IF_SETCLICKMASK) {
	            Component component = Component.fromHash((int) value);
	            component.clickMask = event.primary == 1;
	        } else if (type == IF_SETTEXTANTIMACRO) {
	            Component component = Component.fromHash((int) value);
	            if (event.primary == 1 != component.textAntiMacro) {
	                component.textAntiMacro = event.primary == 1;
	                Component.redrawComponent(component);
	            }
	        }
	    }
	}
}
