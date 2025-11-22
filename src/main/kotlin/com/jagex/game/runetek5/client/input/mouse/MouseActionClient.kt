package com.jagex.game.runetek5.client.input.mouse

/**
 * Mouse action that will be processed client side with object pooling.
 *
 * @author Brainpower
 */
class MouseActionClient : MouseRecord() {

    @JvmField var mouseY: Int = 0
    @JvmField var clickType: Int = 0
    @JvmField var mouseX: Int = 0
    @JvmField var time: Long = 0L
    @JvmField var meta: Int = 0

    override fun getClickType(): Int {
        return clickType
    }

    override fun getX(): Int {
        return mouseX
    }

    override fun getY(): Int {
        return mouseY
    }

    override fun getMeta(): Int {
        return meta
    }

    override fun pushToTop() {
        synchronized(cache) {
            if (cursorPos < capacity - 1) {
                cache[++cursorPos - 1] = this
            }
        }
    }

    override fun getTime(): Long {
        return time
    }

    companion object {
        @JvmField var capacity: Int = 0
        @JvmField var cache: Array<MouseActionClient?> = emptyArray()
        @JvmField var cursorPos: Int = 0

        @JvmStatic
        fun initialize() {
            capacity = 100
            cache = arrayOfNulls(100)
            cursorPos = 0
        }

        @JvmStatic
        fun insert(clickType: Int, mouseX: Int, mouseY: Int, time: Long, meta: Int): MouseActionClient {
            synchronized(cache) {
                val record: MouseActionClient
                if (cursorPos == 0) {
                    record = MouseActionClient()
                } else {
                    record = cache[--cursorPos]!!
                }
                record.clickType = clickType
                record.mouseX = mouseX
                record.mouseY = mouseY
                record.time = time
                record.meta = meta
                return record
            }
        }
    }
}
