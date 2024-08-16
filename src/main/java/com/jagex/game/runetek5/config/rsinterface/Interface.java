package com.jagex.game.runetek5.config.rsinterface;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.HashTableIterator;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuEntry;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.trigger.worldmap.WorldMapTrigger;
import com.jagex.game.runetek5.utils.ArrayUtils;

public class Interface {
    public static Interface[] INTERFACES;
    public static boolean[] INTERFACES_LOADED;
    public static Js5ResourceProvider interfaceResource;
    public static int anInt3008;
    public Component[] components;
    public boolean aBool999;
    public Component[] subComponents;
	public static int preferenceMask;

    public Interface(boolean bool1, Component[] components) {
        this.components = components;
        this.aBool999 = bool1;
    }
    
    public static void initialize(Js5ResourceProvider interfacesResource, Js5ResourceProvider meshResource, Js5ResourceProvider spriteResource) {
        interfaceResource = interfacesResource;
        Resource.MESH = meshResource;
        StaticMethods.SPRITES_INDEX = spriteResource;
        INTERFACES = new Interface[interfaceResource.getArchiveCount()];
        INTERFACES_LOADED = new boolean[interfaceResource.getArchiveCount()];
    }
    
    public static void initializeTables() {
        INTERFACES = new Interface[interfaceResource.getArchiveCount()];
        INTERFACES_LOADED = new boolean[interfaceResource.getArchiveCount()];
    }

    public static boolean interfaceLoaded(int interfaceId, int[] xteas) {
        if (INTERFACES_LOADED[interfaceId]) {
            return true;
        } 
        INTERFACES[interfaceId] = decodeComponents(interfaceId, xteas, INTERFACES[interfaceId], false);
        if (INTERFACES[interfaceId] == null) {
            return false;
        } 
        INTERFACES_LOADED[interfaceId] = true;
        return true;
    }
    
    public static Interface getInterface(int interfaceAndComponent) {
        return INTERFACES[interfaceAndComponent >> 16];
    }
    
    public Component getComponent(int componentId) {
        if (componentId >>> 16 != this.components[0].idHash >>> 16) {
            throw new IllegalArgumentException();
        } 
        return this.components[componentId & 0xffff];
    }

	public static void unloadInterface(int interfaceId) {
	    INTERFACES_LOADED[interfaceId] = false;
	    clearComponents(interfaceId);
	}
    
    // may have wrong name
	public static void open_sub(int interfaceId, int i_1) {
	    if (interfaceLoaded(interfaceId, null)) {
	        Interface.method2591(INTERFACES[interfaceId].components, i_1);
	    }
	}
	
	public static void resetComponentSequences(int interfaceId, int[] xteas) {
	    if (interfaceLoaded(interfaceId, xteas)) {
	        Component[] componenents = INTERFACES[interfaceId].components;
	        for (int index = 0; index < componenents.length; index++) {
	            Component components = componenents[index];
	            if (components != null && components.animatingSequence != null) {
	                components.animatingSequence.reset();
	            }
	        }
	    }
	}

	public static void clearComponents(int interfaceId) {
	    if (interfaceId != -1 && !INTERFACES_LOADED[interfaceId]) {
	        interfaceResource.clearArchiveFiles(interfaceId);
	        INTERFACES[interfaceId] = null;
	    }
	}

    public static void closeChildInterface(SubInterface parentInterface, boolean unload, boolean bool2) {
        int parentInterfaceId = parentInterface.subInterfaceId;
        int interfaceHash = (int) parentInterface.pointer;
        parentInterface.unlink();
        if (unload) {
            Interface.unloadInterface(parentInterfaceId);
        }
        method14650(parentInterfaceId);
        Component component = Component.fromHash(interfaceHash);
        if (component != null) {
            Component.redrawComponent(component);
        }
        MiniMenu.method420();
        if (!bool2 && client.BASE_WINDOW_ID != -1) {
            Interface.open_sub(client.BASE_WINDOW_ID, 1);
        }
        HashTableIterator openInterfaceIt = new HashTableIterator(client.OPEN_INTERFACES);
        for (SubInterface child = (SubInterface) openInterfaceIt.first(); child != null; child = (SubInterface) openInterfaceIt.next()) {
            if (!child.linked()) {
                child = (SubInterface) openInterfaceIt.first();
                if (child == null) {
                    break;
                }
            }
            if (child.overlay == 3) {
                int uid = (int) child.pointer;
                if (uid >>> 16 == parentInterfaceId) {
                    closeChildInterface(child, true, bool2);
                }
            }
        }
    }
    

	public static SubInterface openSub(int parentId, SubInterface subInterface, int[] ints_2, boolean bool3) {
	    SubInterface currentSubInterface = client.OPEN_INTERFACES.get(parentId);
	    if (currentSubInterface != null) {
	        closeChildInterface(currentSubInterface, currentSubInterface.subInterfaceId != subInterface.subInterfaceId, bool3);
	    }
	    client.OPEN_INTERFACES.put(subInterface, parentId);
	    resetComponentSequences(subInterface.subInterfaceId, ints_2);
	    Component parentComponent = Component.fromHash(parentId);
	    if (parentComponent != null) {
	        Component.redrawComponent(parentComponent);
	    }
	    if (client.aClass1187352 != null) {
	        Component.redrawComponent(client.aClass1187352);
	        client.aClass1187352 = null;
	    }
	    MiniMenu.method420();
	    if (parentComponent != null) {
	        Component.method483(INTERFACES[parentComponent.idHash >>> 16], parentComponent, !bool3);
	    }
	    if (!bool3) {
	        Component.runScriptsIfLoaded(subInterface.subInterfaceId, ints_2);
	    }
	    if (!bool3 && client.BASE_WINDOW_ID != -1) {
	    	open_sub(client.BASE_WINDOW_ID, 1);
	    }
	    return subInterface;
	}

    public static Interface decodeComponents(int interfaceId, int[] xteas, Interface rsInterface, boolean bool3) {
        if (!interfaceResource.loadArchive(interfaceId)) {
            return null;
        } 
        int componentCount = interfaceResource.getArchiveFileCount(interfaceId);
        Component[] interfaceComponents;
        if (componentCount == 0) {
            interfaceComponents = new Component[0];
        } else if (rsInterface == null) {
            interfaceComponents = new Component[componentCount];
        } else {
            interfaceComponents = rsInterface.components;
        }
        if (rsInterface == null) {
            rsInterface = new Interface(bool3, interfaceComponents);
        } else {
            rsInterface.components = interfaceComponents;
            rsInterface.aBool999 = bool3;
        }
        for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
            if (rsInterface.components[componentIndex] == null) {
                byte[] interfaceData = interfaceResource.getFile(interfaceId, componentIndex, xteas);
                if (interfaceData != null) {
                    Component component = rsInterface.components[componentIndex] = new Component();
                    component.idHash = componentIndex + (interfaceId << 16);
                    component.decode(new Packet(interfaceData));
                }
            }
        }
        return rsInterface;
    }
    
    public static void open_sub(SubInterface subInterface, int subInterfaceId, int startX, int startY, int endX, int endY, int offsetX, int offsetY, int mouseX, int mouseY) {
        if (interfaceLoaded(subInterfaceId, null)) {
            if (subInterface != null && !subInterface.executeTrigger()) {
                closeChildInterface(subInterface, true, false);
            } else {
                Interface interface11 = INTERFACES[subInterfaceId];
                InterfaceRenderer.processInterfaceCycle(interface11, interface11.getComponents(), -1, startX, startY, endX, endY, offsetX, offsetY, mouseX, mouseY);
            }
        }
    }

    public static void method14650(int i_0) {
        for (Node node2 = client.ICOMPONENT_SETTINGS_SLOTS.first(); node2 != null; node2 = client.ICOMPONENT_SETTINGS_SLOTS.next()) {
            if (i_0 == (node2.pointer >> 48 & 0xffffL)) {
                node2.unlink();
            }
        }
    }

    public static boolean method4248(int interfaceAndComponent, int i_1) {
        if (!MiniMenu.isOpen) {
            return false;
        } 
        int interId = interfaceAndComponent >> 16;
        int compId = interfaceAndComponent & 0xffff;
        if (INTERFACES[interId] != null && INTERFACES[interId].components[compId] != null) {
            Component component = INTERFACES[interId].components[compId];
            MiniMenuEntry menuEntry;
            if (i_1 == -1 && component.type == ComponentType.CONTAINER) {
                for (menuEntry = (MiniMenuEntry) MiniMenu.aClass482171.getFirst(); menuEntry != null; menuEntry = (MiniMenuEntry) MiniMenu.aClass482171.next()) {
                    if (menuEntry.clickType == 58 || menuEntry.clickType == 1007 || menuEntry.clickType == 25 || menuEntry.clickType == 57 || menuEntry.clickType == 30) {
                        for (Component component2 = Component.fromHash(menuEntry.anInt9582); component2 != null; component2 = Interface.getSub(INTERFACES[component2.idHash >> 16], component2)) {
                            if (component2.idHash == component.idHash) {
                                return true;
                            }
                        }
                    }
                }
            } else {
                for (menuEntry = (MiniMenuEntry) MiniMenu.aClass482171.getFirst(); menuEntry != null; menuEntry = (MiniMenuEntry) MiniMenu.aClass482171.next()) {
                    if (menuEntry.anInt9581 == i_1 && component.idHash == menuEntry.anInt9582 && (menuEntry.clickType == 58 || menuEntry.clickType == 1007 || menuEntry.clickType == 25 || menuEntry.clickType == 57 || menuEntry.clickType == 30)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Component[] getComponents() {
        return this.subComponents == null ? this.components : this.subComponents;
    }

    public Component[] method1617() {
        if (this.subComponents == null) {
            int i_2 = this.components.length;
            this.subComponents = new Component[i_2];
            System.arraycopy(this.components, 0, this.subComponents, 0, this.components.length);
        }
        return this.subComponents;
    }

	
    public static void ifHover() {
	    if (client.BASE_WINDOW_ID != -1) {
	        int x = MouseRecord.recorder.getMouseX();
	        int y = MouseRecord.recorder.getMouseY();
	        MouseRecord record = (MouseRecord) client.MOUSE_MOVEMENTS.getFirst();
	        if (record != null) {
	            x = record.getX();
	            y = record.getY();
	        }
	        if (client.aClass1187257 != null && client.frameComponent == client.aClass1187247) {
	            client.aBool7403 = true;
	            client.anInt7432 = 0;
	            client.anInt7265 = 0;
	            client.anInt7367 = GameShell.screenCanvasWidth;
	            client.anInt7476 = client.screenCanvasHeight * -969250379;
	        }
	        open_sub(null, client.BASE_WINDOW_ID, 0, 0, GameShell.screenCanvasWidth, client.screenCanvasHeight * -969250379, 0, 0, x, y);
	        if (WorldMap.worldmapComponent != null) {
	            WorldMapTrigger.ifWorldmapHover(x, y);
	        }
	    }
	}

	public static void ifOptionInteract(int option, int interfaceHash, int slotId, String optionName) {
	    Component component = Component.listIcomponent(interfaceHash, slotId);
	    if (component != null) {
	        if (component.params != null) {
	            HookRequest hookRequest = new HookRequest();
	            hookRequest.source = component;
	            hookRequest.anInt8051 = option;
	            hookRequest.optionName = optionName;
	            hookRequest.params = component.params;
	            CS2Executor.executeHookInner(hookRequest);
	        }
	        if (client.listComponentSettings(component).clickOptionEnabled(option - 1)) {
	            BufferedConnectionContext connection = client.getConnectionContext();
	            if (client.gameState == GameState.UNK_0 || client.gameState == GameState.LOGGED_INGAME) {
	                TcpPacket packet;
	                if (option == 1) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP1, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 2) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP2, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 3) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP3, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 4) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP4, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 5) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP5, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 6) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP6, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 7) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP7, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 8) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP8, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 9) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP9, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	                if (option == 10) {
	                    packet = PacketEncoder.createPacket(ClientProt.IF_OP10, connection.isaac);
	                    PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId);
	                    connection.queuePacket(packet);
	                }
	            }
	        }
	    }
	}

	public static Component getSub(Interface inter, Component component) {
	    if (component.parent != -1) {
	        return inter.getComponent(component.parent);
	    } 
	    if (!inter.aBool999) {
	        int interfaceId = component.idHash >>> 16;
	        HashTableIterator subInterfaces = new HashTableIterator(client.OPEN_INTERFACES);
	
	        for (SubInterface sub = (SubInterface) subInterfaces.first(); sub != null; sub = (SubInterface) subInterfaces.next()) {
	            if (interfaceId == sub.subInterfaceId) {
	                return Component.fromHash((int) sub.pointer);
	            }
	        }
	    }
	    return null;
	}

	public static String getUseOptionString(Component component) {
        return client.listComponentSettings(component).getUseOptionFlags() == 0 ? null
        		: (component.useOptionString != null && !component.useOptionString.trim().isEmpty() 
        				? component.useOptionString 
        				: (client.DISPLAY_HIDDEN_MENU_OPTIONS ? "Hidden-use" : null));
    }

	public static void method2591(Component[] components, int i_1) {
	    for (int index = 0; index < components.length; index++) {
	        Component component = components[index];
	        if (component != null) {
	            if (component.type == ComponentType.CONTAINER) {
	                if (component.itemSlots != null) {
	                    method2591(component.itemSlots, i_1);
	                }
	                SubInterface subInterface = client.OPEN_INTERFACES.get(component.idHash);
	                if (subInterface != null) {
	                    open_sub(subInterface.subInterfaceId, i_1);
	                }
	            }
	            HookRequest hookRequest;
	            if (i_1 == 0 && component.hookrequestType0Params != null) {
	                hookRequest = new HookRequest();
	                hookRequest.source = component;
	                hookRequest.params = component.hookrequestType0Params;
	                CS2Executor.executeHookInner(hookRequest);
	            }
	            if (i_1 == 1 && component.hookrequestType1Params != null) {
	                if (component.slotId >= 0) {
	                    Component icomponentdefinitions7 = Component.fromHash(component.idHash);
	                    if (icomponentdefinitions7 == null || icomponentdefinitions7.slotChildren == null || component.slotId >= icomponentdefinitions7.slotChildren.length || component != icomponentdefinitions7.slotChildren[component.slotId]) {
	                        continue;
	                    }
	                }
	                hookRequest = new HookRequest();
	                hookRequest.source = component;
	                hookRequest.params = component.hookrequestType1Params;
	                CS2Executor.executeHookInner(hookRequest);
	            }
	        }
	    }
	}
	
    public static void method7554(Interface inter, Component defs) {
        if (defs != null) {
            int i_4;
            if (defs.slotId != -1) {
                Component interfaceComponent = inter.getComponent(defs.parent);
                if (interfaceComponent != null) {
                    if (interfaceComponent.itemSlots == interfaceComponent.slotChildren) {
                        interfaceComponent.itemSlots = new Component[interfaceComponent.slotChildren.length];
                        interfaceComponent.itemSlots[interfaceComponent.itemSlots.length - 1] = defs;
                        ArrayUtils.arrayCopy(interfaceComponent.slotChildren, 0, interfaceComponent.itemSlots, 0, defs.slotId);
                        ArrayUtils.arrayCopy(interfaceComponent.slotChildren, defs.slotId + 1, interfaceComponent.itemSlots, defs.slotId, interfaceComponent.slotChildren.length - defs.slotId - 1);
                    } else {
                        i_4 = 0;
                        Component[] arr_5;
                        for (arr_5 = interfaceComponent.itemSlots; i_4 < arr_5.length && arr_5[i_4] != defs; i_4++) {
                        }
                        if (i_4 < arr_5.length) {
                            ArrayUtils.arrayCopy(arr_5, i_4 + 1, arr_5, i_4, arr_5.length - i_4 - 1);
                            arr_5[interfaceComponent.itemSlots.length - 1] = defs;
                        }
                    }
                }
            } else {
                Component[] arr_6 = inter.method1617();
                for (i_4 = 0; i_4 < arr_6.length && arr_6[i_4] != defs; i_4++) {
                }
                if (i_4 < arr_6.length) {
                    ArrayUtils.arrayCopy(arr_6, i_4 + 1, arr_6, i_4, arr_6.length - i_4 - 1);
                    arr_6[arr_6.length - 1] = defs;
                }
            }
        }
    }

    public static void method3710(Interface inter, Component defs) {
        if (defs != null) {
            int i_4;
            if (defs.slotId != -1) {
                Component interfaceComponent = inter.components[defs.parent & 0xffff];
                if (interfaceComponent != null) {
                    if (interfaceComponent.slotChildren == interfaceComponent.itemSlots) {
                        interfaceComponent.itemSlots = new Component[interfaceComponent.slotChildren.length];
                        interfaceComponent.itemSlots[0] = defs;
                        ArrayUtils.arrayCopy(interfaceComponent.slotChildren, 0, interfaceComponent.itemSlots, 1, defs.slotId);
                        ArrayUtils.arrayCopy(interfaceComponent.slotChildren, defs.slotId + 1, interfaceComponent.itemSlots, defs.slotId + 1, interfaceComponent.slotChildren.length - defs.slotId - 1);
                    } else {
                        i_4 = 0;
                        Component[] itemSlotComponents;
                        for (itemSlotComponents = interfaceComponent.itemSlots; i_4 < itemSlotComponents.length && itemSlotComponents[i_4] != defs; i_4++) { }

                        if (i_4 < itemSlotComponents.length) {
                            ArrayUtils.arrayCopy(itemSlotComponents, 0, itemSlotComponents, 1, i_4);
                            itemSlotComponents[0] = defs;
                        }
                    }
                }
            } else {
                Component[] arr_6 = inter.method1617();
                for (i_4 = 0; i_4 < arr_6.length && arr_6[i_4] != defs; i_4++) {
                }
                if (i_4 < arr_6.length) {
                    ArrayUtils.arrayCopy(arr_6, 0, arr_6, 1, i_4);
                    arr_6[0] = defs;
                }
            }
        }
    }
}
