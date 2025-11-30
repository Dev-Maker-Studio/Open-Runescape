package com.jagex.game.runetek5.client.input.mouse

import java.awt.Point
import java.util.LinkedList
import java.util.Queue

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.network.packet.MouseClickPacket
import com.jagex.game.runetek5.network.packet.MouseMovementPacket
import com.jagex.game.runetek5.utils.Utils

/**
 * Mouse action that will be processed server side
 * @author Brainpower
 */
open class MouseActionServer : MouseRecord() {

    @JvmField var clickMeta: Int = 0
    @JvmField var constant: Int = 0
    @JvmField var time: Long = 0
    @JvmField var x: Int = 0
    @JvmField var y: Int = 0

    fun getConstant(): Int {
        return this.constant
    }

    override fun pushToTop() {
        synchronized(cache) {
            if (cachePtr < cacheSize - 1) {
                cache[++cachePtr - 1] = this
            }
        }
    }

    override fun getX(): Int {
        return this.x
    }

    fun getClickMeta(): Int {
        return this.clickMeta
    }

    fun clientSidedOnly(): Boolean {
        return when (this.clickMeta) {
            160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 672, 674 -> true
            else -> false
        }
    }

    override fun getMeta(): Int {
        return when (this.clickMeta) {
            160, 512 -> 0
            163, 166, 169, 173, 515, 518, 521, 525 -> 2
            else -> 1
        }
    }

    override fun getTime(): Long {
        return this.time
    }

    fun offsetBy(point: Point) {
        this.x -= point.x
        this.y -= point.y
    }

    override fun getClickType(): Int {
        return when (this.clickMeta) {
            160, 512 -> -1
            161, 163, 513, 515 -> 0
            162, 514 -> 3
            164, 166, 516, 518 -> 2
            165, 517 -> 5
            167, 169, 519, 521 -> 1
            168, 520 -> 4
            170, 522 -> 6
            else -> -2
        }
    }

    override fun getY(): Int {
        return this.y
    }

    companion object {
        @JvmField var cachePtr: Int = 0
        @JvmField var cache: Array<MouseActionServer?> = arrayOfNulls(0)
        @JvmField var lastSubmitted: Long = -1L
        @JvmField var appletHasFocus: Boolean = true
        @JvmField val MOVE_PACKET: MouseMovementPacket = MouseMovementPacket()
        @JvmField val CLICK_PACKET: MouseClickPacket = MouseClickPacket()
        @JvmField val MOUSE_ACTIONS: Queue<MouseActionServer> = LinkedList<MouseActionServer>()
        @JvmField var cacheSize: Int = 0

        @JvmStatic
        fun refreshCache() {
            synchronized(MOUSE_ACTIONS) {
                while (true) {
                    val record = MOUSE_ACTIONS.poll() ?: return
                    record.pushToTop()
                }
            }
        }

        @JvmStatic
        fun cycle() {
            synchronized(MOUSE_ACTIONS) {
                val screenLocation: Point? = if (GameShell.gameCanvas.isShowing) {
                    GameShell.gameCanvas.locationOnScreen
                } else {
                    null
                }
                while (true) {
                    val record = MOUSE_ACTIONS.poll() ?: return
                    if (screenLocation != null && GameShell.gameCanvas.isShowing && GameShell.appletHasFocus) {
                        record.offsetBy(screenLocation)
                        if (!record.clientSidedOnly() && record.getX() < GameShell.screenCanvasWidth && record.getY() < GameShell.screenCanvasHeight * -969250379 && record.getX() >= 0 && record.getY() >= 0) {
                            val clickType = record.getClickType()
                            if (record.getClickType() == -1) {
                                CLICK_PACKET.append(record)
                            } else if (MouseRecord.clicktypeRequiresRecord(clickType)) {
                                CLICK_PACKET.addToQueue(record)
                            }
                        }
                    } else {
                        record.pushToTop()
                    }
                }
            }
        }

        @JvmStatic
        fun initialize() {
            cacheSize = 100
            cache = arrayOfNulls(100)
            cachePtr = 0
        }

        @JvmStatic
        fun createMouseRecord(x: Int, y: Int, meta: Int, constant: Int): MouseActionServer {
            synchronized(cache) {
                val record: MouseActionServer = if (cachePtr == 0) {
                    MouseActionServer()
                } else {
                    cache[--cachePtr]!!
                }
                record.x = x
                record.y = y
                record.clickMeta = meta
                record.constant = constant
                record.time = Utils.time()
                return record
            }
        }

        @JvmStatic
        fun reset() {
            MOVE_PACKET.reset()
            CLICK_PACKET.reset()
            lastSubmitted = -1L
            appletHasFocus = true
        }
    }
}
