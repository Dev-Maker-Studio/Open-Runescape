package com.jagex.game.runetek5.config.inputsubscriber

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.MouseRecord

class CombinedInputSubscriber(
    @JvmField val clickType: Int,
    @JvmField val minimumClicks: Int,
    @JvmField val requiredKeys: IntArray
) : InputSubscriber {

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): CombinedInputSubscriber {
            val clickType = buffer.readUByte()
            val minimumClicks = buffer.readUByte()
            val count = buffer.readUByte()
            val keys = IntArray(count)
            for (index in 0 until count) {
                keys[index] = buffer.readUByte()
            }
            return CombinedInputSubscriber(clickType, minimumClicks, keys)
        }
    }

    override fun valid(record: MouseRecord?, records: Array<KeyRecord?>?, i_3: Int, recorder: KeyRecorder?): Boolean {
        if (record == null) {
            return this.clickType == -1
        }
        if (this.clickType != record.getClickType()) {
            return false
        }
        if (this.minimumClicks > record.getMeta()) {
            return false
        }
        for (index in this.requiredKeys.indices) {
            val key = this.requiredKeys[index]
            if (recorder?.held(key) != true) {
                return false
            }
        }
        return true
    }
}
