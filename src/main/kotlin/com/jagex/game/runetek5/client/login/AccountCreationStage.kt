package com.jagex.game.runetek5.client.login

import com.jagex.game.launch.Loader.Companion.MAJOR_BUILD
import com.jagex.game.launch.Loader.Companion.MINOR_BUILD
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.network.AsyncConnection
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.tools.Isaac
import java.io.IOException

class AccountCreationStage {

    companion object {
        @JvmField val REQUEST = AccountCreationStage()
        @JvmField val RESPONSE = AccountCreationStage()

        @JvmField var LOGIN_XTEAS: IntArray? = null
        @JvmField var anInt134 = 0
        @JvmField var anInt133 = 0
        @JvmField var ACCOUNT_CREATION_STAGE: AccountCreationStage? = null
        @JvmField var aClass4942916: CreateAccountRequest? = null
        @JvmField var RECEIVED_RESPONSE: AccountCreationResponseOpcodes? = null
        @JvmField var checkemailRequeststatus: CheckEmailRequest? = null

        @JvmStatic
        fun method4251() {
            ACCOUNT_CREATION_STAGE = REQUEST
            RECEIVED_RESPONSE = AccountCreationResponseOpcodes.aClass4835729
            aClass4942916 = CreateAccountRequest.aClass4945786
            checkemailRequeststatus = CheckEmailRequest.DEFAULT
        }

        @JvmStatic
        fun method297() {
            client.LOBBY_CONNECTION_CONTEXT.closeConnection()
            if (anInt134 < 2) {
                WorldConnection.LOBBY_CONNECTION_INFO!!.swap()
                anInt133 = 0
                ++anInt134
                ACCOUNT_CREATION_STAGE = REQUEST
            } else {
                ACCOUNT_CREATION_STAGE = null
                RECEIVED_RESPONSE = AccountCreationResponseOpcodes.aClass4835731
                client.setGameState(GameState.UNK_5)
            }
        }

        @JvmStatic
        fun handleAccountCreationStart() {
            if (ACCOUNT_CREATION_STAGE != null) {
                try {
                    val s_1: Short
                    if (anInt134 == 0) {
                        s_1 = 250
                    } else {
                        s_1 = 2000
                    }

                    ++anInt133
                    if (anInt133 > s_1) {
                        method297()
                    }

                    if (ACCOUNT_CREATION_STAGE == REQUEST) {
                        client.LOBBY_CONNECTION_CONTEXT.init(
                            AsyncConnection.createAsyncConnection(
                                WorldConnection.LOBBY_CONNECTION_INFO!!.createSocket(),
                                15000
                            ), WorldConnection.LOBBY_CONNECTION_INFO!!.host!!
                        )
                        client.LOBBY_CONNECTION_CONTEXT.clearAllQueuedPackets()
                        val packet = TcpPacket.createLoginPacket()
                        packet.buffer.writeByte(LoginProt.CREATE_ACCOUNT_CONNECT.id)
                        packet.buffer.writeShort(0)
                        val dataStartOffset = packet.buffer.currentPosition
                        packet.buffer.writeShort(MAJOR_BUILD)
                        packet.buffer.writeShort(MINOR_BUILD)
                        LOGIN_XTEAS = PacketEncoder.generateSeed(packet)
                        val i_4 = packet.buffer.currentPosition
                        packet.buffer.writeString(client.aString7164!!)
                        packet.buffer.writeShort(client.AFFILIATE)
                        packet.buffer.writeLong(client.aLong7153)
                        packet.buffer.writeString(client.aString5555!!)
                        packet.buffer.writeByte(client.CURRENT_LANGUAGE.getValue())
                        packet.buffer.writeByte(client.CURRENT_GAME.id)
                        LoginManager.pushRandomDat(packet.buffer)
                        val string_5 = client.aString7156
                        packet.buffer.writeByte(if (string_5 == null) 0 else 1)
                        if (string_5 != null) {
                            packet.buffer.writeString(string_5)
                        }

                        SystemInfo.SYSTEM_INFO.writeMachineInformation(packet.buffer)
                        packet.buffer.currentPosition += 7
                        packet.buffer.encryptXtea(LOGIN_XTEAS!!, i_4, packet.buffer.currentPosition)
                        packet.buffer.writeShortLength(packet.buffer.currentPosition - dataStartOffset)
                        client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet)
                        client.LOBBY_CONNECTION_CONTEXT.flush()
                        ACCOUNT_CREATION_STAGE = RESPONSE
                    }

                    if (RESPONSE == ACCOUNT_CREATION_STAGE) {
                        if (client.LOBBY_CONNECTION_CONTEXT.getConnection() == null) {
                            method297()
                        } else if (client.LOBBY_CONNECTION_CONTEXT.getConnection()!!.available(1)) {
                            client.LOBBY_CONNECTION_CONTEXT.getConnection()!!
                                .read(client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1)
                            RECEIVED_RESPONSE = Identifiable.identify(
                                AccountCreationResponseOpcodes.getValues(),
                                client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.buffer[0].toInt() and 0xff
                            ) as AccountCreationResponseOpcodes
                            if (RECEIVED_RESPONSE != AccountCreationResponseOpcodes.CONTINUE) {
                                client.LOBBY_CONNECTION_CONTEXT.closeConnection()
                            } else {
                                client.LOBBY_CONNECTION_CONTEXT.isaac = Isaac(LOGIN_XTEAS!!)
                                val seed = IntArray(4)

                                for (index in 0..3) {
                                    seed[index] = LOGIN_XTEAS!![index] + 50
                                }

                                client.LOBBY_CONNECTION_CONTEXT.instreamEncryption = Isaac(seed)
                                client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.setIsaacCipher(client.LOBBY_CONNECTION_CONTEXT.instreamEncryption)
                                client.setGameState(GameState.IN_ACCOUNT_CREATION)
                                client.LOBBY_CONNECTION_CONTEXT.clearAllQueuedPackets()
                                client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0
                                client.LOBBY_CONNECTION_CONTEXT.lastPacket = null
                                client.LOBBY_CONNECTION_CONTEXT.secondLastPacket = null
                                client.LOBBY_CONNECTION_CONTEXT.thirdLastPacket = null
                                client.LOBBY_CONNECTION_CONTEXT.idleReadPulses = 0
                            }

                            client.LOBBY_CONNECTION_CONTEXT.currentPacket = null
                            ACCOUNT_CREATION_STAGE = null
                        }
                    }
                } catch (ioexception: IOException) {
                    method297()
                }
            }
        }

        @JvmStatic
        fun method1466(): Boolean {
            return ACCOUNT_CREATION_STAGE != null
        }

        @JvmStatic
        fun setCheckemailRequeststatus(status: CheckEmailRequest) {
            checkemailRequeststatus = status
        }

        @JvmStatic
        fun method12948(class4940: CreateAccountRequest) {
            aClass4942916 = class4940
        }
    }
}
