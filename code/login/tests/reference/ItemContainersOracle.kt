import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.invSlotFromHeader
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.readInvSlotTooltip
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.readInvTooltips
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path

private data class Key(val id: Int, val negative: Boolean)
private data class Entry(val item: Int, val amount: Int, val extended: Boolean = false)
private data class Change(val header: Int, val item: Int, val amount: Int = 0, val tooltip: ByteArray? = null, val extended: Boolean = false)
private data class Case(val name: String, val opcode: Int, val payload: ByteArray)

private class Wire {
    private val bytes = ByteArrayOutputStream()

    fun byte(value: Int) {
        bytes.write(value and 255)
    }

    fun short(value: Int) {
        require(value in 0..65535)
        byte(value ushr 8)
        byte(value)
    }

    fun smart(value: Int) {
        require(value in 0..32767)
        if (value < 128) byte(value) else short(value + 32768)
    }

    fun string(value: ByteArray) {
        require(value.none { it == 0.toByte() })
        bytes.write(value)
        byte(0)
    }

    fun amount(value: Int, full: Boolean, extended: Boolean) {
        val encoded = if (value in 0..254 && !extended) value else 255
        byte(if (full) 128 - encoded else encoded)
        if (encoded == 255) {
            if (full) {
                byte(value ushr 16)
                byte(value ushr 24)
                byte(value)
                byte(value ushr 8)
            } else {
                byte(value ushr 24)
                byte(value ushr 16)
                byte(value ushr 8)
                byte(value)
            }
        }
    }

    fun result(): ByteArray = bytes.toByteArray()
}

private fun text(value: String): ByteArray = value.toByteArray(Charsets.US_ASCII)
private fun raw(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

private fun full(name: String, id: Int, flags: Int, entries: List<Entry>, mask: List<ByteArray?>? = null, sparse: List<Pair<Int, ByteArray>> = emptyList(), extraMaskBits: Int = 0): Case {
    val wire = Wire()
    wire.short(id)
    wire.byte(flags)
    wire.short(entries.size)
    entries.forEach {
        wire.amount(it.amount, true, it.extended)
        wire.byte(it.item + 128)
        wire.byte(it.item ushr 8)
    }
    if ((flags and 2) != 0) {
        val tooltips = mask ?: List(entries.size) { null }
        require(tooltips.size == entries.size)
        val bits = ByteArray((entries.size + 7) / 8)
        tooltips.forEachIndexed { slot, tooltip ->
            if (tooltip != null) bits[slot / 8] = (bits[slot / 8].toInt() or (1 shl (slot % 8))).toByte()
        }
        if (bits.isNotEmpty()) bits[bits.lastIndex] = (bits.last().toInt() or extraMaskBits).toByte()
        bits.forEach { wire.byte(it.toInt()) }
        tooltips.filterNotNull().forEach { wire.string(it) }
    } else if ((flags and 4) != 0) {
        wire.smart(sparse.size)
        sparse.forEach { (slot, tooltip) ->
            wire.smart(slot)
            wire.string(tooltip)
        }
    }
    return Case(name, 5, wire.result())
}

private fun partial(name: String, id: Int, flags: Int, vararg changes: Change): Case {
    val wire = Wire()
    wire.short(id)
    wire.byte(flags)
    changes.forEach {
        wire.smart(it.header)
        wire.short(it.item)
        if (it.item != 0) wire.amount(it.amount, false, it.extended)
        val setsTooltip = (flags and 2) != 0 && (it.header and 3) == 2
        require(setsTooltip == (it.tooltip != null))
        if (setsTooltip) wire.string(it.tooltip!!)
    }
    return Case(name, 4, wire.result())
}

private fun delete(name: String, id: Int, flags: Int): Case {
    val wire = Wire()
    wire.byte(-flags)
    wire.byte(id)
    wire.byte(id ushr 8)
    return Case(name, 42, wire.result())
}

private fun cases(): List<Case> {
    val id = 4100
    val values = listOf(0, 1, 254, 255, 256, Int.MAX_VALUE, Int.MIN_VALUE, -1, 0x12345678, 0)
    val items = listOf(0, 1, 65535, 1, 65535, 2, 3, 4, 0, 65535)
    val mask = List<ByteArray?>(10) { slot ->
        when (slot) {
            0 -> raw(0x80, 0x81, 0x82, 0x8d, 0x8f, 0x90, 0x9d, 0x9f)
            7 -> text("")
            8 -> raw(0x8c, 0x9c, 0x8a, 0x9a, 0xa0, 0xff)
            9 -> text("tail")
            else -> null
        }
    }
    return listOf(
        full("zero-full-absent", id, 0, emptyList()),
        partial("header-only-partial-absent", id, 0),
        delete("delete-absent", id, 0),
        full("full-quantities-literal", id, 0, values.indices.map { Entry(items[it], values[it]) }),
        full("full-mask-cp1252", id, 2, List(10) { Entry(it + 1, it) }, mask, extraMaskBits = 0x80),
        full("full-shrink-sparse-tail", id, 4, listOf(Entry(1, 1), Entry(65535, 254)), sparse = listOf(9 to text("retained"), 127 to text("ignored"), 0 to text("first"), 0 to text("last"), 1 to text(""), 10 to text("outside"))),
        partial("partial-packed-keep", id, 2, Change(0, 1, 0), Change(7, 65535, -1)),
        partial("partial-packed-clear", id, 2, Change(1, 0), Change(37, 1, 1)),
        partial("partial-packed-set-empty-item", id, 2, Change(2, 0, tooltip = text("empty item accepted")), Change(6, 65535, 254, text(""))),
        partial("partial-bare-clears", id, 0, Change(0, 1, 255), Change(1, 0)),
        partial("partial-smart-127-128", id, 0, Change(127, 1, 256), Change(128, 65535, Int.MAX_VALUE)),
        full("full-shrink-resets-tail", id, 0, listOf(Entry(2, 1))),
        full("full-sparse-tail-duplicate", id, 4, listOf(Entry(1, 0)), sparse = listOf(128 to text("first tail"), 128 to text("last tail"), 127 to text("slot127"), 32767 to text("ignored max"))),
        partial("header-only-partial-existing", id, 0),
        partial("partial-packed-keep-three", id, 2, Change(515, 1, Int.MIN_VALUE), Change(508, 1, -1)),
        full("negative-full-same-id", id, 3, listOf(Entry(65535, 255), Entry(0, Int.MIN_VALUE)), listOf(text("negative"), text(""))),
        partial("negative-partial", id, 3, Change(2, 65535, 0x12345678, text("negative changed"))),
        delete("delete-positive-opposite", id, 0x80),
        full("zero-full-positive-opposite", id, 0, emptyList()),
        partial("header-only-positive-opposite", id, 0),
        full("positive-recreate", id, 0, listOf(Entry(1, 256))),
        delete("delete-negative-existing", id, 0x81),
        delete("delete-negative-absent", id, 1),
        full("zero-full-existing", id, 0, emptyList()),
        full("full-both-flags-precedence", id, 0x86, listOf(Entry(1, 1), Entry(0, 254), Entry(65535, 0)), listOf(text("precedence"), text(""), null)),
        partial("partial-high-flags-packed", id, 0xf6, Change(3, 1, 1), Change(6, 0, tooltip = text("high flags"))),
        partial("partial-high-flags-bare", id, 0xc4, Change(0, 0), Change(1, 65535, -1)),
        full("negative-zero-full-absent", id, 1, emptyList()),
        partial("negative-header-only-absent", id, 1),
        full("full-mask-zero-absent", 4103, 2, emptyList()),
        full("full-sparse-zero-absent", 4103, 4, emptyList(), sparse = listOf(0 to text("no container"), 32767 to text("still absent"))),
        full("full-sparse-count-127", id, 4, listOf(Entry(1, 0), Entry(2, 1)), sparse = List(127) { (it % 2) to text("sparse127-$it") }),
        full("full-sparse-count-128", id, 4, listOf(Entry(1, 0), Entry(2, 1)), sparse = List(128) { (it % 2) to text("sparse128-$it") }),
        partial("partial-bare-duplicates", id, 0, Change(0, 1, 255), Change(0, 65535, -1)),
        full("full-extended-small-amounts", id, 0, listOf(Entry(0, 0, true), Entry(1, 1, true), Entry(65535, 254, true), Entry(1, 255, true))),
        full("full-mask-empty-items", id, 2, listOf(Entry(0, Int.MIN_VALUE), Entry(0, -1)), listOf(text("empty zero"), text(""))),
        full("full-zero-existing-mask", id, 2, emptyList()),
        full("full-sparse-zero-existing", id, 4, emptyList(), sparse = listOf(0 to text("zero-size tail"), 1 to text(""), 4 to text("outside retained"))),
        partial("partial-packed-modes-all", id, 2, Change(0, 1, 0), Change(7, 0), Change(1, 1, 1), Change(6, 0, tooltip = raw(0x81, 0x80))),
        partial("max-bare-slot", 4101, 0, Change(32767, 65535, -1)),
        partial("max-bare-header-only", 4101, 0),
        delete("delete-max-bare", 4101, 0),
        partial("max-packed-slot", 4102, 2, Change(32767, 1, Int.MIN_VALUE)),
        partial("max-packed-set-empty-item", 4102, 2, Change(32766, 0, tooltip = text("packed tail"))),
        full("zero-full-max-packed-existing", 4102, 0, emptyList()),
        delete("delete-max-packed", 4102, 0),
        delete("delete-zero-id-absent", 0, 0),
        partial("partial-zero-id-create", 0, 0, Change(0, 1, 1)),
        delete("delete-zero-id", 0, 0),
        full("full-max-id-positive", 65535, 0, listOf(Entry(65535, 0))),
        full("full-max-id-negative", 65535, 1, listOf(Entry(0, -1))),
        delete("delete-max-id-positive", 65535, 0x80),
        delete("delete-max-id-negative", 65535, 0x81),
        full("zero-full-zero-id-negative", 0, 1, emptyList()),
        partial("header-only-max-id-absent", 65535, 0),
        partial("partial-extended-small-amounts", id, 0, Change(0, 1, 0, extended = true), Change(1, 65535, 1, extended = true), Change(2, 1, 254, extended = true))
    )
}

private fun apply(case: Case): Pair<Key, Int> {
    val reader = Packet(case.payload)
    val id: Int
    val flags: Int
    if (case.opcode == 42) {
        flags = reader.readUByteInverse()
        id = reader.readUShortLittle()
    } else {
        id = reader.readUShort()
        flags = reader.readUByte()
    }
    require(id != 93 && id != 95)
    val negative = (flags and 1) != 0
    when (case.opcode) {
        5 -> {
            ItemContainer.resetContainer(id, negative)
            val size = reader.readUShort()
            repeat(size) { slot ->
                var amount = reader.readUByteSubtract()
                if (amount == 255) amount = reader.readUIntInverseMiddle()
                val item = reader.readUShortAddLittle()
                ItemContainer.setItemInContainer(id, slot, item - 1, amount, negative)
            }
            reader.readInvTooltips(id, size, flags, negative)
        }
        4 -> {
            while (reader.currentPosition < case.payload.size) {
                val header = reader.readUSmart()
                val slot = invSlotFromHeader(header, flags)
                val item = reader.readUShort()
                var amount = 0
                if (item != 0) {
                    amount = reader.readUByte()
                    if (amount == 255) amount = reader.readInt()
                }
                ItemContainer.setItemInContainer(id, slot, item - 1, amount, negative)
                reader.readInvSlotTooltip(id, slot, header, flags, negative)
            }
        }
        42 -> ItemContainer.deleteItemContainer(id, negative)
        else -> error("Unexpected opcode ${case.opcode}")
    }
    check(reader.currentPosition == case.payload.size) { "${case.name}: consumed ${reader.currentPosition}/${case.payload.size}" }
    return Key(id, negative) to reader.currentPosition
}

private fun snapshot(consumed: Int, keys: Set<Key>): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { out ->
        out.writeInt(consumed)
        out.writeInt(keys.size)
        keys.sortedWith(compareBy<Key> { it.id }.thenBy { it.negative }).forEach { key ->
            val container = ItemContainer.getContainer(key.id, key.negative)
            out.writeInt(key.id)
            out.writeInt(if (key.negative) 1 else 0)
            out.writeInt(if (container != null) 1 else 0)
            out.writeInt(container?.itemIds?.size ?: 0)
            if (container != null) {
                check(container.itemIds.size == container.amounts.size && container.itemIds.size == container.tooltips.size)
                container.itemIds.indices.forEach { slot ->
                    out.writeInt(container.itemIds[slot])
                    out.writeInt(container.amounts[slot])
                    val tooltip = container.tooltips[slot]?.toByteArray(Charsets.UTF_8)
                    out.writeInt(tooltip?.size ?: -1)
                    if (tooltip != null) out.write(tooltip)
                }
            }
        }
    }
    return bytes.toByteArray()
}

fun main(args: Array<String>) {
    require(args.size == 1)
    val output = Path.of(args[0])
    Files.createDirectories(output)
    ItemContainer.resetContainerMap()
    val keys = linkedSetOf<Key>()
    val names = hashSetOf<String>()
    val manifest = StringBuilder("order\tname\topcode\tid\tnegative\tpayload\tsnapshot\n")
    val cases = cases()
    cases.forEachIndexed { index, case ->
        check(names.add(case.name))
        val (key, consumed) = apply(case)
        keys.add(key)
        val stem = "item-containers-${case.name}"
        val payload = "$stem.payload"
        val values = "$stem.native-values"
        Files.write(output.resolve(payload), case.payload)
        Files.write(output.resolve(values), snapshot(consumed, keys))
        manifest.append("${index + 1}\t${case.name}\t${case.opcode}\t${key.id}\t${if (key.negative) 1 else 0}\t$payload\t$values\n")
    }
    Files.writeString(output.resolve("item-containers-cases.tsv"), manifest.toString())
    println("Item-container native oracle: ${cases.size} stateful cases, ${keys.size} tracked keys")
}
