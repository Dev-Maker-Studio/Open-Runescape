package com.jagex.game.runetek5.client.menu;

import java.util.List;

import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.entity.grounditem.GroundItem;
import com.jagex.game.runetek5.entity.grounditem.GroundItemNode;
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.scene.MapLoader;
import com.jagex.game.runetek5.scene.InteractiveEntityCache;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.TextUtils;
import com.jagex.game.runetek5.vardomain.VarProviders;

/**
 * 
 * @author Brainpower - 27/01/2023
 * 
 * Value 2000 = lower the priority of the attack option (move it down if on top)
 *
 */
public class MiniMenuBuilder {

	public static boolean SHOW_FACE_HERE;
	
	public static void playerHovered(PlayerEntity player, boolean onDifferentPlane) {
	    if (MiniMenu.entryCount < 412) {
	        if (PlayerUpdate.MY_PLAYER == player) {
	            if (client.interfaceSelectionActive && (MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x10) != 0) {
	                MiniMenu.addEntry(client.useOnType, client.useOnTarget + " " + "->" + " " + TextUtils.setTextColor(16777215) + LocalizedText.SELF.translate(client.CURRENT_LANGUAGE), MiniMenu.useOnCursor, 16, -1, 0L, 0, 0, true, false, player.playerIndex, false);
	            }
	        } else {
	            String username;
	            if (player.elo == 0) {
	                boolean combatColorDisplayed = true;
	                if (PlayerUpdate.MY_PLAYER.cbLevelRelated != -1 && player.cbLevelRelated != -1) {
	                    int cmbRelatedDelta = Math.min(PlayerUpdate.MY_PLAYER.cbLevelRelated, player.cbLevelRelated);
	                    int cmbDelta = PlayerUpdate.MY_PLAYER.combatLevel - player.combatLevel;
	                    if (cmbDelta < 0) {
	                        cmbDelta = -cmbDelta;
	                    }
	                    if (cmbDelta > cmbRelatedDelta) {
	                        combatColorDisplayed = false;
	                    }
	                }
	                String level = client.CURRENT_GAME == Game.STELLAR_DAWN 
	                		? LocalizedText.RATING_.translate(client.CURRENT_LANGUAGE) 
	                		: LocalizedText.MINIMENU_LEVEL.translate(client.CURRENT_LANGUAGE);
	                if (player.combatLevel >= player.cbLevelWithSumm) {
	                    username = player.getUsernameWithTitle() + (combatColorDisplayed
                    		? TextUtils.calculateCombatBasedColor(player.combatLevel, PlayerUpdate.MY_PLAYER.combatLevel)
                    		: TextUtils.setTextColor(16777215)) + " (" + level + player.combatLevel + ")";
	                } else {
	                    username = player.getUsernameWithTitle() + (combatColorDisplayed
	                    	? TextUtils.calculateCombatBasedColor(player.combatLevel, PlayerUpdate.MY_PLAYER.combatLevel)
	                    	: TextUtils.setTextColor(16777215)) + " (" + level + player.combatLevel + "+" + (player.cbLevelWithSumm - player.combatLevel) + ")";
	                }
	            } else if (player.elo == -1) {
	                username = player.getUsernameWithTitle();
	            } else {
	                username = player.getUsernameWithTitle() + " (" + LocalizedText.SKILL_.translate(client.CURRENT_LANGUAGE) + player.elo + ")";
	            }
	            if (client.interfaceSelectionActive && !onDifferentPlane && (MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x8) != 0) {
	                MiniMenu.addEntry(client.useOnType, client.useOnTarget + " " + "->" + " " + TextUtils.setTextColor(16777215) + username, MiniMenu.useOnCursor, 15, -1, player.playerIndex, 0, 0, true, false, player.playerIndex, false);
	            }
	            if (onDifferentPlane) {
	                MiniMenu.addEntry(TextUtils.setTextColor(13421772) + username, "", -1, -1, 0, 0L, 0, 0, false, true, player.playerIndex, false);
	            } else {
	                for (int index = 7; index >= 0; --index) {
	                    if (client.PLAYER_OPTIONS[index] != null) {
	                        short attackClickType = 0;
	                        if (Game.RUNESCAPE == client.CURRENT_GAME && client.PLAYER_OPTIONS[index].equalsIgnoreCase(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE))) {
	                            if (client.REDUCE_ATTACK_PRIORITY && player.combatLevel > PlayerUpdate.MY_PLAYER.combatLevel) {
	                                attackClickType = 2000;
	                            }
	                            if (PlayerUpdate.MY_PLAYER.teamId != 0 && player.teamId != 0) {
	                                if (player.teamId == PlayerUpdate.MY_PLAYER.teamId) {
	                                    attackClickType = 2000;
	                                } else {
	                                    attackClickType = 0;
	                                }
	                            } else if (player.reduceAttackPriority) {
	                                attackClickType = 2000;
	                            }
	                        } else if (client.PLAYER_OPTION_REDUCED_PRIORITY[index]) {
	                            attackClickType = 2000;
	                        }
	                        short clickType = (short) (client.aShortArray7239[index] + attackClickType);
	                        int cursor = client.PLAYER_OPTION_CURSORS[index] != -1 ? client.PLAYER_OPTION_CURSORS[index] : CursorManager.defaultAttackCursor;
	                        MiniMenu.addEntry(client.PLAYER_OPTIONS[index], TextUtils.setTextColor(16777215) + username, cursor, clickType, -1, player.playerIndex, 0, 0, true, false, player.playerIndex, false);
	                    }
	                }
	            }
	            if (!onDifferentPlane) {
	                for (MiniMenuEntry entry = (MiniMenuEntry) MiniMenu.aClass482171.getFirst(); entry != null; entry = (MiniMenuEntry) MiniMenu.aClass482171.next()) {
	                    if (entry.clickType == 23) {
	                        entry.aString9585 = TextUtils.setTextColor(16777215) + username;
	                        break;
	                    }
	                }
	            }
	        }
	    }
	}

	public static void tileHovered(final AbstractToolkit toolkit, final int mouseX, final int mouseY) {
	    if (mouseX < 0 || mouseY < 0 || Camera.aClass384158 == null) {
	        return;
	    }
	    final CoordGrid regionBase = Resource.mapRegion.getBaseCoordinates();
	    final Matrix44 matrix445 = toolkit.method8449();
	    toolkit.setModelMatrix(Camera.aClass384158);
	    toolkit.setSunPosition(Camera.aClass294155);
	    toolkit.setClipping(MiniMenu.anInt180, MiniMenu.anInt181, MiniMenu.anInt180 + Camera.anInt176, Camera.anInt184 + MiniMenu.anInt181);
	    toolkit.setDrawableArea(MiniMenu.anInt180, MiniMenu.anInt181, Camera.anInt176, Camera.anInt184);
	    matrix445.fromMatrix(Camera.aClass294155);
	    matrix445.multiplyBy(Camera.aClass384158);
	    matrix445.invert();
	    final int i_3 = mouseX - MiniMenu.anInt180;
	    final int i_4 = mouseY - MiniMenu.anInt181;
	    if (Resource.mapRegion.getScene() != null && (!client.interfaceSelectionActive || (MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x40) != 0x0)) {
	        int x = -1;
	        int y = -1;
	        final float f_10 = 2.0f * i_3 / Camera.anInt176 - 1.0f;
	        final float f_11 = 2.0f * i_4 / Camera.anInt184 - 1.0f;
	        matrix445.scale(f_10, f_11, -1.0f, StaticMethods.aFloatArray194);
	        final float f_12 = StaticMethods.aFloatArray194[0] / StaticMethods.aFloatArray194[3];
	        final float f_13 = StaticMethods.aFloatArray194[1] / StaticMethods.aFloatArray194[3];
	        final float f_14 = StaticMethods.aFloatArray194[2] / StaticMethods.aFloatArray194[3];
	        matrix445.scale(f_10, f_11, 1.0f, StaticMethods.aFloatArray194);
	        final float f_15 = StaticMethods.aFloatArray194[0] / StaticMethods.aFloatArray194[3];
	        final float f_16 = StaticMethods.aFloatArray194[1] / StaticMethods.aFloatArray194[3];
	        final float f_17 = StaticMethods.aFloatArray194[2] / StaticMethods.aFloatArray194[3];
	        final float f_18 = MiniMenu.method288(f_12, f_13, f_14, f_15, f_16, f_17, 4);
	        if (f_18 > 0.0f) {
	            final float f_19 = f_15 - f_12;
	            final float f_20 = f_17 - f_14;
	            final int posX = (int)(f_19 * f_18 + f_12);
	            final int posY = (int)(f_18 * f_20 + f_14);
	            x = posX + (PlayerUpdate.MY_PLAYER.getSize() - 1 << 8) >> 9;
	            y = posY + (PlayerUpdate.MY_PLAYER.getSize() - 1 << 8) >> 9;
	            final byte plane = PlayerUpdate.MY_PLAYER.plane;
	            if (plane < 3 && (Resource.mapRegion.getSettingsBits().tileFunctionMasks[1][posX >> 9][posY >> 9] & 0x2) != 0x0) {}
	        }
	        if (x != -1 && y != -1) {
	            if (client.interfaceSelectionActive && (MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x40) != 0x0) {
	                final Component component = Component.listIcomponent(client.anInt56, client.anInt7345);
	                if (component != null) {
	                    MiniMenu.addEntry(client.useOnType, " -> ", MiniMenu.useOnCursor, 59, -1, 0L, x, y, true, false, x | y, true);
	                }
	                else {
	                    InterfaceComponentRenderer.method1170();
	                }
	            }
	            else {
	                if (MiniMenuBuilder.SHOW_FACE_HERE) {
	                    MiniMenu.addEntry(LocalizedText.FACE_HERE.translate(client.CURRENT_LANGUAGE), "", -1, 60, -1, 0L, x, y, true, false, x | y, true);
	                }
	                MiniMenu.addEntry(InterfaceRenderer.translatedUptext, "", CursorManager.activeCursorId, 23, -1, 0L, x, y, true, false, x | y, true);
	            }
	        }
	    }
	    assert Resource.mapRegion.getScene() != null;
	    final InteractiveEntityCache interactiveEntities = Resource.mapRegion.getScene().interactiveEntities;
	    final int MouseX = mouseX;
	    final int MouseY = mouseY;
	    final List<RenderedEntity> renderedEntity = interactiveEntities.entities;
	    for (final RenderedEntity reference : renderedEntity) {
	        if ((client.playingAltGame || reference.entity.plane == PlayerUpdate.MY_PLAYER.plane) && reference.hovered(toolkit, MouseX, MouseY)) {
	            int startX;
	            int startZ;
	            if (reference.entity instanceof AbstractInteractiveEntity) {
	                startX = ((AbstractInteractiveEntity)reference.entity).startX;
	                startZ = ((AbstractInteractiveEntity)reference.entity).startZ;
	            }
	            else {
	                final Vector3 abs = reference.entity.getAbsolutePosition().coords;
	                startX = (int)abs.x >> 9;
	                startZ = (int)abs.z >> 9;
	            }
	            if (reference.entity instanceof PlayerEntity player) {
                    final int playerSize = player.getSize();
	                final Vector3 pAbs = player.getAbsolutePosition().coords;
	                if (((playerSize & 0x1) == 0x0 && ((int) pAbs.x & 0x1FF) == 0x0 && ((int) pAbs.z & 0x1FF) == 0x0) || ((playerSize & 0x1) == 0x1 && ((int) pAbs.x & 0x1FF) == 0x100 && ((int) pAbs.z & 0x1FF) == 0x100)) {
	                    final int plrStartX = (int) pAbs.x - (player.getSize() - 1 << 8);
	                    final int plrStartZ = (int) pAbs.z - (player.getSize() - 1 << 8);
	                    for (int actionCode = 0; actionCode < client.NPC_UPDATE_INDEX; ++actionCode) {
	                        final ObjectNode npcNode = client.NPC_MAP.get(client.NPC_UPDATE_INDICES[actionCode]);
	                        if (npcNode != null) {
	                            final NpcEntity npcEntity = (NpcEntity) npcNode.value;
	                            if (npcEntity.clientCycles != client.cycles && npcEntity.isHidden) {
	                                final Vector3 nAbs = npcEntity.getAbsolutePosition().coords;
	                                final int npcAbsX = (int) nAbs.x - (npcEntity.definitions.size - 1 << 8);
	                                final int npcAbsY = (int) nAbs.z - (npcEntity.definitions.size - 1 << 8);
	                                if (npcAbsX >= plrStartX && npcEntity.definitions.size <= player.getSize() - (npcAbsX - plrStartX >> 9) && npcAbsY >= plrStartZ && npcEntity.definitions.size <= player.getSize() - (npcAbsY - plrStartZ >> 9)) {
	                                    MiniMenuBuilder.npcHovered(npcEntity, reference.entity.plane != PlayerUpdate.MY_PLAYER.plane);
	                                    npcEntity.clientCycles = client.cycles;
	                                }
	                            }
	                        }
	                    }
	                    final int numIndices = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
	                    final int[] indices = PlayerUpdate.OUT_PLAYER_INDICES;
	                    for (int index = 0; index < numIndices; ++index) {
	                        final PlayerEntity playerEntity = PlayerUpdate.players[indices[index]];
	                        if (playerEntity != null && playerEntity.clientCycles != client.cycles && player != playerEntity && playerEntity.isHidden) {
	                            final Vector3 playerAbsPos = playerEntity.getAbsolutePosition().coords;
	                            final int playerAbsX = (int) playerAbsPos.x - (playerEntity.getSize() - 1 << 8);
	                            final int playerAbsZ = (int) playerAbsPos.z - (playerEntity.getSize() - 1 << 8);
	                            if (playerAbsX >= plrStartX && playerEntity.getSize() <= player.getSize() - (playerAbsX - plrStartX >> 9) && playerAbsZ >= plrStartZ && playerEntity.getSize() <= player.getSize() - (playerAbsZ - plrStartZ >> 9)) {
	                                playerHovered(playerEntity, reference.entity.plane != PlayerUpdate.MY_PLAYER.plane);
	                                playerEntity.clientCycles = client.cycles;
	                            }
	                        }
	                    }
	                }
	                if (player.clientCycles == client.cycles) {
	                    continue;
	                }
	                playerHovered(player, reference.entity.plane != PlayerUpdate.MY_PLAYER.plane);
	                player.clientCycles = client.cycles;
	            }
	            if (reference.entity instanceof NpcEntity sceneNpc) {
                    if (sceneNpc.definitions != null) {
	                    final Vector3 npcAbsPos = sceneNpc.getAbsolutePosition().coords;
	                    if (((sceneNpc.definitions.size & 0x1) == 0x0 && ((int) npcAbsPos.x & 0x1FF) == 0x0 && ((int) npcAbsPos.z & 0x1FF) == 0x0) || ((sceneNpc.definitions.size & 0x1) == 0x1 && ((int) npcAbsPos.x & 0x1FF) == 0x100 && ((int) npcAbsPos.z & 0x1FF) == 0x100)) {
	                        final int npcAbsStartX = (int) npcAbsPos.x - (sceneNpc.definitions.size - 1 << 8);
	                        final int npcAbsStartZ = (int) npcAbsPos.z - (sceneNpc.definitions.size - 1 << 8);
	                        for (int npcIndex = 0; npcIndex < client.NPC_UPDATE_INDEX; ++npcIndex) {
	                            final ObjectNode npcNode = client.NPC_MAP.get(client.NPC_UPDATE_INDICES[npcIndex]);
	                            if (npcNode != null) {
	                                final NpcEntity npc = (NpcEntity) npcNode.value;
	                                if (npc.clientCycles != client.cycles && sceneNpc != npc && npc.isHidden) {
	                                    final Vector3 vector337 = npc.getAbsolutePosition().coords;
	                                    final int i_12 = (int) vector337.x - (npc.definitions.size - 1 << 8);
	                                    final int i_13 = (int) vector337.z - (npc.definitions.size - 1 << 8);
	                                    if (i_12 >= npcAbsStartX && npc.definitions.size <= sceneNpc.definitions.size - (i_12 - npcAbsStartX >> 9) && i_13 >= npcAbsStartZ && npc.definitions.size <= sceneNpc.definitions.size - (i_13 - npcAbsStartZ >> 9)) {
	                                        MiniMenuBuilder.npcHovered(npc, reference.entity.plane != PlayerUpdate.MY_PLAYER.plane);
	                                        npc.clientCycles = client.cycles;
	                                    }
	                                }
	                            }
	                        }
	                        final int numIndices = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
	                        final int[] playerIndices = PlayerUpdate.OUT_PLAYER_INDICES;
	                        for (int index = 0; index < numIndices; ++index) {
	                            final PlayerEntity player = PlayerUpdate.players[playerIndices[index]];
	                            if (player != null && player.clientCycles != client.cycles && player.isHidden) {
	                                final Vector3 abs = player.getAbsolutePosition().coords;
	                                final int posX2 = (int)abs.x - (player.getSize() - 1 << 8);
	                                final int posZ2 = (int)abs.z - (player.getSize() - 1 << 8);
	                                if (posX2 >= npcAbsStartX && player.getSize() <= sceneNpc.definitions.size - (posX2 - npcAbsStartX >> 9) && posZ2 >= npcAbsStartZ && player.getSize() <= sceneNpc.definitions.size - (posZ2 - npcAbsStartZ >> 9)) {
	                                    playerHovered(player, reference.entity.plane != PlayerUpdate.MY_PLAYER.plane);
	                                    player.clientCycles = client.cycles;
	                                }
	                            }
	                        }
	                    }
	                    if (sceneNpc.clientCycles == client.cycles) {
	                        continue;
	                    }
	                    MiniMenuBuilder.npcHovered(sceneNpc, reference.entity.plane != PlayerUpdate.MY_PLAYER.plane);
	                    sceneNpc.clientCycles = client.cycles;
	                }
	            }
	            if (reference.entity instanceof GroundItem) {
	                final int posX = startX + regionBase.x;
	                final int posY = startZ + regionBase.y;
	                final GroundItemWrapper groundItems = client.GROUND_ITEMS.get((long) reference.entity.plane << 28 | (long) posY << 14 | posX);
	                if (groundItems != null) {
	                    int actionIndex2 = 0;
	                    for (GroundItemNode groundItem = (GroundItemNode) groundItems.groundItems.first(); groundItem != null; groundItem = (GroundItemNode) groundItems.groundItems.getNext(), ++actionIndex2) {
	                        final ItemType objtype = TypeLists.OBJ.list(groundItem.id);
	                        int color;
	                        if (objtype.hasTooltipColor) {
	                            color = objtype.tooltipColor;
	                        }
	                        else if (objtype.membersOnly) {
	                            color = TypeLists.INTERACTION_DEFAULTS.tooltipColorP2p;
	                        }
	                        else {
	                            color = TypeLists.INTERACTION_DEFAULTS.tooltipColorF2p;
	                        }
	                        if (client.interfaceSelectionActive && reference.entity.plane == PlayerUpdate.MY_PLAYER.plane) {
	                            final ParamType param = (MiniMenu.USE_INTERFACE_ID != -1) ? TypeLists.PARAMS.list(MiniMenu.USE_INTERFACE_ID) : null;
	                            if ((MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x1) != 0x0 && (param == null || objtype.getCs2Integer(MiniMenu.USE_INTERFACE_ID, param.defaultInt) != param.defaultInt)) {
	                                MiniMenu.addEntry(client.useOnType, client.useOnTarget + " " + "->" + " " + TextUtils.setTextColor(color) + objtype.name, MiniMenu.useOnCursor, 17, -1, groundItem.id, startX, startZ, true, false, actionIndex2, false);
	                            }
	                        }
	                        if (reference.entity.plane == PlayerUpdate.MY_PLAYER.plane) {
	                            final String[] groundActions = objtype.groundActions;
	                            for (int groundActionIndex = groundActions.length - 1; groundActionIndex >= 0; --groundActionIndex) {
	                                if (groundActions[groundActionIndex] != null) {
	                                    short clickType = 0;
	                                    int cursor = CursorManager.defaultAttackCursor;
	                                    if (groundActionIndex == 0) {
	                                        clickType = 18;
	                                    }
	                                    if (groundActionIndex == 1) {
	                                        clickType = 19;
	                                    }
	                                    if (groundActionIndex == 2) {
	                                        clickType = 20;
	                                    }
	                                    if (groundActionIndex == 3) {
	                                        clickType = 21;
	                                    }
	                                    if (groundActionIndex == 4) {
	                                        clickType = 22;
	                                    }
	                                    if (groundActionIndex == 5) {
	                                        clickType = 1004;
	                                    }
	                                    if (groundActionIndex == objtype.primaryCursorActionIndex) {
	                                        cursor = objtype.primaryCursor;
	                                    }
	                                    if (groundActionIndex == objtype.secondaryCursorActionIndex) {
	                                        cursor = objtype.secondaryCursor;
	                                    }
	                                    MiniMenu.addEntry(groundActions[groundActionIndex], TextUtils.setTextColor(color) + objtype.name, cursor, clickType, -1, groundItem.id, startX, startZ, true, false, actionIndex2, false);
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	            if (!(reference.entity instanceof GameObject sceneobject60)) {
	                continue;
	            }
                ObjectType loctype = Resource.mapRegion.getLoctypeList().list(sceneobject60.getId());
	            if (loctype.alternativeIds != null) {
	                loctype = loctype.getMultiLoc(VarProviders.PLAYER_VAR_PROVIDER);
	            }
	            if (loctype == null) {
	                continue;
	            }
	            if (client.interfaceSelectionActive && reference.entity.plane == PlayerUpdate.MY_PLAYER.plane) {
	                final ParamType attributedefault62 = (MiniMenu.USE_INTERFACE_ID != -1) ? TypeLists.PARAMS.list(MiniMenu.USE_INTERFACE_ID) : null;
	                if ((MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x4) != 0x0 && (attributedefault62 == null || loctype.getParamsInt(MiniMenu.USE_INTERFACE_ID, attributedefault62.defaultInt) != attributedefault62.defaultInt)) {
	                    MiniMenu.addEntry(client.useOnType, client.useOnTarget + " " + "->" + " " + TextUtils.setTextColor(65535) + loctype.name, MiniMenu.useOnCursor, 2, -1, MapLoader.calculateLocUid(sceneobject60, startX, startZ), startX, startZ, true, false, sceneobject60.hashCode(), false);
	                }
	            }
	            if (reference.entity.plane != PlayerUpdate.MY_PLAYER.plane) {
	                continue;
	            }
	            final String[] actions = loctype.actions;
	            if (actions == null) {
	                continue;
	            }
	            for (int actionIndex = actions.length - 1; actionIndex >= 0; actionIndex--) {
	                if (actions[actionIndex] != null) {
	                    short clickType = 0;
	                    int cursor = CursorManager.defaultAttackCursor;
	                    if (actionIndex == 0) {
	                        clickType = 3;
	                    }
	                    if (actionIndex == 1) {
	                        clickType = 4;
	                    }
	                    if (actionIndex == 2) {
	                        clickType = 5;
	                    }
	                    if (actionIndex == 3) {
	                        clickType = 6;
	                    }
	                    if (actionIndex == 4) {
	                        clickType = 1001;
	                    }
	                    if (actionIndex == 5) {
	                        clickType = 1002;
	                    }
	                    if (actionIndex == loctype.primaryCursorActionIndex) {
	                        cursor = loctype.primaryCursor;
	                    }
	                    if (actionIndex == loctype.secondaryCursorActionIndex) {
	                        cursor = loctype.secondaryCursor;
	                    }
	                    MiniMenu.addEntry(actions[actionIndex], TextUtils.setTextColor(65535) + loctype.name, cursor, clickType, -1, MapLoader.calculateLocUid(sceneobject60, startX, startZ), startX, startZ, true, false, sceneobject60.hashCode(), false);
	                }
	            }
	        }
	    }
	}

	public static void interfaceComponentHovered(Component component) {
	    if (client.interfaceSelectionActive) {
	        ParamType param = MiniMenu.USE_INTERFACE_ID != -1 ? TypeLists.PARAMS.list(MiniMenu.USE_INTERFACE_ID) : null;
	        if (client.listComponentSettings(component).isTargetableByUse() && (MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x20) != 0 && (param == null || component.method1997(MiniMenu.USE_INTERFACE_ID, param.defaultInt) != param.defaultInt)) {
	            MiniMenu.addEntry(client.useOnType, client.useOnTarget + " " + "->" + " " + component.opBase, MiniMenu.useOnCursor, 58, component.containerItemId, 0L, component.slotId, component.idHash, true, false, component.slotId | component.idHash, false);
	        }
	    }
	    for (int option = 9; option >= 5; --option) {
	        String action = Component.componentMenuOption(component, option);
	        if (action != null) {
	            MiniMenu.addEntry(action, component.opBase, Component.getCursor(component, option), 1007, component.containerItemId, option + 1, component.slotId, component.idHash, true, false, component.slotId | component.idHash, false);
	        }
	    }
	    String action = Interface.getUseOptionString(component);
	    if (action != null) {
	        MiniMenu.addEntry(action, component.opBase, component.clickType, 25, component.containerItemId, 0L, component.slotId, component.idHash, true, false, component.slotId | component.idHash, false);
	    }
	    for (int option = 4; option >= 0; --option) {
	        String menuAction = Component.componentMenuOption(component, option);
	        if (menuAction != null) {
	            MiniMenu.addEntry(menuAction, component.opBase, Component.getCursor(component, option), 57, component.containerItemId, option + 1, component.slotId, component.idHash, true, false, component.slotId | component.idHash, false);
	        }
	    }
	    if (client.listComponentSettings(component).continueOptionEnabled()) {
	        if (component.resumeText != null) {
	            MiniMenu.addEntry(component.resumeText, "", -1, 30, component.containerItemId, 0L, component.slotId, component.idHash, true, false, component.slotId | component.idHash, false);
	        } else {
	            MiniMenu.addEntry(LocalizedText.CONTINUE.translate(client.CURRENT_LANGUAGE), "", -1, 30, component.containerItemId, 0L, component.slotId, component.idHash, true, false, component.slotId | component.idHash, false);
	        }
	    }
	}

	public static void npcHovered(NpcEntity npc, boolean samePlaneAsPlayer) {
	    if (MiniMenu.entryCount < 412) {
	        NPCType npcType = npc.definitions;
	        String name = npc.modifiedName;
	        if (npcType.alternativeIds != null) {
	            npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
	            if (npcType == null) {
	                return;
	            }
	            name = npcType.name;
	        }
	        if (npcType.visible) {
	            if (npc.modifiedCbLvl != 0) {
	                String level = client.CURRENT_GAME == Game.STELLAR_DAWN ? LocalizedText.RATING_.translate(client.CURRENT_LANGUAGE) : LocalizedText.MINIMENU_LEVEL.translate(client.CURRENT_LANGUAGE);
	                name = name + TextUtils.calculateCombatBasedColor(npc.modifiedCbLvl, PlayerUpdate.MY_PLAYER.combatLevel) + " (" + level + npc.modifiedCbLvl + ")";
	            }
	            if (client.interfaceSelectionActive && !samePlaneAsPlayer) {
	                ParamType paramtype = MiniMenu.USE_INTERFACE_ID != -1 ? TypeLists.PARAMS.list(MiniMenu.USE_INTERFACE_ID) : null;
	                if ((MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x2) != 0 && (paramtype == null || npcType.getCs2Value(MiniMenu.USE_INTERFACE_ID, paramtype.defaultInt) != paramtype.defaultInt)) {
	                    MiniMenu.addEntry(client.useOnType, client.useOnTarget + " " + "->" + " " + TextUtils.setTextColor(16776960) + name, MiniMenu.useOnCursor, 8, -1, npc.playerIndex, 0, 0, true, false, npc.playerIndex, false);
	                }
	            }
	            if (!samePlaneAsPlayer) {
	                String[] npcOptions = npcType.actions;
	                if (client.prefixMinimenuOps) {
	                    npcOptions = MiniMenu.prefixWithIndex(npcOptions);
	                }
	                if (npcOptions != null) {
	                    for (int actionIndex = npcOptions.length - 1; actionIndex >= 0; --actionIndex) {
	                        if (npcOptions[actionIndex] != null && (npcType.gameType == 0 || !npcOptions[actionIndex].equalsIgnoreCase(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE)) && !npcOptions[actionIndex].equalsIgnoreCase(LocalizedText.EXAMINE.translate(client.CURRENT_LANGUAGE)))) {
	                            short clickType = 0;
	                            int cursor = CursorManager.defaultAttackCursor;
	                            if (actionIndex == 0) {
	                                clickType = 9;
	                            }
	                            if (actionIndex == 1) {
	                                clickType = 10;
	                            }
	                            if (actionIndex == 2) {
	                                clickType = 11;
	                            }
	                            if (actionIndex == 3) {
	                                clickType = 12;
	                            }
	                            if (actionIndex == 4) {
	                                clickType = 13;
	                            }
	                            if (actionIndex == 5) {
	                                clickType = 1003;
	                            }
	                            if (actionIndex == npcType.primaryCursorActionIndex) {
	                                cursor = npcType.primaryCursor;
	                            }
	                            if (actionIndex == npcType.secondaryCursorActionIndex) {
	                                cursor = npcType.secondaryCursor;
	                            }
	                            MiniMenu.addEntry(npcOptions[actionIndex], TextUtils.setTextColor(16776960) + name, npcOptions[actionIndex].equalsIgnoreCase(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE)) ? npcType.attackCursor : cursor, clickType, -1, npc.playerIndex, 0, 0, true, false, npc.playerIndex, false);
	                        }
	                    }
	                    if (npcType.gameType == 1) {
	                        for (int actionIndex = 0; actionIndex < npcOptions.length; actionIndex++) {
	                            if (npcOptions[actionIndex] != null && (npcOptions[actionIndex].equalsIgnoreCase(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE)) || npcOptions[actionIndex].equalsIgnoreCase(LocalizedText.EXAMINE.translate(client.CURRENT_LANGUAGE)))) {
	                                short attackClickPriority = 0;
	                                if (npc.modifiedCbLvl > PlayerUpdate.MY_PLAYER.combatLevel) {
	                                    attackClickPriority = 2000;
	                                }
	                                short clickType = 0;
	                                int cursor = CursorManager.defaultAttackCursor;
	                                if (actionIndex == 0) {
	                                    clickType = 9;
	                                }
	                                if (actionIndex == 1) {
	                                    clickType = 10;
	                                }
	                                if (actionIndex == 2) {
	                                    clickType = 11;
	                                }
	                                if (actionIndex == 3) {
	                                    clickType = 12;
	                                }
	                                if (actionIndex == 4) {
	                                    clickType = 13;
	                                }
	                                if (actionIndex == 5) {
	                                    clickType = 1003;
	                                }
	                                if (clickType != 0) {
	                                    clickType += attackClickPriority;
	                                }
	                                if (actionIndex == npcType.primaryCursorActionIndex) {
	                                    cursor = npcType.primaryCursor;
	                                }
	                                if (actionIndex == npcType.secondaryCursorActionIndex) {
	                                    cursor = npcType.secondaryCursor;
	                                }
	                                MiniMenu.addEntry(npcOptions[actionIndex], TextUtils.setTextColor(16776960) + name, npcOptions[actionIndex].equalsIgnoreCase(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE)) ? npcType.attackCursor : cursor, clickType, -1, npc.playerIndex, 0, 0, true, false, npc.playerIndex, false);
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	}

	public static int USE_OPTIONS_FLAGS;
}
