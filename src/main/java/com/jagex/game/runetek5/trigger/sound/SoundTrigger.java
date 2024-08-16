package com.jagex.game.runetek5.trigger.sound;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.trigger.ClientTriggerType;

public class SoundTrigger {

	public static void playVorbis(int fileId, boolean sendPacket) {
	    if (sendPacket) {
	        TcpPacket packet = PacketEncoder.createPacket(ClientProt.PLAY_VORBIS, client.GAME_CONNECTION_CONTEXT.isaac);
	        packet.buffer.writeShort(fileId);
	        client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
	    } else {
	        CS2Executor.executeTrigger(ClientTriggerType.PLAY_VORBIS, fileId, -1);
	    }
	}

}
