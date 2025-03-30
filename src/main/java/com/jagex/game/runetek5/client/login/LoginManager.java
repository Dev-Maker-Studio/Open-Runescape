package com.jagex.game.runetek5.client.login;

import java.io.IOException;

import com.jagex.game.runetek5.botdetection.ReflectionCheckManager;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.FileOnDisk;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.QueueIterator;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.JSObjectManager;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.periodic.PulseEventHandler;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer;
import com.jagex.game.runetek5.config.varc.VarcTypeList;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.interaction.FriendList;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat;
import com.jagex.game.runetek5.network.AsyncConnection;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.ConnectionInfo;
import com.jagex.game.runetek5.network.HostNameIdentifier;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.network.WorldConnection;
import com.jagex.game.runetek5.network.packet.GrandExchangeSlot;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.ServerProt;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.network.world.WorldMeta;
import com.jagex.game.runetek5.scene.RegionLoadRequest;
import com.jagex.game.runetek5.scene.LocAction;
import com.jagex.game.runetek5.scene.RegionLoadingType;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.tools.Browser;
import com.jagex.game.runetek5.tools.Isaac;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.Utils;
import com.jagex.game.runetek5.vardomain.VarProviders;

import static com.jagex.game.launch.Loader.MAJOR_BUILD;
import static com.jagex.game.launch.Loader.MINOR_BUILD;

public class LoginManager {

	public enum LoginStage {
		NONE_2,
		UNK_18,
		UNK_35,
		UNK_46,
		UNK_57,
		UNK_65,
		UNK_75,
		UNK_80,
		UNK_97,
		UNK_105,
		UNK_115,
		UNK_128,
		UNK_133,
		UNK_140,
		UNK_157,
		UNK_167,
		UNK_173,
		UNK_180,
		UNK_196,
		UNK_201
	}

	public enum LobbyStage {
		LOGGED_OUT,
		LOGGING_IN_LOBBY,
		LOBBY_TO_WORLD
	}

	private static String lobbyAuthToken;

	public static void setLoginStage(LoginStage loginStage) {
		System.out.println("Login stage: " + LoginManager.loginStage + " -> " + loginStage);
		LoginManager.loginStage = loginStage;
	}

	public static void setLobbyStage(LobbyStage lobbyStage) {
		System.out.println("Lobby stage: " + LoginManager.lobbyStage + " -> " + lobbyStage);
		LoginManager.lobbyStage = lobbyStage;
	}

	public static LoginStage loginStage = LoginStage.NONE_2;
	public static LobbyStage lobbyStage = LobbyStage.LOGGED_OUT;
	public static int anInt1105;
	public static int LAST_IP_ADDRESS;
	public static int anInt1965;
	public static int[] clientSeed;

	public static void loginBlock() {
	    if (LoginManager.loginStage != LoginStage.NONE_2 && LoginManager.loginStage != LoginStage.UNK_105) {
	        try {
	            short s_1;
	            if (LoginManager.anInt104 == 0) {
	                s_1 = 250;
	            } else {
	                s_1 = 2000; 
	            }
	            if (LoginManager.socialNetworkLogin && LoginManager.loginStage.ordinal() >= LoginStage.UNK_65.ordinal()) {
	                s_1 = 6000;
	            }
	            if (LoginManager.lobbyStage != LobbyStage.LOBBY_TO_WORLD || LoginManager.loginStage != LoginStage.UNK_201 && StaticMethods.anInt106 != 42) {
	                LoginManager.anInt90++;
	            }
	            if (LoginManager.anInt90 > s_1) {
	                LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
	                if (LoginManager.anInt104 >= 3) {
	                    LoginManager.loginStage = LoginStage.NONE_2;
	                    LoginManager.method13784(-5);
	                    return;
	                }
	                if (LoginManager.lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
	                    LoginManager.GAME_CONNECTION_INFO.swap();
	                } else {
	                    WorldConnection.LOBBY_CONNECTION_INFO.swap();
	                }
	                LoginManager.anInt90 = 0;
	                LoginManager.anInt104++;
	                LoginManager.loginStage = LoginStage.UNK_18;
	            }
	            TcpPacket tcpPacket;
	            int tcpPacketPositionBefore;
	            int loginBlockPositionBefore;
	            if (LoginManager.loginStage == LoginStage.UNK_18) {
	                if (LoginManager.lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.init(AsyncConnection.createAsyncConnection(LoginManager.GAME_CONNECTION_INFO.createSocket(), 15000), LoginManager.GAME_CONNECTION_INFO.host);
	                } else {
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.init(AsyncConnection.createAsyncConnection(WorldConnection.LOBBY_CONNECTION_INFO.createSocket(), 15000), WorldConnection.LOBBY_CONNECTION_INFO.host);
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.clearAllQueuedPackets();
	                tcpPacket = TcpPacket.createLoginPacket();
	                if (LoginManager.socialNetworkLogin) {
	                    tcpPacket.buffer.writeByte(LoginProt.INIT_SOCIAL_NETWORK_CONNECTION.id);
	                    tcpPacket.buffer.writeShort(0);
	                    tcpPacketPositionBefore = tcpPacket.buffer.currentPosition;
	                    tcpPacket.buffer.writeInt(MAJOR_BUILD);
	                    tcpPacket.buffer.writeInt(MINOR_BUILD);
	                    if (LoginManager.lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
	                        tcpPacket.buffer.writeByte(client.gameState == GameState.UNK_10 ? 1 : 0);
	                    }
	                    Packet seedBlock = LoginManager.encodeClientSeed();
	                    seedBlock.writeByte(StaticMethods.anInt76);
	                    seedBlock.writeShort((int) (Math.random() * 9.9999999E7D));
	                    seedBlock.writeByte(client.CURRENT_LANGUAGE.getValue());
	                    seedBlock.writeInt(client.AFFILIATE);
	                    for (loginBlockPositionBefore = 0; loginBlockPositionBefore < 6; loginBlockPositionBefore++) {
	                        seedBlock.writeInt((int) (Math.random() * 9.9999999E7D));
	                    }
	                    seedBlock.writeLong(client.aLong7409);
	                    seedBlock.writeByte(client.CURRENT_GAME.id);
	                    seedBlock.writeByte((int) (Math.random() * 9.9999999E7D));
	                    seedBlock.applyRSA();
	                    tcpPacket.buffer.writeBytes(seedBlock.buffer, 0, seedBlock.currentPosition);
	                    tcpPacket.buffer.writeShortLength(tcpPacket.buffer.currentPosition - tcpPacketPositionBefore);
	                } else {
	                    tcpPacket.buffer.writeByte(LoginProt.INIT_GAME_CONNECTION.id);
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.queuePacket(tcpPacket);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.flush();
	                LoginManager.loginStage = LoginStage.UNK_35;
	            }
	            if (LoginManager.loginStage == LoginStage.UNK_35) {
	                if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(1)) {
	                    return;
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1);
	                int i_10 = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff;
	                if (i_10 != 0) {
	                    LoginManager.loginStage = LoginStage.NONE_2;
	                    LoginManager.method13784(i_10);
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
	                    GameState.method6270();
	                    return;
	                }
	                if (LoginManager.socialNetworkLogin) {
	                    LoginManager.loginStage = LoginStage.UNK_46;
	                } else {
	                    LoginManager.loginStage = LoginStage.UNK_80;
	                }
	            }
	            if (LoginManager.loginStage == LoginStage.UNK_46) {
	                if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(2)) {
	                    return;
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 2);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.readUShort();
	                LoginManager.loginStage = LoginStage.UNK_57;
	            }
	            if (LoginManager.loginStage == LoginStage.UNK_57) {
	                if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition)) {
	                    return;
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.decryptFullXtea(LoginManager.clientSeed);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	                String url = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.readJagString();
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	                String callName = JSObjectManager.LOGIN_PROMPT_BECOME_MEMBER.getCallName();
	                if (!client.isApplet || !Browser.openBrowser(url, 1, callName)) {
	                    Browser.openBrowser(url, true, callName, client.aBool7158);
	                }
	                LoginManager.loginStage = LoginStage.UNK_65;
	            }
	            if (LoginManager.loginStage == LoginStage.UNK_65) {
	                if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(1)) {
	                    return;
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1);
	                if ((LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff) == 1) {
	                    LoginManager.loginStage = LoginStage.UNK_75;
	                }
	            }
	            if (LoginManager.loginStage == LoginStage.UNK_75) {
	                if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(16)) {
	                    return;
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 16);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 16;
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.decryptFullXtea(LoginManager.clientSeed);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	                LoginManager.aLong77 = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.readLong();
	                LoginManager.aLong86 = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.readLong();
	                LoginManager.loginStage = LoginStage.UNK_80;
	            }
	            Packet.Bit loginBlock;
	            if (LoginManager.loginStage == LoginStage.UNK_80) {
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	                LoginManager.CURRENT_CONNECTION_CONTEXT.clearAllQueuedPackets();
	                tcpPacket = TcpPacket.createLoginPacket();
	                loginBlock = tcpPacket.buffer;
	                int startPositionLoginBlock;
	                LoginProt currentLoginProt;
	                if (LoginManager.lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
	                    if (LoginManager.socialNetworkLogin) {
	                        currentLoginProt = LoginProt.SOCIAL_NETWORK_LOGIN;
	                    } else {
	                        currentLoginProt = LoginProt.GAMELOGIN;
	                    }
	                    loginBlock.writeByte(currentLoginProt.id);
	                    loginBlock.writeShort(0);
	                    loginBlockPositionBefore = loginBlock.currentPosition;
	                    startPositionLoginBlock = loginBlock.currentPosition;
	                    if (!LoginManager.socialNetworkLogin) {
	                        loginBlock.writeInt(MAJOR_BUILD);
	                        loginBlock.writeInt(MINOR_BUILD);
	                        loginBlock.writeByte(client.gameState == GameState.UNK_10 ? 1 : 0);
	                        startPositionLoginBlock = loginBlock.currentPosition;
	                        Packet seedBlock = LoginManager.getSeedBlock(false);
	                        loginBlock.writeBytes(seedBlock.buffer, 0, seedBlock.currentPosition);
	                        startPositionLoginBlock = loginBlock.currentPosition;
	                        loginBlock.writeByte(LoginManager.aLong77 == -1L ? 1 : 0);
	                        if (LoginManager.aLong77 == -1L) {
	                            loginBlock.writeString(StaticMethods.aString99);
	                        } else {
	                            loginBlock.writeLong(LoginManager.aLong77);
	                        }
	                    }
	                    loginBlock.writeByte(WindowMode.getWindowMode());
	                    loginBlock.writeShort(GameShell.screenCanvasWidth);
	                    loginBlock.writeShort(GameShell.screenCanvasHeight * -969250379);
	                    loginBlock.writeByte(PreferenceManager.preferences.antiAliasingPreview.getValue());
	                    LoginManager.pushRandomDat(loginBlock);
	                    loginBlock.writeString(client.affiliateP);
	                    loginBlock.writeInt(client.AFFILIATE);
	                    Packet preferenceBlock = PreferenceManager.preferences.encode();
	                    loginBlock.writeByte(preferenceBlock.currentPosition);
	                    loginBlock.writeBytes(preferenceBlock.buffer, 0, preferenceBlock.currentPosition);
	                    client.preferencesSavedOnServer = true;
	                    Packet systemInfoBlock = new Packet(SystemInfo.SYSTEM_INFO.encode());
	                    SystemInfo.SYSTEM_INFO.writeMachineInformation(systemInfoBlock);
	                    loginBlock.writeBytes(systemInfoBlock.buffer, 0, systemInfoBlock.buffer.length);
	                    loginBlock.writeInt(client.varVerifyId);
	                    loginBlock.writeLong(client.aLong7153);
	                    loginBlock.writeString(client.aString5555);
	                    loginBlock.writeByte(client.aString7156 == null ? 0 : 1);
	                    if (client.aString7156 != null) {
	                        loginBlock.writeString(client.aString7156);
	                    }
	                    loginBlock.writeByte(Resource.NATIVE_LIBRARY_LOADER.isJagtheoraLoaded() ? 1 : 0);
	                    loginBlock.writeByte(client.isApplet ? 1 : 0);
	                    loginBlock.writeByte(client.aBool7160 ? 1 : 0);
	                    loginBlock.writeByte(client.anInt2831);
	                    loginBlock.writeInt(client.anInt7163);
	                    loginBlock.writeString(client.aString7164);
	                    loginBlock.writeByte(WorldConnection.CONNECTION_INFO != null && LoginManager.GAME_CONNECTION_INFO.worldId == WorldConnection.CONNECTION_INFO.worldId ? 0 : 1);
	                    int worldId = LoginManager.GAME_CONNECTION_INFO.worldId;
	                    loginBlock.writeInt(worldId);
	                    LoginManager.writeResourceCrc(loginBlock);
	                } else {
	                    if (LoginManager.socialNetworkLogin) {
	                        currentLoginProt = LoginProt.SOCIAL_NETWORK_LOGIN;
	                    } else {
	                        currentLoginProt = LoginProt.LOBBYLOGIN;
	                    }
	                    loginBlock.writeByte(currentLoginProt.id);
	                    loginBlock.writeShort(0);
	                    loginBlockPositionBefore = loginBlock.currentPosition;
	                    startPositionLoginBlock = loginBlock.currentPosition;
	                    if (!LoginManager.socialNetworkLogin) {
	                        loginBlock.writeInt(MAJOR_BUILD);
	                        loginBlock.writeInt(MINOR_BUILD);
	                        Packet seedBlock = LoginManager.getSeedBlock(true);
	                        loginBlock.writeBytes(seedBlock.buffer, 0, seedBlock.currentPosition);
	                        startPositionLoginBlock = loginBlock.currentPosition;
	                        loginBlock.writeByte(LoginManager.aLong77 == -1L ? 1 : 0);
	                        if (LoginManager.aLong77 == -1L) {
	                            loginBlock.writeString(StaticMethods.aString99);
	                        } else {
	                            loginBlock.writeLong(LoginManager.aLong77);
	                        }
	                    }
	                    loginBlock.writeByte(client.CURRENT_GAME.id);
	                    loginBlock.writeByte(client.CURRENT_LANGUAGE.getValue());
	                    LoginManager.pushRandomDat(loginBlock);
	                    loginBlock.writeString(client.affiliateP);
	                    Packet preferenceBlock = PreferenceManager.preferences.encode();
	                    loginBlock.writeByte(preferenceBlock.currentPosition);
	                    loginBlock.writeBytes(preferenceBlock.buffer, 0, preferenceBlock.currentPosition);
	                    loginBlock.writeString(client.aString5555);
	                    loginBlock.writeInt(client.AFFILIATE);
	                    loginBlock.writeInt(client.anInt7163);
	                    loginBlock.writeString(client.aString7164);
	                    LoginManager.writeResourceCrc(loginBlock);
	                }
	                loginBlock.encryptXtea(LoginManager.clientSeed, startPositionLoginBlock, loginBlock.currentPosition);
	                loginBlock.writeShortLength(loginBlock.currentPosition - loginBlockPositionBefore);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.queuePacket(tcpPacket);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.flush();
	                LoginManager.CURRENT_CONNECTION_CONTEXT.isaac = new Isaac(LoginManager.clientSeed);
	                for (int index = 0; index < 4; index++) {
	                    LoginManager.clientSeed[index] += 50;
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.instreamEncryption = new Isaac(LoginManager.clientSeed);
	                new Isaac(LoginManager.clientSeed);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.setIsaacCipher(LoginManager.CURRENT_CONNECTION_CONTEXT.instreamEncryption);
	                LoginManager.clientSeed = null;
	                LoginManager.loginStage = LoginStage.UNK_97;
	            }
	            if (LoginManager.loginStage == LoginStage.UNK_97) {
	                if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(1)) {
	                    return;
	                }
	                LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1);
	                int i_10 = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff;
					System.out.println("Login response: " + i_10);
	                if (i_10 == 21) {
	                    LoginManager.loginStage = LoginStage.UNK_128;
	                } else if (i_10 != 29 && i_10 != 45) {
	                    if (i_10 == 1) {
	                        LoginManager.loginStage = LoginStage.UNK_105;
	                        LoginManager.method13784(1);
	                        return;
	                    }
	                    if (i_10 == 2) {
	                        LoginManager.loginStage = LoginStage.UNK_133;
	                    } else {
	                        if (i_10 != 15) {
	                            if (i_10 == 23 && LoginManager.anInt104 < 3) {
	                                LoginManager.anInt90 = 0;
	                                LoginManager.anInt104++;
	                                LoginManager.loginStage = LoginStage.UNK_18;
	                                LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
	                                return;
	                            } 
	                            if (i_10 == 42) {
	                                LoginManager.loginStage = LoginStage.UNK_201;
	                                LoginManager.method13784(42);
	                                return;
	                            } 
                                if (StaticMethods.aBool74 && !LoginManager.socialNetworkLogin && StaticMethods.anInt76 != -1 && i_10 == 35) {
                                    LoginManager.socialNetworkLogin = true;
                                    LoginManager.anInt90 = 0;
                                    LoginManager.loginStage = LoginStage.UNK_18;
                                    LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
                                } else {
                                    LoginManager.loginStage = LoginStage.NONE_2;
                                    LoginManager.method13784(i_10);
                                    LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
                                    GameState.method6270();
                                }
                                return;
	                        }
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize = -2;
	                        LoginManager.loginStage = LoginStage.UNK_196;
	                    }
	                } else {
	                    LoginManager.anInt1105 = i_10;
	                    LoginManager.loginStage = LoginStage.UNK_180;
	                }
	            }
	            if (LoginManager.loginStage == LoginStage.UNK_115) {
	                LoginManager.CURRENT_CONNECTION_CONTEXT.clearAllQueuedPackets();
	                tcpPacket = TcpPacket.createLoginPacket();
	                loginBlock = tcpPacket.buffer;
	                loginBlock.setIsaacCipher(LoginManager.CURRENT_CONNECTION_CONTEXT.isaac);
	                loginBlock.writeIsaacByte(LoginProt.GAMELOGIN_CONTINUE.id);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.queuePacket(tcpPacket);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.flush();
	                LoginManager.loginStage = LoginStage.UNK_97;
	            } else if (LoginManager.loginStage == LoginStage.UNK_128) {
	                if (LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(1)) {
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1);
	                    int i_10 = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff;
	                    LoginManager.anInt103 = i_10 * 50;
	                    LoginManager.loginStage = LoginStage.NONE_2;
	                    LoginManager.method13784(21);
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
	                    GameState.method6270();
	                }
	            } else if (LoginManager.loginStage == LoginStage.UNK_201) {
	                if (LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(2)) {
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 2);
	                    StaticMethods.anInt113 = ((LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff) << 8) + (LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[1] & 0xff);
	                    LoginManager.loginStage = LoginStage.UNK_97;
	                }
	            } else if (LoginManager.loginStage == LoginStage.UNK_180) {
	                if (LoginManager.anInt1105 == 29) {
	                    if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(1)) {
	                        return;
	                    }
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1);
	                    LoginManager.anInt112 = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff;
	                } else {
	                    if (LoginManager.anInt1105 != 45) {
	                        throw new IllegalStateException();
	                    }
	                    if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(3)) {
	                        return;
	                    }
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 3);
	                    LoginManager.anInt112 = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff;
	                    LoginManager.anInt94 = (LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[2] & 0xff) + ((LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[1] & 0xff) << 8);
	                }
	                LoginManager.loginStage = LoginStage.NONE_2;
	                LoginManager.method13784(LoginManager.anInt1105);
	                LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
	                GameState.method6270();
	            } else if (LoginManager.loginStage == LoginStage.UNK_133) {
	                if (LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(1)) {
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1);
	                    LoginManager.anInt1965 = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff;
	                    LoginManager.loginStage = LoginStage.UNK_140;
	                }
	            } else {
	                Packet.Bit buffer;
	                if (LoginManager.loginStage == LoginStage.UNK_140) {
	                    buffer = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer;
	                    if (LoginManager.lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
	                        if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(LoginManager.anInt1965)) {
	                            return;
	                        }
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(buffer.buffer, 0, LoginManager.anInt1965);
	                        buffer.currentPosition = 0;
	                        client.PLAYER_RIGHTS = buffer.readUByte();
	                        client.PLAYER_MOD_LEVEL = buffer.readUByte();
	                        client.USERDETAIL_QUICKCHAT = buffer.readUByte() == 1;
	                        client.VERIFIED_EMAIL_ADDRESS = buffer.readUByte() == 1;
	                        client.aBool7322 = buffer.readUByte() == 1;
	                        client.IS_QUICKCHAT_ONLY = buffer.readUByte() == 1;
	                        PlayerUpdate.myPlayerIndex = buffer.readUShort();
	                        client.IS_MEMBER = buffer.readUByte() == 1;
	                        LoginManager.PLAYER_DOB = buffer.readMedium();
	                        client.memberWorld = buffer.readUByte() == 1;
	                        client.aString3643 = buffer.readString();
	                        Resource.mapRegion.getLoctypeList().setMember(client.memberWorld);
	                        Resource.mapRegionLoaderTask.getMapRegion().getLoctypeList().setMember(client.memberWorld);
	                        TypeLists.OBJ.setMembersOnly(client.memberWorld);
	                        TypeLists.NPC.setMember(client.memberWorld);
	                    } else {
	                        if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(LoginManager.anInt1965)) {
	                            return;
	                        }
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(buffer.buffer, 0, LoginManager.anInt1965);
	                        buffer.currentPosition = 0;
	                        client.PLAYER_RIGHTS = buffer.readUByte();
	                        client.PLAYER_MOD_LEVEL = buffer.readUByte();
	                        client.USERDETAIL_QUICKCHAT = buffer.readUByte() == 1;
	                        LoginManager.PLAYER_DOB = buffer.readMedium();
	                        PlayerUpdate.MY_PLAYER.male = (byte) buffer.readUByte();
	                        client.VERIFIED_EMAIL_ADDRESS = buffer.readUByte() == 1;
	                        client.aBool7322 = buffer.readUByte() == 1;
	                        LoginManager.MEMBERSHIP_END = buffer.readLong();
	                        LoginManager.aLong420 = LoginManager.MEMBERSHIP_END - Utils.time() - buffer.read40BitLong();
	                        tcpPacketPositionBefore = buffer.readUByte();
	                        client.IS_MEMBER = (tcpPacketPositionBefore & 0x1) != 0;
	                        LoginManager.IS_MEMBER_SUBSCRIPTION = (tcpPacketPositionBefore & 0x2) != 0;
	                        client.JCOINS = buffer.readInt();
	                        QueueIterator.LOYALTY_ENABLED = buffer.readUByte() == 1;
	                        LoginManager.LOYALTY_BALANCE = buffer.readInt();
	                        LoginManager.RECOVERYQUESTIONSSETDATE = buffer.readUShort();
	                        LoginManager.MESSAGES = buffer.readUShort();
	                        LoginManager.LASTLOGINLDAY = buffer.readUShort();
	                        LoginManager.LAST_IP_ADDRESS = buffer.readInt();
							LoginManager.lobbyAuthToken = buffer.readString();
	                        PacketDecoder.HOSTNAME_IDENTIFIER = new HostNameIdentifier(LoginManager.LAST_IP_ADDRESS);
	                        (new Thread(PacketDecoder.HOSTNAME_IDENTIFIER)).start();
	                        LoginManager.EMAIL_STATUS = buffer.readUByte();
	                        LoginManager.anInt8307 = buffer.readUShort();
	                        FileOnDisk.anInt5357 = buffer.readUShort();
	                        LoginManager.aBool5556 = buffer.readUByte() == 1;
	                        PlayerUpdate.MY_PLAYER.displayName = PlayerUpdate.MY_PLAYER.username = PlayerUpdate.MY_PLAYER_USERNAME = buffer.readJagString();
	                        client.anInt3644 = buffer.readUByte();
	                        LoginManager.anInt1526 = buffer.readInt();
	                        client.aBool7323 = buffer.readUByte() == 1;
	                        WorldConnection.CONNECTION_INFO = new ConnectionInfo();
	                        WorldConnection.CONNECTION_INFO.worldId = buffer.readUShort();
	                        if (WorldConnection.CONNECTION_INFO.worldId == 65535) {
	                            WorldConnection.CONNECTION_INFO.worldId = -1;
	                        }
	                        WorldConnection.CONNECTION_INFO.host = buffer.readJagString();
	                        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
	                            WorldConnection.CONNECTION_INFO.devWorld1 = WorldConnection.CONNECTION_INFO.worldId;
	                            WorldConnection.CONNECTION_INFO.devWorld2 = WorldConnection.CONNECTION_INFO.worldId;
	                        }
	                        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LOCAL && (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.WTQA || client.PLAYER_RIGHTS < 2) && LoginManager.GAME_CONNECTION_INFO.equals(WorldConnection.GAME_CONNECTION)) {
	                            LoginManager.prepareHost();
	                        }
	                    }
	                    if (LoginManager.lobbyStage != LobbyStage.LOBBY_TO_WORLD) {
	                        LoginManager.loginStage = LoginStage.NONE_2;
	                        LoginManager.method13784(2);
	                        LoginManager.prepareIngame();
	                        client.setGameState(GameState.UNK_0);
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacket = null;
	                        return;
	                    }
	                    LoginManager.loginStage = LoginStage.UNK_167;
	                }
	                if (LoginManager.loginStage == LoginStage.UNK_167) {
	                    if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(3)) {
	                        return;
	                    }
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 3);
	                    LoginManager.loginStage = LoginStage.UNK_173;
	                }
	                if (LoginManager.loginStage == LoginStage.UNK_173) {
	                    buffer = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer;
	                    buffer.currentPosition = 0;
	                    if (buffer.nextIsSmart()) {
	                        if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(1)) {
	                            return;
	                        }
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(buffer.buffer, 3, 1);
	                    }
	                    tcpPacketPositionBefore = buffer.readEncryptedSmart();
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacket = ServerProt.forId(tcpPacketPositionBefore);
	                    LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize = buffer.readUShort();
	                    LoginManager.loginStage = LoginStage.UNK_157;
	                }
	                if (LoginManager.loginStage == LoginStage.UNK_157) {
	                    if (LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize)) {
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize);
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	                        int i_10 = LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize;
	                        LoginManager.loginStage = LoginStage.NONE_2;
	                        LoginManager.method13784(2);
	                        LoginManager.onEnterIngame();
	                        PlayerUpdate.decodeOwnPlayer(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer);
	                        tcpPacketPositionBefore = i_10 - LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition;
	                        Packet.Bit rsbitsbuffer21 = new Packet.Bit(tcpPacketPositionBefore);
	                        System.arraycopy(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition, rsbitsbuffer21.buffer, 0, tcpPacketPositionBefore);
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition += tcpPacketPositionBefore;
	                        if (LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacket == ServerProt.DYNAMIC_MAP_REGION) {
	                            Resource.mapRegion.initialize(new RegionLoadRequest(RegionLoadingType.INSTANCE, rsbitsbuffer21));
	                        } else {
	                            Resource.mapRegion.initialize(new RegionLoadRequest(RegionLoadingType.STANDARD, rsbitsbuffer21));
	                        }
	                        if (LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition != i_10) {
	                            throw new RuntimeException(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition + " " + i_10);
	                        }
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacket = null;
	                    }
	                } else if (LoginManager.loginStage == LoginStage.UNK_196) {
	                    if (LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize == -2) {
	                        if (!LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(2)) {
	                            return;
	                        }
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 2);
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize = LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.readUShort();
	                    }
	                    if (LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().available(LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize)) {
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.getConnection().read(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize);
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	                        int i_10 = LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacketSize;
	                        LoginManager.loginStage = LoginStage.NONE_2;
	                        LoginManager.method13784(15);
	                        LoginManager.method8350();
	                        PlayerUpdate.decodeOwnPlayer(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer);
	                        if (LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition != i_10) {
	                            throw new RuntimeException(LoginManager.CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition + " " + i_10);
	                        }
	                        LoginManager.CURRENT_CONNECTION_CONTEXT.currentPacket = null;
	                    }
	                }
	            }
	        } catch (IOException ioexception) {
	            LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
	            if (LoginManager.anInt104 < 3) {
	                if (LoginManager.lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
	                    LoginManager.GAME_CONNECTION_INFO.swap();
	                } else {
	                    WorldConnection.LOBBY_CONNECTION_INFO.swap();
	                }
	                LoginManager.anInt90 = 0;
	                ++LoginManager.anInt104;
	                LoginManager.loginStage = LoginStage.UNK_18;
	            } else {
	                LoginManager.loginStage = LoginStage.NONE_2;
	                LoginManager.method13784(-4);
	                GameState.method6270();
	            }
	        }
	    }
	}

	public static boolean method3360() {
	    return LoginManager.loginStage != LoginStage.NONE_2;
	}

	@SuppressWarnings("unused")
	public static void lobbyLogin(String string_0, String string_1) {
	    LoginManager.lobbyStage = LobbyStage.LOGGING_IN_LOBBY;
	    LoginManager.CURRENT_CONNECTION_CONTEXT = client.LOBBY_CONNECTION_CONTEXT;
	    LoginManager.method7558(false, false, string_0, string_1, -1L);
	}

	public static void method4964() {
	    LoginManager.lobbyStage = LobbyStage.LOGGING_IN_LOBBY;
	    LoginManager.CURRENT_CONNECTION_CONTEXT = client.LOBBY_CONNECTION_CONTEXT;
	    LoginManager.method7558(LoginManager.aLong77 == -1L, true, "", "", LoginManager.aLong77);
	}

	public static void method6160() {
	    LoginManager.lobbyStage = LobbyStage.LOGGING_IN_LOBBY;
	    LoginManager.CURRENT_CONNECTION_CONTEXT = client.LOBBY_CONNECTION_CONTEXT;
	    if (client.base64Url != null) {
	        Packet buffer = new Packet(client.base64Url);
	        LoginManager.aLong77 = buffer.readLong();
	        LoginManager.aLong86 = buffer.readLong();
	    }
	    if (LoginManager.aLong77 < 0L) {
	        LoginManager.method13784(35);
	    } else {
	        LoginManager.method7558(false, true, "", "", LoginManager.aLong77);
	    }
	}

	public static void method5921() {
	    if (client.base64Url != null) {
	        method6160();
	    } else if (StaticMethods.anInt76 != -1) {
	        method4964();
	    } else {
	        lobbyLogin(StaticMethods.aString99, StaticMethods.aString102);
	    }
	}

	public static void openLobby(boolean ingame) {
	    if (ingame) {
	        if (client.BASE_WINDOW_ID != -1) {
	            Interface.unloadInterface(client.BASE_WINDOW_ID);
	        }
	        for (SubInterface class282Sub442 = client.OPEN_INTERFACES.first(); class282Sub442 != null; class282Sub442 = client.OPEN_INTERFACES.next()) {
	            if (!class282Sub442.linked()) {
	                class282Sub442 = client.OPEN_INTERFACES.first();
	                if (class282Sub442 == null) {
	                    break;
	                }
	            }
	            Interface.closeChildInterface(class282Sub442, true, false);
	        }
	        client.BASE_WINDOW_ID = -1;
	        client.OPEN_INTERFACES = new IterableNodeMap<SubInterface>(8);
	        Interface.initializeTables();
	        client.BASE_WINDOW_ID = client.ENTITY_DEFAULTS.lobbyWindow;
	        WindowMode.interfaceResize(false);
	        GameShell.setComponentsOnTop();
	        Component.runScriptsIfLoaded(client.BASE_WINDOW_ID, null);
	    }
	    LoginManager.currentlyInLobby = true;
	}

	public static Packet getSeedBlock(boolean password) {
	    Packet clientSeed = LoginManager.encodeClientSeed();
	    clientSeed.writeLong(0L);
	    clientSeed.writeString(password ? StaticMethods.aString102 : lobbyAuthToken);
	    clientSeed.writeLong(LoginManager.aLong86);
	    clientSeed.writeLong(client.aLong7409);
	    clientSeed.applyRSA();
	    return clientSeed;
	}

	public static void method7777() {
	    if (client.gameState == GameState.UNK_0 && !method3360() && !AccountCreationStage.method1466()) {
	        client.setGameState(GameState.UNK_7);
	    }
	}

	public static void worldLogin(String string_0, String string_1) {
	    LoginManager.lobbyStage = LobbyStage.LOBBY_TO_WORLD;
	    LoginManager.CURRENT_CONNECTION_CONTEXT = client.GAME_CONNECTION_CONTEXT;
	    LoginManager.method7558(false, false, string_0, string_1, -1L);
	}

	public static void method1384() {
	    if (client.base64Url != null) {
	        LoginManager.method7022();
	    } else if (StaticMethods.anInt76 != -1) {
	        LoginManager.method537();
	    } else {
	        worldLogin(StaticMethods.aString99, StaticMethods.aString102);
	    }
	}

	public static void method1478() {
	    if (LoginManager.loginStage != LoginStage.NONE_2) {
	        LoginManager.CURRENT_CONNECTION_CONTEXT.closeConnection();
	        LoginManager.method5558();
	        GameState.method6270();
	    }
	}

	public static void method13784(int i_0) {
	    if (LoginManager.lobbyStage == LobbyStage.LOGGING_IN_LOBBY) {
	        StaticMethods.anInt72 = i_0;
	    } else if (LoginManager.lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
	        StaticMethods.anInt106 = i_0;
	    }
	
	}

	public static void method1774() {
	    if (LoginManager.loginStage == LoginStage.UNK_105) {
	        LoginManager.loginStage = LoginStage.UNK_115;
	    }
	}

	public static void method2635(String string_0, String string_1) {
	    if (string_0.length() <= 320 && GameState.method6693()) {
	        client.LOBBY_CONNECTION_CONTEXT.closeConnection();
	        LoginManager.method5118();
	        StaticMethods.aString99 = string_0;
	        StaticMethods.aString102 = string_1;
	        client.setGameState(GameState.UNK_14);
	    }
	}

	public static void method5558() {
	    LoginManager.loginStage = LoginStage.NONE_2;
	    StaticMethods.anInt106 = -2;
	    StaticMethods.anInt72 = -2;
	}

	public static void directLogin(String string_0, String string_1) {
	    if (string_0.length() <= 320 && GameState.method6693()) {
	        LoginManager.method5118();
	        StaticMethods.aString99 = string_0;
	        StaticMethods.aString102 = string_1;
	        client.setGameState(GameState.UNK_19);
	    }
	}

	public static void snLogin(int i_0) {
	    if (GameState.method6693()) {
	        if (i_0 != StaticMethods.anInt76) {
	            LoginManager.aLong77 = -1L;
	        }
	        StaticMethods.anInt76 = i_0;
	        client.setGameState(GameState.UNK_19);
	    }
	}

	public static boolean socialNetworkLogin;

	public static void pushRandomDat(Packet buffer) {
	    byte[] randomDatData = new byte[24];
	    try {
	        GameShell.playerRandomDat.setPosition(0L);
	        GameShell.playerRandomDat.load(randomDatData);
	        int emptyStats;
	        for (emptyStats = 0; emptyStats < 24 && randomDatData[emptyStats] == 0; emptyStats++) { }
	        if (emptyStats >= 24) {
	            throw new IOException();
	        }
	    } catch (Exception exception) {
	        for (int index = 0; index < 24; index++) {
	            randomDatData[index] = -1;
	        }
	    }
	    buffer.writeBytes(randomDatData, 0, 24);
	}

	public static void writeResourceCrc(Packet.Bit buffer) {
	    buffer.writeInt(Resource.FRAME_SET.getCrc());
	    buffer.writeInt(Resource.FRAME_BASE.getCrc());
	    buffer.writeInt(Resource.CONFIG.getCrc());
	    buffer.writeInt(Resource.INTERFACE.getCrc());
	    buffer.writeInt(Resource.SOUND_EFFECT.getCrc());
	    buffer.writeInt(Resource.MAPS.getCrc());
	    buffer.writeInt(Resource.MUSIC.getCrc());
	    buffer.writeInt(Resource.MODEL.getCrc());
	    buffer.writeInt(Resource.SPRITES.getCrc());
	    buffer.writeInt(Resource.TEXTURE.getCrc());
	    buffer.writeInt(Resource.HUFFMAN.getCrc());
	    buffer.writeInt(Resource.JINGLE.getCrc());
	    buffer.writeInt(Resource.CS2.getCrc());
	    buffer.writeInt(Resource.FONT_METRICS.getCrc());
	    buffer.writeInt(Resource.MIDI_INSTRUMENT.getCrc());
	    buffer.writeInt(Resource.SOUND_MIDI.getCrc());
	    buffer.writeInt(Resource.LOC.getCrc());
	    buffer.writeInt(Resource.ENUM.getCrc());
	    buffer.writeInt(Resource.NPC.getCrc());
	    buffer.writeInt(Resource.OBJ.getCrc());
	    buffer.writeInt(Resource.SEQ.getCrc());
	    buffer.writeInt(Resource.GFX.getCrc());
	    buffer.writeInt(Resource.VARBIT.getCrc());
	    buffer.writeInt(Resource.WORLD_MAP.getCrc());
	    buffer.writeInt(Resource.QC_MESSAGES.getCrc());
	    buffer.writeInt(Resource.QC_MENU.getCrc());
	    buffer.writeInt(Resource.MATERIAL.getCrc());
	    buffer.writeInt(Resource.PARTICLE.getCrc());
	    buffer.writeInt(Resource.DEFAULTS.getCrc());
	    buffer.writeInt(Resource.BILLBOARD.getCrc());
	    buffer.writeInt(Resource.NATIVE_LIBRARY.getCrc());
	    buffer.writeInt(Resource.SHADER.getCrc());
	    buffer.writeInt(Resource.getFontCrc());
	    buffer.writeInt(Resource.getTipsCrc());
	    buffer.writeInt(Resource.CUTSCENE.getCrc());
	    buffer.writeInt(Resource.VORBIS.getCrc());
	}

	public static Packet encodeClientSeed() {
	    Packet buffer = new Packet(518);
	    LoginManager.clientSeed = new int[4];
	    LoginManager.clientSeed[0] = (int) (Math.random() * 9.9999999E7D);
	    LoginManager.clientSeed[1] = (int) (Math.random() * 9.9999999E7D);
	    LoginManager.clientSeed[2] = (int) (Math.random() * 9.9999999E7D);
	    LoginManager.clientSeed[3] = (int) (Math.random() * 9.9999999E7D);
	    buffer.writeByte(10);
	    buffer.writeInt(LoginManager.clientSeed[0]);
	    buffer.writeInt(LoginManager.clientSeed[1]);
	    buffer.writeInt(LoginManager.clientSeed[2]);
	    buffer.writeInt(LoginManager.clientSeed[3]);
	    return buffer;
	}

	public static void method5118() {
	    LoginManager.aLong77 = -1L;
	    LoginManager.aLong86 = 0L;
	    StaticMethods.anInt76 = -1;
	}

	public static int anInt1526;

	public static void prepareIngame() {
	    LoginManager.prepareForIngame();
	}

	public static ConnectionInfo GAME_CONNECTION_INFO;
	public static BufferedConnectionContext CURRENT_CONNECTION_CONTEXT;

	public static void method8350() {
	    CURRENT_CONNECTION_CONTEXT.clearAllQueuedPackets();
	    CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	    CURRENT_CONNECTION_CONTEXT.currentPacket = null;
	    CURRENT_CONNECTION_CONTEXT.lastPacket = null;
	    CURRENT_CONNECTION_CONTEXT.secondLastPacket = null;
	    CURRENT_CONNECTION_CONTEXT.thirdLastPacket = null;
	    CURRENT_CONNECTION_CONTEXT.currentPacketSize = 0;
	    CURRENT_CONNECTION_CONTEXT.idleReadPulses = 0;
	    client.REBOOT_TIMER = 0;
	    MiniMenu.reset();
	    ComponentMinimap.resetMinimapFlag();
	
	    for (int index = 0; index < 2048; index++) {
	        PlayerUpdate.players[index] = null;
	    }
	
	    PlayerUpdate.MY_PLAYER = null;
	
	    for (int index = 0; index < client.npcCount; index++) {
	        PathingEntity pathingEntity = (PathingEntity) client.NPC_ARRAY[index].value;
	        if (pathingEntity != null) {
	            pathingEntity.faceEntity = -1;
	        }
	    }
	
	    ItemContainer.resetContainerMap();
	    Camera.cameraCalculationType = 2;
	    StaticMethods.anInt833 = -1;
	    PingRequester.anInt5864 = -1;
	    client.setGameState(GameState.LOGGED_INGAME);
	
	    for (int index = 0; index < 107; index++) {
	        client.IS_COMPONENT_ON_TOP[index] = true;
	    }
	
	    PacketEncoder.sendWindowmodePreferenceToServer();
	    PingRequester.CURRENT_REQUEST = null;
	    PingRequester.NEXT_PING_UPDATE_TO_SERVER = 0L;
	    client.resetCutscenes();
	}

	public static int MESSAGES;
	public static int RECOVERYQUESTIONSSETDATE;

	public static boolean setGameHost(int world, String host) {
	    GAME_CONNECTION_INFO = new ConnectionInfo();
	    GAME_CONNECTION_INFO.worldId = world;
	    GAME_CONNECTION_INFO.host = host;
	    if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
	        GAME_CONNECTION_INFO.devWorld1 = GAME_CONNECTION_INFO.worldId;
	        GAME_CONNECTION_INFO.devWorld2 = GAME_CONNECTION_INFO.worldId;
	    }
	    return true;
	}

	public static int PLAYER_DOB;

	public static void method537() {
	    lobbyStage = LobbyStage.LOBBY_TO_WORLD;
	    CURRENT_CONNECTION_CONTEXT = client.GAME_CONNECTION_CONTEXT;
	    LoginManager.method7558(LoginManager.aLong77 == -1L, true, "", "", LoginManager.aLong77);
	}

	public static void onEnterIngame() {
	    if (client.gameState == GameState.UNK_17) {
	        client.method14562();
	    }
	
	    LoginManager.prepareForIngame();
	    client.LOBBY_CONNECTION_CONTEXT.closeConnection();
	    GameShell.appletHasFocus = true;
	    ReflectionCheckManager.setupPendingChecks();
	
	    for (int index = 0; index < PlayerUpdate.HINT_ARROWS.length; index++) {
	        PlayerUpdate.HINT_ARROWS[index] = null;
	    }
	
	    client.interfaceSelectionActive = false;
	    AreaSoundPlayer.initialize();
	    Camera.cameraRandomisationH = (int) (Math.random() * 100.0D) - 50;
	    Camera.cameraRandomisationV = (int) (Math.random() * 110.0D) - 55;
	    Camera.cameraRandomisationA = (int) (Math.random() * 80.0D) - 40;
	    client.minimapRotation = (int) (Math.random() * 120.0D) - 60;
	    client.minimapZoom = (int) (Math.random() * 30.0D) - 20;
	    Camera.camAngleY = ((int) (Math.random() * 160.0D) - 80 & 0x3fff);
	    ComponentMinimap.resetMinimapFlagAndPlane();
	
	    for (int index = 0; index < 2048; index++) {
	        PlayerUpdate.players[index] = null;
	    }
	
	    PlayerUpdate.MY_PLAYER = null;
	    client.NPC_UPDATE_INDEX = 0;
	    client.npcCount = 0;
	    client.NPC_MAP.clear();
	    client.PROJECTILES.remove_all();
	    client.ACTIVE_GFX.clear();
	    client.TILE_MESSAGES.unlink();
	    client.GROUND_ITEMS.clear();
	    LocAction.pendingRemovals = new NodeCollection();
	    LocAction.pendingCustomizations = new NodeCollection();
	    VarProviders.PLAYER_VAR_PROVIDER.reset();
	    PulseEventHandler.clearAllPulseEvents();
	    Camera.cameraMovementLocalX = 0;
	    Camera.cameraMovementLocalY = 0;
	    Camera.cameraMovementZ = 0;
	    Camera.cameraConstantMoveSpeed = 0;
	    Camera.cameraVariableMoveSpeed = 0;
	    Camera.lookX = 0;
	    Camera.lookY = 0;
	    Camera.anInt1527 = 0;
	    Camera.moveCameraSpeed = 0;
	    Camera.moveCameraAngle = 0;
	
	    for (int index = 0; index < VarcTypeList.VARC_INT.length; index++) {
	        if (!VarcTypeList.VARC_BOOL[index]) {
	            VarcTypeList.VARC_INT[index] = -1;
	        }
	    }
	
	    if (client.BASE_WINDOW_ID != -1) {
	        Interface.unloadInterface(client.BASE_WINDOW_ID);
	    }
	
	    for (SubInterface subInterface = client.OPEN_INTERFACES.first(); subInterface != null; subInterface = client.OPEN_INTERFACES.next()) {
	        if (!subInterface.linked()) {
	            subInterface = client.OPEN_INTERFACES.first();
	            if (subInterface == null) {
	                break;
	            }
	        }
	
	        Interface.closeChildInterface(subInterface, true, false);
	    }
	
	    client.BASE_WINDOW_ID = -1;
	    client.OPEN_INTERFACES = new IterableNodeMap<SubInterface>(8);
	    Interface.initializeTables();
	    client.aClass1187352 = null;
	
	    for (int index = 0; index < 8; index++) {
	        client.PLAYER_OPTIONS[index] = null;
	        client.PLAYER_OPTION_REDUCED_PRIORITY[index] = false;
	        client.PLAYER_OPTION_CURSORS[index] = -1;
	    }
	
	    ItemContainer.resetContainerMap();
	    client.awaitingPriorityRequests = true;
	
	    for (int index = 0; index < 107; index++) {
	        client.IS_COMPONENT_ON_TOP[index] = true;
	    }
	
	    for (int index = 0; index < 6; index++) {
	        client.GRAND_EXCHANGE_SLOTS[index] = new GrandExchangeSlot();
	    }
	
	    Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
	    client.sendCameraAngleToServer = true;
	    client.recolorPalette = ObjectType.locRecolorPalette = NPCType.npcRecolorPalette = ItemType.recolorPalette = new short[256];
	    InterfaceRenderer.translatedUptext = LocalizedText.WALK_HERE.translate(client.CURRENT_LANGUAGE);
	    PreferenceManager.preferences.setValue(PreferenceManager.preferences.removeRoofsOptionOverride, PreferenceManager.preferences.removeRoofs.getValue());
	    client.varVerifyId = 0;
	    MiniMenu.reset();
	    PacketEncoder.sendWindowmodePreferenceToServer();
	    PingRequester.CURRENT_REQUEST = null;
	    PingRequester.NEXT_PING_UPDATE_TO_SERVER = 0L;
	}

	public static void prepareHost() {
	    if (WorldConnection.CONNECTION_INFO.worldId != -1) {
	        setGameHost(WorldConnection.CONNECTION_INFO.worldId, WorldConnection.CONNECTION_INFO.host);
	    }
	}

	public static long MEMBERSHIP_END;
	public static int EMAIL_STATUS;

	public static void method7558(boolean socialNetworkLogin, boolean bool1, String string_2, String string_3, long long_4) {
	    LoginManager.socialNetworkLogin = socialNetworkLogin;
	    if (!bool1) {
	        StaticMethods.anInt76 = -1;
	    }
	    StaticMethods.aBool74 = bool1;
	    StaticMethods.aString99 = string_2;
	    StaticMethods.aString102 = string_3;
	    LoginManager.aLong77 = long_4;
	    if (StaticMethods.aBool74 || !StaticMethods.aString99.isEmpty() && !StaticMethods.aString102.isEmpty()) {
	        if (lobbyStage != LobbyStage.LOGGING_IN_LOBBY) {
	            LoginManager.anInt103 = 0;
	            LoginManager.anInt112 = -1;
	            LoginManager.anInt94 = -1;
	        }
	        CURRENT_CONNECTION_CONTEXT.dropped = false;
	        method13784(-3);
	        loginStage = LoginStage.UNK_18;
	        LoginManager.anInt90 = 0;
	        LoginManager.anInt104 = 0;
	    } else {
	        method13784(3);
	    }
	}

	public static boolean IS_MEMBER_SUBSCRIPTION;

	public static void method7022() {
	    lobbyStage = LobbyStage.LOBBY_TO_WORLD;
	    CURRENT_CONNECTION_CONTEXT = client.GAME_CONNECTION_CONTEXT;
	    if (client.base64Url != null) {
	        Packet rsbytebuffer1 = new Packet(client.base64Url);
	        LoginManager.aLong77 = rsbytebuffer1.readLong();
	        LoginManager.aLong86 = rsbytebuffer1.readLong();
	    }
	    if (LoginManager.aLong77 < 0L) {
	        method13784(35);
	    } else {
	        method7558(false, true, "", "", LoginManager.aLong77);
	    }
	}

	public static void prepareForIngame() {
	    CURRENT_CONNECTION_CONTEXT.clearAllQueuedPackets();
	    CURRENT_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
	    CURRENT_CONNECTION_CONTEXT.lastPacket = null;
	    CURRENT_CONNECTION_CONTEXT.secondLastPacket = null;
	    CURRENT_CONNECTION_CONTEXT.thirdLastPacket = null;
	    CURRENT_CONNECTION_CONTEXT.idleReadPulses = 0;
	    client.REBOOT_TIMER = 0;
	    ChatBox.reset();
	    FriendList.LOADED = 0;
	    FriendList.FRIEND_COUNT = 0;
	    FriendsChat.FC_NAME = null;
	    FriendsChat.FC_PLAYER_COUNT = 0;
	    FriendsChat.FC_PLAYERS = null;
	    client.GUEST_CLAN_SETTINGS = null;
	    client.CLAN_SETTINGS = null;
	    MouseActionServer.reset();
	
	    for (int skillIndex = 0; skillIndex < 25; skillIndex++) {
	        client.SKILL_LEVEL_VISIBLE[skillIndex] = 0;
	        client.SKILL_LEVEL_ACTUAL[skillIndex] = 0;
	        client.SKILL_XP[skillIndex] = 0;
	    }	
	}

	public static WorldMeta[] WORLDS;
	public static int LOYALTY_BALANCE;
	public static int LASTLOGINLDAY;
	public static int anInt90;
	public static int anInt104;
	public static int anInt103;
	public static int anInt112 = -1;
	public static int anInt94 = -1;
	public static long aLong77 = -1L;
	public static long aLong86;
	public static int anInt8307;
	public static long aLong420;
	public static boolean currentlyInLobby;
	public static boolean aBool5556; 
}
