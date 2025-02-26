package com.jagex.game.runetek5.config.inputsubscriber;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;

public interface InputSubscriber {
    static InputSubscriber decode(Packet buffer) {
        InputSubscriberType subscriberType = InputSubscriberType.forId(buffer.readUByte());
        if (subscriberType == InputSubscriberType.COMBINED)
            return CombinedInputSubscriber.decode(buffer);
        if (subscriberType == InputSubscriberType.KEY_PRESS)
            return KeyPressInputSubscriber.decode(buffer);
        if (subscriberType == InputSubscriberType.KEY_HOLD)
            return KeyHoldInputSubscriber.decode(buffer);
        return null;
    }

    boolean valid(MouseRecord var1, KeyRecord[] var2, int var3, KeyRecorder var4);
}
