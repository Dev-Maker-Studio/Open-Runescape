package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.mouse.MouseRecord

abstract class AbstractMousePacket {

    var lastServerSubmission: Long = -1L
    var lastUpdate: Long = -1L
    var lastMouseX: Int = -1
    var lastMouseY: Int = -1
    val nodeCollection: NodeCollection = NodeCollection()

    fun getServerSubmissionDelta(record: MouseRecord, delta: Int): Int {
        val timeDelta: Long
        if (lastServerSubmission == -1L) {
            timeDelta = delta.toLong()
        } else {
            var calculatedDelta = record.getTime() - lastServerSubmission
            if (calculatedDelta > delta) {
                calculatedDelta = delta.toLong()
            }
            timeDelta = calculatedDelta
        }
        lastServerSubmission = record.getTime()
        return timeDelta.toInt()
    }

    fun reset() {
        nodeCollection.remove_all()
        lastServerSubmission = -1L
        lastUpdate = -1L
        lastMouseX = -1
        lastMouseY = -1
    }

    fun append(record: MouseRecord) {
        nodeCollection.addFirst(record)
    }

    abstract fun additionalPayloadLength(): Int

    abstract fun writeConstantValue(packet: Packet, record: MouseRecord)

    abstract fun queuePacket()

    abstract fun createPacketBase(): TcpPacket

    fun submitAll() {
        if (canSubmit()) {
            var packet: TcpPacket? = null
            var startOffset = 0
            var i_4 = 0
            var count = 0
            val recordIterator = nodeCollection.iterator()

            loop@ while (true) {
                while (true) {
                    if (!recordIterator.hasNext()) {
                        break@loop
                    }
                    val record = recordIterator.next() as MouseRecord
                    if (packet != null && packet.buffer.currentPosition - startOffset >= 252 - (6 + additionalPayloadLength())) {
                        break@loop
                    }
                    record.unlink()
                    var mouseY = record.getY()
                    if (mouseY < -1) {
                        mouseY = -1
                    } else if (mouseY > 65534) {
                        mouseY = 65534
                    }
                    var mouseX = record.getX()
                    if (mouseX < -1) {
                        mouseX = -1
                    } else if (mouseX > 65534) {
                        mouseX = 65534
                    }
                    if (mouseX != lastMouseX || mouseY != lastMouseY) {
                        if (packet == null) {
                            packet = createPacketBase()
                            packet.buffer.writeByte(0)
                            startOffset = packet.buffer.currentPosition
                            packet.buffer.currentPosition += 2
                            i_4 = 0
                            count = 0
                        }
                        val mouseDeltaX: Int
                        val mouseDeltaY: Int
                        val i_12: Int
                        if (lastUpdate != -1L) {
                            mouseDeltaX = mouseX - lastMouseX
                            mouseDeltaY = mouseY - lastMouseY
                            i_12 = ((record.getTime() - lastUpdate) / 20L).toInt()
                            i_4 = (i_4 + (record.getTime() - lastUpdate) % 20L).toInt()
                        } else {
                            mouseDeltaX = mouseX
                            mouseDeltaY = mouseY
                            i_12 = Int.MAX_VALUE
                        }
                        lastMouseX = mouseX
                        lastMouseY = mouseY
                        if (i_12 < 8 && mouseDeltaX >= -32 && mouseDeltaX <= 31 && mouseDeltaY >= -32 && mouseDeltaY <= 31) {
                            val adjustedDeltaX = mouseDeltaX + 32
                            val adjustedDeltaY = mouseDeltaY + 32
                            packet.buffer.writeShort((i_12 shl 12) + adjustedDeltaY + (adjustedDeltaX shl 6))
                        } else if (i_12 < 32 && mouseDeltaX >= -128 && mouseDeltaX <= 127 && mouseDeltaY >= -128 && mouseDeltaY <= 127) {
                            val adjustedDeltaX = mouseDeltaX + 128
                            val adjustedDeltaY = mouseDeltaY + 128
                            packet.buffer.writeByte(i_12 + 128)
                            packet.buffer.writeShort(adjustedDeltaY + (adjustedDeltaX shl 8))
                        } else if (i_12 < 32) {
                            packet.buffer.writeByte(i_12 + 192)
                            if (mouseX != -1 && mouseY != -1) {
                                packet.buffer.writeInt(mouseX or (mouseY shl 16))
                            } else {
                                packet.buffer.writeInt(Int.MIN_VALUE)
                            }
                        } else {
                            packet.buffer.writeShort((i_12 and 0x1fff) + 57344)
                            if (mouseX != -1 && mouseY != -1) {
                                packet.buffer.writeInt(mouseX or (mouseY shl 16))
                            } else {
                                packet.buffer.writeInt(Int.MIN_VALUE)
                            }
                        }
                        ++count
                        writeConstantValue(packet.buffer, record)
                        lastUpdate = record.getTime()
                    }
                    record.pushToTop()
                }
            }
            if (packet != null) {
                packet.buffer.writeIndex(packet.buffer.currentPosition - startOffset)
                val positionBeforeWriting = packet.buffer.currentPosition
                packet.buffer.currentPosition = startOffset
                packet.buffer.writeByte(i_4 / count)
                packet.buffer.writeByte(i_4 % count)
                packet.buffer.currentPosition = positionBeforeWriting
                client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
            }
        }
        queuePacket()
    }

    abstract fun canSubmit(): Boolean

    companion object {
        @JvmField
        var aShort7079: Short = 0
    }
}
