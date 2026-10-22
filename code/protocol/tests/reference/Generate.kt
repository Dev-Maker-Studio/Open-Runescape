import com.jagex.game.runetek5.tools.Isaac
import com.jagex.game.runetek5.buffer.Packet
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val directory = Path.of(args.single())
    fun intBytes(value: Int) = byteArrayOf((value ushr 24).toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte())
    fun encode(data: ByteArray): ByteArray {
        val compression = data[0].toInt()
        val length = java.nio.ByteBuffer.wrap(data, 1, 4).int
        val write = ByteArrayOutputStream()
        write.write(compression)
        write.write(intBytes(length))
        val realLength = if (compression != 0) length + 4 else length
        for (index in 5 until realLength + 5) {
            if ((write.size() + (10 - 5)) % 512 == 0) write.write(255)
            write.write(data[index].toInt())
        }
        return write.toByteArray()
    }
    fun reply(name: String, index: Int, archive: Int, prefetch: Boolean, container: ByteArray) {
        val data = encode(container)
        if (prefetch) data[0] = (data[0].toInt() or 128).toByte()
        Files.write(directory.resolve("$name.wire"), byteArrayOf(index.toByte()) + intBytes(archive) + data)
        Files.write(directory.resolve("$name.container"), container)
    }
    reply("plain1024", 8, 0x89abcdef.toInt(), true, byteArrayOf(0) + intBytes(1024) + ByteArray(1024) { ((it * 37 + 11) and 255).toByte() })
    reply("compressed500", 30, 70000, false, byteArrayOf(2) + intBytes(500) + intBytes(4096) + ByteArray(500) { ((it * 19 + 3) and 255).toByte() })
    reply("empty", 255, 255, false, byteArrayOf(0, 0, 0, 0, 0))
    val table = ByteArrayOutputStream()
    table.write(2)
    for (index in 0 until 2) {
        table.write(intBytes(if (index == 0) 0x89abcdef.toInt() else 0x12345678))
        table.write(intBytes(if (index == 0) 727 else -1))
        table.write(ByteArray(64) { (it + index * 64).toByte() })
    }
    table.write(ByteArray(65) { if (it == 0) 1 else (255 - it).toByte() })
    reply("version2", 255, 255, false, byteArrayOf(0) + intBytes(table.size()) + table.toByteArray())
    val handshake = Packet(64)
    handshake.writeByte(15)
    handshake.writeByte(12)
    handshake.writeInt(727)
    handshake.writeInt(1)
    handshake.writeByte(97)
    handshake.writeByte(98)
    handshake.writeByte(99)
    handshake.writeByte(0)
    Files.write(directory.resolve("handshake.wire"), handshake.buffer.copyOf(handshake.currentPosition))
    val isaac = Isaac(intArrayOf(1, 2, 3, 4))
    val frames = ByteArrayOutputStream()
    val opcodes = intArrayOf(0, 127, 128, 255, 256, 32767, 5)
    for ((position, opcode) in opcodes.withIndex()) {
        if (opcode >= 128) frames.write((opcode ushr 8) + 128 + isaac.getNextKey())
        frames.write(opcode + isaac.getNextKey())
        when (position % 3) {
            0 -> frames.write(byteArrayOf(0x11, 0x22))
            1 -> frames.write(byteArrayOf(3, 0x80.toByte(), 0, 0x7f))
            2 -> frames.write(byteArrayOf(0, 1, 0xff.toByte()))
        }
    }
    Files.write(directory.resolve("packets.wire"), frames.toByteArray())
    for ((name, seed) in listOf(
        "isaac-zero" to intArrayOf(),
        "isaac-mixed" to intArrayOf(1, -2, 0x12345678, Int.MIN_VALUE),
        "isaac-full" to IntArray(256) { it * 0x1234567 + Int.MIN_VALUE }
    )) {
        val cipher = Isaac(seed)
        val words = ByteArrayOutputStream()
        repeat(520) { words.write(intBytes(cipher.getNextKey())) }
        Files.write(directory.resolve("$name.words"), words.toByteArray())
    }
}
