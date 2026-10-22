import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.varp.VarpTypeList
import com.jagex.game.runetek5.network.packet.ServerProt
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.PlayerVarDomain
import kotlinx.serialization.json.*
import java.util.HexFormat

private const val CLOCK_ANCHOR = 1L shl 60
private const val TIME_MASK = 0x3fffffffffffffffL
private const val PUBLISH_FLAG = 0x4000000000000000L
private const val IMMEDIATE = PUBLISH_FLAG or 1L

private data class Step(val operation: String, val now: Long, val id: Int = 0, val value: Int = 0)

private fun server(id: Int, value: Int, now: Long) = Step("server", now, id, value)
private fun local(id: Int, value: Int, now: Long) = Step("local", now, id, value)
private fun service(now: Long) = Step("service", now)
private fun clear(now: Long) = Step("clear", now)

private fun freezeClock(now: Long) {
    Utils.lastSystemTime = CLOCK_ANCHOR
    Utils.timeOffset = now - CLOCK_ANCHOR
}

private fun syntheticDomain(count: Int): PlayerVarDomain {
    val definitions = VarpTypeList(null)
    VarpTypeList::class.java.getDeclaredField("size").apply {
        isAccessible = true
        setInt(definitions, count)
    }
    TypeLists.VAR = definitions
    return PlayerVarDomain().also {
        check(it.activeVars.size == count && it.pendingVars.size == count)
    }
}

private fun packet(opcode: ServerProt, hex: String): JsonObject {
    val bytes = HexFormat.of().parseHex(hex)
    check(bytes.size == opcode.size)
    val input = Packet(bytes)
    var id = 0
    var value = 0
    when (opcode) {
        ServerProt.VARP_SMALL -> {
            value = input.readByte().toInt()
            id = input.readUShortAddLittle()
        }
        ServerProt.VARP_LARGE -> {
            value = input.readUIntInverseMiddle()
            id = input.readUShortAddLittle()
        }
        ServerProt.CLEAR_VARPS -> Unit
        else -> error("unsupported oracle packet $opcode")
    }
    check(input.currentPosition == bytes.size)
    return buildJsonObject {
        put("opcode", opcode.opcode)
        put("name", opcode.name)
        put("payloadHex", hex)
        if (opcode != ServerProt.CLEAR_VARPS) {
            put("id", id)
            put("value", value)
        }
    }
}

private fun scenario(name: String, count: Int, steps: List<Step>): JsonObject {
    val domain = syntheticDomain(count)
    val observed = steps.filter { it.operation == "server" || it.operation == "local" }
        .map { it.id }.distinct().sorted()
    val results = buildJsonArray {
        for (step in steps) {
            val published = mutableListOf<Int>()
            freezeClock(step.now)
            when (step.operation) {
                "server" -> domain.setVarValueFromServer(step.id, step.value)
                "local" -> domain.setVarValue(step.id, step.value)
                "clear" -> domain.reset()
                "service" -> {
                    var startHead = true
                    while (true) {
                        freezeClock(step.now)
                        val id = domain.service(startHead)
                        if (id == -1) break
                        published.add(id)
                        startHead = false
                    }
                }
                else -> error("unsupported oracle operation ${step.operation}")
            }
            add(buildJsonObject {
                put("operation", step.operation)
                put("now", step.now)
                if (step.operation == "server" || step.operation == "local") {
                    put("id", step.id)
                    put("value", step.value)
                }
                put("published", buildJsonArray { published.forEach { add(it) } })
                put("scheduled", domain.updates.size())
                put("slots", buildJsonArray {
                    for (id in observed) {
                        add(buildJsonObject {
                            put("id", id)
                            put("active", domain.getVarValue(id))
                            put("pending", domain.pendingVars[id])
                        })
                    }
                })
                put("updates", buildJsonArray {
                    for (node in domain.updates) {
                        add(buildJsonObject {
                            put("id", node.pointer)
                            put("deadline", node.bitpacked and TIME_MASK)
                            put("publish", node.bitpacked and PUBLISH_FLAG != 0L)
                            put("immediate", node.bitpacked == IMMEDIATE)
                        })
                    }
                })
            })
        }
    }
    return buildJsonObject {
        put("name", name)
        put("count", count)
        put("steps", results)
    }
}

fun main() {
    val report = buildJsonObject {
        put("packets", buildJsonArray {
            add(packet(ServerProt.VARP_SMALL, "808000"))
            add(packet(ServerProt.VARP_SMALL, "ff7fff"))
            add(packet(ServerProt.VARP_SMALL, "7fb412"))
            add(packet(ServerProt.VARP_SMALL, "000080"))
            add(packet(ServerProt.VARP_LARGE, "34127856b412"))
            add(packet(ServerProt.VARP_LARGE, "008000008000"))
            add(packet(ServerProt.VARP_LARGE, "ff7fffff7fff"))
            add(packet(ServerProt.VARP_LARGE, "ffffffff0080"))
            add(packet(ServerProt.CLEAR_VARPS, ""))
        })
        put("scenarios", buildJsonArray {
            add(scenario("immediate-coalescing-and-strict-sentinel", 2, listOf(
                server(1, -128, 0), service(0), server(1, 127, 1), service(1), service(2),
                server(1, 127, 3), service(3)
            )))
            add(scenario("native-bucket-and-insertion-order", 400, listOf(
                server(257, 258, 100), server(2, 3, 100), server(129, 130, 100),
                server(128, 129, 100), server(0, 1, 100), server(1, 2, 100),
                server(256, 257, 100), server(257, -1, 101), service(101),
                server(129, -2, 102), server(1, -2, 102), server(257, -2, 102), service(102)
            )))
            add(scenario("local-only-strict-deadline", 1, listOf(
                local(0, 55, 100), service(600), service(601)
            )))
            add(scenario("server-after-local-and-repeated-rearm", 1, listOf(
                local(0, 1, 100), server(0, 2, 200), server(0, 3, 300),
                service(700), service(800), service(801)
            )))
            add(scenario("local-cancels-immediate-server-publication", 1, listOf(
                server(0, 7, 100), local(0, 9, 200), service(701),
                server(0, 8, 702), service(702)
            )))
            add(scenario("local-cancels-protected-server-publication", 1, listOf(
                local(0, 1, 100), server(0, 2, 200), local(0, 3, 300), service(801)
            )))
            add(scenario("expired-but-unserviced-local-node-is-rearmed", 1, listOf(
                local(0, 1, 100), server(0, 2, 1000), service(1500), service(1501)
            )))
            add(scenario("mixed-expiry-unlinks-and-rescheduling", 400, listOf(
                local(0, 5, 100), server(128, 6, 100), local(256, 7, 100),
                server(384, 8, 100), service(200), server(128, 9, 200), service(201),
                server(384, 10, 201), service(600), service(601),
                server(256, 11, 601), service(601)
            )))
            add(scenario("clear-both-banks-and-schedules", 130, listOf(
                server(1, Int.MIN_VALUE, 100), local(129, Int.MAX_VALUE, 100),
                server(129, -1, 100), service(100), clear(200), service(1000),
                clear(1000), server(129, 4, 1001), service(1001)
            )))
        })
    }
    println(report)
}
