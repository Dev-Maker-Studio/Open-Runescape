package org.darkan.world.harness

import io.ktor.utils.io.ByteChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.darkan.core.net.Isaac
import org.darkan.core.net.ProtSession
import org.darkan.core.net.Session
import org.darkan.core.net.prot.ClientProt
import org.darkan.core.net.prot.Codec
import org.darkan.core.net.prot.ServerProt

/**
 * A [Session] with no socket behind it.
 *
 * The real session hands every outgoing packet to a private writer coroutine that encodes it onto
 * a [io.ktor.utils.io.ByteWriteChannel]. The harness cancels that coroutine and waits for it to
 * die before anything is queued - a bare `cancel()` returns while the writer is still draining, and
 * whatever it swallowed in that window is lost to the recorder - so everything the server queues
 * piles up in the session's outbound queue where the test can read it back as the original
 * [ServerProt] objects rather than as bytes. Nothing in the production session is
 * modified or subclassed away - `queuePacket`, `requestFlush` and `flush` all take their normal
 * path, they just never reach a socket.
 *
 * Both kinds of outbound work are recorded, in order: queued packets and flush requests. That
 * distinction matters - a packet the server queues but never flushes never reaches the client, so
 * a test that only asserted "was it queued" would pass on a bug the player sees as nothing
 * happening. Use [flushed] for "the client would actually have received this".
 *
 * Inbound packets are pushed straight onto the session's public read channel, which is exactly
 * where [Session.readPackets] would have put them, so `Player.processPackets()` sees them
 * unchanged.
 */
class RecordingSession(codec: Codec) : Session(
    write = ByteChannel(autoFlush = true),
    isaacIn = Isaac(intArrayOf(0, 0, 0, 0)),
    isaacOut = null,
    ip = "127.0.0.1",
    codec = codec,
) {

    /** One unit of outbound work the server handed to the session. */
    sealed interface Event {
        data class Queued(val packet: ServerProt) : Event
        data object Flush : Event
    }

    private val outbound: Channel<Any>
    private val recorded = mutableListOf<Event>()

    init {
        val writerJobField = ProtSession::class.java.getDeclaredField("writerJob").apply { isAccessible = true }
        runBlocking { (writerJobField.get(this@RecordingSession) as Job).cancelAndJoin() }
        val outboundField = ProtSession::class.java.getDeclaredField("outbound").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        outbound = outboundField.get(this) as Channel<Any>
    }

    /** Packets and flushes in the order the server produced them. */
    val events: List<Event>
        get() {
            drainOutbound()
            return recorded.toList()
        }

    /** Everything queued to the client so far, flushed or not, oldest first. */
    val sent: List<ServerProt>
        get() = events.filterIsInstance<Event.Queued>().map { it.packet }

    /** Only the packets a flush has actually pushed - i.e. what the client would have received. */
    val flushed: List<ServerProt>
        get() {
            val all = events
            val lastFlush = all.indexOfLast { it is Event.Flush }
            if (lastFlush == -1) return emptyList()
            return all.take(lastFlush).filterIsInstance<Event.Queued>().map { it.packet }
        }

    /** Number of flush requests the server has made since the last [clearSent]. */
    val flushes: Int
        get() = events.count { it is Event.Flush }

    /** Forgets everything recorded so far, so a test can assert on one tick in isolation. */
    fun clearSent() {
        drainOutbound()
        recorded.clear()
    }

    /** Feeds [packet] to the player as if the client had sent it. */
    fun pushClientPacket(packet: ClientProt) {
        check(readChannel.trySend(packet).isSuccess) { "Read channel full; the player is not draining packets" }
    }

    private fun drainOutbound() {
        while (true) {
            val item = outbound.tryReceive().getOrNull() ?: return
            val prot = protOf(item)
            recorded.add(if (prot != null) Event.Queued(prot) else Event.Flush)
        }
    }

    private fun protOf(item: Any): ServerProt? {
        val field = item.javaClass.declaredFields.firstOrNull { it.name == "prot" } ?: return null
        field.isAccessible = true
        return field.get(item) as? ServerProt
    }
}
