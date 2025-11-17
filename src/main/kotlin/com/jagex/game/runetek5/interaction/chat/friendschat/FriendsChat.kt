package com.jagex.game.runetek5.interaction.chat.friendschat

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.utils.TextUtils

object FriendsChat {
    @JvmField var FC_MIN_RANK_CAN_KICK: Byte = 0
    @JvmField var FC_PLAYER_COUNT: Int = 0
    @JvmField var MY_PLAYER_FC_RANK: Byte = 0
    @JvmField var FC_NAME: String? = null
    @JvmField var FC_OWNER_NAME: String? = null
    @JvmField var FC_PLAYERS: Array<FriendsChatPlayer?>? = null

    @JvmStatic
    fun kickPlayer(username: String) {
        if (FC_PLAYERS != null) {
            val connection: BufferedConnectionContext = client.getConnectionContext()
            val packet: TcpPacket = PacketEncoder.createPacket(ClientProt.FC_KICK, connection.isaac)
            packet.buffer.writeByte(TextUtils.getLength(username))
            packet.buffer.writeString(username)
            connection.queuePacket(packet)
        }
    }

    @JvmStatic
    fun setRank(username: String, rank: Int) {
        val connection: BufferedConnectionContext = client.getConnectionContext()
        val packet: TcpPacket = PacketEncoder.createPacket(ClientProt.FC_SET_RANK, connection.isaac)
        packet.buffer.writeByte(1 + TextUtils.getLength(username))
        packet.buffer.writeByteSubtract(rank)
        packet.buffer.writeString(username)
        connection.queuePacket(packet)
    }
}
