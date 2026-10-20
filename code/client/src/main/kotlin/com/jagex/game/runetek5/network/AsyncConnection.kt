package com.jagex.game.runetek5.network

import java.io.IOException
import java.net.Socket

class AsyncConnection(
    @JvmField val socket: Socket,
    capacity: Int
) : Connection(), AutoCloseable {

    @JvmField var inputStream: AsyncInputStream
    @JvmField var outputStream: AsyncOutputStream

    init {
        socket.soTimeout = 30000
        socket.tcpNoDelay = true
        inputStream = AsyncInputStream(socket.getInputStream(), capacity)
        outputStream = AsyncOutputStream(socket.getOutputStream(), capacity)
    }

    @Throws(IOException::class)
    override fun getInstreamCapacityLeft(): Int {
        return inputStream.capacityLeft()
    }

    @Throws(IOException::class)
    override fun available(capacityRequired: Int): Boolean {
        return inputStream.available(capacityRequired)
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, destinationPosition: Int, copyLength: Int): Int {
        return inputStream.read(buffer, destinationPosition, copyLength)
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray, size: Int) {
        outputStream.write(buffer, 0, size)
    }

    override fun drop() {
        inputStream.drop()
        outputStream.drop()
    }

    override fun close() {
        outputStream.shutdown()
        try {
            socket.close()
        } catch (ioexception: IOException) {
        }
        inputStream.shutdown()
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun createAsyncConnection(socket: Socket, capacity: Int): Connection {
            return AsyncConnection(socket, capacity)
        }
    }
}
