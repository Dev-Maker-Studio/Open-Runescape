package com.jagex.game.runetek5.network;

import java.io.IOException;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.network.packet.ServerProt;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.Isaac;

/**
 * 
 * @author Brainpower - 02/02/2023
 *
 */
public class BufferedConnectionContext {

	public Isaac isaac;
    public int lastFlush;
    public boolean dropped;
    public ServerProt lastPacket;
    public ServerProt thirdLastPacket;
    public Isaac instreamEncryption;
    public ServerProt secondLastPacket;
    public int wrote;
    public int readDataSize;
    public Connection connection;
    public final NodeCollection queuedPackets = new NodeCollection();
    public final Packet writeBuffer = new Packet(1600);
    public final Packet.Bit receivedBuffer = new Packet.Bit(15000);
    public ServerProt currentPacket;
    public int currentPacketSize;
    public boolean readPadding = true;
    public int idleReadPulses;
    public final ConnectionPinger pinger = new ConnectionPinger();
    public int outBytesPerSec;
    public int inBytesPerSec;
    public int read;

    public BufferedConnectionContext() {
        Thread pingerThread = new Thread(this.pinger);
        pingerThread.setPriority(1);
        pingerThread.start();
    }

    public void flush() throws IOException {
        if (this.connection != null && this.readDataSize > 0) {
            this.writeBuffer.currentPosition = 0;
            while (true) {
                TcpPacket packet = (TcpPacket) this.queuedPackets.getFirst();
                if (packet == null || packet.capacity > this.writeBuffer.buffer.length - this.writeBuffer.currentPosition) {
                    this.connection.write(this.writeBuffer.buffer, this.writeBuffer.currentPosition);
                    this.wrote += this.writeBuffer.currentPosition;
                    this.lastFlush = 0;
                    break;
                }
                this.writeBuffer.writeBytes(packet.buffer.buffer, 0, packet.capacity);
                this.readDataSize -= packet.capacity;
                packet.unlink();
                packet.buffer.flush();
                packet.push();
            }
        }
    }

    public void queuePacket(TcpPacket tcpPacket) {
        this.queuedPackets.addFirst(tcpPacket);
        tcpPacket.capacity = tcpPacket.buffer.currentPosition;
        tcpPacket.buffer.currentPosition = 0;
        this.readDataSize += tcpPacket.capacity;
    }

    public void init(Connection connection, String ip) {
        this.connection = connection;
        this.pinger.setIp(ip);
    }

    public void closeConnection() {
        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
        this.pinger.setIp(null);
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void clearAllQueuedPackets() {
        this.queuedPackets.remove_all();
        this.readDataSize = 0;
    }

    public void cycle() {
        if (client.cycles % 50 == 0) {
            this.outBytesPerSec = this.wrote;
            this.wrote = 0;
            this.inBytesPerSec = this.read;
            this.read = 0;
        }
    }

    public void reset() {
        this.connection = null;
        this.pinger.setIp(null);
    }
}
