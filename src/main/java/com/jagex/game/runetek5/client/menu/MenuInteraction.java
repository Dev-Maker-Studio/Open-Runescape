package com.jagex.game.runetek5.client.menu;

import com.jagex.game.runetek5.cache.node.NodeIterator;
import com.jagex.game.runetek5.cache.node.QueueIterator;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.console.CommandExecutor;
import com.jagex.game.runetek5.client.input.keyboard.KeyDownCommand;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.entity.gameobject.StationaryWall;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder;
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.trigger.worldmap.WorldMapTrigger;
import com.jagex.game.runetek5.utils.TextUtils;

public class MenuInteraction {

	public static void processMenuclick(MiniMenuEntry entry, int x, int y) {
	    if (entry != null && MiniMenu.aClass482171.tail != entry) {
	        int attr1 = entry.anInt9581;
	        int attr2 = entry.anInt9582;
	        int clickType = entry.clickType;
	        int uid = (int) entry.entityUid;
	        long bitpackedEntity = entry.entityUid;
	        if (clickType >= 2000) {
	            clickType -= 2000;
	        }
	
	        CoordGrid baseCoordinates = Resource.mapRegion.getBaseCoordinates();
	        ClientProt playerInteract = null;
	        if (clickType == 44) {
	            playerInteract = ClientProt.PLAYER_OP1;
	        } else if (clickType == 45) {
	            playerInteract = ClientProt.PLAYER_OP2;
	        } else if (clickType == 46) {
	            playerInteract = ClientProt.PLAYER_OP3;
	        } else if (clickType == 47) {
	            playerInteract = ClientProt.PLAYER_OP4;
	        } else if (clickType == 48) {
	            playerInteract = ClientProt.PLAYER_OP5;
	        } else if (clickType == 49) {
	            playerInteract = ClientProt.PLAYER_OP6;
	        } else if (clickType == 50) {
	            playerInteract = ClientProt.PLAYER_OP7;
	        } else if (clickType == 51) {
	            playerInteract = ClientProt.PLAYER_OP8;
	        } else if (clickType == 52) {
	            playerInteract = ClientProt.PLAYER_OP9;
	        } else if (clickType == 53) {
	            playerInteract = ClientProt.PLAYER_OP10;
	        }
	
	        if (playerInteract != null) {
	        	PlayerEntity player = PlayerUpdate.players[uid];
	            if (player != null) {
	                client.crossX = x;
	                client.crossY = y;
	                client.crossType = 2;
	                client.crossSpritePtr = 0;
	                TcpPacket ifPlayerPacket = PacketEncoder.createPacket(playerInteract, client.GAME_CONNECTION_CONTEXT.isaac);
	                ifPlayerPacket.buffer.writeShort(uid);
	                ifPlayerPacket.buffer.writeByteSubtract(KeyDownCommand.runKeyDown() ? 1 : 0);
	                client.GAME_CONNECTION_CONTEXT.queuePacket(ifPlayerPacket);
	                RouteFinder.calculateRoute(player.movementBaseX[0], player.movementBaseY[0], true, RouteStrategies.forPlayer(player.movementBaseX[0], player.movementBaseY[0], player.getSize(), player.getSize()));
	            }
	        }
	
	        TcpPacket packet;
	        if (clickType == 16) {
	            client.crossX = x;
	            client.crossY = y;
	            client.crossType = 2;
	            client.crossSpritePtr = 0;
	            packet = PacketEncoder.createPacket(ClientProt.IF_ON_PLAYER, client.GAME_CONNECTION_CONTEXT.isaac);
	            packet.buffer.writeShort(client.anInt7345);
	            packet.buffer.writeShortLittle(PlayerUpdate.MY_PLAYER.playerIndex);
	            packet.buffer.writeByteSubtract(KeyDownCommand.runKeyDown() ? 1 : 0);
	            packet.buffer.writeIntInverseMiddle(client.anInt56);
	            packet.buffer.writeShortLittle(client.anInt7346);
	            client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
	        }
	
	        if (clickType == 57 || clickType == 1007) {
	            Interface.ifOptionInteract(uid, attr2, attr1, entry.coloredName);
	        }
	
	        if (clickType == 15) {
	        	PlayerEntity player = PlayerUpdate.players[uid];
	            if (player != null) {
	                client.crossX = x;
	                client.crossY = y;
	                client.crossType = 2;
	                client.crossSpritePtr = 0;
	                TcpPacket ifPlayerPacket = PacketEncoder.createPacket(ClientProt.IF_ON_PLAYER, client.GAME_CONNECTION_CONTEXT.isaac);
	                ifPlayerPacket.buffer.writeShort(client.anInt7345);
	                ifPlayerPacket.buffer.writeShortLittle(uid);
	                ifPlayerPacket.buffer.writeByteSubtract(KeyDownCommand.runKeyDown() ? 1 : 0);
	                ifPlayerPacket.buffer.writeIntInverseMiddle(client.anInt56);
	                ifPlayerPacket.buffer.writeShortLittle(client.anInt7346);
	                client.GAME_CONNECTION_CONTEXT.queuePacket(ifPlayerPacket);
	                RouteFinder.calculateRoute(player.movementBaseX[0], player.movementBaseY[0], true, RouteStrategies.forPlayer(player.movementBaseX[0], player.movementBaseY[0], player.getSize(), player.getSize()));
	            }
	        }
	
	        if (clickType == 2) {
	            client.crossX = x;
	            client.crossY = y;
	            client.crossType = 2;
	            client.crossSpritePtr = 0;
	            packet = PacketEncoder.createPacket(ClientProt.IF_ON_OBJECT, client.GAME_CONNECTION_CONTEXT.isaac);
	            packet.buffer.writeShortAddLittle(attr1 + baseCoordinates.x);
	            packet.buffer.writeByteSubtract(KeyDownCommand.runKeyDown() ? 1 : 0);
	            packet.buffer.writeIntMiddle((int) (bitpackedEntity >>> 32) & 0x7fffffff);
	            packet.buffer.writeInt(client.anInt56);
	            packet.buffer.writeShortLittle(client.anInt7346);
	            packet.buffer.writeShortAdd(client.anInt7345);
	            packet.buffer.writeShortLittle(attr2 + baseCoordinates.y);
	            client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
	            RouteFinder.routeToSceneobject(attr1, attr2, bitpackedEntity);
	        }
	
	        ClientProt npcInteract = null;
	        if (clickType == 9) {
	            npcInteract = ClientProt.NPC_OP1;
	        } else if (clickType == 10) {
	            npcInteract = ClientProt.NPC_OP2;
	        } else if (clickType == 11) {
	            npcInteract = ClientProt.NPC_OP3;
	        } else if (clickType == 12) {
	            npcInteract = ClientProt.NPC_OP4;
	        } else if (clickType == 13) {
	            npcInteract = ClientProt.NPC_OP5;
	        } else if (clickType == 1003) {
	            npcInteract = ClientProt.NPC_EXAMINE;
	        }
	
	        if (npcInteract != null) {
	            ObjectNode entity = client.NPC_MAP.get(uid);
	            if (entity != null) {
	                NpcEntity npc = (NpcEntity) entity.value;
	                client.crossX = x;
	                client.crossY = y;
	                client.crossType = 2;
	                client.crossSpritePtr = 0;
	                TcpPacket npcInteractPacket = PacketEncoder.createPacket(npcInteract, client.GAME_CONNECTION_CONTEXT.isaac);
	                npcInteractPacket.buffer.writeShort(uid);
	                npcInteractPacket.buffer.writeByte(KeyDownCommand.runKeyDown() ? 1 : 0);
	                client.GAME_CONNECTION_CONTEXT.queuePacket(npcInteractPacket);
	                RouteFinder.calculateRoute(npc.movementBaseX[0], npc.movementBaseY[0], true, RouteStrategies.forPlayer(npc.movementBaseX[0], npc.movementBaseY[0], npc.getSize(), npc.getSize()));
	            }
	        }
	
	        if (clickType == 60) {
	            if (client.PLAYER_RIGHTS > 0 && KeyDownCommand.teleportKeyDown()) {
	                CommandExecutor.executeTeleportCommand(PlayerUpdate.MY_PLAYER.plane, attr1 + baseCoordinates.x, attr2 + baseCoordinates.y);
	            } else {
	                client.crossX = x;
	                client.crossY = y;
	                client.crossType = 1;
	                client.crossSpritePtr = 0;
	                TcpPacket walkPacket = PacketEncoder.createPacket(ClientProt.UNK_63, client.GAME_CONNECTION_CONTEXT.isaac);
	                walkPacket.buffer.writeShortAdd(attr1 + baseCoordinates.x);
	                walkPacket.buffer.writeShort(attr2 + baseCoordinates.y);
	                client.GAME_CONNECTION_CONTEXT.queuePacket(walkPacket);
	            }
	        }
	
	        if (clickType == 23) {
	            if (client.PLAYER_RIGHTS > 0 && KeyDownCommand.teleportKeyDown()) {
	                CommandExecutor.executeTeleportCommand(PlayerUpdate.MY_PLAYER.plane, attr1 + baseCoordinates.x, attr2 + baseCoordinates.y);
	            } else {
	            	TcpPacket walkPacket = PacketEncoder.walkToTile(attr1, attr2, uid);
	                if (uid == 1) {
	                    walkPacket.buffer.writeByte(-1);
	                    walkPacket.buffer.writeByte(-1);
	                    walkPacket.buffer.writeShort((int) Camera.camAngleY);
	                    walkPacket.buffer.writeByte(57);
	                    walkPacket.buffer.writeByte(client.minimapRotation);
	                    walkPacket.buffer.writeByte(client.minimapZoom);
	                    walkPacket.buffer.writeByte(89);
	                    Vector3 absolutePosition = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
	                    walkPacket.buffer.writeShort((int) absolutePosition.x);
	                    walkPacket.buffer.writeShort((int) absolutePosition.z);
	                    walkPacket.buffer.writeByte(63);
	                } else {
	                    client.crossX = x;
	                    client.crossY = y;
	                    client.crossType = 1;
	                    client.crossSpritePtr = 0;
	                }
	
	                client.GAME_CONNECTION_CONTEXT.queuePacket(walkPacket);
	                RouteFinder.calculateRoute(attr1, attr2, true, RouteStrategies.forFixedTile(attr1, attr2));
	            }
	        }
	
	        if (clickType == 17) {
	            client.crossX = x;
	            client.crossY = y;
	            client.crossType = 2;
	            client.crossSpritePtr = 0;
	            TcpPacket ifGrounditemPacket = PacketEncoder.createPacket(ClientProt.IF_ON_GROUND_ITEM, client.GAME_CONNECTION_CONTEXT.isaac);
	            ifGrounditemPacket.buffer.writeShortAdd(client.anInt7346);
	            ifGrounditemPacket.buffer.writeIntInverseMiddle(client.anInt56);
	            ifGrounditemPacket.buffer.writeShort(uid);
	            ifGrounditemPacket.buffer.writeByteSubtract(KeyDownCommand.runKeyDown() ? 1 : 0);
	            ifGrounditemPacket.buffer.writeShortAddLittle(client.anInt7345);
	            ifGrounditemPacket.buffer.writeShortAddLittle(attr2 + baseCoordinates.y);
	            ifGrounditemPacket.buffer.writeShortLittle(attr1 + baseCoordinates.x);
	            client.GAME_CONNECTION_CONTEXT.queuePacket(ifGrounditemPacket);
	            RouteFinder.routeToGrounditem(attr1, attr2);
	        }
	
	        if (clickType == 59) {
	            client.crossX = x;
	            client.crossY = y;
	            client.crossType = 1;
	            client.crossSpritePtr = 0;
	            TcpPacket ifTilePacket = PacketEncoder.createPacket(ClientProt.IF_ON_TILE, client.GAME_CONNECTION_CONTEXT.isaac);
	            ifTilePacket.buffer.writeShortLittle(client.anInt7346);
	            ifTilePacket.buffer.writeShortLittle(attr2 + baseCoordinates.y);
	            ifTilePacket.buffer.writeIntMiddle(client.anInt56);
	            ifTilePacket.buffer.writeShortAdd(client.anInt7345);
	            ifTilePacket.buffer.writeShortLittle(attr1 + baseCoordinates.x);
	            client.GAME_CONNECTION_CONTEXT.queuePacket(ifTilePacket);
	            RouteFinder.calculateRoute(attr1, attr2, true, RouteStrategies.forFixedTile(attr1, attr2));
	        }
	
	        if (clickType == 30 && client.aClass1187352 == null) {
	            PacketEncoder.continueInterfacePressed(attr2, attr1);
	            client.aClass1187352 = Component.listIcomponent(attr2, attr1);
	            if (client.aClass1187352 != null) {
	                Component.redrawComponent(client.aClass1187352);
	            }
	        }
	
	        if (clickType == 58) {
	        	Component component = Component.listIcomponent(attr2, attr1);
	            if (component != null) {
	                PacketEncoder.method2590(component);
	            }
	        }
	
	        if (clickType == 25) {
	        	Component component = Component.listIcomponent(attr2, attr1);
	            if (component != null) {
	                InterfaceComponentRenderer.method1170();
	                IFEvents class282Sub1021 = client.getComponentSettings(component);
	                InterfaceRenderer.setUseOptionFlags(component, class282Sub1021.getUseOptionFlags(), class282Sub1021.interfaceId);
	                client.useOnType = Interface.getUseOptionString(component);
	                if (client.useOnType == null) {
	                    client.useOnType = "Null";
	                }
	
	                client.useOnTarget = component.opBase + TextUtils.setTextColor(16777215);
	            }
	        } else {
	            ClientProt groundItemInteract = null;
	            if (clickType == 18) {
	                groundItemInteract = ClientProt.GROUND_ITEM_OP1;
	            } else if (clickType == 19) {
	                groundItemInteract = ClientProt.GROUND_ITEM_OP2;
	            } else if (clickType == 20) {
	                groundItemInteract = ClientProt.GROUND_ITEM_OP3;
	            } else if (clickType == 21) {
	                groundItemInteract = ClientProt.GROUND_ITEM_OP4;
	            } else if (clickType == 22) {
	                groundItemInteract = ClientProt.GROUND_ITEM_OP5;
	            } else if (clickType == 1004) {
	                groundItemInteract = ClientProt.GROUND_ITEM_EXAMINE;
	            }
	
	            if (groundItemInteract != null) {
	                client.crossX = x;
	                client.crossY = y;
	                client.crossType = 2;
	                client.crossSpritePtr = 0;
	                TcpPacket groundItemPacket = PacketEncoder.createPacket(groundItemInteract, client.GAME_CONNECTION_CONTEXT.isaac);
	                groundItemPacket.buffer.writeShortAddLittle(uid);
	                groundItemPacket.buffer.writeByteInverse(KeyDownCommand.runKeyDown() ? 1 : 0);
	                groundItemPacket.buffer.writeShort(attr2 + baseCoordinates.y);
	                groundItemPacket.buffer.writeShortAdd(attr1 + baseCoordinates.x);
	                client.GAME_CONNECTION_CONTEXT.queuePacket(groundItemPacket);
	                RouteFinder.routeToGrounditem(attr1, attr2);
	            }
	
	            if (clickType == 8) {
	                ObjectNode entity = client.NPC_MAP.get(uid);
	                if (entity != null) {
	                    NpcEntity npc = (NpcEntity) entity.value;
	                    client.crossX = x;
	                    client.crossY = y;
	                    client.crossType = 2;
	                    client.crossSpritePtr = 0;
	                    TcpPacket ifNpcPacket = PacketEncoder.createPacket(ClientProt.IF_ON_NPC, client.GAME_CONNECTION_CONTEXT.isaac);
	                    ifNpcPacket.buffer.writeIntInverseMiddle(client.anInt56);
	                    ifNpcPacket.buffer.writeShortAddLittle(uid);
	                    ifNpcPacket.buffer.writeByteSubtract(KeyDownCommand.runKeyDown() ? 1 : 0);
	                    ifNpcPacket.buffer.writeShortAddLittle(client.anInt7346);
	                    ifNpcPacket.buffer.writeShortAdd(client.anInt7345);
	                    client.GAME_CONNECTION_CONTEXT.queuePacket(ifNpcPacket);
	                    RouteFinder.calculateRoute(npc.movementBaseX[0], npc.movementBaseY[0], true, RouteStrategies.forPlayer(npc.movementBaseX[0], npc.movementBaseY[0], npc.getSize(), npc.getSize()));
	                }
	            }
	
	            ClientProt locInteract = null;
	            if (clickType == 3) {
	                locInteract = ClientProt.OBJECT_OP1;
	            } else if (clickType == 4) {
	                locInteract = ClientProt.OBJECT_OP2;
	            } else if (clickType == 5) {
	                locInteract = ClientProt.OBJECT_OP3;
	            } else if (clickType == 6) {
	                locInteract = ClientProt.OBJECT_OP4;
	            } else if (clickType == 1001) {
	                locInteract = ClientProt.OBJECT_OP5;
	            } else if (clickType == 1002) {
	                locInteract = ClientProt.OBJECT_EXAMINE;
	            }
	
	            if (locInteract != null) {
	                client.crossX = x;
	                client.crossY = y;
	                client.crossType = 2;
	                client.crossSpritePtr = 0;
	                TcpPacket locInteractPacket = PacketEncoder.createPacket(locInteract, client.GAME_CONNECTION_CONTEXT.isaac);
	                locInteractPacket.buffer.writeShort(attr2 + baseCoordinates.y);
	                locInteractPacket.buffer.writeShort(attr1 + baseCoordinates.x);
	                locInteractPacket.buffer.writeInt((int) (bitpackedEntity >>> 32) & 0x7fffffff);
	                locInteractPacket.buffer.writeByteAdd(KeyDownCommand.runKeyDown() ? 1 : 0);
	                client.GAME_CONNECTION_CONTEXT.queuePacket(locInteractPacket);
	                RouteFinder.routeToSceneobject(attr1, attr2, bitpackedEntity);
	            }
	
	            if (clickType == 1008 || clickType == 1009 || clickType == 1010 || clickType == 1011 || clickType == 1012) {
	                WorldMapTrigger.onWorldmapMinimenuClick(clickType, uid, attr1);
	            }
	
	            if (client.interfaceSelectionActive) {
	                InterfaceComponentRenderer.method1170();
	            }
	
	            if (MenuInteraction.aClass1184825 != null && client.anInt7297 == 0) {
	                Component.redrawComponent(MenuInteraction.aClass1184825);
	            }
	        }
	    }
	
	}

	public static void method7703() {
	    if (client.CURRENT_CUTSCENE < 0) {
	        FontMetrics menufont = FontMetrics.getForMinimenu();
	        if (!MiniMenu.isOpen) {
	            MiniMenu.aBool162 = MiniMenu.anInt198 != -1 && MiniMenu.entryCount >= MiniMenu.anInt198 || (FontMetrics.useBoldFont ? 26 : 22) + MiniMenu.anInt178 * MiniMenu.entryCount > GameShell.screenCanvasHeight * -969250379;
	        }
	        MiniMenu.aClass482174.remove_all();
	        MiniMenu.aClass482175.remove_all();
	        for (MiniMenuEntry entry = (MiniMenuEntry) MiniMenu.aClass482171.getFirst(); entry != null; entry = (MiniMenuEntry) MiniMenu.aClass482171.next()) {
	            int clickType = entry.clickType;
	            if (clickType < 1000) {
	                entry.unlink();
	                if (clickType != 59 && clickType != 2 && clickType != 8 && clickType != 17 && clickType != 15 && clickType != 16 && clickType != 58) {
	                    MiniMenu.aClass482174.addFirst(entry);
	                } else {
	                    MiniMenu.aClass482175.addFirst(entry);
	                }
	            }
	        }
	        MiniMenu.aClass482174.method8063(MiniMenu.aClass482171);
	        MiniMenu.aClass482175.method8063(MiniMenu.aClass482171);
	        if (MiniMenu.entryCount <= 1) {
	            MiniMenu.aCacheableNodeSub79445 = null;
	            MiniMenu.aCacheableNodeSub712 = null;
	            MiniMenu.aCacheableNodeSub7168 = null;
	        } else {
	            if (KeyDownCommand.minimenuKeyDown() && MiniMenu.entryCount > 2) {
	                MiniMenu.aCacheableNodeSub79445 = (MiniMenuEntry) MiniMenu.aClass482171.tail.previous.previous;
	            } else {
	                MiniMenu.aCacheableNodeSub79445 = (MiniMenuEntry) MiniMenu.aClass482171.tail.previous;
	            }
	            MiniMenu.aCacheableNodeSub712 = (MiniMenuEntry) MiniMenu.aClass482171.tail.previous;
	            if (MiniMenu.entryCount > 2) {
	                MiniMenu.aCacheableNodeSub7168 = (MiniMenuEntry) MiniMenu.aCacheableNodeSub712.previous;
	            } else {
	                MiniMenu.aCacheableNodeSub7168 = null;
	            }
	        }
	        MouseRecord mouseRecord = (MouseRecord) client.MOUSE_MOVEMENTS.getFirst();
	        int mouseX;
	        int mouseY;
	        if (mouseRecord != null) {
	            mouseX = mouseRecord.getX();
	            mouseY = mouseRecord.getY();
	        } else {
	            mouseX = MouseRecord.recorder.getMouseX();
	            mouseY = MouseRecord.recorder.getMouseY();
	        }
	        if (MiniMenu.isOpen) {
	            int i_6;
	            int i_11;
	            if (KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS.openMinimenuInputSubscriber, mouseRecord)) {
	                int i_5;
	                QueueIterator class46915;
	                if (MiniMenu.aCacheableNodeSub15163 != null && mouseX >= MiniMenu.anInt3996 && mouseX <= MiniMenu.anInt3996 + MiniMenu.anInt8518 && mouseY >= MiniMenu.anInt9471 && mouseY <= StationaryWall.anInt10526 + MiniMenu.anInt9471) {
	                    i_5 = -1;
	                    for (i_6 = 0; i_6 < MiniMenu.aCacheableNodeSub15163.queueSize; i_6++) {
	                        if (FontMetrics.useBoldFont) {
	                            i_11 = i_6 * MiniMenu.anInt178 + menufont.topPadding + MiniMenu.anInt9471 + 1 + 20;
	                            if (mouseY > i_11 - menufont.topPadding - 1 && mouseY < i_11 + menufont.bottomPadding) {
	                                i_5 = i_6;
	                            }
	                        } else {
	                            i_11 = i_6 * MiniMenu.anInt178 + MiniMenu.anInt9471 + 31;
	                            if (mouseY > i_11 - 13 && mouseY < i_11 + 3) {
	                                i_5 = i_6;
	                            }
	                        }
	                    }
	                    if (i_5 != -1) {
	                        i_6 = 0;
	                        class46915 = new QueueIterator(MiniMenu.aCacheableNodeSub15163.nodeQueue);
	                        for (MiniMenuEntry clickedEntry = (MiniMenuEntry) class46915.method7790(); clickedEntry != null; clickedEntry = (MiniMenuEntry) class46915.next()) {
	                            if (i_5 == i_6) {
	                                processMenuclick(clickedEntry, mouseX, mouseY);
	                                break;
	                            }
	                            ++i_6;
	                        }
	                    }
	                    MiniMenu.close();
	                } else if (mouseX >= StaticMethods.anInt3555 && mouseX <= MiniMenu.anInt8975 + StaticMethods.anInt3555 && mouseY >= StaticMethods.anInt2300 && mouseY <= StaticMethods.anInt2300 + MiniMenu.anInt1104) {
	                    if (!MiniMenu.aBool162) {
	                        i_5 = -1;
	                        for (i_6 = 0; i_6 < MiniMenu.entryCount; i_6++) {
	                            if (FontMetrics.useBoldFont) {
	                                i_11 = (MiniMenu.entryCount - 1 - i_6) * MiniMenu.anInt178 + menufont.topPadding + StaticMethods.anInt2300 + 1 + 20;
	                                if (mouseY > i_11 - menufont.topPadding - 1 && mouseY < i_11 + menufont.bottomPadding) {
	                                    i_5 = i_6;
	                                }
	                            } else {
	                                i_11 = (MiniMenu.entryCount - 1 - i_6) * MiniMenu.anInt178 + StaticMethods.anInt2300 + 31;
	                                if (mouseY > i_11 - 13 && mouseY < i_11 + 3) {
	                                    i_5 = i_6;
	                                }
	                            }
	                        }
	                        if (i_5 != -1) {
	                            i_6 = 0;
	                            NodeIterator nodeiterator7 = new NodeIterator(MiniMenu.aClass482171);
	                            for (MiniMenuEntry clickedEntry = (MiniMenuEntry) nodeiterator7.method7683(); clickedEntry != null; clickedEntry = (MiniMenuEntry) nodeiterator7.next()) {
	                                if (i_6 == i_5) {
	                                    processMenuclick(clickedEntry, mouseX, mouseY);
	                                    break;
	                                }
	                                ++i_6;
	                            }
	                        }
	                        MiniMenu.close();
	                    } else {
	                        i_5 = -1;
	                        for (i_6 = 0; i_6 < MiniMenu.anInt170; i_6++) {
	                            if (FontMetrics.useBoldFont) {
	                                i_11 = i_6 * MiniMenu.anInt178 + menufont.topPadding + StaticMethods.anInt2300 + 1 + 20;
	                                if (mouseY > i_11 - menufont.topPadding - 1 && mouseY < i_11 + menufont.bottomPadding) {
	                                    i_5 = i_6;
	                                    break;
	                                }
	                            } else {
	                                i_11 = i_6 * MiniMenu.anInt178 + StaticMethods.anInt2300 + 31;
	                                if (mouseY > i_11 - 13 && mouseY < i_11 + 3) {
	                                    i_5 = i_6;
	                                    break;
	                                }
	                            }
	                        }
	                        if (i_5 != -1) {
	                            i_6 = 0;
	                            class46915 = new QueueIterator(MiniMenu.aClass477182);
	                            for (ColoredMiniMenuEntry class282Sub50Sub1517 = (ColoredMiniMenuEntry) class46915.method7790(); class282Sub50Sub1517 != null; class282Sub50Sub1517 = (ColoredMiniMenuEntry) class46915.next()) {
	                                if (i_5 == i_6) {
	                                    processMenuclick((MiniMenuEntry) class282Sub50Sub1517.nodeQueue.current.next, mouseX, mouseY);
	                                    MiniMenu.close();
	                                    break;
	                                }
	                                ++i_6;
	                            }
	                        }
	                    }
	                }
	            } else {
	            	boolean bool16 = false;
	                if (MiniMenu.aCacheableNodeSub15163 != null) {
	                    if (mouseX >= MiniMenu.anInt3996 - 10 && mouseX <= MiniMenu.anInt8518 + MiniMenu.anInt3996 + 10 && mouseY >= MiniMenu.anInt9471 - 10 && mouseY <= StationaryWall.anInt10526 + MiniMenu.anInt9471 + 10) {
	                        bool16 = true;
	                    } else {
	                        MiniMenu.method6810();
	                    }
	                }
	                if (!bool16) {
	                    if (mouseX >= StaticMethods.anInt3555 - 10 && mouseX <= MiniMenu.anInt8975 + StaticMethods.anInt3555 + 10 && mouseY >= StaticMethods.anInt2300 - 10 && mouseY <= MiniMenu.anInt1104 + StaticMethods.anInt2300 + 10) {
	                        if (MiniMenu.aBool162) {
	                            i_6 = -1;
	                            i_11 = -1;
	                            int i_12;
	                            for (i_12 = 0; i_12 < MiniMenu.anInt170; i_12++) {
	                                int i_13;
	                                if (FontMetrics.useBoldFont) {
	                                    i_13 = i_12 * MiniMenu.anInt178 + menufont.topPadding + StaticMethods.anInt2300 + 1 + 20;
	                                    if (mouseY > i_13 - menufont.topPadding - 1 && mouseY < i_13 + menufont.bottomPadding) {
	                                        i_6 = i_12;
	                                        i_11 = i_13 - menufont.topPadding - 1;
	                                        break;
	                                    }
	                                } else {
	                                    i_13 = i_12 * MiniMenu.anInt178 + StaticMethods.anInt2300 + 31;
	                                    if (mouseY > i_13 - 13 && mouseY < i_13 + 3) {
	                                        i_6 = i_12;
	                                        i_11 = i_13 - 13;
	                                        break;
	                                    }
	                                }
	                            }
	                            if (i_6 != -1) {
	                                i_12 = 0;
	                                QueueIterator class4699 = new QueueIterator(MiniMenu.aClass477182);
	                                for (ColoredMiniMenuEntry class282Sub50Sub1510 = (ColoredMiniMenuEntry) class4699.method7790(); class282Sub50Sub1510 != null; class282Sub50Sub1510 = (ColoredMiniMenuEntry) class4699.next()) {
	                                    if (i_6 == i_12) {
	                                        if (class282Sub50Sub1510.queueSize > 1) {
	                                            MiniMenu.method12212(class282Sub50Sub1510, i_11);
	                                        }
	                                        break;
	                                    }
	                                    ++i_12;
	                                }
	                            }
	                        }
	                    } else {
	                        MiniMenu.close();
	                    }
	                }
	            }
	        } else {
	        	boolean bool16 = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS.anInterface175878, mouseRecord);
	            boolean bool18 = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS.anInterface175874, mouseRecord);
	            boolean bool20 = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS.anInterface175875, mouseRecord);
	            if ((bool16 || bool18) && (client.anInt7339 == 1 && MiniMenu.entryCount > 2 || MiniMenu.method12562())) {
	                bool20 = true;
	            }
	            if (bool20 && MiniMenu.entryCount > 0) {
	                if (client.draggingComponent == null && client.menuItemCount == 0) {
	                    StaticMethods.method4653(mouseX, mouseY);
	                } else {
	                    MiniMenu.anInt167 = 2;
	                }
	            } else if (bool18) {
	                if (MiniMenu.aCacheableNodeSub7168 != null) {
	                    processMenuclick(MiniMenu.aCacheableNodeSub7168, mouseX, mouseY);
	                }
	            } else if (bool16) {
	                if (MiniMenu.aCacheableNodeSub79445 == null) {
	                    if (client.interfaceSelectionActive) {
	                        InterfaceComponentRenderer.method1170();
	                    }
	                } else {
	                    boolean bool19 = client.draggingComponent != null || client.menuItemCount > 0;
	                    if (bool19) {
	                        MiniMenu.anInt167 = 1;
	                        MiniMenu.aCacheableNodeSub7438 = MiniMenu.aCacheableNodeSub79445;
	                    } else {
	                        processMenuclick(MiniMenu.aCacheableNodeSub79445, mouseX, mouseY);
	                    }
	                }
	            }
	            if (client.draggingComponent == null && client.menuItemCount == 0) {
	                MiniMenu.anInt167 = 0;
	                MiniMenu.aCacheableNodeSub7438 = null;
	            }
	        }
	    }
	}

	public static Component aClass1184825;

}
