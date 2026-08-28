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

    override fun valid(mouseRecord: MouseRecord?, keyRecords: Array<KeyRecord?>?, keyRecordCount: Int, keyRecorder: KeyRecorder?): Boolean {
        if (mouseRecord == null) {
            return this.clickType == -1
        }
        if (this.clickType != mouseRecord.getClickType()) {
            return false
        }
        if (this.minimumClicks > mouseRecord.getMeta()) {
            return false
        }
        for (index in this.requiredKeys.indices) {
            val key = this.requiredKeys[index]
            if (keyRecorder?.held(key) != true) {
                return false
            }
        }
        return true
    }
}
