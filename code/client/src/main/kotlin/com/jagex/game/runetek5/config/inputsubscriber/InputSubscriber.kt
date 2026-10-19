package com.jagex.game.runetek5.config.inputsubscriber

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.MouseRecord

interface InputSubscriber {
    fun valid(mouseRecord: MouseRecord?, keyRecords: Array<KeyRecord?>?, keyRecordCount: Int, keyRecorder: KeyRecorder?): Boolean

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): InputSubscriber? {
            val subscriberType = InputSubscriberType.forId(buffer.readUByte())
            if (subscriberType == InputSubscriberType.COMBINED)
                return CombinedInputSubscriber.decode(buffer)
            if (subscriberType == InputSubscriberType.KEY_PRESS)
                return KeyPressInputSubscriber.decode(buffer)
            if (subscriberType == InputSubscriberType.KEY_HOLD)
                return KeyHoldInputSubscriber.decode(buffer)
            return null
        }
    }
}
