package com.jagex.game.runetek5.config.inputsubscriber

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.MouseRecord

class KeyHoldInputSubscriber(@JvmField val requiredKeys: IntArray) : InputSubscriber {

    override fun valid(dymmy: MouseRecord?, dummy1: Array<KeyRecord?>?, dummy2: Int, record: KeyRecorder?): Boolean {
        for (key in requiredKeys) {
            if (record?.held(key) != true) {
                return false
            }
        }
        return true
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): KeyHoldInputSubscriber {
            val count = buffer.readUByte()
            val reqKeys = IntArray(count)
            for (i in 0 until count) {
                reqKeys[i] = buffer.readUByte()
            }
            return KeyHoldInputSubscriber(reqKeys)
        }
    }
}
