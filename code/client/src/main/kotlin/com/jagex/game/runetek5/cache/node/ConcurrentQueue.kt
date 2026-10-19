package com.jagex.game.runetek5.cache.node

import java.util.*

@Suppress("UNCHECKED_CAST")
class ConcurrentQueue(capacity: Int) : AbstractQueue<Any>() {

    var queuedObjectNodes: Array<QueuedObjectNode?> = arrayOfNulls(capacity)
    var accessingThreadId = 0
    val referenceMap: MutableMap<Any, QueuedObjectNode> = HashMap()
    val queuedObjectComparator: Comparator<Any>? = null
    var currentPosition = 0

    /**
     * Doubles the size of the cache
     */
    fun expand() {
        val newSize = (queuedObjectNodes.size shl 1) + 1
        queuedObjectNodes = queuedObjectNodes.copyOf(newSize)
    }

    override val size: Int
        get() = currentPosition

    override fun peek(): Any? {
        return if (currentPosition == 0) null else queuedObjectNodes[0]?.`object`
    }

    override fun remove(element: Any?): Boolean {
        if (element == null) return false
        val queuedObjectNode = referenceMap.remove(element) ?: return false

        accessingThreadId++
        currentPosition--

        if (queuedObjectNode.slot == currentPosition) {
            queuedObjectNodes[currentPosition] = null
        } else {
            val nodeAtCurrentPosition = queuedObjectNodes[currentPosition]!!
            queuedObjectNodes[currentPosition] = null
            queuedObjectNodes[queuedObjectNode.slot] = nodeAtCurrentPosition
            queuedObjectNodes[queuedObjectNode.slot]!!.slot = queuedObjectNode.slot
            shiftForward(queuedObjectNode.slot)
            if (nodeAtCurrentPosition == queuedObjectNodes[queuedObjectNode.slot]) {
                shiftBackwards(queuedObjectNode.slot)
            }
        }

        return true
    }

    fun shiftBackwards(slot: Int) {
        var currentSlot = slot
        val queuedNode = queuedObjectNodes[currentSlot]!!

        while (currentSlot > 0) {
            val nextSlot = (currentSlot - 1) ushr 1
            val nextQueuedNode = queuedObjectNodes[nextSlot]!!

            if (queuedObjectComparator != null) {
                if (queuedObjectComparator.compare(queuedNode.`object`, nextQueuedNode.`object`) >= 0) {
                    break
                }
            } else if ((queuedNode.`object` as Comparable<Any>).compareTo(nextQueuedNode.`object`) >= 0) {
                break
            }

            queuedObjectNodes[currentSlot] = nextQueuedNode
            queuedObjectNodes[currentSlot]!!.slot = currentSlot
            currentSlot = nextSlot
        }

        queuedObjectNodes[currentSlot] = queuedNode
        queuedObjectNodes[currentSlot]!!.slot = currentSlot
    }

    fun shiftForward(slot: Int) {
        var currentSlot = slot
        val nodeAtCurrentSlot = queuedObjectNodes[currentSlot]!!
        val i5 = currentPosition ushr 1

        while (currentSlot < i5) {
            val forward1 = (currentSlot shl 1) + 1
            val queuedNodeForward1 = queuedObjectNodes[forward1]!!
            val forward2 = (currentSlot shl 1) + 2
            val queuedNodeForward2 = queuedObjectNodes[forward2]

            val nextSlot = if (queuedObjectComparator != null) {
                if (forward2 < currentPosition &&
                    queuedObjectComparator.compare(queuedNodeForward1.`object`, queuedNodeForward2!!.`object`) > 0
                ) {
                    forward2
                } else {
                    forward1
                }
            } else if (forward2 < currentPosition &&
                (queuedNodeForward1.`object` as Comparable<Any>).compareTo(queuedNodeForward2!!.`object`) > 0
            ) {
                forward2
            } else {
                forward1
            }

            if (queuedObjectComparator != null) {
                if (queuedObjectComparator.compare(nodeAtCurrentSlot.`object`, queuedObjectNodes[nextSlot]!!.`object`) <= 0) {
                    break
                }
            } else if ((nodeAtCurrentSlot.`object` as Comparable<Any>).compareTo(queuedObjectNodes[nextSlot]!!.`object`) <= 0) {
                break
            }

            queuedObjectNodes[currentSlot] = queuedObjectNodes[nextSlot]
            queuedObjectNodes[currentSlot]!!.slot = currentSlot
            currentSlot = nextSlot
        }

        queuedObjectNodes[currentSlot] = nodeAtCurrentSlot
        queuedObjectNodes[currentSlot]!!.slot = currentSlot
    }

    override fun contains(element: Any?): Boolean {
        return if (element == null) false else referenceMap.containsKey(element)
    }

    override fun toArray(): Array<Any> {
        val objects = super.toArray()
        if (queuedObjectComparator != null) {
            Arrays.sort(objects, queuedObjectComparator)
        } else {
            Arrays.sort(objects)
        }
        return objects
    }

    override fun iterator(): MutableIterator<Any> {
        return ConcurrentQueueIterator(this)
    }

    override fun offer(element: Any?): Boolean {
        if (element == null) return false
        if (referenceMap.containsKey(element)) {
            throw IllegalArgumentException("")
        }

        accessingThreadId++
        val position = currentPosition

        if (position >= queuedObjectNodes.size) {
            expand()
        }

        currentPosition++

        if (position == 0) {
            queuedObjectNodes[0] = QueuedObjectNode(element, 0)
            referenceMap[element] = queuedObjectNodes[0]!!
        } else {
            queuedObjectNodes[position] = QueuedObjectNode(element, position)
            referenceMap[element] = queuedObjectNodes[position]!!
            shiftBackwards(position)
        }

        return true
    }

    override fun poll(): Any? {
        if (currentPosition == 0) {
            return null
        }

        accessingThreadId++
        val object1 = queuedObjectNodes[0]!!.`object`
        referenceMap.remove(object1)
        currentPosition--

        if (currentPosition == 0) {
            queuedObjectNodes[currentPosition] = null
        } else {
            queuedObjectNodes[0] = queuedObjectNodes[currentPosition]
            queuedObjectNodes[0]!!.slot = 0
            queuedObjectNodes[currentPosition] = null
            shiftForward(0)
        }

        return object1
    }
}
