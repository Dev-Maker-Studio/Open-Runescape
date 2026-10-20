package com.jagex.game.runetek5.icmp

import jaclib.ping.IcmpService

open class IcmpServiceJaclib : IcmpService() {
    @JvmField var listeners: MutableList<ICmpServiceListener> = ArrayList()

    override protected fun notify(state: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onNotify(0 == state, -1942989186)
        }
    }

    override protected fun notify(bitpackedPosition: Int, meta: Int, dummyAlways0: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onMouseEvent(bitpackedPosition, meta, dummyAlways0, 53718982)
        }
    }

    override protected fun method11597(state: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onNotify(0 == state, -1493708806)
        }
    }

    override protected fun method11598(state: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onNotify(0 == state, 1478443735)
        }
    }

    override protected fun method11599(bitpackedPosition: Int, meta: Int, dummyAlways0: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onMouseEvent(bitpackedPosition, meta, dummyAlways0, 53718982)
        }
    }

    override protected fun method11600(bitpackedPosition: Int, meta: Int, dummyAlways0: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onMouseEvent(bitpackedPosition, meta, dummyAlways0, 53718982)
        }
    }

    companion object {
        @JvmStatic @Volatile var icmpService: IcmpServiceJaclib? = null

        @JvmStatic
        fun appendListener(listener: ICmpServiceListener) {
            if (icmpService == null) {
                throw IllegalStateException("")
            } else {
                icmpService!!.listeners.add(listener)
            }
        }

        @JvmStatic
        fun initialize() {
            if (start(-1)) {
                appendListener(MouseEventICmpServiceListener())
            }
        }

        @JvmStatic
        fun start(dummy: Int): Boolean {
            do {
                val isAvailable: Boolean = try {
                    if (available()) break
                    false
                } catch (throwable: Throwable) {
                    return false
                }
                return isAvailable
            } while (false)
            if (icmpService != null) throw IllegalStateException("")
            icmpService = IcmpServiceJaclib()
            val thread = Thread(IcmpServiceRunnable())
            thread.isDaemon = true
            thread.start()
            return true
        }

        @JvmStatic
        fun exit() {
            if (icmpService != null) {
                try {
                    icmpService!!.quit()
                } catch (ignored: Throwable) {
                }
                icmpService = null
            }
        }
    }
}
