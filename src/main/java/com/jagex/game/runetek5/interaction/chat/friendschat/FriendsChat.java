package com.jagex.game.runetek5.interaction.chat.friendschat;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.utils.TextUtils;

public class FriendsChat {

	public static byte FC_MIN_RANK_CAN_KICK;
	public static int FC_PLAYER_COUNT;
	public static byte MY_PLAYER_FC_RANK;
	public static String FC_NAME;
	public static String FC_OWNER_NAME;
	public static FriendsChatPlayer[] FC_PLAYERS;
	
	public static void kickPlayer(String username) {
	    if (FC_PLAYERS != null) {
	        BufferedConnectionContext connection = client.getConnectionContext();
	        TcpPacket packet = PacketEncoder.createPacket(ClientProt.FC_KICK, connection.isaac);
	        packet.buffer.writeByte(TextUtils.getLength(username));
	        packet.buffer.writeString(username);
	        connection.queuePacket(packet);
	    }
	}
	
	public static void setRank(String username, int rank) {
	    BufferedConnectionContext connection = client.getConnectionContext();
	    TcpPacket packet = PacketEncoder.createPacket(ClientProt.FC_SET_RANK, connection.isaac);
	    packet.buffer.writeByte(1 + TextUtils.getLength(username));
	    packet.buffer.writeByteSubtract(rank);
	    packet.buffer.writeString(username);
	    connection.queuePacket(packet);
	}

}
