package com.jagex.game.runetek5.network.packet;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;

public class TcpPacket extends Node<TcpPacket> {

    public static final TcpPacket[] OUTGOING_PACKETS = new TcpPacket[300];
    public static int index;
    public int capacity;
    public Packet.Bit buffer;
    public int position;
    public ClientProt packet;

    public void push() {
        if (index < OUTGOING_PACKETS.length) {
            OUTGOING_PACKETS[++index - 1] = this;
        }
    }

	public static TcpPacket createLoginPacket() {
	    TcpPacket tcpPacket = createPacket();
	    tcpPacket.packet = null;
	    tcpPacket.position = 0;
	    tcpPacket.buffer = new Packet.Bit(5000);
	    return tcpPacket;
	}

	public static TcpPacket createPacket() {
	    return index == 0 ? new TcpPacket() : OUTGOING_PACKETS[--index];
	}
}
