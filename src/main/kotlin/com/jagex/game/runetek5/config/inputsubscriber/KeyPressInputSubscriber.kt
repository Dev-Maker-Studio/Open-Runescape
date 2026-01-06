package com.jagex.game.runetek5.config.inputsubscriber

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.MouseRecord

class KeyPressInputSubscriber(
    @JvmField val code: Int,
    @JvmField val flags: Int
) : InputSubscriber {

    override fun valid(mouseRecord: MouseRecord?, keyRecords: Array<KeyRecord?>?, count: Int, keyrecorder: KeyRecorder?): Boolean {
        for (index in 0 until count.coerceAtMost(keyRecords?.size ?: 0)) {
            val keyRecord = keyRecords?.get(index)
            if (keyRecord != null && keyRecord!!.getCode() == this.code && keyRecord!!.getFlags() == this.flags) {
                return true
            }
        }
        return false
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): KeyPressInputSubscriber {
            val code = buffer.readUByte()
            val flags = buffer.readUByte()
            return KeyPressInputSubscriber(code, flags)
        }
    }
}
