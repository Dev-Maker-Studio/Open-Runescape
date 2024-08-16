package com.jagex.game.runetek5.config.inputsubscriber;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;

public class CombinedInputSubscriber implements InputSubscriber {

    public final int clickType;
    public final int minimumClicks;
    public final int[] requiredKeys;

    public CombinedInputSubscriber(int clickType, int minimumClicks, int[] requiredKeys) {
    	this.clickType = clickType;
    	this.minimumClicks = minimumClicks;
    	this.requiredKeys = requiredKeys;
    }

    public static CombinedInputSubscriber decode(Packet buffer) {
        int clickType = buffer.readUnsignedByte();
        int minimumClicks = buffer.readUnsignedByte();
        int count = buffer.readUnsignedByte();
        int[] keys = new int[count];
        for (int index = 0; index < count; index++) {
            keys[index] = buffer.readUnsignedByte();
        }
        return new CombinedInputSubscriber(clickType, minimumClicks, keys);
    }

    @Override
    public boolean valid(MouseRecord record, KeyRecord[] records, int i_3, KeyRecorder recorder) {
        if (record == null) {
            return this.clickType == -1;
        } 
        if (this.clickType != record.getClickType()) {
            return false;
        }
        if (this.minimumClicks > record.getMeta()) {
            return false;
        }
        for (int index = 0; index < this.requiredKeys.length; index++) {
            int key = this.requiredKeys[index];
            if (!recorder.held(key)) {
                return false;
            }
        }
        return true;
    }
}
