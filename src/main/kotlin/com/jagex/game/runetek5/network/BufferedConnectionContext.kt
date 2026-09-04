package com.jagex.game.runetek5.network

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.network.packet.ServerProt
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.tools.Isaac
import java.io.IOException

/**
 *
 * @author Brainpower - 02/02/2023
 *
 */
class BufferedConnectionContext {

    @JvmField var isaac: Isaac? = null
    @JvmField var lastFlush: Int = 0
    @JvmField var dropped: Boolean = false
    @JvmField var lastPacket: ServerProt? = null
    @JvmField var thirdLastPacket: ServerProt? = null
    @JvmField var instreamEncryption: Isaac? = null
    @JvmField var secondLastPacket: ServerProt? = null
    @JvmField var wrote: Int = 0
    @JvmField var readDataSize: Int = 0
    @JvmField var connection: Connection? = null
    @JvmField val queuedPackets = NodeCollection()
    @JvmField val writeBuffer = Packet(1600)
    @JvmField val receivedBuffer = Packet.Bit(15000)
    @JvmField var currentPacket: ServerProt? = null
    @JvmField var currentPacketSize: Int = 0
    @JvmField var readPadding: Boolean = true
    @JvmField var idleReadPulses: Int = 0
    @JvmField val pinger = ConnectionPinger()
    @JvmField var outBytesPerSec: Int = 0
    @JvmField var inBytesPerSec: Int = 0
    @JvmField var read: Int = 0

    init {
        val pingerThread = Thread(pinger)
        pingerThread.priority = 1
        pingerThread.start()
    }

    @Throws(IOException::class)
    fun flush() {
        if (connection != null && readDataSize > 0) {
            writeBuffer.currentPosition = 0
            while (true) {
                val packet = queuedPackets.getFirst() as TcpPacket?
                if (packet == null || packet.capacity > writeBuffer.buffer.size - writeBuffer.currentPosition) {
                    connection!!.write(writeBuffer.buffer, writeBuffer.currentPosition)
                    wrote += writeBuffer.currentPosition
                    lastFlush = 0
                    break
                }
                writeBuffer.writeBytes(packet.buffer.buffer, 0, packet.capacity)
                readDataSize -= packet.capacity
                packet.unlink()
                packet.buffer.flush()
                packet.push()
            }
        }
    }

    fun queuePacket(tcpPacket: TcpPacket) {
        queuedPackets.addFirst(tcpPacket)
        tcpPacket.capacity = tcpPacket.buffer.currentPosition
        tcpPacket.buffer.currentPosition = 0
        readDataSize += tcpPacket.capacity
    }

    fun init(connection: Connection, ip: String) {
        this.connection = connection
        pinger.setIp(ip)
    }

    fun closeConnection() {
        if (connection != null) {
            connection!!.close()
            connection = null
        }
        pinger.setIp(null)
    }

    fun getConnection(): Connection? {
        return connection
    }

    fun clearAllQueuedPackets() {
        queuedPackets.remove_all()
        readDataSize = 0
    }

    fun cycle() {
        if (Client.cycles % 50 == 0) {
            outBytesPerSec = wrote
            wrote = 0
            inBytesPerSec = read
            read = 0
        }
    }

    fun reset() {
        connection = null
        pinger.setIp(null)
    }
}
