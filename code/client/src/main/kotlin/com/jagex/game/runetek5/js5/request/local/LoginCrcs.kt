package com.jagex.game.runetek5.js5.request.local

import com.darkan.bot.Bot
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.client.login.LoginProt
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.js5.Js5Endpoint
import com.jagex.game.runetek5.js5.dependency.CacheableDependencies
import com.jagex.game.runetek5.network.ConnectionInfo
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

object LoginCrcs {

    private val tables = HashMap<String, IntArray>()

    @JvmStatic
    @Synchronized
    fun of(endpoint: ConnectionInfo, index: Int): Int? =
        (tables[endpoint.host + ":" + endpoint.worldId] ?: fetch(endpoint)?.also { tables[endpoint.host + ":" + endpoint.worldId] = it })?.getOrNull(index)

    @JvmStatic
    fun target(): ConnectionInfo = Js5Endpoint.forWorld(LoginManager.GAME_CONNECTION_INFO?.worldId ?: -1)

    private fun fetch(endpoint: ConnectionInfo): IntArray? = runCatching {
        endpoint.createSocket().use { socket ->
            socket.soTimeout = 10000
            val out = DataOutputStream(socket.getOutputStream().buffered())
            val input = DataInputStream(socket.getInputStream().buffered())
            val token = Client.js5AuthToken!!.toByteArray()
            val prod = if (Bot.PROD) 4 else 0
            out.writeByte(LoginProt.INIT_JS5REMOTE_CONNECTION.id)
            out.writeByte(9 + prod + token.size)
            if (prod != 0) out.writeInt(6)
            out.writeInt(Loader.MAJOR_BUILD)
            out.writeInt(Loader.MINOR_BUILD)
            out.write(token)
            out.writeByte(0)
            out.flush()
            check(input.readUnsignedByte() == 0)
            input.skipNBytes(CacheableDependencies.getAll().size * 4L)
            out.write(byteArrayOf(6, 0, 0, 3, 0, 0, 3, 0, 0, 0, 0, 0, 1, -1))
            out.writeInt(255)
            out.flush()
            input.skipNBytes(5)
            val compression = input.readUnsignedByte() and 0x7f
            val length = input.readInt()
            check(compression == 0)
            val data = ByteArray(length)
            var position = 10
            var read = 0
            while (read < length) {
                if (position % 512 == 0) { input.readByte(); position++ }
                val chunk = minOf(length - read, 512 - position % 512)
                input.readFully(data, read, chunk)
                read += chunk
                position += chunk
            }
            IntArray(data[0].toInt() and 0xff) { ByteBuffer.wrap(data).getInt(1 + it * 72) }
        }
    }.onFailure { println("[LoginCrcs] ${endpoint.host}: $it") }.getOrNull()
}
