package com.jagex.game.runetek5.client.input.keyboard

import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.utils.Utils

class CanvasKeyRecorder(component: Component) : KeyRecorder(), KeyListener, FocusListener {

    @JvmField val keysAwaitingProcessing: NodeCollection = NodeCollection()

    @JvmField val keyRecordEntries: NodeCollection = NodeCollection()

    @JvmField val keysHeld: BooleanArray = BooleanArray(112)

    @JvmField var component: Component? = null

    init {
        remap()
        setComponent(component)
    }

    override fun held(index: Int): Boolean {
        return (index >= 0 && index < 112) && this.keysHeld[index]
    }

    fun addKeyRecordEntry(type: Int, character: Char, code: Int) {
        val entry = KeyRecordEntry()
        entry.type = type
        entry.keyCharacter = character
        entry.keyCode = code
        entry.timeSubmitted = Utils.time()
        this.keyRecordEntries.addFirst(entry)
    }

    @Synchronized
    override fun prepareAwaitingKeys() {
        this.keysAwaitingProcessing.remove_all()
        var entry = this.keyRecordEntries.popHead() as KeyRecordEntry?
        while (entry != null) {
            entry.flags = getFlags()
            if (entry.type == 0) {
                if (!this.keysHeld[entry.keyCode]) {
                    val keyHeldEntry = KeyRecordEntry()
                    keyHeldEntry.type = 0
                    keyHeldEntry.keyCharacter = 0xffff.toChar()
                    keyHeldEntry.keyCode = entry.keyCode
                    keyHeldEntry.timeSubmitted = entry.timeSubmitted
                    keyHeldEntry.flags = entry.flags
                    this.keysAwaitingProcessing.addFirst(keyHeldEntry)
                    this.keysHeld[entry.keyCode] = true
                }
                entry.type = 2
                this.keysAwaitingProcessing.addFirst(entry)
            } else if (entry.type == 1) {
                if (this.keysHeld[entry.keyCode]) {
                    this.keysAwaitingProcessing.addFirst(entry)
                    this.keysHeld[entry.keyCode] = false
                }
            } else if (entry.type == -1) {
                for (index in 0 until 112) {
                    if (this.keysHeld[index]) {
                        val unicodeKey = KeyRecordEntry()
                        unicodeKey.type = 1
                        unicodeKey.keyCharacter = 0xffff.toChar()
                        unicodeKey.keyCode = index
                        unicodeKey.timeSubmitted = entry.timeSubmitted
                        unicodeKey.flags = entry.flags
                        this.keysAwaitingProcessing.addFirst(unicodeKey)
                        this.keysHeld[index] = false
                    }
                }
            } else if (entry.type == 3) {
                this.keysAwaitingProcessing.addFirst(entry)
            }
            entry = this.keyRecordEntries.popHead() as KeyRecordEntry?
        }
    }

    override fun popHead(): KeyRecord? {
        return this.keysAwaitingProcessing.popHead() as? KeyRecord
    }

    fun onKeyPress(keyEvent: KeyEvent, type: Int) {
        var keyCode = keyEvent.keyCode
        if (keyCode != 0) {
            if (keyCode >= 0 && keyCode < REMAPPED_KEYS.size) {
                keyCode = REMAPPED_KEYS[keyCode]
                if (type == 0 && (keyCode and 0x80) != 0) {
                    keyCode = 0
                } else {
                    keyCode = keyCode and -129
                }
            } else {
                keyCode = 0
            }
        }
        if (keyCode != 0) {
            addKeyRecordEntry(type, 0xffff.toChar(), keyCode)
            keyEvent.consume()
        }
    }

    @Synchronized
    override fun keyPressed(keyEvent: KeyEvent) {
        onKeyPress(keyEvent, 0)
    }

    @Synchronized
    override fun keyReleased(keyEvent: KeyEvent) {
        onKeyPress(keyEvent, 1)
    }

    override fun focusGained(focusevent1: FocusEvent) {
    }

    @Synchronized
    override fun focusLost(focusEvent: FocusEvent) {
        addKeyRecordEntry(-1, '\u0000', 0)
    }

    fun reset() {
        if (this.component != null) {
            this.component!!.removeKeyListener(this)
            this.component!!.removeFocusListener(this)
            this.component = null
            for (index in 0 until 112) {
                this.keysHeld[index] = false
            }
            this.keysAwaitingProcessing.remove_all()
            this.keyRecordEntries.remove_all()
        }
    }

    @Synchronized
    override fun keyTyped(keyEvent: KeyEvent) {
        val keyChar = keyEvent.keyChar
        if (keyChar != 65535.toChar() && isValidKeyChar(keyChar)) {
            addKeyRecordEntry(3, keyChar, -1)
            keyEvent.consume()
        }
    }

    override fun addKeyListeners() {
        reset()
    }

    fun setComponent(component: Component) {
        reset()
        this.component = component
        this.component!!.addKeyListener(this)
        this.component!!.addFocusListener(this)
    }

    fun getFlags(): Int {
        var flags = 0
        if (this.keysHeld[81]) {
            flags = flags or 0x1
        }
        if (this.keysHeld[82]) {
            flags = flags or 0x4
        }
        if (this.keysHeld[86]) {
            flags = flags or 0x2
        }
        return flags
    }

    companion object {
        @JvmField val REMAPPED_KEYS: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 85, 80, 84, 0, 91, 0, 0, 0, 81, 82, 86, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 83, 104, 105, 103, 102, 96, 98, 97, 99, 0, 0, 0, 0, 0, 0, 0, 25, 16, 17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 48, 68, 66, 50, 34, 51, 52, 53, 39, 54, 55, 56, 70, 69, 40, 41, 32, 35, 49, 36, 38, 67, 33, 65, 37, 64, 0, 0, 0, 0, 0, 228, 231, 227, 233, 224, 219, 225, 230, 226, 232, 89, 87, 0, 88, 229, 90, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 0, 0, 101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        @JvmStatic
        fun create(component: Component): KeyRecorder {
            return CanvasKeyRecorder(component)
        }

        @JvmStatic
        fun remap() {
            REMAPPED_KEYS[44] = 71
            REMAPPED_KEYS[45] = 26
            REMAPPED_KEYS[46] = 72
            REMAPPED_KEYS[47] = 73
            REMAPPED_KEYS[59] = 57
            REMAPPED_KEYS[61] = 27
            REMAPPED_KEYS[91] = 42
            REMAPPED_KEYS[92] = 74
            REMAPPED_KEYS[93] = 43
            REMAPPED_KEYS[192] = 28
            REMAPPED_KEYS[222] = 58
            REMAPPED_KEYS[520] = 59
        }

        @JvmStatic
        fun isValidKeyChar(typedChar: Char): Boolean {
            if ((typedChar <= 0.toChar() || typedChar >= 128.toChar()) && (typedChar < 160.toChar() || typedChar > 255.toChar())) {
                if (typedChar != 0.toChar()) {
                    val unicodeTable = Utils.UNICODE_TABLE
                    for (index in unicodeTable.indices) {
                        val unicode = unicodeTable[index]
                        if (typedChar == unicode) {
                            return true
                        }
                    }
                }
                return false
            }
            return true
        }
    }
}
