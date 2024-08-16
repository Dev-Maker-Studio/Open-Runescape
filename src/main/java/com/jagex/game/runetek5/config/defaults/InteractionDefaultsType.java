package com.jagex.game.runetek5.config.defaults;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.DefaultsFile;
import com.jagex.game.runetek5.config.inputsubscriber.InputSubscriber;
import com.jagex.game.runetek5.config.inputsubscriber.KeyHoldInputSubscriber;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 *
 * @author Brainpower - 19/04/2022
 * before: Defaults7Loader
 *
 */
public class InteractionDefaultsType {

    public InputSubscriber anInterface175878;
    public InputSubscriber anInterface175874;
    public InputSubscriber anInterface175875;
    public InputSubscriber openMinimenuInputSubscriber;
    public KeyHoldInputSubscriber minimenuKey;
    public KeyHoldInputSubscriber toggleRunKey;
    public KeyHoldInputSubscriber teleportKey;
    public boolean minimenuCursor;
    public int tooltipColorP2p;
    public int tooltipColorF2p;
    
	public InteractionDefaultsType(Js5ResourceProvider resource) {
        byte[] data = resource.getFile(DefaultsFile.INTERACTIONS.fileId);
        decode(new Packet(data));
    }

    public void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                return;
            }
            if (opcode == 1) {
                this.anInterface175878 = InputSubscriber.decode(buffer); // minimenu
            } else if (opcode == 2) {
                this.anInterface175874 = InputSubscriber.decode(buffer); // minimenu
            } else if (opcode == 3) {
                this.anInterface175875 = InputSubscriber.decode(buffer); // minimenu
            } else if (opcode == 4) {
                this.openMinimenuInputSubscriber = InputSubscriber.decode(buffer);
            } else if (opcode == 5) {
                this.minimenuKey = KeyHoldInputSubscriber.decode(buffer); // minimenu
            } else if (opcode == 6) {
                this.toggleRunKey = KeyHoldInputSubscriber.decode(buffer);
            } else if (opcode == 7) {
                this.teleportKey = KeyHoldInputSubscriber.decode(buffer);
            } else if (opcode == 8) {
                InputSubscriber.decode(buffer);
            } else if (opcode == 9) {
                InputSubscriber.decode(buffer);
            } else if (opcode == 10) {
                InputSubscriber.decode(buffer);
            } else if (opcode == 11) {
                this.minimenuCursor = true;
            } else if (opcode == 12) {
                this.tooltipColorP2p = buffer.readInt();
            } else if (opcode == 13) {
                this.tooltipColorF2p = buffer.readInt();
            }
        }
    }
}
