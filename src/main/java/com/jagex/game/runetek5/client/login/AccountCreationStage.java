package com.jagex.game.runetek5.client.login;

import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Identifiable;
import com.jagex.game.runetek5.network.AsyncConnection;
import com.jagex.game.runetek5.network.WorldConnection;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.tools.Isaac;

import java.io.IOException;

import static com.jagex.game.launch.Loader.MAJOR_BUILD;
import static com.jagex.game.launch.Loader.MINOR_BUILD;

public class AccountCreationStage {

    public static final AccountCreationStage REQUEST = new AccountCreationStage();
    public static final AccountCreationStage RESPONSE = new AccountCreationStage();

    public static void method4251() {
        AccountCreationStage.ACCOUNT_CREATION_STAGE = REQUEST;
        AccountCreationStage.RECEIVED_RESPONSE = AccountCreationResponseOpcodes.aClass4835729;
        AccountCreationStage.aClass4942916 = CreateAccountRequest.aClass4945786;
        AccountCreationStage.checkemailRequeststatus = CheckEmailRequest.DEFAULT;
    }

    public static void method297() {
        client.LOBBY_CONNECTION_CONTEXT.closeConnection();
        if (AccountCreationStage.anInt134 < 2) {
            WorldConnection.LOBBY_CONNECTION_INFO.swap();
            AccountCreationStage.anInt133 = 0;
            ++AccountCreationStage.anInt134;
            AccountCreationStage.ACCOUNT_CREATION_STAGE = REQUEST;
        } else {
            AccountCreationStage.ACCOUNT_CREATION_STAGE = null;
            AccountCreationStage.RECEIVED_RESPONSE = AccountCreationResponseOpcodes.aClass4835731;
            client.setGameState(GameState.UNK_5);
        }
    }

    public static void handleAccountCreationStart() {
        if (AccountCreationStage.ACCOUNT_CREATION_STAGE != null) {
            try {
                short s_1;
                if (AccountCreationStage.anInt134 == 0) {
                    s_1 = 250;
                } else {
                    s_1 = 2000;
                }

                ++AccountCreationStage.anInt133;
                if (AccountCreationStage.anInt133 > s_1) {
                    method297();
                }

                if (AccountCreationStage.ACCOUNT_CREATION_STAGE == REQUEST) {
                    client.LOBBY_CONNECTION_CONTEXT.init(AsyncConnection.createAsyncConnection(WorldConnection.LOBBY_CONNECTION_INFO.createSocket(), 15000), WorldConnection.LOBBY_CONNECTION_INFO.host);
                    client.LOBBY_CONNECTION_CONTEXT.clearAllQueuedPackets();
                    TcpPacket packet = TcpPacket.createLoginPacket();
                    packet.buffer.writeByte(LoginProt.CREATE_ACCOUNT_CONNECT.id);
                    packet.buffer.writeShort(0);
                    int dataStartOffset = packet.buffer.currentPosition;
                    packet.buffer.writeShort(MAJOR_BUILD);
                    packet.buffer.writeShort(MINOR_BUILD);
                    AccountCreationStage.LOGIN_XTEAS = PacketEncoder.generateSeed(packet);
                    int i_4 = packet.buffer.currentPosition;
                    packet.buffer.writeString(client.aString7164);
                    packet.buffer.writeShort(client.AFFILIATE);
                    packet.buffer.writeLong(client.aLong7153);
                    packet.buffer.writeString(client.aString5555);
                    packet.buffer.writeByte(client.CURRENT_LANGUAGE.getValue());
                    packet.buffer.writeByte(client.CURRENT_GAME.id);
                    LoginManager.pushRandomDat(packet.buffer);
                    String string_5 = client.aString7156;
                    packet.buffer.writeByte(string_5 == null ? 0 : 1);
                    if (string_5 != null) {
                        packet.buffer.writeString(string_5);
                    }

                    SystemInfo.SYSTEM_INFO.writeMachineInformation(packet.buffer);
                    packet.buffer.currentPosition += 7;
                    packet.buffer.encryptWithXtea(AccountCreationStage.LOGIN_XTEAS, i_4, packet.buffer.currentPosition);
                    packet.buffer.method13281(packet.buffer.currentPosition - dataStartOffset);
                    client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet);
                    client.LOBBY_CONNECTION_CONTEXT.flush();
                    AccountCreationStage.ACCOUNT_CREATION_STAGE = RESPONSE;
                }

                if (RESPONSE == AccountCreationStage.ACCOUNT_CREATION_STAGE) {
                    if (client.LOBBY_CONNECTION_CONTEXT.getConnection() == null) {
                        method297();
                    } else if (client.LOBBY_CONNECTION_CONTEXT.getConnection().available(1)) {
                        client.LOBBY_CONNECTION_CONTEXT.getConnection().read(client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1);
                        AccountCreationStage.RECEIVED_RESPONSE = (AccountCreationResponseOpcodes) Identifiable.identify(AccountCreationResponseOpcodes.getValues(), client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.buffer[0] & 0xff);
                        if (AccountCreationStage.RECEIVED_RESPONSE != AccountCreationResponseOpcodes.CONTINUE) {
                            client.LOBBY_CONNECTION_CONTEXT.closeConnection();
                        } else {
                            client.LOBBY_CONNECTION_CONTEXT.isaac = new Isaac(AccountCreationStage.LOGIN_XTEAS);
                            int[] seed = new int[4];

                            for (int index = 0; index < 4; index++) {
                                seed[index] = AccountCreationStage.LOGIN_XTEAS[index] + 50;
                            }

                            client.LOBBY_CONNECTION_CONTEXT.instreamEncryption = new Isaac(seed);
                            client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.setIsaacCipher(client.LOBBY_CONNECTION_CONTEXT.instreamEncryption);
                            client.setGameState(GameState.IN_ACCOUNT_CREATION);
                            client.LOBBY_CONNECTION_CONTEXT.clearAllQueuedPackets();
                            client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0;
                            client.LOBBY_CONNECTION_CONTEXT.lastPacket = null;
                            client.LOBBY_CONNECTION_CONTEXT.secondLastPacket = null;
                            client.LOBBY_CONNECTION_CONTEXT.thirdLastPacket = null;
                            client.LOBBY_CONNECTION_CONTEXT.idleReadPulses = 0;
                        }

                        client.LOBBY_CONNECTION_CONTEXT.currentPacket = null;
                        AccountCreationStage.ACCOUNT_CREATION_STAGE = null;
                    }
                }
            } catch (IOException ioexception) {
                method297();
            }
        }

    }

	public static boolean method1466() {
	    return AccountCreationStage.ACCOUNT_CREATION_STAGE != null;
	}

	public static int[] LOGIN_XTEAS;
	public static int anInt134;
	public static int anInt133;
	public static AccountCreationStage ACCOUNT_CREATION_STAGE;
	public static CreateAccountRequest aClass4942916;
	public static AccountCreationResponseOpcodes RECEIVED_RESPONSE;

	public static void setCheckemailRequeststatus(CheckEmailRequest status) {
	    AccountCreationStage.checkemailRequeststatus = status;
	}

	public static CheckEmailRequest checkemailRequeststatus;

	public static void method12948(CreateAccountRequest class4940) {
	    aClass4942916 = class4940;
	}
}
