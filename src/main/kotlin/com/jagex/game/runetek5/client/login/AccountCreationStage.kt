package com.jagex.game.runetek5.client.login

import com.jagex.game.launch.Loader.Companion.MAJOR_BUILD
import com.jagex.game.launch.Loader.Companion.MINOR_BUILD
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.Client
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
        @JvmField var connectionRetryCount = 0
        @JvmField var connectionTickCount = 0
        @JvmField var ACCOUNT_CREATION_STAGE: AccountCreationStage? = null
        @JvmField var currentRequest: CreateAccountRequest? = null
        @JvmField var RECEIVED_RESPONSE: AccountCreationResponseOpcodes? = null
        @JvmField var checkemailRequeststatus: CheckEmailRequest? = null

        @JvmStatic
        fun initAccountCreation() {
            ACCOUNT_CREATION_STAGE = REQUEST
            RECEIVED_RESPONSE = AccountCreationResponseOpcodes.NOT_STARTED
            currentRequest = CreateAccountRequest.NOT_STARTED
            checkemailRequeststatus = CheckEmailRequest.DEFAULT
        }

        @JvmStatic
        fun retryOrFailConnection() {
            Client.LOBBY_CONNECTION_CONTEXT.closeConnection()
            if (connectionRetryCount < 2) {
                WorldConnection.LOBBY_CONNECTION_INFO!!.swap()
                connectionTickCount = 0
                ++connectionRetryCount
                ACCOUNT_CREATION_STAGE = REQUEST
            } else {
                ACCOUNT_CREATION_STAGE = null
                RECEIVED_RESPONSE = AccountCreationResponseOpcodes.CONNECTION_FAILED
                Client.setGameState(GameState.UNK_5)
            }
        }

        @JvmStatic
        fun handleAccountCreationStart() {
            if (ACCOUNT_CREATION_STAGE != null) {
                try {
                    val s_1: Short
                    if (connectionRetryCount == 0) {
                        s_1 = 250
                    } else {
                        s_1 = 2000
                    }

                    ++connectionTickCount
                    if (connectionTickCount > s_1) {
                        retryOrFailConnection()
                    }

                    if (ACCOUNT_CREATION_STAGE == REQUEST) {
                        Client.LOBBY_CONNECTION_CONTEXT.init(
                            AsyncConnection.createAsyncConnection(
                                WorldConnection.LOBBY_CONNECTION_INFO!!.createSocket(),
                                15000
                            ), WorldConnection.LOBBY_CONNECTION_INFO!!.host!!
                        )
                        Client.LOBBY_CONNECTION_CONTEXT.clearAllQueuedPackets()
                        val packet = TcpPacket.createLoginPacket()
                        packet.buffer.writeByte(LoginProt.CREATE_ACCOUNT_CONNECT.id)
                        packet.buffer.writeShort(0)
                        val dataStartOffset = packet.buffer.currentPosition
                        packet.buffer.writeShort(MAJOR_BUILD)
                        packet.buffer.writeShort(MINOR_BUILD)
                        LOGIN_XTEAS = PacketEncoder.generateSeed(packet)
                        val i_4 = packet.buffer.currentPosition
                        packet.buffer.writeString(Client.js5AuthToken!!)
                        packet.buffer.writeShort(Client.AFFILIATE)
                        packet.buffer.writeLong(Client.machineUid)
                        packet.buffer.writeString(Client.loginToken!!)
                        packet.buffer.writeByte(Client.CURRENT_LANGUAGE!!.getValue())
                        packet.buffer.writeByte(Client.CURRENT_GAME!!.id)
                        LoginManager.pushRandomDat(packet.buffer)
                        val string_5 = Client.supplementaryLoginData
                        packet.buffer.writeByte(if (string_5 == null) 0 else 1)
                        if (string_5 != null) {
                            packet.buffer.writeString(string_5)
                        }

                        SystemInfo.SYSTEM_INFO!!.writeMachineInformation(packet.buffer)
                        packet.buffer.currentPosition += 7
                        packet.buffer.encryptXtea(LOGIN_XTEAS!!, i_4, packet.buffer.currentPosition)
                        packet.buffer.writeShortLength(packet.buffer.currentPosition - dataStartOffset)
                        Client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet)
                        Client.LOBBY_CONNECTION_CONTEXT.flush()
                        ACCOUNT_CREATION_STAGE = RESPONSE
                    }

                    if (RESPONSE == ACCOUNT_CREATION_STAGE) {
                        if (Client.LOBBY_CONNECTION_CONTEXT.getConnection() == null) {
                            retryOrFailConnection()
                        } else if (Client.LOBBY_CONNECTION_CONTEXT.getConnection()!!.available(1)) {
                            Client.LOBBY_CONNECTION_CONTEXT.getConnection()!!
                                .read(Client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.buffer, 0, 1)
                            RECEIVED_RESPONSE = Identifiable.identify(
                                AccountCreationResponseOpcodes.getValues(),
                                Client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.buffer[0].toInt() and 0xff
                            ) as AccountCreationResponseOpcodes
                            if (RECEIVED_RESPONSE != AccountCreationResponseOpcodes.CONTINUE) {
                                Client.LOBBY_CONNECTION_CONTEXT.closeConnection()
                            } else {
                                Client.LOBBY_CONNECTION_CONTEXT.isaac = Isaac(LOGIN_XTEAS!!)
                                val seed = IntArray(4)

                                for (index in 0..3) {
                                    seed[index] = LOGIN_XTEAS!![index] + 50
                                }

                                Client.LOBBY_CONNECTION_CONTEXT.instreamEncryption = Isaac(seed)
                                Client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.setIsaacCipher(Client.LOBBY_CONNECTION_CONTEXT.instreamEncryption)
                                Client.setGameState(GameState.IN_ACCOUNT_CREATION)
                                Client.LOBBY_CONNECTION_CONTEXT.clearAllQueuedPackets()
                                Client.LOBBY_CONNECTION_CONTEXT.receivedBuffer.currentPosition = 0
                                Client.LOBBY_CONNECTION_CONTEXT.lastPacket = null
                                Client.LOBBY_CONNECTION_CONTEXT.secondLastPacket = null
                                Client.LOBBY_CONNECTION_CONTEXT.thirdLastPacket = null
                                Client.LOBBY_CONNECTION_CONTEXT.idleReadPulses = 0
                            }

                            Client.LOBBY_CONNECTION_CONTEXT.currentPacket = null
                            ACCOUNT_CREATION_STAGE = null
                        }
                    }
                } catch (ioexception: IOException) {
                    retryOrFailConnection()
                }
            }
        }

        @JvmStatic
        fun isCreatingAccount(): Boolean {
            return ACCOUNT_CREATION_STAGE != null
        }

        @JvmStatic
        fun setCheckemailRequeststatus(status: CheckEmailRequest) {
            checkemailRequeststatus = status
        }

        @JvmStatic
        fun setCurrentRequest(class4940: CreateAccountRequest) {
            currentRequest = class4940
        }
    }
}
