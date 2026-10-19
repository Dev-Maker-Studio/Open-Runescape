package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.NodeCollection

/**
 * Audio stream queue manager that handles multiple PCM streams with timing/priority.
 */
class PcmStreamMixer : PcmStream() {

    val subStreams: NodeCollection = NodeCollection()
    val scheduledEvents: NodeCollection = NodeCollection()
    var currentPosition: Int = 0
    var nextEventTime: Int = -1

    @Suppress("UNCHECKED_CAST")
    fun insertEvent(node: Node<Any?>, event: ScheduledAudioEvent) {
        var currentNode: Node<Any?> = node
        while (scheduledEvents.tail != currentNode && (currentNode as ScheduledAudioEvent).scheduledTime <= event.scheduledTime) {
            currentNode = currentNode.next as Node<Any?>
        }

        Node.replace(event, currentNode)
        nextEventTime = (scheduledEvents.tail.next as ScheduledAudioEvent).scheduledTime
    }

    @Synchronized
    fun addSubStream(stream: PcmStream) {
        subStreams.addLast(stream)
    }

    @Synchronized
    fun removeSubStream(stream: PcmStream) {
        stream.unlink()
    }

    fun updateEventTimings() {
        if (currentPosition > 0) {
            var event = scheduledEvents.getFirst() as ScheduledAudioEvent?
            while (event != null) {
                event.scheduledTime -= currentPosition
                event = scheduledEvents.next() as ScheduledAudioEvent?
            }

            nextEventTime -= currentPosition
            currentPosition = 0
        }
    }

    fun removeEvent(event: ScheduledAudioEvent) {
        event.unlink()
        event.onRemove()
        val nextNode = scheduledEvents.tail.next
        if (nextNode === scheduledEvents.tail) {
            nextEventTime = -1
        } else {
            nextEventTime = (nextNode as ScheduledAudioEvent).scheduledTime
        }
    }

    fun skipAllStreams(amount: Int) {
        var stream = subStreams.getFirst() as PcmStream?
        while (stream != null) {
            stream.skip(amount)
            stream = subStreams.next() as PcmStream?
        }
    }

    override fun getActiveVoiceCount(): Int = 0

    @Synchronized
    override fun fill(buffer: IntArray, offset: Int, length: Int) {
        var currentOffset = offset
        var remainingLength = length

        while (true) {
            if (nextEventTime < 0) {
                fillFromStreams(buffer, currentOffset, remainingLength)
                return
            } else if (currentPosition + remainingLength < nextEventTime) {
                currentPosition += remainingLength
                fillFromStreams(buffer, currentOffset, remainingLength)
                return
            } else {
                val amountToFill = nextEventTime - currentPosition
                fillFromStreams(buffer, currentOffset, amountToFill)
                currentOffset += amountToFill
                remainingLength -= amountToFill
                currentPosition += amountToFill
                updateEventTimings()

                val event = scheduledEvents.getFirst() as ScheduledAudioEvent
                synchronized(event) {
                    val result = event.processEvent(this)
                    if (result < 0) {
                        event.scheduledTime = 0
                        removeEvent(event)
                    } else {
                        event.scheduledTime = result
                        insertEvent(event.next as Node<Any?>, event)
                    }
                }

                if (remainingLength == 0) {
                    return
                }
            }
        }
    }

    fun fillFromStreams(buffer: IntArray, offset: Int, length: Int) {
        var stream = subStreams.getFirst() as PcmStream?
        while (stream != null) {
            stream.process(buffer, offset, length)
            stream = subStreams.next() as PcmStream?
        }
    }

    @Synchronized
    override fun skip(amount: Int) {
        var remainingAmount = amount

        while (true) {
            if (nextEventTime < 0) {
                skipAllStreams(remainingAmount)
                return
            } else if (currentPosition + remainingAmount < nextEventTime) {
                currentPosition += remainingAmount
                skipAllStreams(remainingAmount)
                return
            } else {
                val amountToSkip = nextEventTime - currentPosition
                skipAllStreams(amountToSkip)
                remainingAmount -= amountToSkip
                currentPosition += amountToSkip
                updateEventTimings()

                val event = scheduledEvents.getFirst() as ScheduledAudioEvent
                synchronized(event) {
                    val result = event.processEvent(this)
                    if (result < 0) {
                        event.scheduledTime = 0
                        removeEvent(event)
                    } else {
                        event.scheduledTime = result
                        insertEvent(event.next as Node<Any?>, event)
                    }
                }

                if (remainingAmount == 0) {
                    return
                }
            }
        }
    }

    override fun getNextStream(): PcmStream? = subStreams.next() as PcmStream?

    override fun getFirstStream(): PcmStream? = subStreams.getFirst() as PcmStream?
}
