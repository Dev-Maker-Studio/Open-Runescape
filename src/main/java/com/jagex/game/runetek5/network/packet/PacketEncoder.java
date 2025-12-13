package com.jagex.game.runetek5.network.packet;

import java.io.DataInputStream;
import java.net.URL;

import com.darkan.bot.Bot;
import com.darkan.bot.UtilKt;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.JFS32;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.console.Console;
import com.jagex.game.runetek5.client.input.keyboard.KeyDownCommand;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer;
import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.CheckEmailRequest;
import com.jagex.game.runetek5.client.login.CreateAccountRequest;
import static com.jagex.game.runetek5.client.menu.MiniMenuBuilderKt.USE_OPTIONS_FLAGS;
import com.jagex.game.runetek5.client.periodic.PulseEventQueue;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.midi.SongReference;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.HookRequest;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.config.varc.VarcTypeList;
import com.jagex.game.runetek5.encoding.HuffmanList;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannelPlayer;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder;
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.world.WorldMeta;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.tools.DebugDisplay;
import com.jagex.game.runetek5.tools.Isaac;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.TextUtils;
import com.jagex.game.runetek5.utils.Utils;

public class PacketEncoder {

    public static void sendJoinFriendchatPacket() {
        BufferedConnectionContext connectionContext = client.getConnectionContext();
        TcpPacket tcpPacket = createPacket(ClientProt.FC_JOIN, connectionContext.isaac);
        tcpPacket.buffer.writeByte(0);
        connectionContext.queuePacket(tcpPacket);
    }

    public static void method2074(String string_0, String string_1, int i_2, boolean bool3) {
        if (client.gameState == GameState.IN_ACCOUNT_CREATION) {
            TcpPacket tcpmessage5 = createPacket(ClientProt.SEND_SIGN_UP_FORM, client.LOBBY_CONNECTION_CONTEXT.isaac);
            tcpmessage5.buffer.writeShort(0);
            int i_6 = tcpmessage5.buffer.currentPosition;
            tcpmessage5.buffer.writeString(string_0);
            tcpmessage5.buffer.writeString(string_1);
            tcpmessage5.buffer.writeByte(i_2);
            tcpmessage5.buffer.writeByte(bool3 ? 1 : 0);
            tcpmessage5.buffer.currentPosition += 7;
            tcpmessage5.buffer.encryptXtea(AccountCreationStage.LOGIN_XTEAS, i_6, tcpmessage5.buffer.currentPosition);
            tcpmessage5.buffer.writeShortLength(tcpmessage5.buffer.currentPosition - i_6);
            client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage5);
            if (i_2 < 13) {
                client.aBool7189 = true;
            }

            AccountCreationStage.aClass4942916 = CreateAccountRequest.aClass4945790;
        }

    }

    public static void method2075(int i_0, String string_1) {
        int i_3 = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
        int[] ints_4 = PlayerUpdate.OUT_PLAYER_INDICES;
        boolean bool5 = false;

        for (int i_6 = 0; i_6 < i_3; i_6++) {
            PlayerEntity player7 = PlayerUpdate.players[ints_4[i_6]];
            if (player7 != null && player7 != PlayerUpdate.MY_PLAYER && player7.displayName != null && player7.displayName.equalsIgnoreCase(string_1)) {
                ClientProt outgoingpacket8 = null;
                if (i_0 == 1) {
                    outgoingpacket8 = ClientProt.PLAYER_OP1;
                } else if (i_0 == 4) {
                    outgoingpacket8 = ClientProt.PLAYER_OP4;
                } else if (i_0 == 5) {
                    outgoingpacket8 = ClientProt.PLAYER_OP5;
                } else if (i_0 == 6) {
                    outgoingpacket8 = ClientProt.PLAYER_OP6;
                } else if (i_0 == 7) {
                    outgoingpacket8 = ClientProt.PLAYER_OP7;
                } else if (i_0 == 9) {
                    outgoingpacket8 = ClientProt.PLAYER_OP9;
                }

                if (outgoingpacket8 != null) {
                    TcpPacket tcpmessage9 = createPacket(outgoingpacket8, client.GAME_CONNECTION_CONTEXT.isaac);
                    tcpmessage9.buffer.writeShort(ints_4[i_6]);
                    tcpmessage9.buffer.writeByteSubtract(0);
                    client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage9);
                }

                bool5 = true;
                break;
            }
        }

        if (!bool5) {
            ChatBox.writeGameMessage(LocalizedText.UNABLE_TO_FIND.translate(client.CURRENT_LANGUAGE) + string_1);
        }

    }

    public static void method2076(Component icomponentdefinitions0, Component icomponentdefinitions1) {
        TcpPacket tcpmessage3 = createPacket(ClientProt.IF_DRAG_ONTO_IF, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage3.buffer.writeShortAddLittle(icomponentdefinitions1.slotId);
        tcpmessage3.buffer.writeShortLittle(icomponentdefinitions0.slotId);
        tcpmessage3.buffer.writeShort(icomponentdefinitions1.containerItemId);
        tcpmessage3.buffer.writeShortAddLittle(icomponentdefinitions0.containerItemId);
        tcpmessage3.buffer.writeIntMiddle(icomponentdefinitions0.idHash);
        tcpmessage3.buffer.writeIntLittle(icomponentdefinitions1.idHash);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage3);
    }

    public static void method2590(Component icomponentdefinitions0) {
        if (client.interfaceSelectionActive) {
            if (icomponentdefinitions0.anObjectArray1393 != null) {
                Component icomponentdefinitions2 = Component.listIcomponent(client.anInt56, client.anInt7345);
                if (icomponentdefinitions2 != null) {
                    HookRequest hookrequest3 = new HookRequest();
                    hookrequest3.source = icomponentdefinitions0;
                    hookrequest3.aClass1188057 = icomponentdefinitions2;
                    hookrequest3.params = icomponentdefinitions0.anObjectArray1393;
                    CS2Executor.executeHookInner(hookrequest3);
                }
            }
            TcpPacket tcpmessage4 = createPacket(ClientProt.IF_ON_IF, client.GAME_CONNECTION_CONTEXT.isaac);
            Console.writeLine("IF_ON_IF: [" + UtilKt.interfaceIdFromHash(client.anInt56) + ", " + UtilKt.componentIdFromHash(client.anInt56) + ", " + client.anInt7345 + ", " + client.anInt7346 + "] -> [" + UtilKt.interfaceIdFromHash(icomponentdefinitions0.idHash) + ", " + UtilKt.componentIdFromHash(icomponentdefinitions0.idHash) + ", " + icomponentdefinitions0.slotId + ", " + icomponentdefinitions0.containerItemId + "]");
            tcpmessage4.buffer.writeShortAddLittle(icomponentdefinitions0.slotId);
            tcpmessage4.buffer.writeShortLittle(client.anInt7345);
            tcpmessage4.buffer.writeShortAddLittle(icomponentdefinitions0.containerItemId);
            tcpmessage4.buffer.writeIntLittle(icomponentdefinitions0.idHash);
            tcpmessage4.buffer.writeIntInverseMiddle(client.anInt56);
            tcpmessage4.buffer.writeShortLittle(client.anInt7346);
            client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage4);
        }
    }

    public static void method3675() {
        if (SongPlayer.currentSong != null && !SongPlayer.currentSong.isLoaded() && SongPlayer.currentSong.initialize()) {
            TcpPacket tcpmessage1 = createPacket(ClientProt.SONG_LOADED, client.GAME_CONNECTION_CONTEXT.isaac);
            tcpmessage1.buffer.writeInt(SongPlayer.currentSong.musicId);
            client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage1);
        }
    }

    public static TcpPacket createPacket(ClientProt clientprot, Isaac isaac) {
        TcpPacket packet = TcpPacket.createPacket();
        packet.packet = clientprot;
        packet.position = clientprot.size;
        if (packet.position == -1) {
            packet.buffer = new Packet.Bit(260);
        } else if (packet.position == -2) {
            packet.buffer = new Packet.Bit(10000);
        } else if (packet.position <= 18) {
            packet.buffer = new Packet.Bit(20);
        } else if (packet.position <= 98) {
            packet.buffer = new Packet.Bit(100);
        } else {
            packet.buffer = new Packet.Bit(260);
        }
        packet.buffer.setIsaacCipher(isaac);
        packet.buffer.writeIsaacByte(packet.packet.id);
        packet.capacity = 0;
        return packet;
    }

    public static void preloadSong(int songId) {
        TcpPacket tcpmessage2 = createPacket(ClientProt.SONG_LOADED, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage2.buffer.writeInt(-1);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage2);
        SongPlayer.currentSong = new SongReference(Resource.MUSIC, songId);
    }

    public static void method12590(TcpPacket packet, int interfaceHash, int slotId, int itemId) {
        Console.writeLine(packet.packet + ": interfaceId: " + UtilKt.interfaceIdFromHash(interfaceHash) + " componentId: " + UtilKt.componentIdFromHash(interfaceHash) + " slotId: " + slotId + " itemId: " + itemId);
        packet.buffer.writeIntLittle(interfaceHash);
        packet.buffer.writeShort(itemId);
        packet.buffer.writeShortAdd(slotId);
    }

    public static void decodePlayerCounts(Packet rsbytebuffer0) {
        for (int i_2 = 0; i_2 < WorldMeta.WORLD_LIST_SIZE; i_2++) {
            int worldId = rsbytebuffer0.readUSmart();
            int playerCount = rsbytebuffer0.readUShort();
            if (playerCount == 65535) {
                playerCount = -1;
            }
            if (WorldMeta.WORLD_LIST_DESCRIPTORS[worldId] != null) {
                WorldMeta.WORLD_LIST_DESCRIPTORS[worldId].playerCount = playerCount;
            }
        }
    }

    public static void method6811(int i_0) {
        if (client.CLAN_CHANNEL != null && i_0 >= 0 && i_0 < client.CLAN_CHANNEL.numPlayers) {
            ClanChannelPlayer class572 = client.CLAN_CHANNEL.players[i_0];
            if (class572.rank == -1) {
                BufferedConnectionContext class1843 = client.getConnectionContext();
                TcpPacket tcpmessage4 = createPacket(ClientProt.UNUSED_CLAN_OP, class1843.isaac);
                tcpmessage4.buffer.writeByte(2 + TextUtils.getLength(class572.name));
                tcpmessage4.buffer.writeShort(i_0);
                tcpmessage4.buffer.writeString(class572.name);
                class1843.queuePacket(tcpmessage4);
            }
        }

    }

    public static void sendTransmitVarVerifyid() {
        TcpPacket tcpmessage1 = createPacket(ClientProt.TRANSMITVAR_VERIFYID, client.GAME_CONNECTION_CONTEXT.isaac);
        tcpmessage1.buffer.writeInt(client.varVerifyId);
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage1);
    }

    public static void sendCheckEmailPacket(String string_0) {
        if (client.gameState == GameState.IN_ACCOUNT_CREATION) {
            TcpPacket message = createPacket(ClientProt.CHECK_EMAIL_VALIDITY, client.LOBBY_CONNECTION_CONTEXT.isaac);
            message.buffer.writeShort(0);
            int start = message.buffer.currentPosition;
            message.buffer.writeString(string_0);
            message.buffer.currentPosition += 7;
            message.buffer.encryptXtea(AccountCreationStage.LOGIN_XTEAS, start, message.buffer.currentPosition);
            message.buffer.writeShortLength(message.buffer.currentPosition - start);
            client.LOBBY_CONNECTION_CONTEXT.queuePacket(message);
            AccountCreationStage.checkemailRequeststatus = CheckEmailRequest.SENT_REQUEST;
        }

    }

    public static void method11404(String string_0, String string_1, String string_2, boolean bool3, boolean bool4) {
        BufferedConnectionContext class1846 = client.getConnectionContext();
        if (class1846.getConnection() != null) {
            TcpPacket tcpmessage7 = createPacket(ClientProt.LOBBY_HYPERLINK, class1846.isaac);
            tcpmessage7.buffer.writeShort(TextUtils.getLength(string_0) + TextUtils.getLength(string_1) + TextUtils.getLength(string_2) + 1);
            tcpmessage7.buffer.writeString(string_0);
            tcpmessage7.buffer.writeString(string_1);
            tcpmessage7.buffer.writeString(string_2);
            int i_8 = 0;
            if (bool3) {
                i_8 |= 0x1;
            }
            if (bool4) {
                i_8 |= 0x2;
            }
            tcpmessage7.buffer.writeByte(i_8);
            class1846.queuePacket(tcpmessage7);
        }
    }

    public static void method3345(String string_0) {
        if (!string_0.isEmpty()) {
            BufferedConnectionContext class1842 = client.getConnectionContext();
            TcpPacket tcpmessage3 = createPacket(ClientProt.FC_JOIN, class1842.isaac);
            tcpmessage3.buffer.writeByte(TextUtils.getLength(string_0));
            tcpmessage3.buffer.writeString(string_0);
            class1842.queuePacket(tcpmessage3);
        }
    }

    public static TcpPacket walkToTile(int x, int y, int movementType) {
        TcpPacket packet = null;
        if (movementType == 0) {
            packet = createPacket(ClientProt.WALK, client.GAME_CONNECTION_CONTEXT.isaac);
        }

        if (movementType == 1) {
            packet = createPacket(ClientProt.MINI_WALK, client.GAME_CONNECTION_CONTEXT.isaac);
        }

        CoordGrid base = Resource.mapRegion.getBaseCoordinates();
        packet.buffer.writeByte(KeyDownCommand.runKeyDown() ? 1 : 0);
        packet.buffer.writeShort(x + base.x);
        packet.buffer.writeShortLittle(y + base.y);
        ComponentMinimap.minimapFlagX = x;
        ComponentMinimap.minimapFlagY = y;
        ComponentMinimap.minimapContainsFlag = false;
        PulseEventQueue.delayedClearMinimapFlagUpdate();
        return packet;
    }

    public static void kickPlayer(boolean mainCC, int playerIndex) {
        ClanChannel cc = mainCC ? client.CLAN_CHANNEL : client.LISTENED_CLAN_CHANNEL;
        if (cc != null && playerIndex >= 0 && playerIndex < cc.numPlayers) {
            ClanChannelPlayer ccPlayer = cc.players[playerIndex];
            if (ccPlayer.rank == -1) {
                String ccPlayerName = ccPlayer.name;
                BufferedConnectionContext connection = client.getConnectionContext();
                TcpPacket packet = createPacket(ClientProt.CLANCHANNEL_KICKUSER, connection.isaac);
                packet.buffer.writeByte(3 + TextUtils.getLength(ccPlayerName));
                packet.buffer.writeByte(mainCC ? 1 : 0);
                packet.buffer.writeShort(playerIndex);
                packet.buffer.writeString(ccPlayerName);
                connection.queuePacket(packet);
            }
        }

    }

    public static void closeInterface() {
        TcpPacket packet = createPacket(ClientProt.CLOSE_INTERFACE, client.GAME_CONNECTION_CONTEXT.isaac);
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
        for (SubInterface subInterface = client.OPEN_INTERFACES.first(); subInterface != null; subInterface = client.OPEN_INTERFACES.next()) {
            if (!subInterface.linked()) {
                subInterface = client.OPEN_INTERFACES.first();
                if (subInterface == null) {
                    break;
                }
            }
            if (subInterface.overlay == 0) {
                Interface.closeChildInterface(subInterface, true, true);
            }
        }
        if (client.aClass1187352 != null) {
            Component.redrawComponent(client.aClass1187352);
            client.aClass1187352 = null;
        }
    }

    public static int chatMessageToBuffer(Packet buffer, String chatMessage) {
        int length = buffer.currentPosition;
        byte[] data = Utils.characterSequenceToBytearray(chatMessage);
        buffer.writeSmart(data.length);
        buffer.currentPosition += HuffmanList.HUFFMAN.decompress(data, 0, data.length, buffer.buffer, buffer.currentPosition);
        return buffer.currentPosition - length;
    }

	public static void reportBug(int i_0, String string_1, String string_2) {
		TcpPacket packet = createPacket(ClientProt.BUG_REPORT, client.GAME_CONNECTION_CONTEXT.isaac);
		packet.buffer.writeShort(1 + Utils.stringLengthPlus2(string_1) + Utils.stringLengthPlus2(string_2));
		packet.buffer.writeByte(i_0);
		packet.buffer.writeJagString(string_2);
		packet.buffer.writeJagString(string_1);
		client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
	}

    public static int[] generateSeed(TcpPacket tcpmessage0) {
        Packet rsbytebuffer2 = new Packet(518);
        int[] ints_3 = new int[4];
        int i_4;
        for (i_4 = 0; i_4 < 4; i_4++) {
            ints_3[i_4] = (int) (Math.random() * 9.9999999E7D);
        }
        rsbytebuffer2.writeByte(10);
        rsbytebuffer2.writeInt(ints_3[0]);
        rsbytebuffer2.writeInt(ints_3[1]);
        rsbytebuffer2.writeInt(ints_3[2]);
        rsbytebuffer2.writeInt(ints_3[3]);
        for (i_4 = 0; i_4 < 10; i_4++) {
            rsbytebuffer2.writeInt((int) (Math.random() * 9.9999999E7D));
        }
        rsbytebuffer2.writeShort((int) (Math.random() * 9.9999999E7D));
        rsbytebuffer2.applyRSA();
        tcpmessage0.buffer.writeBytes(rsbytebuffer2.buffer, 0, rsbytebuffer2.currentPosition);
        return ints_3;
    }

    public static void sendWindowmodePreferenceToServer() {
        TcpPacket clientServerPacket = createPacket(ClientProt.SCREEN_SIZE, client.GAME_CONNECTION_CONTEXT.isaac);
        clientServerPacket.buffer.writeByte(WindowMode.getWindowMode());
        clientServerPacket.buffer.writeShort(GameShell.screenCanvasWidth);
        clientServerPacket.buffer.writeShort(client.screenCanvasHeight * -969250379);
        clientServerPacket.buffer.writeByte(PreferenceManager.preferences.antiAliasingPreview.getValue());
        client.GAME_CONNECTION_CONTEXT.queuePacket(clientServerPacket);
    }

	public static void writeVarcPreferences() {
	    JFS32 preferenceFile = null;
	    try {
	        preferenceFile = PreferenceManager.loadPreferenceFile("2", client.CURRENT_GAME.name, true);
	        Packet buffer = new Packet(client.VARCS_BOOLS_LOADED * 6 + 3);
	        buffer.writeByte(1);
	        buffer.writeShort(client.VARCS_BOOLS_LOADED);
	        for (int i_3 = 0; i_3 < VarcTypeList.VARC_INT.length; i_3++) {
	            if (VarcTypeList.VARC_BOOL[i_3]) {
	                buffer.writeShort(i_3);
	                buffer.writeInt(VarcTypeList.VARC_INT[i_3]);
	            }
	        }
	        preferenceFile.write(buffer.buffer, 0, buffer.currentPosition);
	    } catch (Exception exception) {
	    	exception.printStackTrace();
	    }
	    try {
	        if (preferenceFile != null) {
	            preferenceFile.close();
	        }
	    } catch (Exception exception) {
	    	exception.printStackTrace();
	    }
	    client.exitTime = Utils.time();
	    client.clientIsActive = false;
	}

	public static void submitQueuedPackets() {
	    MouseActionServer.cycle();
	    MouseActionServer.MOVE_PACKET.submitAll();
	    MouseActionServer.CLICK_PACKET.submitAll();
	    TcpPacket tcpPacket;
	    if (client.maximumHeldKeys > 0) {
	        tcpPacket = createPacket(ClientProt.KEY_PRESS, client.GAME_CONNECTION_CONTEXT.isaac);
	        tcpPacket.buffer.writeShort(client.maximumHeldKeys * 4);
	
	        for (int index = 0; index < client.maximumHeldKeys; index++) {
	            KeyRecord record = client.KEY_RECORDS[index];
	            long timeBetweenClicks = record.getTimeSubmitted() - MouseActionServer.lastSubmitted;
	            if (timeBetweenClicks > 16777215L) {
	                timeBetweenClicks = 16777215L;
	            }
	
	            MouseActionServer.lastSubmitted = record.getTimeSubmitted();
	            tcpPacket.buffer.writeByte(record.getCode());
	            tcpPacket.buffer.writeMedium((int) timeBetweenClicks);
	        }
	
	        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket);
	    }
	
	    if (StaticMethods.anInt2912 > 0) {
	        StaticMethods.anInt2912--;
	    }
	
	    if (client.sendCameraAngleToServer && StaticMethods.anInt2912 <= 0) {
	        StaticMethods.anInt2912 = 20;
	        client.sendCameraAngleToServer = false;
	        tcpPacket = createPacket(ClientProt.MOVE_CAMERA, client.GAME_CONNECTION_CONTEXT.isaac);
	        tcpPacket.buffer.writeShortAddLittle((int) Camera.camAngleX >> 3);
	        tcpPacket.buffer.writeShortAdd((int) Camera.camAngleY >> 3);
	        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket);
	    }
	
	    if (MouseActionServer.appletHasFocus != GameShell.appletHasFocus) {
	        MouseActionServer.appletHasFocus = GameShell.appletHasFocus;
	        tcpPacket = createPacket(ClientProt.CLIENT_FOCUS, client.GAME_CONNECTION_CONTEXT.isaac);
	        tcpPacket.buffer.writeByte(GameShell.appletHasFocus ? 1 : 0);
	        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket);
	    }
	
	    if (!client.preferencesSavedOnServer) {
	        tcpPacket = createPacket(ClientProt.SEND_PREFERENCES, client.GAME_CONNECTION_CONTEXT.isaac);
	        tcpPacket.buffer.writeByte(0);
	        int position = tcpPacket.buffer.currentPosition;
	        Packet buffer = PreferenceManager.preferences.encode();
	        tcpPacket.buffer.writeBytes(buffer.buffer, 0, buffer.currentPosition);
	        tcpPacket.buffer.writeIndex(tcpPacket.buffer.currentPosition - position);
	        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket);
	        client.preferencesSavedOnServer = true;
	    }	
	}

	public static void continueInterfacePressed(int i_0, int i_1) {
        Console.writeLine("IF_CONTINUE interfaceId: " + UtilKt.interfaceIdFromHash(i_0) + " componentId: " + UtilKt.componentIdFromHash(i_0) + " slotId: " + i_1);
	    TcpPacket packet = createPacket(ClientProt.IF_CONTINUE, client.GAME_CONNECTION_CONTEXT.isaac);
	    packet.buffer.writeIntMiddle(i_0);
	    packet.buffer.writeShortAddLittle(i_1);
	    client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
	}

	public static void method4167(String string_0) {
	    if (client.interfaceSelectionActive && (USE_OPTIONS_FLAGS & 0x18) != 0) {
	        boolean bool2 = false;
	        int count = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
	        int[] indices = PlayerUpdate.OUT_PLAYER_INDICES;
	        for (int i = 0; i < count; i++) {
	            PlayerEntity player6 = PlayerUpdate.players[indices[i]];
	            if (player6.displayName != null && player6.displayName.equalsIgnoreCase(string_0) && (player6 == PlayerUpdate.MY_PLAYER && (USE_OPTIONS_FLAGS & 0x10) != 0 || (USE_OPTIONS_FLAGS & 0x8) != 0)) {
	                TcpPacket packet = createPacket(ClientProt.IF_ON_PLAYER, client.GAME_CONNECTION_CONTEXT.isaac);
	                packet.buffer.writeShort(client.anInt7345);
	                packet.buffer.writeShortLittle(indices[i]);
	                packet.buffer.writeByteSubtract(0);
	                packet.buffer.writeIntInverseMiddle(client.anInt56);
	                packet.buffer.writeShortLittle(client.anInt7346);
	                client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
	                int size = player6.getSize();
	                RouteFinder.calculateRoute(player6.movementBaseX[0], player6.movementBaseY[0], true, RouteStrategies.forPlayer(player6.movementBaseX[0], player6.movementBaseY[0], size, size));
	                bool2 = true;
	                break;
	            }
	        }
	        if (!bool2) {
	            ChatBox.writeGameMessage(LocalizedText.UNABLE_TO_FIND.translate(client.CURRENT_LANGUAGE) + string_0);
	        }
	        if (client.interfaceSelectionActive) {
	            InterfaceComponentRenderer.cancelInterfaceSelection();
	        }
	    }
	}

	public static void sendErrorToServer(String addition, Throwable throwable) {
	    try {
	        String errorString = "";
	        if (throwable != null) {
	            errorString = DebugDisplay.getStringFromThrowable(throwable);
	        }
	        if (addition != null) {
	            if (throwable != null) {
	                errorString += " | ";
	            }
	            errorString += addition;
	        }
	        CS2Executor.logError(errorString);
	        errorString = Utils.formatRequestParameter(errorString);
	        if (StaticMethods.initialApplet != null) {
	            String vendor = "Unknown";
	            String version = "1.1";
	            try {
	                vendor = System.getProperty("java.vendor");
	                version = System.getProperty("java.version");
	            } catch (Exception exception) {
	            	// NOOP
	            }
	            URL url = new URL(StaticMethods.initialApplet.getCodeBase(), "clienterror.ws?c=" + SystemInfo.clientBuild + "&cs=" + SystemInfo.clientBuildSub + "&u=" + (PlayerUpdate.MY_PLAYER_USERNAME != null ? Utils.formatRequestParameter(PlayerUpdate.MY_PLAYER_USERNAME) : "" + StaticMethods.aLong10459) + "&v1=" + Utils.formatRequestParameter(vendor) + "&v2=" + Utils.formatRequestParameter(version) + "&e=" + errorString);
	            DataInputStream dataInputStream = new DataInputStream(url.openStream());
	            dataInputStream.read();
	            dataInputStream.close();
	        }
	    } catch (Exception exception) {
	        exception.printStackTrace();
	    }
	}

	public static void method16088(int i_0) {
	    if (client.gameState == GameState.IN_ACCOUNT_CREATION) {
	        TcpPacket packet = createPacket(ClientProt.ACCOUNT_CREATION_STAGE, client.LOBBY_CONNECTION_CONTEXT.isaac);
	        packet.buffer.writeByte(i_0);
	        client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet);
	    }
	}

	public static void queueCutsceneExit(boolean bool0) {
	    if (client.MAP_LOADING_STAGE != 2 && client.MAP_LOADING_STAGE != 1) {
	        if (!bool0) {
	            CutsceneAction[] arr_2 = CutsceneTypeList.CUTSCENE_MISC_ACTIONS;
	
	            for (int i_3 = 0; i_3 < arr_2.length; i_3++) {
	                CutsceneAction class964 = arr_2[i_3];
	                class964.method1593();
	            }
	        }
	
	        client.MAP_LOADING_STAGE = 2;
	        MapRegion.CUTSCENE_MAP_XTEAS = null;
	        CutsceneTypeList.CUTSCENE_BUFFER = null;
	        client.aBool7321 = false;
	        CutsceneTypeList.resetCutsceneConfigs();
	        TcpPacket cutsceneExitPacket = createPacket(ClientProt.CUTSCENE_FINISHED, client.GAME_CONNECTION_CONTEXT.isaac);
	        cutsceneExitPacket.buffer.writeByte(bool0 ? 1 : 0);
	        client.GAME_CONNECTION_CONTEXT.queuePacket(cutsceneExitPacket);
	    }
	
	}
}
