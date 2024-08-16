package com.jagex.game.runetek5.config.inputsubscriber;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;

public class KeyHoldInputSubscriber implements InputSubscriber {

    public final int[] requiredKeys;

    public KeyHoldInputSubscriber(int[] requiredKeys) {
        this.requiredKeys = requiredKeys;
    }

    public static KeyHoldInputSubscriber decode(Packet buffer) {
        int count = buffer.readUnsignedByte();
        int[] reqKeys = new int[count];
        for (int i = 0; i < count; i++) {
            reqKeys[i] = buffer.readUnsignedByte();
        }
        return new KeyHoldInputSubscriber(reqKeys);
    }

    @Override
    public boolean valid(MouseRecord dymmy, KeyRecord[] dummy1, int dummy2, KeyRecorder record) {
        int[] reqKeys = this.requiredKeys;
        for (int i = 0; i < reqKeys.length; i++) {
            int key = reqKeys[i];
            if (!record.held(key)) {
                return false;
            }
        }
        return true;
    }
}
