package com.jagex.game.runetek5.config.inputsubscriber;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;

public class KeyPressInputSubscriber implements InputSubscriber {
    public final int code;
    public final int flags;
    
	public KeyPressInputSubscriber(int code, int flags) {
        this.code = code;
        this.flags = flags;
    }

    static KeyPressInputSubscriber decode(Packet buffer) {
        int code = buffer.readUnsignedByte();
        int flags = buffer.readUnsignedByte();
        return new KeyPressInputSubscriber(code, flags);
    }

    @Override
    public boolean valid(MouseRecord mouseRecord, KeyRecord[] keyRecords, int count, KeyRecorder keyrecorder) {
        for (int index = 0; index < count && index < keyRecords.length; index++) {
            KeyRecord keyRecord = keyRecords[index];
            if (keyRecord != null && keyRecord.getCode() == this.code && keyRecord.getFlags() == this.flags) {
                return true;
            }
        }
        return false;
    }
}
