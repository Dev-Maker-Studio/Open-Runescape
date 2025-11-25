package com.jagex.game.runetek5.icmp

import jaclib.ping.IcmpService

/**
 *
 * @author Brainpower - 28/01/2023
 *
 * Important note:
 * Several seemingly unused methods here are actually used,
 * be careful with removing code or you'll find your client
 * getting stuck loading
 *
 */
open class IcmpServiceJaclib : IcmpService() {
    @JvmField var listeners: MutableList<ICmpServiceListener> = ArrayList()

    override protected fun notify(i: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.dummy(0 == i, -1942989186)
        }
    }

    override protected fun notify(bitpackedPosition: Int, meta: Int, dummyAlways0: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onMouseMoved(bitpackedPosition, meta, dummyAlways0, 53718982)
        }
    }

    override protected fun method11597(i: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.dummy(0 == i, -1493708806)
        }
    }

    override protected fun method11598(i: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.dummy(0 == i, 1478443735)
        }
    }

    override protected fun method11599(i: Int, i2: Int, i3: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onMouseMoved(i, i2, i3, 53718982)
        }
    }

    override protected fun method11600(i: Int, i4: Int, i5: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            listener.onMouseMoved(i, i4, i5, 53718982)
        }
    }

    companion object {
        @JvmStatic @Volatile var icmpService: IcmpServiceJaclib? = null

        @JvmStatic
        fun appendListener(toAdd: ICmpServiceListener) {
            if (icmpService == null) {
                throw IllegalStateException("")
            } else {
                icmpService!!.listeners.add(toAdd)
            }
        }

        @JvmStatic
        fun initialize() {
            if (start(-1)) {
                appendListener(MouseMovedICmpServiceListener())
            }
        }

        @JvmStatic
        fun start(dummy: Int): Boolean {
            do {
                val available: Boolean = try {
                    if (available()) break
                    false
                } catch (throwable: Throwable) {
                    return false
                }
                return available
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
                    // NOOP
                }
                icmpService = null
            }
        }
    }
}
