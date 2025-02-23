package com.jagex.game.runetek5.network.packet;

import com.jagex.game.runetek5.botdetection.ReflectionCheckManager;
import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.console.CommandExecutor;
import com.jagex.game.runetek5.client.input.console.Console;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.CheckEmailRequest;
import com.jagex.game.runetek5.client.login.CreateAccountRequest;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.client.login.LoginStateUpdate;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuBuilder;
import com.jagex.game.runetek5.client.periodic.PulseEventQueue;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Identifiable;
import com.jagex.game.runetek5.config.rsinterface.*;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer;
import com.jagex.game.runetek5.utils.*;
import com.jagex.game.runetek5.vardomain.VarProviders;

import java.io.IOException;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.QueueIterator;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.cache.node.impl.ProjectileSpawnRequest;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.object.ObjectTypeCustomisation;
import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.quickchat.QuickChatMessage;
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.config.sequence.PositionedGfx;
import com.jagex.game.runetek5.config.sequence.Projectile;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;
import com.jagex.game.runetek5.encoding.HuffmanList;
import com.jagex.game.runetek5.entity.PathingEntitySpotAnimation;
import com.jagex.game.runetek5.entity.grounditem.GroundItemNode;
import com.jagex.game.runetek5.entity.grounditem.GroundItemUpdating;
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCUpdate;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation;
import com.jagex.game.runetek5.graphics.hint.HintArrow;
import com.jagex.game.runetek5.graphics.hint.HintTrail;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.interaction.Friend;
import com.jagex.game.runetek5.interaction.FriendList;
import com.jagex.game.runetek5.interaction.Ignore;
import com.jagex.game.runetek5.interaction.IgnoreList;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.interaction.chat.ChatFilter;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannelDelta;
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings;
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettingsDelta;
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat;
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChatPlayer;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.Connection;
import com.jagex.game.runetek5.network.HostNameIdentifier;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.network.WorldConnection;
import com.jagex.game.runetek5.network.world.WorldMeta;
import com.jagex.game.runetek5.network.world.WorldDescriptor;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.RegionLoadRequest;
import com.jagex.game.runetek5.scene.GroundItemFetcher;
import com.jagex.game.runetek5.scene.LocAction;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.RegionLoadingType;
import com.jagex.game.runetek5.scene.SceneGraph;
import com.jagex.game.runetek5.scene.tile.TileMessage;
import com.jagex.game.runetek5.sound.VorbisSoundManager;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.tools.Browser;
import com.jagex.game.runetek5.tools.DebugDisplay;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.trigger.ClientTriggerType;
import com.jagex.game.runetek5.trigger.rsinterface.IFSubActiveGroundItem;
import com.jagex.game.runetek5.trigger.rsinterface.IFSubActiveNpc;
import com.jagex.game.runetek5.trigger.rsinterface.IFSubActiveObject;
import com.jagex.game.runetek5.trigger.rsinterface.IFSubActivePlayer;
import com.jagex.game.runetek5.trigger.rsinterface.IFSubObjectPosition;

public class PacketDecoder {
	
	public static boolean decodePacketFromStream(BufferedConnectionContext connectionContext) {
	    try {
	        boolean success = decode(connectionContext);
	        return success;
	    } catch (IOException ioexception) {
	    	ioexception.printStackTrace();
	        if (client.gameState == GameState.UNK_7) {
	            connectionContext.reset();
	            return false;
	        } else {
	            WorldConnection.dropClient();
	            return true;
	        }
	    } catch (Exception exception) {
	    	exception.printStackTrace();
	        CoordGrid basepos = Resource.mapRegion.getBaseCoordinates();
	        String debug = (connectionContext.currentPacket != null ? connectionContext.currentPacket.opcode : -1) + "," + (connectionContext.secondLastPacket != null ? connectionContext.secondLastPacket.opcode : -1) + "," + (connectionContext.thirdLastPacket != null ? connectionContext.thirdLastPacket.opcode : -1) + " " + connectionContext.currentPacketSize + "," + (PlayerUpdate.MY_PLAYER.movementBaseX[0] + basepos.x) + "," + (PlayerUpdate.MY_PLAYER.movementBaseY[0] + basepos.y) + " ";
	        for (int index = 0; index < connectionContext.currentPacketSize && index < 50; index++) {
	            debug = debug + connectionContext.receivedBuffer.buffer[index] + ",";
	        }
	        PacketEncoder.sendErrorToServer(debug, exception);
	        client.logout(false);
	        return true;
	    }
	}

    public static boolean decode(BufferedConnectionContext context) throws IOException {
        Connection connection = context.getConnection();
        Packet.Bit buffer = context.receivedBuffer;
        if (connection == null)
            return false;
        if (context.currentPacket == null) {
            if (context.readPadding) {
                if (!connection.available(1)) {
                    return false;
                }
                connection.read(context.receivedBuffer.buffer, 0, 1);
                context.read++;
                context.idleReadPulses = 0;
                context.readPadding = false;
            }
            buffer.currentPosition = 0;
            if (buffer.nextIsSmart()) {
                if (!connection.available(1)) {
                    return false;
                }
                connection.read(context.receivedBuffer.buffer, 1, 1);
                context.read++;
                context.idleReadPulses = 0;
            }
            context.readPadding = true;
            int opcode = buffer.readEncryptedSmart();
            ServerProt packet = ServerProt.forId(opcode);
            if (packet == null) {
                throw new IOException(opcode + " " + buffer.currentPosition);
            }
            context.currentPacket = packet;
            context.currentPacketSize = context.currentPacket.size;
        }
        if (context.currentPacketSize == -1) {
            if (!connection.available(1)) {
                return false;
            }
            connection.read(buffer.buffer, 0, 1);
            context.currentPacketSize = buffer.buffer[0] & 0xff;
            context.read++;
            context.idleReadPulses = 0;
        }
        if (context.currentPacketSize == -2) {
            if (!connection.available(2)) {
                return false;
            }
            connection.read(buffer.buffer, 0, 2);
            buffer.currentPosition = 0;
            context.currentPacketSize = buffer.readUnsignedShort();
            context.read += 2;
            context.idleReadPulses = 0;
        }
        if (context.currentPacketSize > 0) {
            if (!connection.available(context.currentPacketSize)) {
                return false;
            }
            buffer.currentPosition = 0;
            connection.read(buffer.buffer, 0, context.currentPacketSize);
            context.read += context.currentPacketSize;
            context.idleReadPulses = 0;
        }
        context.thirdLastPacket = context.secondLastPacket;
        context.secondLastPacket = context.lastPacket;
        context.lastPacket = context.currentPacket;
        
        if (context.currentPacket == ServerProt.IF_SETMODEL) {
            int componentHash = buffer.readInt();
            int primaryModelId = buffer.readIntV2();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfcontentUpdate(componentHash, 1, primaryModelId, -1);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.PROCESS_DEV_CONSOLE_COMMAND) {
            String commandString = buffer.readString();
            CommandExecutor.executeCommand(commandString, false, false);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_INV_FULL) {
            int itemContainer = buffer.readUnsignedShort();
            int flags = buffer.readUnsignedByte();
            boolean isNegativeKey = (flags & 0x1) == 1;
            ItemContainer.resetContainer(itemContainer, isNegativeKey);
            int size = buffer.readUnsignedShort();
            for (int slot = 0; slot < size; slot++) {
                int amount = buffer.readUnsigned128Byte();
                if (amount == 255) {
                    amount = buffer.readIntV2();
                }
                int itemId = buffer.readUnsignedShortLE128();
                ItemContainer.setItemInContainer(itemContainer, slot, itemId - 1, amount, isNegativeKey);
            }
            client.RECENTLY_USED_ITEM_CONTAINERS[++client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - 1 & 0x1f] = itemContainer;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SET_DRAW_ORDER) {
            int key = buffer.readUnsignedByteC();
            client.requestVarVerifyid();
            client.gameDrawOrder = key;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SET_CURSOR) {
            InterfaceRenderer.translatedUptext = context.currentPacketSize > 2 ? buffer.readString() : LocalizedText.WALK_HERE.translate(client.CURRENT_LANGUAGE);
            CursorManager.activeCursorId = context.currentPacketSize > 0 ? buffer.readUnsignedShort() : -1;
            if (CursorManager.activeCursorId == 65535) {
                CursorManager.activeCursorId = -1;
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CAM_FORCEANGLE) {
            int angleY = buffer.readUnsignedShort128();
            int angleX = buffer.readUnsignedShortLE128();
            client.requestVarVerifyid();
            Camera.setAngle(angleX, angleY, 0);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.LOGOUT_FULL) {
            client.logout(false);
            context.currentPacket = null;
            return false;
        } else if (context.currentPacket == ServerProt.MAP_PROJANIM_HALFSQ) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.MAP_PROJANIM_HALFSQ);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CLANCHANNEL_FULL) {
            client.anInt7395 = client.anInt7347;
            boolean nonGuest = buffer.readUnsignedByte() == 1;
            if (context.currentPacketSize == 1) {
                if (nonGuest) {
                    client.CLAN_CHANNEL = null;
                } else {
                    client.LISTENED_CLAN_CHANNEL = null;
                }
            } else {
                if (nonGuest) {
                    client.CLAN_CHANNEL = new ClanChannel(buffer);
                } else {
                    client.LISTENED_CLAN_CHANNEL = new ClanChannel(buffer);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_GE_SLOT) {
            int key = buffer.readUnsignedByte();
            if (buffer.readUnsignedByte() == 0) {
                client.GRAND_EXCHANGE_SLOTS[key] = new GrandExchangeSlot();
            } else {
                buffer.currentPosition--;
                client.GRAND_EXCHANGE_SLOTS[key] = new GrandExchangeSlot(buffer);
            }
            client.anInt7320 = client.anInt7347;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETANGLE) {
            int componentHash = buffer.readInt();
            int pitch = buffer.readShortLE();
            int roll = buffer.readShortLE();
            int scale = buffer.readUnsignedShortLE128();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfangleUpdate(componentHash, pitch, roll, scale);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETTEXT) {
            String text = buffer.readString();
            int componentHash = buffer.readIntV1();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIftextUpdate(componentHash, text);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_OPENTOP) {
            int key3 = buffer.readIntV1();
            int key4 = buffer.readIntV1();
            int interfaceId = buffer.readUnsignedShort128();
            int type = buffer.readUnsigned128Byte();
            int key2 = buffer.readInt();
            int key1 = buffer.readInt();
            client.requestVarVerifyid();
            if (type == 2) {
                client.setLoading();
            }
            int[] xteas = {key1, key2, key3, key4};
            client.BASE_WINDOW_ID = interfaceId;
            Interface.resetComponentSequences(interfaceId, xteas);
            WindowMode.interfaceResize(false);
            Component.runScriptsIfLoaded(client.BASE_WINDOW_ID, xteas);
            for (int index = 0; index < 107; index++) {
                client.IS_COMPONENT_ON_TOP[index] = true;
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERMODEL_OTHER) {
            int primaryModelId = buffer.readUnsignedShort128();
            int componentHash = buffer.readIntV1();
            int secondaryModelId = buffer.readIntV1();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfcontentUpdate(componentHash, 5, primaryModelId, secondaryModelId);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CAM_SMOOTHRESET) {
            client.requestVarVerifyid();
            Camera.smoothReset();
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CLIENT_SETVARCSTR_SMALL) {
            String string_63 = buffer.readString();
            int flags = buffer.readUnsignedShortLE128();
            client.requestVarVerifyid();
            PulseEventQueue.priorityVarcstringUpdate(flags, string_63);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETSCROLLPOS) {
            int key = buffer.readUnsignedShort();
            int flags = buffer.readInt();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfscrollposUpdate(flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_PRIVATE_ECHO) {
            boolean noCrown = buffer.readUnsignedByte() == 1;
            String crownedName = buffer.readString();
            String name = crownedName;
            if (noCrown) {
                name = buffer.readString();
            }
            long long_28 = buffer.readUnsignedShort();
            long long_30 = buffer.read24BitUnsignedInteger();
            int rights = buffer.readUnsignedByte();
            int qcMsgId = buffer.readUnsignedShort();
            long long_13 = long_30 + (long_28 << 32);
            boolean ignore = false;
            int i_81 = 0;
            while (true) {
                if (i_81 >= 100) {
                    if (rights <= 1 && IgnoreList.contains(name)) {
                        ignore = true;
                    }
                    break;
                }
                if (client.aLongArray7424[i_81] == long_13) {
                    ignore = true;
                    break;
                }
                ++i_81;
            }
            if (!ignore) {
                client.aLongArray7424[client.anInt7389] = long_13;
                client.anInt7389 = (client.anInt7389 + 1) % 100;
                String qcMessage = TypeLists.QUICK_CHAT_MESSAGE.listMessage(qcMsgId).fillDynamicValues(buffer);
                if (rights == 2) {
                    ChatBox.writeMessage(18, 0, TextUtils.getCrown(1) + crownedName, TextUtils.getCrown(1) + name, crownedName, qcMessage, null, qcMsgId);
                } else if (rights == 1) {
                    ChatBox.writeMessage(18, 0, TextUtils.getCrown(0) + crownedName, TextUtils.getCrown(0) + name, crownedName, qcMessage, null, qcMsgId);
                } else {
                    ChatBox.writeMessage(18, 0, crownedName, name, crownedName, qcMessage, null, qcMsgId);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETCOLOR) {
            int key = buffer.readIntLE();
            int flags = buffer.readUnsignedShort128();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfcolorUpdate(key, flags);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VORBIS_SOUND) {
            int soundId = buffer.readUnsignedShort();
            if (soundId == 65535) {
                soundId = -1;
            }
            int repeat = buffer.readUnsignedByte();
            int delay = buffer.readUnsignedShort();
            int volume = buffer.readUnsignedByte();
            int sampleRate = buffer.readUnsignedShort();
            AreaSoundPlayer.playSoundVorbis(soundId, repeat, delay, volume, false, sampleRate);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.RUN_ENERGY) {
            client.RUN_ENERGY = buffer.readUnsignedByte();
            client.anInt7397 = client.anInt7347;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERHEAD_OTHER) {
            int key = buffer.readInt();
            int flags = buffer.readUnsignedShortLE128();
            int i_6 = buffer.readIntV1();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfcontentUpdate(i_6, 3, flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SPOT_ANIM_SPECIFIC) {
            int idk = buffer.readUnsignedByteC();
            int reqGfxId = buffer.readUnsignedShort128();
            if (reqGfxId == 65535) {
                reqGfxId = -1;
            }
            int settings = buffer.readUnsignedByteC();
            int speed = buffer.readUnsignedShort128();
            int targetHash = buffer.readIntLE();
            int height = buffer.readShortLE();
            int rotation = settings & 0x7;
            int setting1 = settings >> 3 & 0xf;
            if (setting1 == 15) {
                setting1 = -1;
            }
            boolean setting2 = (settings >> 7 & 0x1) == 1;
            if (targetHash >> 30 != 0) {
                CoordGrid base = Resource.mapRegion.getBaseCoordinates();
                int fromPlane = targetHash >> 28 & 0x3;
                int x = (targetHash >> 14 & 0x3fff) - base.x;
                int y = (targetHash & 0x3fff) - base.y;
                if (x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                    if (reqGfxId == -1) {
                        PositionedGfx positionedGfx = client.ACTIVE_GFX.get((long) x << 16 | y);
                        if (positionedGfx != null) {
                            positionedGfx.objectGfx.resetCycle();
                            positionedGfx.unlink();
                        }
                    } else {
                        int tileX = x * 512 + 256;
                        int tileY = y * 512 + 256;
                        int toPlane = fromPlane;
                        if (fromPlane < 3 && Resource.mapRegion.getSettingsBits().areRoofsHidden(x, y)) {
                            toPlane = fromPlane + 1;
                        }
                        SpotAnimation spotAnimation = new SpotAnimation(Resource.mapRegion.getScene(), reqGfxId, speed, fromPlane, toPlane, tileX, MapRegion.tileheightForAbsolutePosition(tileX, tileY, fromPlane) - height, tileY, x, x, y, y, rotation, setting2);
                        client.ACTIVE_GFX.put(new PositionedGfx(spotAnimation), (long) x << 16 | y);
                    }
                }
            } else if (targetHash >> 29 != 0) {
                int npcId = targetHash & 0xffff;
                ObjectNode npcObject = client.NPC_MAP.get(npcId);
                if (npcObject != null) {
                    NpcEntity npc = (NpcEntity) npcObject.value;
                    PathingEntitySpotAnimation npcGfx = npc.spotAnimations[idk];
                    boolean overwriteCurrent = true;
                    int curGfxId = npcGfx.spotAnimationId;
                    if (reqGfxId != -1 && curGfxId != -1) {
                        if (reqGfxId == curGfxId) {
                            SpotAnimType reqGfx = TypeLists.SPOT_ANIM.list(reqGfxId);
                            if (reqGfx.replay && reqGfx.sequenceId != -1) {
                                SeqType reqGfxSeq = TypeLists.SEQ.list(reqGfx.sequenceId);
                                int reqLoopType = reqGfxSeq.loopType;
                                if (reqLoopType != 0 && reqLoopType != 2) {
                                    if (reqLoopType == 1) {
                                    }
                                } else {
                                    overwriteCurrent = false;
                                }
                            }
                        } else {
                            SpotAnimType reqGfx = TypeLists.SPOT_ANIM.list(reqGfxId);
                            SpotAnimType curGfx = TypeLists.SPOT_ANIM.list(curGfxId);
                            if (reqGfx.sequenceId != -1 && curGfx.sequenceId != -1) {
                                SeqType reqSeq = TypeLists.SEQ.list(reqGfx.sequenceId);
                                SeqType curSeq = TypeLists.SEQ.list(curGfx.sequenceId);
                                if (reqSeq.priority < curSeq.priority) {
                                    overwriteCurrent = false;
                                }
                            }
                        }
                    }
                    if (overwriteCurrent) {
                        npcGfx.spotAnimationId = reqGfxId;
                        npcGfx.height = height;
                        npcGfx.adjustmentType = setting1;
                        if (reqGfxId != -1) {
                            SpotAnimType reqGfx = TypeLists.SPOT_ANIM.list(reqGfxId);
                            int replayMode = reqGfx.replay ? 0 : 2;
                            if (setting2) {
                                replayMode = 1;
                            }
                            npcGfx.sequence.animateSequence(reqGfx.sequenceId, speed, replayMode, false);
                        } else {
                            npcGfx.sequence.update(-1);
                        }
                    }
                }
            } else if (targetHash >> 28 != 0) {
                int i_35 = targetHash & 0xffff;
                PlayerEntity player117;
                if (i_35 == PlayerUpdate.myPlayerIndex) {
                    player117 = PlayerUpdate.MY_PLAYER;
                } else {
                    player117 = PlayerUpdate.players[i_35];
                }
                if (player117 != null) {
                    PathingEntitySpotAnimation class161121 = player117.spotAnimations[idk];
                    boolean bool16 = true;
                    int i_34 = class161121.spotAnimationId;
                    if (reqGfxId != -1 && i_34 != -1) {
                        if (i_34 == reqGfxId) {
                            SpotAnimType spotanimdefinitions125 = TypeLists.SPOT_ANIM.list(reqGfxId);
                            if (spotanimdefinitions125.replay && spotanimdefinitions125.sequenceId != -1) {
                                SeqType animationdefinitions124 = TypeLists.SEQ.list(spotanimdefinitions125.sequenceId);
                                int i_40 = animationdefinitions124.loopType;
                                if (i_40 != 0 && i_40 != 2) {
                                    if (i_40 == 1) {
                                    }
                                } else {
                                    bool16 = false;
                                }
                            }
                        } else {
                            SpotAnimType spotanimdefinitions125 = TypeLists.SPOT_ANIM.list(reqGfxId);
                            SpotAnimType spotanimdefinitions38 = TypeLists.SPOT_ANIM.list(i_34);
                            if (spotanimdefinitions125.sequenceId != -1 && spotanimdefinitions38.sequenceId != -1) {
                                SeqType animationdefinitions86 = TypeLists.SEQ.list(spotanimdefinitions125.sequenceId);
                                SeqType animationdefinitions123 = TypeLists.SEQ.list(spotanimdefinitions38.sequenceId);
                                if (animationdefinitions86.priority < animationdefinitions123.priority) {
                                    bool16 = false;
                                }
                            }
                        }
                    }
                    if (bool16) {
                        class161121.spotAnimationId = reqGfxId;
                        class161121.height = height;
                        class161121.adjustmentType = setting1;
                        class161121.rotation = rotation;
                        if (reqGfxId != -1) {
                            SpotAnimType spotanimdefinitions125 = TypeLists.SPOT_ANIM.list(reqGfxId);
                            int i_83 = spotanimdefinitions125.replay ? 0 : 2;
                            if (setting2) {
                                i_83 = 1;
                            }
                            class161121.sequence.animateSequence(spotanimdefinitions125.sequenceId, speed, i_83, false);
                        } else {
                            class161121.sequence.update(-1);
                        }
                    }
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CAM_LOOKAT) {
            int viewX = buffer.readUnsignedByte();
            int viewY = buffer.readUnsignedShort128() << 2;
            int viewZ = buffer.readUnsignedByte128();
            int i_7 = buffer.readUnsignedByteC();
            int i_8 = buffer.readUnsigned128Byte();
            client.requestVarVerifyid();
            Camera.lookat(viewX, viewZ, viewY, i_8, i_7);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.aClass3754368) {
            int key = buffer.readUnsignedByte();
            int flags = buffer.readUnsignedShort128();
            client.requestVarVerifyid();
            VorbisSoundManager.method1575(flags, key, true);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.GAME_MESSAGE) {
            int type = buffer.readUnsignedSmart();
            int effectFlags = buffer.readInt();
            int flags = buffer.readUnsignedByte();
            String nameSimple = "";
            String nameFormatted = nameSimple;
            if ((flags & 0x1) != 0) {
                nameSimple = buffer.readString();
                if ((flags & 0x2) != 0) {
                    nameFormatted = buffer.readString();
                } else {
                    nameFormatted = nameSimple;
                }
            }
            String message = buffer.readString();
            if (type == 99) {
                Console.writeLine(message);
            } else if (type == 98) {
                Console.setConsoleText(message);
            } else {
                if (!nameFormatted.isEmpty() && IgnoreList.contains(nameFormatted)) {
                    context.currentPacket = null;
                    return true;
                }
                ChatBox.writeMessage(type, effectFlags, nameSimple, nameFormatted, nameSimple, message);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.OBJECT_PREFETCH) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.OBJECT_PREFETCH);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.OPEN_URL) {
            if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
                WindowMode.requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false);
            }
            byte[] bytes = new byte[context.currentPacketSize - 1];
            boolean bool66 = buffer.readUnsignedByte() == 1;
            buffer.readBytes(bytes, context.currentPacketSize - 1);
            Packet rsbytebuffer127 = new Packet(bytes);
            String url = rsbytebuffer127.readString();
            if (bool66) {
                String string_137 = rsbytebuffer127.readString();
                if (string_137.isEmpty()) {
                    string_137 = url;
                }
                if (!client.isApplet || SystemInfo.operatingSystemLowercase.startsWith("mac") || !Browser.openBrowser(url, 1, JSObjectManager.SERVER_PACKET_URL.getCallName())) {
                    Browser.openOpenjs(string_137, true, client.aBool7158);
                }
            } else {
                Browser.openOpenjs(url, true, client.aBool7158);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CAM_SHAKE) {
            int key = buffer.readUnsigned128Byte();
            int flags = buffer.readUnsignedShort();
            int i_6 = buffer.readUnsignedByteC();
            int i_7 = buffer.readUnsignedByteC();
            int i_8 = buffer.readUnsigned128Byte();
            client.requestVarVerifyid();
            client.CAM_SHAKING[i_6] = true;
            client.anIntArray7268[i_6] = i_7;
            client.CUSTOM_LOWEST_YAW[i_6] = key;
            client.anIntArray7462[i_6] = i_8;
            client.anIntArray7435[i_6] = flags;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_PLAYER_GROUP) {
            boolean bool91 = buffer.readUnsignedByte() == 1;
            String string_88 = buffer.readString();
            String str92 = string_88;
            if (bool91) {
                str92 = buffer.readString();
            }
            int i_7 = buffer.readUnsignedByte();
            boolean ignore = false;
            if (i_7 <= 1) {
                if ((!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
                    if (IgnoreList.contains(str92)) {
                        ignore = true;
                    }
                } else {
                    ignore = true;
                }
            }
            if (!ignore) {
                String message = TextUtils.atLtGtTags(HuffmanList.decode(buffer));
                if (i_7 == 2) {
                    ChatBox.writeMessage(24, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + str92, string_88, message, null, -1);
                } else if (i_7 == 1) {
                    ChatBox.writeMessage(24, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + str92, string_88, message, null, -1);
                } else {
                    ChatBox.writeMessage(24, 0, string_88, str92, string_88, message, null, -1);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_PLAYER_GROUP) {
            boolean hasRights = buffer.readUnsignedByte() == 1;
            String username = buffer.readString();
            String crownedUsername = username;
            if (hasRights) {
                crownedUsername = buffer.readString();
            }
            int rights = buffer.readUnsignedByte();
            int msgId = buffer.readUnsignedShort();
            boolean ignore = rights <= 1 && IgnoreList.contains(crownedUsername);
            if (!ignore) {
                String message = TypeLists.QUICK_CHAT_MESSAGE.listMessage(msgId).fillDynamicValues(buffer);
                if (rights == 2) {
                    ChatBox.writeMessage(25, 0, TextUtils.getCrown(1) + username, TextUtils.getCrown(1) + crownedUsername, username, message, null, msgId);
                } else if (rights == 1) {
                    ChatBox.writeMessage(25, 0, TextUtils.getCrown(0) + username, TextUtils.getCrown(0) + crownedUsername, username, message, null, msgId);
                } else {
                    ChatBox.writeMessage(25, 0, username, crownedUsername, username, message, null, msgId);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CHAT_FILTER_SETTINGS_PRIVATECHAT) {
            ChatBox.PRIVATE_FILTER = ChatFilter.forId(buffer.readUnsignedByte());
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.ANIMATE_NPC) {
            int npcIndex = buffer.readUnsignedShortLE128();
            int[] animationIds = new int[MovementType.values().length];
            for (int i_6 = 0; i_6 < MovementType.values().length; i_6++) {
                animationIds[i_6] = buffer.readIntLE();
            }
            int speed = buffer.readUnsignedByte();
            ObjectNode class282Sub47131 = client.NPC_MAP.get(npcIndex);
            if (class282Sub47131 != null) {
                PlayerUpdate.animate((PathingEntity) class282Sub47131.value, animationIds, speed, true);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETPOSITION) {
            int y = buffer.readShort128();
            int component = buffer.readIntV2();
            int x = buffer.readShortLE128();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfpositionUpdate(component, x, y);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.DYNAMIC_MAP_REGION) {
            Packet.Bit bitBuffer = new Packet.Bit(context.currentPacketSize);
            System.arraycopy(context.receivedBuffer.buffer, context.receivedBuffer.currentPosition, bitBuffer.buffer, 0, context.currentPacketSize);
            client.resetCutscenes();
            if (PreferenceManager.preferences.asyncMaploading.getValue() == 1) {
                Resource.mapRegionLoaderTask.requestMapUpdate(new RegionLoadRequest(RegionLoadingType.INSTANCE, bitBuffer));
            } else {
                Resource.mapRegion.initialize(new RegionLoadRequest(RegionLoadingType.INSTANCE, bitBuffer));
            }
            context.currentPacket = null;
            return false;
        } else if (context.currentPacket == ServerProt.DEPRECATED_52_CLIENTPACKET_97) {
            client.aByte7458 = buffer.readByte();
            if (client.aByte7458 == 0 || client.aByte7458 == 1) {
                client.aBool7323 = true;
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETNPCHEAD) {
            int key = buffer.readIntLE();
            int flags = buffer.readIntV1();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfcontentUpdate(key, 2, flags, -1);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_PUBLIC) {
            int playerIndex = buffer.readUnsignedShort();
            PlayerEntity player;
            if (playerIndex == PlayerUpdate.myPlayerIndex) {
                player = PlayerUpdate.MY_PLAYER;
            } else {
                player = PlayerUpdate.players[playerIndex];
            }
            if (player != null) {
                int chatEffects = buffer.readUnsignedShort();
                int icon = buffer.readUnsignedByte();
                boolean is0x8000 = (chatEffects & 0x8000) != 0;
                if (player.displayName != null && player.playerAppearance != null) {
                    boolean bool69 = false;
                    if (icon <= 1) {
                        if (!is0x8000 && (client.USERDETAIL_QUICKCHAT && !client.VERIFIED_EMAIL_ADDRESS || client.IS_QUICKCHAT_ONLY)) {
                            bool69 = true;
                        } else if (IgnoreList.contains(player.displayName)) {
                            bool69 = true;
                        }
                    }
                    if (!bool69) {
                        int qcMessageId = -1;
                        String message;
                        if (is0x8000) {
                            chatEffects &= 0x7fff;
                            QuickChatMessage quickchatMessage = QuickchatMessageTypeList.decodeQuickchatMessage(buffer);
                            qcMessageId = quickchatMessage.id;
                            message = quickchatMessage.message.fillDynamicValues(buffer);
                        } else {
                            message = TextUtils.atLtGtTags(HuffmanList.decode(buffer));
                        }
                        player.sendChat(message.trim(), chatEffects >> 8, chatEffects & 0xff);
                        int messageType;
                        if (icon != 1 && icon != 2) {
                            messageType = is0x8000 ? 17 : 2;
                        } else {
                            messageType = is0x8000 ? 17 : 1;
                        }
                        if (icon == 2) {
                            ChatBox.writeMessage(messageType, 0, TextUtils.getCrown(1) + player.getUsernameWithTitle(), TextUtils.getCrown(1) + player.getDisplayName(), player.username, message, null, qcMessageId);
                        } else if (icon == 1) {
                            ChatBox.writeMessage(messageType, 0, TextUtils.getCrown(0) + player.getUsernameWithTitle(), TextUtils.getCrown(0) + player.getDisplayName(), player.username, message, null, qcMessageId);
                        } else {
                            ChatBox.writeMessage(messageType, 0, player.getUsernameWithTitle(), player.getDisplayName(), player.username, message, null, qcMessageId);
                        }
                    }
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.QUICK_HOP_WORLDS) {
            int port = buffer.readUnsignedShort();
            String host = buffer.readString();
            boolean reconnecting = buffer.readUnsignedByte() == 1;
            WorldConnection.connectionInfo = LoginManager.GAME_CONNECTION_INFO;
            WorldConnection.reconnectingWorld = reconnecting;
            LoginManager.setGameHost(port, host);
            client.setGameState(GameState.UNK_17);
            context.currentPacket = null;
            return false;
        } else if (context.currentPacket == ServerProt.LOGOUT_LOBBY) {
            client.logout(LoginManager.currentlyInLobby);
            context.currentPacket = null;
            return false;
        } else if (context.currentPacket == ServerProt.CLIENT_SETVARC_LARGE) {
            int key = buffer.readShortLE();
            int flags = buffer.readIntV2();
            client.requestVarVerifyid();
            PulseEventQueue.priorityClientvarcUpdate(key, flags);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_OPENSUB_ACTIVE_GROUNDITEM) {
            int interfaceId = buffer.readIntV2();
            int xtea3 = buffer.readIntLE();
            int subInterfaceId = buffer.readShortLE();
            int xtea4 = buffer.readInt();
            int xtea1 = buffer.readIntV1();
            int overlay = buffer.readUnsigned128Byte();
            CoordGrid absolutePosition = new CoordGrid(buffer.readIntV1());
            int xtea2 = buffer.readIntV1();
            int itemId = buffer.readUnsignedShortLE128();
            client.requestVarVerifyid();
            Interface.openSub(interfaceId, new IFSubActiveGroundItem(subInterfaceId, overlay, new GroundItemFetcher(absolutePosition, itemId)),
            		new int[]{xtea1, xtea2, xtea3, xtea4}, false);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.FRIENDS_CHAT_CHANNEL) {
            client.anInt7179 = client.anInt7347;
            if (context.currentPacketSize == 0) {
                FriendsChat.FC_NAME = null;
                FriendsChat.FC_OWNER_NAME = null;
                FriendsChat.FC_PLAYER_COUNT = 0;
                FriendsChat.FC_PLAYERS = null;
                context.currentPacket = null;
                return true;
            } else {
                FriendsChat.FC_OWNER_NAME = buffer.readString();
                boolean isOwner = buffer.readUnsignedByte() == 1;
                if (isOwner) {
                    buffer.readString();
                }
                long encodedName = buffer.readLong();
                FriendsChat.FC_NAME = Utils.getStringFromLong(encodedName);
                FriendsChat.FC_MIN_RANK_CAN_KICK = buffer.readByte();
                int playerCount = buffer.readUnsignedByte();
                if (playerCount != 255) {
                    FriendsChat.FC_PLAYER_COUNT = playerCount;
                    FriendsChatPlayer[] friendsChatPlayers = new FriendsChatPlayer[100];
                    for (int index = 0; index < FriendsChat.FC_PLAYER_COUNT; index++) {
                        friendsChatPlayers[index] = new FriendsChatPlayer();
                        friendsChatPlayers[index].displayName = buffer.readString();
                        boolean changedName = buffer.readUnsignedByte() == 1;
                        if (changedName) {
                            friendsChatPlayers[index].username = buffer.readString();
                        } else {
                            friendsChatPlayers[index].username = friendsChatPlayers[index].displayName;
                        }
                        friendsChatPlayers[index].textName = TextUtils.formatDisplayname(friendsChatPlayers[index].username);
                        friendsChatPlayers[index].worldId = buffer.readUnsignedShort();
                        friendsChatPlayers[index].rank = buffer.readByte();
                        friendsChatPlayers[index].worldName = buffer.readString();
                        if (friendsChatPlayers[index].username.equals(PlayerUpdate.MY_PLAYER.displayName)) {
                            FriendsChat.MY_PLAYER_FC_RANK = friendsChatPlayers[index].rank;
                        }
                    }
                    boolean bool69 = false;
                    int total = FriendsChat.FC_PLAYER_COUNT;
                    while (total > 0) {
                        bool69 = true;
                        --total;
                        for (int index = 0; index < total; index++) {
                            if (friendsChatPlayers[index].textName.compareTo(friendsChatPlayers[index + 1].textName) > 0) {
                                FriendsChatPlayer tmp = friendsChatPlayers[index];
                                friendsChatPlayers[index] = friendsChatPlayers[index + 1];
                                friendsChatPlayers[index + 1] = tmp;
                                bool69 = false;
                            }
                        }
                        if (bool69) {
                            break;
                        }
                    }
                    FriendsChat.FC_PLAYERS = friendsChatPlayers;
                }
                context.currentPacket = null;
                return true;
            }
        } else if (context.currentPacket == ServerProt.CAM_RESET) {
            client.requestVarVerifyid();
            Camera.method12606();
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SET_CLAN_STRING) {
            int key = buffer.readUnsignedShort();
            String string_88 = buffer.readString();
            if (ClanVarTypeList.CLAN_VARS == null) {
                ClanVarTypeList.CLAN_VARS = new Object[TypeLists.CLAN_VAR.size];
            }
            ClanVarTypeList.CLAN_VARS[key] = string_88;
            client.CLAN_VAR_KEYS[++client.CLAN_VAR_COUNTER - 1 & 0x1f] = key;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MUSIC_TRACK) {
            int key = buffer.readUnsignedByte128();
            int flags = buffer.readUnsignedByte128();
            int i_6 = buffer.readUnsignedShort128();
            if (i_6 == 65535) {
                i_6 = -1;
            }
            SongPlayer.playSong(i_6, flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETCLICKMASK) {
            boolean bool91 = buffer.readUnsigned128Byte() == 1;
            int flags = buffer.readIntLE();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfclickmaskUpdate(flags, bool91);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MUSIC_EFFECT) {
            int key = buffer.method13132();
            int flags = buffer.readUnsigned128Byte();
            int i_6 = buffer.readShortLE();
            if (i_6 == 65535) {
                i_6 = -1;
            }
            SoundEffectPlayer.playSoundJingle(i_6, flags);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CHAT_FILTER_SETTINGS) {
            ChatBox.TRADE_FILTER = buffer.readUnsignedByteC();
            ChatBox.PUBLIC_FILTER = buffer.readUnsignedByte128();
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CLEAR_VARPS) {
            VarProviders.PLAYER_VAR_PROVIDER.reset();
            client.pulseEventConfigsPtr += 32;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.TILE_MESSAGE) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.TILE_MESSAGE);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.PLAYER_WEIGHT) {
            client.RUN_WEIGHT = buffer.readShort();
            client.anInt7397 = client.anInt7347;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.aClass3754399) {
            int key = buffer.readIntLE();
            client.requestVarVerifyid();
            if (key == -1) {
                PingRequester.anInt5864 = -1;
                StaticMethods.anInt833 = -1;
            } else {
                CoordGrid coordgrid114 = Resource.mapRegion.getBaseCoordinates();
                int i_6 = key >> 14 & 0x3fff;
                int i_7 = key & 0x3fff;
                i_6 -= coordgrid114.x;
                if (i_6 < 0) {
                    i_6 = 0;
                } else if (i_6 >= Resource.mapRegion.getSceneSizeX()) {
                    i_6 = Resource.mapRegion.getSceneSizeX();
                }
                i_7 -= coordgrid114.y;
                if (i_7 < 0) {
                    i_7 = 0;
                } else if (i_7 >= Resource.mapRegion.getSceneSizeY()) {
                    i_7 = Resource.mapRegion.getSceneSizeY();
                }
                PingRequester.anInt5864 = (i_6 << 9) + 256;
                StaticMethods.anInt833 = (i_7 << 9) + 256;
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.RESET_ALL_ANIMATIONS) {
            for (int key = 0; key < PlayerUpdate.players.length; key++) {
                if (PlayerUpdate.players[key] != null) {
                    PlayerUpdate.players[key].currentAnimations = null;
                    PlayerUpdate.players[key].currentSequence.update(-1);
                }
            }
            for (int key = 0; key < client.npcCount; key++) {
                ((PathingEntity) client.NPC_ARRAY[key].value).currentAnimations = null;
                ((PathingEntity) client.NPC_ARRAY[key].value).currentSequence.update(-1);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_CLANCHANNEL) {
            boolean bool91 = buffer.readUnsignedByte() == 1;
            String string_88 = buffer.readString();
            long long_49 = buffer.readUnsignedShort();
            long long_51 = buffer.read24BitUnsignedInteger();
            int i_10 = buffer.readUnsignedByte();
            int i_11 = buffer.readUnsignedShort();
            long long_53 = long_51 + (long_49 << 32);
            boolean bool14 = false;
            ClanChannel class282Sub4102 = bool91 ? client.CLAN_CHANNEL : client.LISTENED_CLAN_CHANNEL;
            if (class282Sub4102 == null) {
                bool14 = true;
            } else {
                label2385:
                {
                    for (int i_34 = 0; i_34 < 100; i_34++) {
                        if (long_53 == client.aLongArray7424[i_34]) {
                            bool14 = true;
                            break label2385;
                        }
                    }
                    if (i_10 <= 1 && IgnoreList.contains(string_88)) {
                        bool14 = true;
                    }
                }
            }
            if (!bool14) {
                client.aLongArray7424[client.anInt7389] = long_53;
                client.anInt7389 = (client.anInt7389 + 1) % 100;
                String string_17 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_11).fillDynamicValues(buffer);
                int i_82 = bool91 ? 42 : 45;
                if (i_10 != 2 && i_10 != 3) {
                    if (i_10 == 1) {
                        ChatBox.writeMessage(i_82, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + string_88, string_88, string_17, class282Sub4102.clanName, i_11);
                    } else {
                        ChatBox.writeMessage(i_82, 0, string_88, string_88, string_88, string_17, class282Sub4102.clanName, i_11);
                    }
                } else {
                    ChatBox.writeMessage(i_82, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + string_88, string_88, string_17, class282Sub4102.clanName, i_11);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VARCLAN_SET_LONG) {
            int key = buffer.readUnsignedShort();
            long long_47 = buffer.readLong();
            if (ClanVarTypeList.CLAN_VARS == null) {
                ClanVarTypeList.CLAN_VARS = new Object[TypeLists.CLAN_VAR.size];
            }
            ClanVarTypeList.CLAN_VARS[key] = Long.valueOf(long_47);
            client.CLAN_VAR_KEYS[++client.CLAN_VAR_COUNTER - 1 & 0x1f] = key;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_FRIENDS_CHAT) {
            boolean bool91 = buffer.readUnsignedByte() == 1;
            String string_88 = buffer.readString();
            String str92 = string_88;
            if (bool91) {
                str92 = buffer.readString();
            }
            long long_28 = buffer.readLong();
            long long_30 = buffer.readUnsignedShort();
            long long_55 = buffer.read24BitUnsignedInteger();
            int i_35 = buffer.readUnsignedByte();
            int i_77 = buffer.readUnsignedShort();
            long long_57 = (long_30 << 32) + long_55;
            boolean bool115 = false;
            int i_82 = 0;
            while (true) {
                if (i_82 >= 100) {
                    if (i_35 <= 1 && IgnoreList.contains(str92)) {
                        bool115 = true;
                    }
                    break;
                }
                if (client.aLongArray7424[i_82] == long_57) {
                    bool115 = true;
                    break;
                }
                ++i_82;
            }
            if (!bool115) {
                client.aLongArray7424[client.anInt7389] = long_57;
                client.anInt7389 = (client.anInt7389 + 1) % 100;
                String string_41 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_77).fillDynamicValues(buffer);
                if (i_35 == 2) {
                    ChatBox.writeMessage(20, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + str92, string_88, string_41, TextUtils.decodeUidName(long_28), i_77);
                } else if (i_35 == 1) {
                    ChatBox.writeMessage(20, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + str92, string_88, string_41, TextUtils.decodeUidName(long_28), i_77);
                } else {
                    ChatBox.writeMessage(20, 0, string_88, str92, string_88, string_41, TextUtils.decodeUidName(long_28), i_77);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_PRIVATE) {
            String string_63 = buffer.readString();
            int flags = buffer.readUnsignedShort();
            String str92 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(flags).fillDynamicValues(buffer);
            ChatBox.writeMessage(19, 0, string_63, string_63, string_63, str92, null, flags);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VARCLAN_SET_INT) {
            int key = buffer.readUnsignedShort();
            int flags = buffer.readInt();
            if (ClanVarTypeList.CLAN_VARS == null) {
                ClanVarTypeList.CLAN_VARS = new Object[TypeLists.CLAN_VAR.size];
            }
            ClanVarTypeList.CLAN_VARS[key] = Integer.valueOf(flags);
            client.CLAN_VAR_KEYS[++client.CLAN_VAR_COUNTER - 1 & 0x1f] = key;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETTARGETPARAM) {
            int toSlot = buffer.readUnsignedShortLE128();
            if (toSlot == 65535) {
                toSlot = -1;
            }
            int interfaceHash = buffer.readIntV2();
            int fromSlot = buffer.readUnsignedShort();
            if (fromSlot == 65535) {
                fromSlot = -1;
            }
            int settings = buffer.readIntLE();
            client.requestVarVerifyid();
            for (int slot = fromSlot; slot <= toSlot; slot++) {
                long slots = slot + ((long) interfaceHash << 32);
                IFEvents currentSettings = (IFEvents) client.ICOMPONENT_SETTINGS_SLOTS.get(slots);
                IFEvents newSettings;
                if (currentSettings == null) {
                    if (slot == -1) {
                        newSettings = new IFEvents(settings, Component.fromHash(interfaceHash).targetParams.interfaceId);
                    } else {
                        newSettings = new IFEvents(settings, -1);
                    }
                } else {
                    newSettings = new IFEvents(settings, currentSettings.interfaceId);
                    currentSettings.unlink();
                }
                client.ICOMPONENT_SETTINGS_SLOTS.put(newSettings, slots);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_RESETTARGETPARAM) {
            int interfaceHash = buffer.readIntV1();
            int interfaceId = buffer.readUnsignedShortLE128();
            int toSlot = buffer.readUnsignedShort();
            if (toSlot == 65535) {
                toSlot = -1;
            }
            int fromSlot = buffer.readUnsignedShort128();
            if (fromSlot == 65535) {
                fromSlot = -1;
            }
            client.requestVarVerifyid();
            for (int slot = fromSlot; slot <= toSlot; slot++) {
                long slots = slot + ((long) interfaceHash << 32);
                IFEvents currentSettings = (IFEvents) client.ICOMPONENT_SETTINGS_SLOTS.get(slots);
                IFEvents newSettings;
                if (currentSettings == null) {
                    if (slot == -1) {
                        newSettings = new IFEvents(Component.fromHash(interfaceHash).targetParams.settingsHash, interfaceId);
                    } else {
                        newSettings = new IFEvents(0, interfaceId);
                    }
                } else {
                    newSettings = new IFEvents(currentSettings.settingsHash, interfaceId);
                    currentSettings.unlink();
                }
                client.ICOMPONENT_SETTINGS_SLOTS.put(newSettings, slots);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.HINT_TRAIL) {
            int idx = buffer.readUnsignedByte();
            int modelId = buffer.readBigSmart();
            if (client.HINT_TRAILS[idx] != null) {
                client.HINT_TRAILS[idx].removeFromScene(Resource.mapRegion.getScene());
                client.HINT_TRAILS[idx] = null;
            }
            if (modelId != -1) {
                client.HINT_TRAILS[idx] = new HintTrail(Renderers.activeToolkit, buffer, modelId);
                client.HINT_TRAILS[idx].addTo(Resource.mapRegion.getScene());
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.RESET_SOUNDS) {
            AreaSoundPlayer.resetSounds();
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.REQUEST_FPS) {
            int key = buffer.readInt();
            int flags = buffer.readInt();
            TcpPacket tcpmessage111 = PacketEncoder.createPacket(ClientProt.SEND_FPS, context.isaac);
            tcpmessage111.buffer.writeIntInverseMiddle(key);
            tcpmessage111.buffer.writeIntMiddle(flags);
            tcpmessage111.buffer.write128Byte(client.FPS);
            context.queuePacket(tcpmessage111);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.OBJ_ANIM) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.OBJ_ANIM);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VARCLAN_SET_BYTE) {
            int key = buffer.readUnsignedShort();
            byte b_84 = buffer.readByte();
            if (ClanVarTypeList.CLAN_VARS == null) {
                ClanVarTypeList.CLAN_VARS = new Object[TypeLists.CLAN_VAR.size];
            }
            ClanVarTypeList.CLAN_VARS[key] = Integer.valueOf(b_84);
            client.CLAN_VAR_KEYS[++client.CLAN_VAR_COUNTER - 1 & 0x1f] = key;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.GROUND_ITEM_REVEAL) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.GROUND_ITEM_REVEAL);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_INV_STOP_TRANSMIT) {
            int key = buffer.readUnsignedByteC();
            int flags = buffer.readShortLE();
            boolean isNegativeKey = (key & 0x1) == 1;
            ItemContainer.deleteItemContainer(flags, isNegativeKey);
            client.RECENTLY_USED_ITEM_CONTAINERS[++client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - 1 & 0x1f] = flags;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_REBOOT_TIMER) {
            if (GameState.inLobby(client.gameState)) {
                client.REBOOT_TIMER = (int) (buffer.readUnsignedShort() * 2.5F);
            } else {
                client.REBOOT_TIMER = buffer.readUnsignedShort() * 30;
            }
            client.anInt7397 = client.anInt7347;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MIDI_SONG_LOCATION) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.MIDI_SONG_LOCATION);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_ZONE_PARTIAL_FOLLOWS) {
            UpdateZonePacket.UPDATE_ZONE_Y = buffer.readByte128() << 3;
            UpdateZonePacket.UPDATE_ZONE_PLANE = buffer.readUnsignedByte128();
            UpdateZonePacket.UPDATE_ZONE_X = buffer.readByte128() << 3;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_ZONE_PARTIAL_ENCLOSED) {
            UpdateZonePacket.UPDATE_ZONE_Y = buffer.read128Byte() << 3;
            UpdateZonePacket.UPDATE_ZONE_PLANE = buffer.readUnsignedByte128();
            UpdateZonePacket.UPDATE_ZONE_X = buffer.readByte() << 3;
            while (buffer.currentPosition < context.currentPacketSize) {
                UpdateZonePacket tilestreampacket68 = UpdateZonePacket.values()[buffer.readUnsignedByte()];
                PacketDecoder.decodeTilestreamPacket(tilestreampacket68);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_ZONE_FULL_FOLLOWS) {
            UpdateZonePacket.UPDATE_ZONE_X = buffer.read128Byte() << 3;
            UpdateZonePacket.UPDATE_ZONE_Y = buffer.readByte() << 3;
            UpdateZonePacket.UPDATE_ZONE_PLANE = buffer.readUnsignedByte();
            CoordGrid base = Resource.mapRegion.getBaseCoordinates();
            for (GroundItemWrapper groundItems = client.GROUND_ITEMS.first(); groundItems != null; groundItems = client.GROUND_ITEMS.next()) {
                int plane = (int) (groundItems.pointer >> 28 & 0x3L);
                int absX = (int) (groundItems.pointer & 0x3fffL);
                int x = absX - base.x;
                int absY = (int) (groundItems.pointer >> 14 & 0x3fffL);
                int y = absY - base.y;
                if (plane == UpdateZonePacket.UPDATE_ZONE_PLANE && x >= UpdateZonePacket.UPDATE_ZONE_X && x < UpdateZonePacket.UPDATE_ZONE_X + 8 && y >= UpdateZonePacket.UPDATE_ZONE_Y && y < UpdateZonePacket.UPDATE_ZONE_Y + 8) {
                    groundItems.unlink();
                    if (x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                        GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y);
                    }
                }
            }
            LocAction locAction;
            for (locAction = (LocAction) LocAction.pendingRemovals.getFirst(); locAction != null; locAction = (LocAction) LocAction.pendingRemovals.next()) {
                if (locAction.x >= UpdateZonePacket.UPDATE_ZONE_X && locAction.x < UpdateZonePacket.UPDATE_ZONE_X + 8 && locAction.y >= UpdateZonePacket.UPDATE_ZONE_Y && locAction.y < UpdateZonePacket.UPDATE_ZONE_Y + 8 && locAction.plane == UpdateZonePacket.UPDATE_ZONE_PLANE) {
                    locAction.inUpdateZone = true;
                }
            }
            for (locAction = (LocAction) LocAction.pendingCustomizations.getFirst(); locAction != null; locAction = (LocAction) LocAction.pendingCustomizations.next()) {
                if (locAction.x >= UpdateZonePacket.UPDATE_ZONE_X && locAction.x < UpdateZonePacket.UPDATE_ZONE_X + 8 && locAction.y >= UpdateZonePacket.UPDATE_ZONE_Y && locAction.y < UpdateZonePacket.UPDATE_ZONE_Y + 8 && locAction.plane == UpdateZonePacket.UPDATE_ZONE_PLANE) {
                    locAction.inUpdateZone = true;
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SOUND_SYNTH) {
            int soundId = buffer.readUnsignedShort();
            if (soundId == 65535) {
                soundId = -1;
            }
            int flags = buffer.readUnsignedByte();
            int i_6 = buffer.readUnsignedShort();
            int delay = buffer.readUnsignedByte();
            int i_8 = buffer.readUnsignedShort();
            AreaSoundPlayer.playSoundSynth(soundId, flags, i_6, delay, i_8);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SHOW_FACE_HERE) {
            boolean showFaceHere = buffer.readUnsignedByte() == 1;
            client.requestVarVerifyid();
            MiniMenuBuilder.SHOW_FACE_HERE = showFaceHere;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_UID192) {
            buffer.currentPosition += 28;
            if (buffer.checkCrc()) {
                GameShell.updateRandomDatUID192(buffer, buffer.currentPosition - 28);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.HINT_ARROW) {
            int flags = buffer.readUnsignedByte();
            int iconIndex = flags >> 5;
            int targetType = flags & 0x1f;
            if (targetType == 0) {
                PlayerUpdate.HINT_ARROWS[iconIndex] = null;
            } else {
                HintArrow arrow = new HintArrow();
                arrow.targetType = targetType;
                arrow.arrowSprite = buffer.readUnsignedByte();
                if (arrow.arrowSprite >= 0 && arrow.arrowSprite < StaticMedia.hintHeadiconSprites.length) {
                    if (arrow.targetType != 1 && arrow.targetType != 10) {
                        if (arrow.targetType <= 6) {
                            if (arrow.targetType == 2) {
                                arrow.x = 256;
                                arrow.y = 256;
                            }
                            if (arrow.targetType == 3) {
                                arrow.x = 0;
                                arrow.y = 256;
                            }
                            if (arrow.targetType == 4) {
                                arrow.x = 512;
                                arrow.y = 256;
                            }
                            if (arrow.targetType == 5) {
                                arrow.x = 256;
                                arrow.y = 0;
                            }
                            if (arrow.targetType == 6) {
                                arrow.x = 256;
                                arrow.y = 512;
                            }
                            arrow.targetType = 2;
                            arrow.plane = buffer.readUnsignedByte();
                            CoordGrid grid = Resource.mapRegion.getBaseCoordinates();
                            arrow.x += buffer.readUnsignedShort() - grid.x << 9;
                            arrow.y += buffer.readUnsignedShort() - grid.y << 9;
                            arrow.height = buffer.readUnsignedByte() << 2;
                            arrow.distance = buffer.readUnsignedShort();
                        }
                    } else {
                        arrow.targetIndex = buffer.readUnsignedShort();
                        arrow.denominator = buffer.readUnsignedShort();
                        buffer.currentPosition += 4;
                    }
                    arrow.modelId = buffer.readInt();
                    PlayerUpdate.HINT_ARROWS[iconIndex] = arrow;
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.PROJANIM_SPECIFIC) {
            int endHeight = buffer.readUnsignedByte128() * 4;
            int source = buffer.readShortLE128();
            int endTime = buffer.readShortLE();
            int lockOn = buffer.readShort();
            int flags = buffer.readUnsignedByte128();
            int y = buffer.readUnsignedShort128();
            int slope = buffer.readUnsignedShort();
            int startTime = buffer.readUnsignedShortLE128();
            int spotAnimId = buffer.readUnsignedShortLE128();
            int angle = buffer.readUnsignedByte128();
            if (angle == 255) {
                angle = -1;
            }
            byte xDist = buffer.readByteC();
            byte yDist = buffer.readByteC();
            int x = buffer.readUnsignedShort128();
            int startHeight = buffer.readUnsignedByteC();
            boolean useFloorHeight = (flags & 0x1) != 0;
            boolean hasBASOffIdx = (flags & 0x2) != 0;
            int basOffIdx = hasBASOffIdx ? flags >> 2 : -1;
            if (hasBASOffIdx) {
                startHeight = (byte) startHeight;
            } else {
                startHeight *= 4;
            }
            CoordGrid base = Resource.mapRegion.getBaseCoordinates();
            int localX = x - base.x * 2;
            int localY = y - base.y * 2;
            int xOff = xDist + localX;
            int yOff = yDist + localY;
            if (localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() * 2 && localY < Resource.mapRegion.getSceneSizeX() * 2 && xOff >= 0 && yOff >= 0 && xOff < Resource.mapRegion.getSceneSizeY() * 2 && yOff < Resource.mapRegion.getSceneSizeY() * 2 && spotAnimId != 65535) {
                localX *= 256;
                localY *= 256;
                xOff *= 256;
                yOff *= 256;
                startHeight <<= 2;
                endHeight <<= 2;
                slope <<= 2;
                ProjectileSpawnRequest.create(spotAnimId, source, lockOn, basOffIdx, startHeight, endHeight, localX, localY, xOff, yOff, startTime, endTime, angle, slope, useFloorHeight);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.WORLD_LIST) {
            boolean refresh = buffer.readUnsignedByte() == 1;
            byte[] data = new byte[context.currentPacketSize - 1];
            buffer.readBytes(data, 0, context.currentPacketSize - 1);
            decodeWorldList(refresh, data);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VARBIT_LARGE) {
            int variable = buffer.readIntV2();
            int varbitId = buffer.readUnsignedShort128();
            VarProviders.PLAYER_VAR_PROVIDER.setVarBitValueFromServer(varbitId, variable);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.aClass3754362) { //request packet 37?
            int key = buffer.readUnsignedShortLE128();
            client.requestVarVerifyid();
            StaticMethods.method1563(key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CLANSETTINGS_FULL) {
            client.anInt7192 = client.anInt7347;
            boolean bool91 = buffer.readUnsignedByte() == 1;
            if (context.currentPacketSize == 1) {
                if (bool91) {
                    client.CLAN_SETTINGS = null;
                } else {
                    client.GUEST_CLAN_SETTINGS = null;
                }
            } else {
                if (bool91) {
                    client.CLAN_SETTINGS = new ClanSettings(buffer);
                } else {
                    client.GUEST_CLAN_SETTINGS = new ClanSettings(buffer);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.aClass3754457) {
            int key = buffer.readUnsignedShort();
            client.requestVarVerifyid();
            StaticMethods.setVorbisSound(key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_INV_PARTIAL) {
            int key = buffer.readUnsignedShort();
            int flags = buffer.readUnsignedByte();
            int i_7, i_8, i_9;
            for (boolean isNegativeKey = (flags & 0x1) == 1; buffer.currentPosition < context.currentPacketSize; ItemContainer.setItemInContainer(key, i_7, i_8 - 1, i_9, isNegativeKey)) {
                i_7 = buffer.readUnsignedSmart();
                i_8 = buffer.readUnsignedShort();
                i_9 = 0;
                if (i_8 != 0) {
                    i_9 = buffer.readUnsignedByte();
                    if (i_9 == 255) {
                        i_9 = buffer.readInt();
                    }
                }
            }
            client.RECENTLY_USED_ITEM_CONTAINERS[++client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - 1 & 0x1f] = key;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.PING) {
            context.currentPacket = null;
            return false;
        } else if (context.currentPacket == ServerProt.VARP_SMALL) {
            byte value = buffer.readByte();
            int configIndex = buffer.readUnsignedShortLE128();
            VarProviders.PLAYER_VAR_PROVIDER.setVarValueFromServer(configIndex, value);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.aClass3753828) {
            int near = buffer.readUnsigned128Byte();
            int volume = buffer.readUnsignedByte();
            int far = buffer.readUnsignedByteC();
            int musicArchive = buffer.readInt();
            int i_8 = buffer.readIntV1();
            int plane = i_8 >> 28;
            int x = i_8 >> 14 & 0x3fff;
            int y = i_8 & 0x3fff;
            SongPlayer.method5731(x, y, near, far, musicArchive, volume);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.TRIGGER_ONDIALOGABORT) {
            if (client.BASE_WINDOW_ID != -1) {
            	Interface.open_sub(client.BASE_WINDOW_ID, 0);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_FRIENDCHAT_CHANNEL_SINGLEUSER) {
            String displayName = buffer.readString();
            boolean hasPreviousName = buffer.readUnsignedByte() == 1;
            String username;
            if (hasPreviousName) {
                username = buffer.readString();
            } else {
                username = displayName;
            }
            int worldId = buffer.readUnsignedShort();
            byte rank = buffer.readByte();
            boolean bool69 = rank == -128;
            int i_11;
            if (bool69) {
                if (FriendsChat.FC_PLAYER_COUNT == 0) {
                    context.currentPacket = null;
                    return true;
                }
                for (i_11 = 0; i_11 < FriendsChat.FC_PLAYER_COUNT && (!FriendsChat.FC_PLAYERS[i_11].username.equals(username) || worldId != FriendsChat.FC_PLAYERS[i_11].worldId); i_11++) {
                }
                if (i_11 < FriendsChat.FC_PLAYER_COUNT) {
                    while (i_11 < FriendsChat.FC_PLAYER_COUNT - 1) {
                        FriendsChat.FC_PLAYERS[i_11] = FriendsChat.FC_PLAYERS[i_11 + 1];
                        ++i_11;
                    }
                    FriendsChat.FC_PLAYERS[--FriendsChat.FC_PLAYER_COUNT] = null;
                }
            } else {
                String worldName = buffer.readString();
                FriendsChatPlayer fcPlayer = new FriendsChatPlayer();
                fcPlayer.displayName = displayName;
                fcPlayer.username = username;
                fcPlayer.textName = TextUtils.formatDisplayname(fcPlayer.username);
                fcPlayer.worldId = worldId;
                fcPlayer.rank = rank;
                fcPlayer.worldName = worldName;
                int i_12;
                for (i_12 = FriendsChat.FC_PLAYER_COUNT - 1; i_12 >= 0; --i_12) {
                    int i_35 = FriendsChat.FC_PLAYERS[i_12].textName.compareTo(fcPlayer.textName);
                    if (i_35 == 0) {
                        FriendsChat.FC_PLAYERS[i_12].worldId = worldId;
                        FriendsChat.FC_PLAYERS[i_12].rank = rank;
                        FriendsChat.FC_PLAYERS[i_12].worldName = worldName;
                        if (username.equals(PlayerUpdate.MY_PLAYER.displayName)) {
                            FriendsChat.MY_PLAYER_FC_RANK = rank;
                        }
                        client.anInt7179 = client.anInt7347;
                        context.currentPacket = null;
                        return true;
                    }
                    if (i_35 < 0) {
                        break;
                    }
                }
                if (FriendsChat.FC_PLAYER_COUNT >= FriendsChat.FC_PLAYERS.length) {
                    context.currentPacket = null;
                    return true;
                }
                if (FriendsChat.FC_PLAYER_COUNT - 1 - i_12 >= 0)
                    System.arraycopy(FriendsChat.FC_PLAYERS, i_12 + 1, FriendsChat.FC_PLAYERS, i_12 + 1 + 1, FriendsChat.FC_PLAYER_COUNT - 1 - i_12);
                if (FriendsChat.FC_PLAYER_COUNT == 0) {
                    FriendsChat.FC_PLAYERS = new FriendsChatPlayer[100];
                }
                FriendsChat.FC_PLAYERS[i_12 + 1] = fcPlayer;
                FriendsChat.FC_PLAYER_COUNT++;
                if (username.equals(PlayerUpdate.MY_PLAYER.displayName)) {
                    FriendsChat.MY_PLAYER_FC_RANK = rank;
                }
            }
            client.anInt7179 = client.anInt7347;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETHIDE) {
            int key = buffer.readUnsignedByte128();
            int flags = buffer.readInt();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfhide(flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CLANCHANNEL_DELTA) {
            client.anInt7395 = client.anInt7347;
            boolean bool91 = buffer.readUnsignedByte() == 1;
            ClanChannelDelta delta = new ClanChannelDelta(buffer);
            ClanChannel clanChannel;
            if (bool91) {
                clanChannel = client.CLAN_CHANNEL;
            } else {
                clanChannel = client.LISTENED_CLAN_CHANNEL;
            }
            delta.apply(clanChannel);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.REGION) {
            Packet.Bit bitBuffer = new Packet.Bit(context.currentPacketSize);
            System.arraycopy(context.receivedBuffer.buffer, context.receivedBuffer.currentPosition, bitBuffer.buffer, 0, context.currentPacketSize);
            client.resetCutscenes();
            if (PreferenceManager.preferences.asyncMaploading.getValue() == 1) {
                Resource.mapRegionLoaderTask.requestMapUpdate(new RegionLoadRequest(RegionLoadingType.STANDARD, bitBuffer));
            } else {
                Resource.mapRegion.initialize(new RegionLoadRequest(RegionLoadingType.STANDARD, bitBuffer));
            }
            context.currentPacket = null;
            return false;
        } else if (context.currentPacket == ServerProt.REMOVE_GROUND_ITEM) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.REMOVE_GROUND_ITEM);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETANIM) {
            int key = buffer.readIntV2();
            int flags = buffer.readInt();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfanimUpdate(flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SPOT_ANIM) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.SPOT_ANIM);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VARP_LARGE) {
            int key = buffer.readIntV2();
            int flags = buffer.readUnsignedShortLE128();
            VarProviders.PLAYER_VAR_PROVIDER.setVarValueFromServer(flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_CLOSESUB) {
            int parentInterface = buffer.readInt();
            client.requestVarVerifyid();
            SubInterface subInterface = client.OPEN_INTERFACES.get(parentInterface);
            if (subInterface != null) {
                Interface.closeChildInterface(subInterface, true, false);
            }
            if (client.aClass1187352 != null) {
                Component.redrawComponent(client.aClass1187352);
                client.aClass1187352 = null;
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_STAT) {
            int xp = buffer.readInt();
            int skillId = buffer.readUnsignedByte();
            int level = buffer.readUnsignedByte();
            client.SKILL_XP[skillId] = xp;
            client.SKILL_LEVEL_VISIBLE[skillId] = level;
            client.SKILL_LEVEL_ACTUAL[skillId] = 1;
            int maxLevel = SkillConstants.SKILL_MAX_LEVEL[skillId] - 1;
            for (int i = 0; i < maxLevel; i++) {
                if (xp >= SkillConstants.SKILL_XP_FOR_LEVEL[i]) {
                    client.SKILL_LEVEL_ACTUAL[skillId] = i + 2;
                }
            }
            client.anIntArray7283[++client.anInt7384 - 1 & 0x1f] = skillId;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.aClass3754453) {
            byte b_100 = buffer.readByte();
            int flags = buffer.readUnsignedByteC();
            client.requestVarVerifyid();
            StaticMethods.method14501(b_100, flags);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VARCLAN_ENABLE) {
            ClanVarTypeList.CLAN_VARS = new Object[TypeLists.CLAN_VAR.size];
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETRECOL) {
            int key = buffer.readIntV2();
            int flags = buffer.readShortLE();
            int i_6 = buffer.readShortLE();
            int i_7 = buffer.readUnsignedByte();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfrecol(key, i_7, flags, i_6);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETTEXTANTIMACRO) {
            boolean enabled = buffer.readUnsigned128Byte() == 1;
            int componentHash = buffer.readIntV2();
            client.requestVarVerifyid();
            PulseEventQueue.prioritySetIftextantimacro(componentHash, enabled);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CREATE_CHECK_EMAIL_REPLY) {
            int key = buffer.readUnsignedByte();
            CheckEmailRequest request = (CheckEmailRequest) Identifiable.identify(CheckEmailRequest.getValues(), key);
            if (request == null) {
                request = CheckEmailRequest.RECEIVED_RESPONSE;
            }
            AccountCreationStage.setCheckemailRequeststatus(request);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_DOB) {
            LoginManager.PLAYER_DOB = buffer.read24BitInteger();
            client.USERDETAIL_QUICKCHAT = buffer.readUnsignedByte() == 1;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_OPENSUB_ACTIVE_PLAYER) {
            int key = buffer.readIntLE();
            int overlay = buffer.readUnsignedByte();
            int i_6 = buffer.readIntV1();
            int subInterfaceId = buffer.readUnsignedShort();
            int i_8 = buffer.readInt();
            int interfaceId = buffer.readIntV1();
            int i_10 = buffer.readIntV1();
            int playerIndex = buffer.readShortLE();
            client.requestVarVerifyid();
            Interface.openSub(interfaceId, new IFSubActivePlayer(subInterfaceId, overlay, playerIndex), new int[]{i_6, i_8, i_10, key}, false);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CLANSETTINGS_DELTA) {
            client.anInt7192 = client.anInt7347;
            boolean clanSettings = buffer.readUnsignedByte() == 1;
            ClanSettingsDelta delta = new ClanSettingsDelta(buffer);
            ClanSettings settings;
            if (clanSettings) {
                settings = client.CLAN_SETTINGS;
            } else {
                settings = client.GUEST_CLAN_SETTINGS;
            }
            delta.queueUpdates(settings);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MINIMAP_FLAG) {
            int key = buffer.readUnsigned128Byte();
            int flags = buffer.readUnsigned128Byte();
            if (key == 255) {
                key = -1;
                flags = -1;
            }
            PulseEventQueue.priorityMinimapFlagUpdate(key, flags);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.BLOCK_MINIMAP_STATE) {
            ComponentMinimap.minimapState = buffer.readUnsignedByte();
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.OUTDATED_SET_THEORA_STRING_SOMETHING) {
            int key = buffer.readUnsignedShort128();
            int flags = buffer.readIntLE();
            client.requestVarVerifyid();
            PulseEventQueue.priorityT17Update(flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_OPENSUB) {
            int key = buffer.readInt();
            int flags = buffer.readIntV2();
            int i_6 = buffer.readIntV2();
            int subId = buffer.readUnsignedShortLE128();
            int i_8 = buffer.readIntLE();
            int topUid = buffer.readInt();
            int overlay = buffer.readUnsignedByteC();
            client.requestVarVerifyid();
            Interface.openSub(topUid, new SubInterface(subId, overlay), new int[]{key, i_6, flags, i_8}, false);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CREATE_ACCOUNT_REPLY) {
            int key = buffer.readUnsignedByte();
            CreateAccountRequest class49496 = (CreateAccountRequest) Identifiable.identify(CreateAccountRequest.method8038(), key);
            if (class49496 == null) {
                class49496 = CreateAccountRequest.aClass4945782;
            }
            AccountCreationStage.method12948(class49496);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.PLAYER_OPTION) {
            String option = buffer.readString();
            int slot = buffer.readUnsignedByte128();
            int cursor = buffer.readUnsignedShortLE128();
            if (cursor == 65535) {
                cursor = -1;
            }
            int onTop = buffer.readUnsignedByteC();
            if (slot >= 1 && slot <= 8) {
                if ("null".equalsIgnoreCase(option)) {
                    option = null;
                }
                client.PLAYER_OPTIONS[slot - 1] = option;
                client.PLAYER_OPTION_CURSORS[slot - 1] = cursor;
                client.PLAYER_OPTION_REDUCED_PRIORITY[slot - 1] = onTop == 0;
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETITEM) {
            int itemId = buffer.readUnsignedShort();
            if (itemId == 65535) {
                itemId = -1;
            }
            int interfaceHash = buffer.readInt();
            int amount = buffer.readIntV2();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfitemUpdate(interfaceHash, itemId, amount);
            ItemType defs = TypeLists.OBJ.list(itemId);
            PulseEventQueue.priorityIfangleUpdate(interfaceHash, defs.modelRotationX, defs.modelRotationY, defs.modelZoom);
            PulseEventQueue.priorityIfoffsetUpdate(interfaceHash, defs.modelOffsetX, defs.modelOffsetY, defs.modelRotationZ);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERHEAD_IGNOREWORN) {
            int idKit1 = buffer.readShortLE();
            int usernameMeme = buffer.readUnsignedShort128();
            int idKit2 = buffer.readUnsignedShort128();
            int interfaceHash = buffer.readIntV2();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfcontentUpdate(interfaceHash, 7, idKit1 << 16 | idKit2, usernameMeme);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.GROUND_ITEM_COUNT) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.GROUND_ITEM_COUNT);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERMODEL) {
            int key = buffer.readIntV1();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfcontentUpdate(key, 5, PlayerUpdate.myPlayerIndex, 0);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.PRELOAD_SONG) {
            int songId = buffer.readUnsignedShortLE128();
            if (songId == 65535) {
                songId = -1;
            }
            PacketEncoder.preloadSong(songId);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.APPLY_DEBUG) {
            int key = buffer.readUnsignedShort();
            DebugDisplay.applyDebugOpcode(key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CREATE_OBJECT) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.CREATE_OBJECT);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SEND_PRIVATE_MESSAGE) {
            String string_63 = buffer.readString();
            String string_88 = TextUtils.atLtGtTags(HuffmanList.decode(buffer));
            ChatBox.writeMessage(6, 0, string_63, string_63, string_63, string_88);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CREATE_GROUND_ITEM) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.CREATE_GROUND_ITEM);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.FRIENDLIST_LOADED) {
            FriendList.LOADED = 1;
            client.anInt7386 = client.anInt7347;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERHEAD) {
            int key = buffer.readIntLE();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfcontentUpdate(key, 3, PlayerUpdate.myPlayerIndex, 0);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.JCOINS_UPDATE) {
            int key = buffer.readIntLE();
            if (key != client.JCOINS) {
                client.JCOINS = key;
                CS2Executor.executeTrigger(ClientTriggerType.JCOINS_CHANGED, -1, -1);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.REFLECTION_CHECK) {
            ReflectionCheckManager.requestReflectionCheck(buffer);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VARBIT_SMALL) {
            int key = buffer.readUnsignedShort();
            int flags = buffer.readUnsignedByte128();
            VarProviders.PLAYER_VAR_PROVIDER.setVarBitValueFromServer(key, flags);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_PRIVATE_ECHO) {
            boolean bool91 = buffer.readUnsignedByte() == 1;
            String string_88 = buffer.readString();
            String str92 = string_88;
            if (bool91) {
                str92 = buffer.readString();
            }
            long long_28 = buffer.readUnsignedShort();
            long long_30 = buffer.read24BitUnsignedInteger();
            int i_11 = buffer.readUnsignedByte();
            long long_53 = (long_28 << 32) + long_30;
            boolean bool14 = false;
            int i_15 = 0;
            while (true) {
                if (i_15 >= 100) {
                    if (i_11 <= 1) {
                        if ((!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
                            if (IgnoreList.contains(str92)) {
                                bool14 = true;
                            }
                        } else {
                            bool14 = true;
                        }
                    }
                    break;
                }
                if (client.aLongArray7424[i_15] == long_53) {
                    bool14 = true;
                    break;
                }
                ++i_15;
            }
            if (!bool14) {
                client.aLongArray7424[client.anInt7389] = long_53;
                client.anInt7389 = (client.anInt7389 + 1) % 100;
                String string_118 = TextUtils.atLtGtTags(HuffmanList.decode(buffer));
                if (i_11 == 2) {
                    ChatBox.writeMessage(7, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + str92, string_88, string_118, null, -1);
                } else if (i_11 == 1) {
                    ChatBox.writeMessage(7, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + str92, string_88, string_118, null, -1);
                } else {
                    ChatBox.writeMessage(3, 0, string_88, str92, string_88, string_118, null, -1);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CLIENT_SETVARC_SMALL) {
            byte b_100 = buffer.read128Byte();
            int flags = buffer.readShortLE();
            client.requestVarVerifyid();
            PulseEventQueue.priorityClientvarcUpdate(flags, b_100);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETTEXTFONT) {
            int key = buffer.readInt();
            int flags = buffer.readIntV1();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIffontUpdate(flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETGRAPHIC) {
            int key = buffer.readIntV2();
            int flags = buffer.readIntV2();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfgraphicUpdate(flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.DEBUG_SERVER_TRIGGERS) {
            int key = buffer.readUnsignedShort();
            int flags = buffer.readUnsignedShort();
            int i_6 = buffer.readUnsignedShort();
            client.requestVarVerifyid();
            int i_8;
            if (Interface.INTERFACES[key] != null) {
                for (int i_7 = flags; i_7 < i_6; i_7++) {
                    i_8 = buffer.read24BitUnsignedInteger();
                    if (i_7 < Interface.INTERFACES[key].components.length && Interface.INTERFACES[key].components[i_7] != null) {
                        Interface.INTERFACES[key].components[i_7].serverTriggers = i_8;
                    }
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CUSTOMIZE_OBJECT) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.CUSTOMIZE_OBJECT);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.PLAYER_UPDATE) {
            PlayerUpdate.decode(buffer, context.currentPacketSize);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CAM_MOVETO) {
            int localY = buffer.readUnsignedByte();
            int localX = buffer.readUnsignedByte();
            int constantSpeed = buffer.readUnsigned128Byte();
            int moveZ = buffer.readShortLE() << 2;
            int variableSpeed = buffer.readUnsignedByte128();
            client.requestVarVerifyid();
            Camera.moveCamera(localX, localY, moveZ, constantSpeed, variableSpeed, true);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_CLANCHANNEL) {
            boolean bool91 = buffer.readUnsignedByte() == 1;
            String string_88 = buffer.readString();
            long long_49 = buffer.readUnsignedShort();
            long long_51 = buffer.read24BitUnsignedInteger();
            int i_10 = buffer.readUnsignedByte();
            long long_55 = long_51 + (long_49 << 32);
            boolean bool112 = false;
            ClanChannel class282Sub437 = bool91 ? client.CLAN_CHANNEL : client.LISTENED_CLAN_CHANNEL;
            if (class282Sub437 == null) {
                bool112 = true;
            } else {
                label2419:
                {
                    for (int i_81 = 0; i_81 < 100; i_81++) {
                        if (client.aLongArray7424[i_81] == long_55) {
                            bool112 = true;
                            break label2419;
                        }
                    }
                    if (i_10 <= 1) {
                        if ((!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
                            if (IgnoreList.contains(string_88)) {
                                bool112 = true;
                            }
                        } else {
                            bool112 = true;
                        }
                    }
                }
            }
            if (!bool112) {
                client.aLongArray7424[client.anInt7389] = long_55;
                client.anInt7389 = (client.anInt7389 + 1) % 100;
                String string_101 = TextUtils.atLtGtTags(HuffmanList.decode(buffer));
                int i_34 = bool91 ? 41 : 44;
                if (i_10 != 2 && i_10 != 3) {
                    if (i_10 == 1) {
                        ChatBox.writeMessage(i_34, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + string_88, string_88, string_101, class282Sub437.clanName, -1);
                    } else {
                        ChatBox.writeMessage(i_34, 0, string_88, string_88, string_88, string_101, class282Sub437.clanName, -1);
                    }
                } else {
                    ChatBox.writeMessage(i_34, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + string_88, string_88, string_101, class282Sub437.clanName, -1);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.WINDOW_CALL_WITH_ARGS) {
            byte[] argsBuffer = new byte[context.currentPacketSize];
            buffer.readBytes(argsBuffer, context.currentPacketSize);
            String windowCallParam = Utils.readString(argsBuffer, 0, context.currentPacketSize);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MAP_PROJANIM) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.MAP_PROJANIM);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_SITESETTINGS_COOKIE) {
            //no applet deprecated
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.REDUCE_ATTACK_PRIORITY) {
            client.REDUCE_ATTACK_PRIORITY = buffer.readUnsignedByte128() == 1;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.FRIEND_STATUS) {
            while (buffer.currentPosition < context.currentPacketSize) {
                boolean warnMessage = buffer.readUnsignedByte() == 1;
                String displayName = buffer.readString();
                String username = buffer.readString();
                int world = buffer.readUnsignedShort();
                int friendchatRank = buffer.readUnsignedByte();
                boolean referrer = buffer.readUnsignedByte() == 1;
                String worldName = "";
                int platform = -1;
                int worldFlags = 0;
                if (world > 0) {
                    worldName = buffer.readString();
                    platform = buffer.readUnsignedByte();
                    worldFlags = buffer.readInt();
                }
                for (int index = 0; index < FriendList.FRIEND_COUNT; index++) {
                    Friend friend = FriendList.FRIENDS[index];
                    if (!warnMessage) {
                        if (displayName.equals(friend.displayName)) {
                            if (world != friend.worldId) {
                                boolean announce = true;
                                for (LoginStateUpdate statusUpdate = (LoginStateUpdate) client.PLAYER_LOGINSTATE_UPDATES.getFirst(); statusUpdate != null; statusUpdate = (LoginStateUpdate) client.PLAYER_LOGINSTATE_UPDATES.getNext()) {
                                    if (statusUpdate.username.equals(displayName)) {
                                        if (world != 0 && statusUpdate.actionType == 0) {
                                            statusUpdate.unlinkNode();
                                            announce = false;
                                        } else if (world == 0 && statusUpdate.actionType != 0) {
                                            statusUpdate.unlinkNode();
                                            announce = false;
                                        }
                                    }
                                }
                                if (announce) {
                                    client.PLAYER_LOGINSTATE_UPDATES.offer(new LoginStateUpdate(displayName, world));
                                }
                                friend.worldId = world;
                            }
                            friend.username = username;
                            friend.worldName = worldName;
                            friend.friendsChatRank = friendchatRank;
                            friend.platform = platform;
                            friend.referrer = referrer;
                            friend.worldFlags = worldFlags;
                            displayName = null;
                            break;
                        }
                    } else if (username.equals(friend.displayName)) {
                        friend.displayName = displayName;
                        friend.username = username;
                        displayName = null;
                        break;
                    }
                }
                if (displayName != null && FriendList.FRIEND_COUNT < 200) {
                    Friend friend = new Friend();
                    FriendList.FRIENDS[FriendList.FRIEND_COUNT] = friend;
                    friend.displayName = displayName;
                    friend.username = username;
                    friend.worldId = world;
                    friend.worldName = worldName;
                    friend.friendsChatRank = friendchatRank;
                    friend.platform = platform;
                    friend.referrer = referrer;
                    friend.worldFlags = worldFlags;
                    ++FriendList.FRIEND_COUNT;
                }
            }
            FriendList.LOADED = 2;
            client.anInt7386 = client.anInt7347;
            boolean bool91 = false;
            int flags = FriendList.FRIEND_COUNT;
            while (flags > 0) {
                bool91 = true;
                --flags;
                for (int i_6 = 0; i_6 < flags; i_6++) {
                    boolean bool70 = false;
                    Friend class6132 = FriendList.FRIENDS[i_6];
                    Friend class6134 = FriendList.FRIENDS[i_6 + 1];
                    if (class6132.worldId != LoginManager.GAME_CONNECTION_INFO.worldId && class6134.worldId == LoginManager.GAME_CONNECTION_INFO.worldId) {
                        bool70 = true;
                    }
                    if (!bool70 && class6132.worldId == 0 && class6134.worldId != 0) {
                        bool70 = true;
                    }
                    if (!bool70 && !class6132.referrer && class6134.referrer) {
                        bool70 = true;
                    }
                    if (bool70) {
                        Friend class687 = FriendList.FRIENDS[i_6];
                        FriendList.FRIENDS[i_6] = FriendList.FRIENDS[i_6 + 1];
                        FriendList.FRIENDS[i_6 + 1] = class687;
                        bool91 = false;
                    }
                }
                if (bool91) {
                    break;
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IDENTIFY_HOST_NAME) {
            int ip = buffer.readInt();
            PacketDecoder.HOSTNAME_IDENTIFIER = new HostNameIdentifier(ip);
            Thread thread94 = new Thread(PacketDecoder.HOSTNAME_IDENTIFIER);
            thread94.setPriority(1);
            thread94.start();
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_MOVESUB) {
            int fromParentUid = buffer.readIntV2();
            int toParentUid = buffer.readInt();
            client.requestVarVerifyid();
            SubInterface toSub = client.OPEN_INTERFACES.get(toParentUid);
            SubInterface fromSub = client.OPEN_INTERFACES.get(fromParentUid);
            if (fromSub != null) {
                Interface.closeChildInterface(fromSub, toSub == null || toSub.subInterfaceId != fromSub.subInterfaceId, false);
            }
            if (toSub != null) {
                toSub.unlink();
                client.OPEN_INTERFACES.put(toSub, fromParentUid);
            }
            Component parent2Def = Component.fromHash(toParentUid);
            if (parent2Def != null) {
                Component.redrawComponent(parent2Def);
            }
            parent2Def = Component.fromHash(fromParentUid);
            if (parent2Def != null) {
                Component.redrawComponent(parent2Def);
                Component.method483(Interface.INTERFACES[parent2Def.idHash >>> 16], parent2Def, true);
            }
            if (client.BASE_WINDOW_ID != -1) {
            	Interface.open_sub(client.BASE_WINDOW_ID, 1);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CUTSCENE) {
            int key = buffer.readUnsignedShort();
            client.cycleCutsceneLoaded = -1;
            client.CURRENT_CUTSCENE = key;
            client.MAP_LOADING_STAGE = 3;
            Resource.CUTSCENE.isFileCached(client.CURRENT_CUTSCENE);
            MiniMenu.close();
            MiniMenu.resetEntries();
            int flags = buffer.readUnsignedShort();
            MapRegion.CUTSCENE_MAP_XTEAS = new int[flags][4];
            for (int i_6 = 0; i_6 < flags; i_6++) {
                for (int i_7 = 0; i_7 < 4; i_7++) {
                    MapRegion.CUTSCENE_MAP_XTEAS[i_6][i_7] = buffer.readInt();
                }
            }
            int i_6 = buffer.readUnsignedByte();
            CutsceneTypeList.CUTSCENE_BUFFER = new Packet(i_6);
            CutsceneTypeList.CUTSCENE_BUFFER.writeBytes(buffer.buffer, buffer.currentPosition, i_6);
            buffer.currentPosition += i_6;
            context.currentPacket = null;
            return false;
        } else if (context.currentPacket == ServerProt.DESTROY_OBJECT) {
            PacketDecoder.decodeTilestreamPacket(UpdateZonePacket.DESTROY_OBJECT);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.LOYALTY_UPDATE) {
            int key = buffer.readInt();
            boolean bool66 = buffer.readUnsignedByteC() == 1;
            if (bool66 != QueueIterator.LOYALTY_ENABLED || key != LoginManager.LOYALTY_BALANCE) {
                QueueIterator.LOYALTY_ENABLED = bool66;
                LoginManager.LOYALTY_BALANCE = key;
                CS2Executor.executeTrigger(ClientTriggerType.LOYALTY_UPDATED, -1, -1);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_OPENSUB_ACTIVE_NPC) {
            int npcIndex = buffer.readUnsignedShort128();
            int flags = buffer.readIntLE();
            int overlay = buffer.readUnsignedByteC();
            int interfaceId = buffer.readInt();
            int i_8 = buffer.readIntLE();
            int subInterfaceId = buffer.readUnsignedShortLE128();
            int i_10 = buffer.readInt();
            int i_11 = buffer.readInt();
            client.requestVarVerifyid();
            Interface.openSub(interfaceId, new IFSubActiveNpc(subInterfaceId, overlay, npcIndex), new int[]{i_11, flags, i_10, i_8}, false);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.ADD_IGNORE) {
            int flags = buffer.readUnsignedByte();
            boolean editDisplayName = (flags & 0x1) == 1;
            String username = buffer.readString();
            String displayName = buffer.readString();
            if (!editDisplayName) {
                Ignore ignore = new Ignore();
                IgnoreList.IGNORED[IgnoreList.IGNORE_COUNT++] = ignore;
                ignore.unfilteredUsername = username;
                ignore.displayName = displayName;
                ignore.temporary = (flags & 0x2) == 2;
            } else {
                for (int i = 0; i < IgnoreList.IGNORE_COUNT; i++) {
                    Ignore ignore = IgnoreList.IGNORED[i];
                    if (displayName.equals(ignore.unfilteredUsername)) {
                        ignore.unfilteredUsername = username;
                        ignore.displayName = displayName;
                        break;
                    }
                }
            }
            client.anInt7386 = client.anInt7347;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.CLIENT_SETVARCSTR_LARGE) {
            int key = buffer.readShortLE();
            String string_88 = buffer.readString();
            client.requestVarVerifyid();
            PulseEventQueue.priorityVarcstringUpdate(key, string_88);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.NPC_UPDATE) {
            NPCUpdate.decode(false);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.MESSAGE_FRIENDS_CHAT) {
            boolean bool91 = buffer.readUnsignedByte() == 1;
            String string_88 = buffer.readString();
            String str92 = string_88;
            if (bool91) {
                str92 = buffer.readString();
            }
            long long_28 = buffer.readLong();
            long long_30 = buffer.readUnsignedShort();
            long long_55 = buffer.read24BitUnsignedInteger();
            int i_35 = buffer.readUnsignedByte();
            long long_60 = (long_30 << 32) + long_55;
            boolean bool16 = false;
            int i_34 = 0;
            while (true) {
                if (i_34 >= 100) {
                    if (i_35 <= 1) {
                        if ((!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
                            if (IgnoreList.contains(str92)) {
                                bool16 = true;
                            }
                        } else {
                            bool16 = true;
                        }
                    }
                    break;
                }
                if (long_60 == client.aLongArray7424[i_34]) {
                    bool16 = true;
                    break;
                }
                ++i_34;
            }
            if (!bool16) {
                client.aLongArray7424[client.anInt7389] = long_60;
                client.anInt7389 = (client.anInt7389 + 1) % 100;
                String string_17 = TextUtils.atLtGtTags(HuffmanList.decode(buffer));
                if (i_35 != 2 && i_35 != 3) {
                    if (i_35 == 1) {
                        ChatBox.writeMessage(9, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + str92, string_88, string_17, TextUtils.decodeUidName(long_28), -1);
                    } else {
                        ChatBox.writeMessage(9, 0, string_88, str92, string_88, string_17, TextUtils.decodeUidName(long_28), -1);
                    }
                } else {
                    ChatBox.writeMessage(9, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + str92, string_88, string_17, TextUtils.decodeUidName(long_28), -1);
                }
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.OBJ_ANIM_SPECIFIC) {
            int tileHash = buffer.readIntV2();
            int plane = tileHash >> 28 & 0x3;
            int x = tileHash >> 14 & 0x3fff;
            int y = tileHash & 0x3fff;
            int animationId = buffer.readIntLE();
            int objFlags = buffer.readUnsigned128Byte();
            int shape = objFlags >> 2;
            int rotation = objFlags & 0x3;
            int type = client.OBJECT_TYPE_SLOTS[shape];
            CoordGrid base = Resource.mapRegion.getBaseCoordinates();
            x -= base.x;
            y -= base.y;
            SceneGraph.animateObject(plane, x, y, type, shape, rotation, animationId);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VORBIS_SPEECH_SOUND) {
            int id = buffer.readUnsignedShort();
            if (id == 65535) {
                id = -1;
            }
            int soundType = buffer.readUnsignedByte();
            int delay = buffer.readUnsignedShort();
            int i_7 = buffer.readUnsignedByte();
            AreaSoundPlayer.playSoundVorbis(id, soundType, delay, i_7, true, 256);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.UPDATE_IGNORE_LIST) {
            IgnoreList.IGNORE_COUNT = buffer.readUnsignedByte();
            for (int key = 0; key < IgnoreList.IGNORE_COUNT; key++) {
                Ignore class1089 = new Ignore();
                IgnoreList.IGNORED[key] = class1089;
                class1089.unfilteredUsername = buffer.readString();
                class1089.displayName = buffer.readString();
                class1089.temporary = false;
            }
            client.anInt7386 = client.anInt7347;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_SETRETEX) {
            int key = buffer.readShortLE();
            int flags = buffer.readUnsignedShort();
            int i_6 = buffer.readUnsigned128Byte();
            int i_7 = buffer.readIntLE();
            client.requestVarVerifyid();
            PulseEventQueue.priorityIfretex(i_7, i_6, flags, key);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.SET_TARGET) {
            client.ENTITY_TARGET = buffer.method13121(); //reads same identifier as projectile (index+1 for npc and -index-1 for player)
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.IF_OPENSUB_ACTIVE_OBJECT) {
            int interfaceId = buffer.readIntV2();
            int xtea3 = buffer.readIntV2();
            int overlay = buffer.readUnsignedByte128();
            int xtea2 = buffer.readInt();
            int xtea1 = buffer.readIntV1();
            int subInterfaceId = buffer.readUnsignedShort128();
            int bitpacked = buffer.readUnsigned128Byte();
            int type = bitpacked >> 2;
            int rotation = bitpacked & 0x3;
            int xtea4 = buffer.readInt();
            CoordGrid absolutePosition = new CoordGrid(buffer.readIntV2());
            int objectId = buffer.readInt();
            client.requestVarVerifyid();
            Interface.openSub(interfaceId, new IFSubActiveObject(subInterfaceId, overlay, new IFSubObjectPosition(absolutePosition, type, objectId)),
            		new int[]{xtea1, xtea2, xtea3, xtea4}, false);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.VARCLAN_DISABLE) {
            ClanVarTypeList.CLAN_VARS = null;
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.aClass3754437) {
            if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
                WindowMode.requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false);
            }
            byte[] bytes_64 = new byte[context.currentPacketSize];
            buffer.readBytes(bytes_64, context.currentPacketSize);
            String string_88 = Utils.readString(bytes_64, 0, context.currentPacketSize);
            String str92 = "opensn";
            if (!client.isApplet || !Browser.openBrowser(string_88, 1, str92)) {
                Browser.openBrowser(string_88, true, str92, client.aBool7158);
            }
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.OUTDATED_INTERFACE_PULSE_EVENT) {
            int key = buffer.readUnsignedShort128();
            int flags = buffer.readIntV2();
            int i_6 = buffer.readShortLE();
            client.requestVarVerifyid();
            PulseEventQueue.priorityT13Update(flags, key + (i_6 << 16));
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.RUN_CS2_SCRIPT) {
            String string_63 = buffer.readString();
            Object[] arr_26 = new Object[string_63.length() + 1];
            for (int i_6 = string_63.length() - 1; i_6 >= 0; --i_6) {
                if (string_63.charAt(i_6) == 115) {
                    arr_26[i_6 + 1] = buffer.readString();
                } else {
                    arr_26[i_6 + 1] = Integer.valueOf(buffer.readInt());
                }
            }
            arr_26[0] = Integer.valueOf(buffer.readInt());
            client.requestVarVerifyid();
            HookRequest hookrequest27 = new HookRequest();
            hookrequest27.params = arr_26;
            CS2Executor.executeHookInner(hookrequest27);
            context.currentPacket = null;
            return true;
        } else if (context.currentPacket == ServerProt.NPC_UPDATE_LARGE) {
            NPCUpdate.decode(true);
            context.currentPacket = null;
            return true;
        } else {
            PacketEncoder.sendErrorToServer((context.currentPacket != null ? context.currentPacket.opcode : -1) + "," + (context.secondLastPacket != null ? context.secondLastPacket.opcode : -1) + "," + (context.thirdLastPacket != null ? context.thirdLastPacket.opcode : -1) + " " + context.currentPacketSize, new RuntimeException());
            client.logout(false);
            return true;
        }
    }

    public static void decodeTilestreamPacket(UpdateZonePacket packet) {
        Packet.Bit buffer = client.GAME_CONNECTION_CONTEXT.receivedBuffer;
        if (packet == UpdateZonePacket.GROUND_ITEM_COUNT) {
            int i_3 = buffer.readUnsignedByte();
            CoordGrid coordgrid4 = Resource.mapRegion.getBaseCoordinates();
            int i_5 = (i_3 & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int y = i_5 + coordgrid4.y;
            int i_7 = (i_3 >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int x = i_7 + coordgrid4.x;
            int i_9 = buffer.readUnsignedShort();
            int i_10 = buffer.readUnsignedShort();
            int amount = buffer.readUnsignedShort();
            if (client.GROUND_ITEMS != null) {
                GroundItemWrapper class282Sub2912 = client.GROUND_ITEMS.get((long) UpdateZonePacket.UPDATE_ZONE_PLANE << 28 | (long) y << 14 | x);
                if (class282Sub2912 != null) {
                    for (GroundItemNode item = (GroundItemNode) class282Sub2912.groundItems.getFirst(); item != null; item = (GroundItemNode) class282Sub2912.groundItems.next()) {
                        if ((i_9 & 0x7fff) == item.id && i_10 == item.amount) {
                            item.unlink();
                            item.amount = amount;
                            GroundItemUpdating.addGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, item);
                            break;
                        }
                    }
                    if (i_7 >= 0 && i_5 >= 0 && i_7 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                        GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, i_7, i_5);
                    }
                }
            }
        } else if (packet == UpdateZonePacket.OBJECT_PREFETCH) {
            int i_3 = buffer.readInt();
            int i_21 = buffer.readUnsignedByte();
            Resource.mapRegion.getLoctypeList().list(i_3).isTypeCached(i_21);
        } else if (packet == UpdateZonePacket.SPOT_ANIM) {
            int bitpacked = buffer.readUnsignedByte();
            int x = (bitpacked >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int y = (bitpacked & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int spotAnimId = buffer.readUnsignedShort();
            if (spotAnimId == 65535) {
                spotAnimId = -1;
            }
            int height = buffer.readUnsignedShort();
            int speed = buffer.readUnsignedShort();
            int rotation = buffer.readUnsignedByte();
            if (Resource.mapRegion.getLoadingType() != RegionLoadingType.CUTSCENE && x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                if (spotAnimId == -1) {
                    PositionedGfx class282Sub50Sub1022 = client.ACTIVE_GFX.get((long) x << 16 | y);
                    if (class282Sub50Sub1022 != null) {
                        class282Sub50Sub1022.objectGfx.resetCycle();
                        class282Sub50Sub1022.unlink();
                    }
                } else {
                    int tileX = x * 512 + 256;
                    int tileY = y * 512 + 256;
                    int adjustedPlane = UpdateZonePacket.UPDATE_ZONE_PLANE;
                    if (adjustedPlane < 3 && Resource.mapRegion.getSettingsBits().areRoofsHidden(x, y)) {
                        adjustedPlane++;
                    }
                    SpotAnimation spotAnimation = new SpotAnimation(Resource.mapRegion.getScene(), spotAnimId, speed, UpdateZonePacket.UPDATE_ZONE_PLANE, adjustedPlane, tileX, MapRegion.tileheightForAbsolutePosition(tileX, tileY, UpdateZonePacket.UPDATE_ZONE_PLANE) - height, tileY, x, x, y, y, rotation, false);
                    client.ACTIVE_GFX.put(new PositionedGfx(spotAnimation), (long) x << 16 | y);
                }
            }
        } else if (packet == UpdateZonePacket.MAP_PROJANIM_HALFSQ) {
            int localCoordsHash = buffer.readUnsignedByte();
            int localX = (localCoordsHash >> 4 & 0xf) + UpdateZonePacket.UPDATE_ZONE_X * 2;
            int localY = UpdateZonePacket.UPDATE_ZONE_Y * 2 + (localCoordsHash & 0xf);
            int flags = buffer.readUnsignedByte();
            boolean adjustToTerrainHeight = (flags & 0x1) != 0;
            boolean adjustToBASAnimFrameHeight = (flags & 0x2) != 0;
            int basOffIdx = adjustToBASAnimFrameHeight ? flags >> 2 : -1;
            int xOff = localX + buffer.readByte();
            int yOff = localY + buffer.readByte();
            int source = buffer.readShort();
            int lockOn = buffer.readShort();
            int spotAnimId = buffer.readUnsignedShort();
            int startHeight = buffer.readUnsignedByte();
            if (adjustToBASAnimFrameHeight) {
                startHeight = (byte) startHeight;
            } else {
                startHeight *= 4;
            }
            int endHeight = buffer.readUnsignedByte() * 4;
            int startTime = buffer.readUnsignedShort();
            int endTime = buffer.readUnsignedShort();
            int angle = buffer.readUnsignedByte();
            int slope = buffer.readUnsignedShort();
            if (angle == 255) {
                angle = -1;
            }
            if (Resource.mapRegion.getLoadingType() != RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() * 2 && localY < Resource.mapRegion.getSceneSizeX() * 2 && xOff >= 0 && yOff >= 0 && xOff < Resource.mapRegion.getSceneSizeY() * 2 && yOff < Resource.mapRegion.getSceneSizeY() * 2 && spotAnimId != 65535) {
                localX *= 256;
                localY *= 256;
                xOff *= 256;
                yOff *= 256;
                startHeight <<= 2;
                endHeight <<= 2;
                slope <<= 2;
                ProjectileSpawnRequest.create(spotAnimId, source, lockOn, basOffIdx, startHeight, endHeight, localX, localY, xOff, yOff, startTime, endTime, angle, slope, adjustToTerrainHeight);
            }
        } else if (packet == UpdateZonePacket.MAP_PROJANIM) {
            int flags = buffer.readUnsignedByte();
            boolean useFloorHeight = (flags & 0x80) != 0;
            int localX = (flags >> 3 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int localY = (flags & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int xOff = localX + buffer.readByte();
            int yOff = localY + buffer.readByte();
            int lockOn = buffer.readShort();
            int spotAnimId = buffer.readUnsignedShort();
            int startHeight = buffer.readUnsignedByte() * 4;
            int endHeight = buffer.readUnsignedByte() * 4;
            int startTime = buffer.readUnsignedShort();
            int endTime = buffer.readUnsignedShort();
            int angle = buffer.readUnsignedByte();
            int slope = buffer.readUnsignedShort();
            if (angle == 255) {
                angle = -1;
            }
            if (Resource.mapRegion.getLoadingType() != RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY() && xOff >= 0 && yOff >= 0 && xOff < Resource.mapRegion.getSceneSizeX() && yOff < Resource.mapRegion.getSceneSizeY() && spotAnimId != 65535) {
                localX = localX * 512 + 256;
                localY = localY * 512 + 256;
                xOff = xOff * 512 + 256;
                yOff = yOff * 512 + 256;
                startHeight <<= 2;
                endHeight <<= 2;
                slope <<= 2;
                Projectile p = new Projectile(Resource.mapRegion.getScene(), spotAnimId, UpdateZonePacket.UPDATE_ZONE_PLANE, UpdateZonePacket.UPDATE_ZONE_PLANE, localX, localY, startHeight, startTime + client.cycles, endTime + client.cycles, angle, slope, 0, lockOn, endHeight, useFloorHeight, -1);
                p.start(xOff, yOff, MapRegion.tileheightForAbsolutePosition(xOff, yOff, UpdateZonePacket.UPDATE_ZONE_PLANE) - endHeight, startTime + client.cycles);
                client.PROJECTILES.addFirst(new ProjectileSpawnRequest(p));
            }
        } else if (packet == UpdateZonePacket.MIDI_SONG_LOCATION) {
            int i_3 = buffer.readUnsignedByte();
            int i_21 = (i_3 >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int i_5 = (i_3 & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int i_6 = buffer.readUnsignedShort();
            if (i_6 == 65535) {
                i_6 = -1;
            }
            int i_7 = buffer.readUnsignedByte();
            int i_8 = i_7 >> 4 & 0xf;
            int i_9 = i_7 & 0x7;
            int i_10 = buffer.readUnsignedByte();
            int i_11 = buffer.readUnsignedByte();
            int i_23 = buffer.readUnsignedShort();
            if (Resource.mapRegion.getLoadingType() != RegionLoadingType.CUTSCENE && i_21 >= 0 && i_5 >= 0 && i_21 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                int i_24 = i_8 + 1;
                if (PlayerUpdate.MY_PLAYER.movementBaseX[0] >= i_21 - i_24 && PlayerUpdate.MY_PLAYER.movementBaseX[0] <= i_21 + i_24 && PlayerUpdate.MY_PLAYER.movementBaseY[0] >= i_5 - i_24 && PlayerUpdate.MY_PLAYER.movementBaseY[0] <= i_24 + i_5) {
                    AreaSoundPlayer.playSoundSynth(i_6, i_9, i_10, i_11, i_8 + (UpdateZonePacket.UPDATE_ZONE_PLANE << 24) + (i_5 << 8) + (i_21 << 16), i_23);
                }
            }
        } else if (packet == UpdateZonePacket.DESTROY_OBJECT) {
            int display = buffer.readUnsignedByte128();
            int shape = display >> 2;
            int rotation = display & 0x3;
            int type = client.OBJECT_TYPE_SLOTS[shape];
            int coords = buffer.readUnsignedByte();
            int x = (coords >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int y = (coords & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            if (Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations() || x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                LocAction.destroyObject(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, type, -1, shape, rotation);
            }
        } else if (packet == UpdateZonePacket.REMOVE_GROUND_ITEM) {
            int i_3 = buffer.readUnsignedByteC();
            CoordGrid coordgrid = Resource.mapRegion.getBaseCoordinates();
            int i_5 = (i_3 & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int i_6 = i_5 + coordgrid.y;
            int i_7 = (i_3 >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int i_8 = i_7 + coordgrid.x;
            int i_9 = buffer.readUnsignedShortLE128();
            GroundItemWrapper class282Sub2939 = client.GROUND_ITEMS.get((long) UpdateZonePacket.UPDATE_ZONE_PLANE << 28 | (long) i_6 << 14 | i_8);
            if (class282Sub2939 != null) {
                for (GroundItemNode class282Sub3426 = (GroundItemNode) class282Sub2939.groundItems.getFirst(); class282Sub3426 != null; class282Sub3426 = (GroundItemNode) class282Sub2939.groundItems.next()) {
                    if ((i_9 & 0x7fff) == class282Sub3426.id) {
                        class282Sub3426.unlink();
                        break;
                    }
                }
                if (class282Sub2939.groundItems.hasNext()) {
                    class282Sub2939.unlink();
                }
                if (i_7 >= 0 && i_5 >= 0 && i_7 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                    GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, i_7, i_5);
                }
            }
        } else if (packet == UpdateZonePacket.CUSTOMIZE_OBJECT) {
            int locId = buffer.readInt();
            ObjectType locType = Resource.mapRegion.getLoctypeList().list(locId);
            int i_5 = buffer.readUnsignedByte128();
            int locShape = i_5 >> 2;
            int type = client.OBJECT_TYPE_SLOTS[locShape];
            int transformFlags = buffer.readUnsignedByte();
            int coords = buffer.readUnsignedByte128();
            int x = (coords >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int y = (coords & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            if (locShape == ObjectShapes.GROUND_INTERACT.shape) {
                locShape = ObjectShapes.SCENERY_INTERACT.shape;
            }
            int modelCount = 0;
            if (locType.shapes != null) {
                int i_24 = -1;
                for (int i_14 = 0; i_14 < locType.shapes.length; i_14++) {
                    if (locShape == locType.shapes[i_14]) {
                        i_24 = i_14;
                        break;
                    }
                }
                modelCount = locType.objectModelIds[i_24].length;
            }
            int recolCount = 0;
            if (locType.recolorDst != null) {
                recolCount = locType.recolorDst.length;
            }
            int retexCount = 0;
            if (locType.retextureDst != null) {
                retexCount = locType.retextureDst.length;
            }
            if ((transformFlags & 0x1) == 1) {
                LocAction.customizeObject(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, type, locId, locShape, null);
            } else {
                int[] locModelTransform = null;
                if ((transformFlags & 0x2) == 2) {
                    locModelTransform = new int[modelCount];
                    for (int index = 0; index < modelCount; index++) {
                        locModelTransform[index] = buffer.readInt();
                    }
                }
                short[] locColorTransform = null;
                if ((transformFlags & 0x4) == 4) {
                    locColorTransform = new short[recolCount];
                    for (int index = 0; index < recolCount; index++) {
                        locColorTransform[index] = (short) buffer.readUnsignedShort();
                    }
                }
                short[] locTextureTransform = null;
                if ((transformFlags & 0x8) == 8) {
                    locTextureTransform = new short[retexCount];
                    for (int index = 0; index < retexCount; index++) {
                        locTextureTransform[index] = (short) buffer.readUnsignedShort();
                    }
                }
                LocAction.customizeObject(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, type, locId, locShape, new ObjectTypeCustomisation(ObjectTypeCustomisation.locCustomisationId++, locModelTransform, locColorTransform, locTextureTransform));
            }
        } else if (packet == UpdateZonePacket.TILE_MESSAGE) {
            buffer.readUnsignedByte();
            int i_3 = buffer.readUnsignedByte();
            int i_21 = (i_3 >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int i_5 = (i_3 & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int i_6 = buffer.readUnsignedShort();
            int i_7 = buffer.readUnsignedByte();
            int i_8 = buffer.read24BitUnsignedInteger();
            String string_27 = buffer.readString();
            if (Resource.mapRegion.getLoadingType() != RegionLoadingType.CUTSCENE) {
                TileMessage.offer(UpdateZonePacket.UPDATE_ZONE_PLANE, i_21, i_5, i_7, i_6, i_8, string_27);
            }
        } else if (packet == UpdateZonePacket.SOUND_AREA) { //sound related again
            int flags = buffer.readUnsignedByte();
            int localX = (flags >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int localY = (flags & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int soundId = buffer.readUnsignedShort();
            if (soundId == 65535) {
                soundId = -1;
            }
            int i_7 = buffer.readUnsignedByte();
            int i_8 = i_7 >> 4 & 0xf;
            int timesRepeated = i_7 & 0x7;
            int delay = buffer.readUnsignedByte();
            int volume = buffer.readUnsignedByte();
            int sampleRate = buffer.readUnsignedShort();
            boolean voiceEffect = buffer.readUnsignedByte() == 1;
            if (Resource.mapRegion.getLoadingType() != RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0
            		&& localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY()) {
                int i_14 = i_8 + 1;
                if (PlayerUpdate.MY_PLAYER.movementBaseX[0] >= localX - i_14 && PlayerUpdate.MY_PLAYER.movementBaseX[0] <= localX + i_14
                		&& PlayerUpdate.MY_PLAYER.movementBaseY[0] >= localY - i_14 && PlayerUpdate.MY_PLAYER.movementBaseY[0] <= i_14 + localY) {
                    AreaSoundPlayer.playSoundVorbis(soundId, timesRepeated, delay, volume,
                    		i_8 + (UpdateZonePacket.UPDATE_ZONE_PLANE << 24) + (localY << 8) + (localX << 16), voiceEffect, sampleRate);
                }
            }
        } else if (packet == UpdateZonePacket.OBJ_ANIM) {
            int i_3 = buffer.readUnsigned128Byte();
            int type = i_3 >> 2;
            int rotation = i_3 & 0x3;
            int slot = client.OBJECT_TYPE_SLOTS[type];
            int i_7 = buffer.readUnsigned128Byte();
            int x = (i_7 >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int y = (i_7 & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int animationId = buffer.readIntLE();
            if (Resource.mapRegion.getLoadingType() != RegionLoadingType.CUTSCENE) {
                SceneGraph.animateObject(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, slot, type, rotation, animationId);
            }
        } else if (packet == UpdateZonePacket.GROUND_ITEM_REVEAL) {
            int playerId = buffer.readUnsignedShort();
            int i_21 = buffer.readUnsignedByte();
            CoordGrid coordgrid25 = Resource.mapRegion.getBaseCoordinates();
            int localY = (i_21 & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int y = localY + coordgrid25.y;
            int localX = (i_21 >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int x = localX + coordgrid25.x;
            int amount = buffer.readUnsignedShortLE128();
            int id = buffer.readUnsignedShortLE128();
            if (playerId != PlayerUpdate.myPlayerIndex) {
                boolean withinSceneRange = localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY();
                if (withinSceneRange || Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations()) {
                    GroundItemUpdating.addGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, new GroundItemNode(id, amount));
                    if (withinSceneRange) {
                        GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, localX, localY);
                    }
                }
            }
        } else if (packet == UpdateZonePacket.CREATE_GROUND_ITEM) {
            int amount = buffer.readUnsignedShortLE128();
            int id = buffer.readUnsignedShort128();
            int i_5 = buffer.readUnsignedByteC();
            CoordGrid coordgrid31 = Resource.mapRegion.getBaseCoordinates();
            int localY = (i_5 & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int y = localY + coordgrid31.y;
            int localX = (i_5 >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int x = localX + coordgrid31.x;
            boolean bool36 = localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY();
            if (bool36 || Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations()) {
                GroundItemUpdating.addGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, new GroundItemNode(id, amount));
                if (bool36) {
                    GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, localX, localY);
                }
            }
        } else if (packet == UpdateZonePacket.CREATE_OBJECT) {
            int i_3 = buffer.readInt();
            int i_21 = buffer.readUnsigned128Byte();
            int i_5 = (i_21 >> 4 & 0x7) + UpdateZonePacket.UPDATE_ZONE_X;
            int i_6 = (i_21 & 0x7) + UpdateZonePacket.UPDATE_ZONE_Y;
            int i_7 = buffer.readUnsigned128Byte();
            int i_8 = i_7 >> 2;
            int i_9 = i_7 & 0x3;
            int i_10 = client.OBJECT_TYPE_SLOTS[i_8];
            if (Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations() || i_5 >= 0 && i_6 >= 0 && i_5 < Resource.mapRegion.getSceneSizeX() && i_6 < Resource.mapRegion.getSceneSizeY()) {
                LocAction.destroyObject(UpdateZonePacket.UPDATE_ZONE_PLANE, i_5, i_6, i_10, i_3, i_8, i_9);
            }
        } else {
            PacketEncoder.sendErrorToServer("" + packet, new RuntimeException());
            client.logout(false);
        }
    }

    public static void decodeWorldList(boolean refresh, byte[] data) {
        if (WorldMeta.WORLD_LIST_BUFFER == null) {
            WorldMeta.WORLD_LIST_BUFFER = new Packet(20000);
        }
        WorldMeta.WORLD_LIST_BUFFER.writeBytes(data, 0, data.length);
        if (refresh) {
            WorldDescriptor.decodeWorldList(WorldMeta.WORLD_LIST_BUFFER.buffer);
            WorldConnection.WORLD_DESCRIPTORS_BYID = new WorldDescriptor[WorldMeta.WORLD_LIST_SIZE];
            int i_3 = 0;
            for (int i_4 = WorldDescriptor.WORLD_LIST_START; i_4 <= WorldMeta.WORLD_LIST_SIZEPLUS1; i_4++) {
                WorldDescriptor world = WorldDescriptor.getWorld(i_4);
                if (world != null) {
                    WorldConnection.WORLD_DESCRIPTORS_BYID[i_3++] = world;
                }
            }
            WorldConnection.aBool5422 = false;
            WorldConnection.aLong5425 = Utils.time();
            WorldMeta.WORLD_LIST_BUFFER = null;
        }
    }

	public static HostNameIdentifier HOSTNAME_IDENTIFIER;
}
