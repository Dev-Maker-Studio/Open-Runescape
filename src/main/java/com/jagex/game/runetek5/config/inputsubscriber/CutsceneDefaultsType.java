package com.jagex.game.runetek5.config.inputsubscriber;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.DefaultsFile;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class CutsceneDefaultsType {

    public InputSubscriber exitCutscene;

    public CutsceneDefaultsType(Js5ResourceProvider defaultsResource) {
        byte[] data = defaultsResource.getFile(DefaultsFile.CUTSCENE.fileId);
        decode(new Packet(data));
    }

    public void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            if (opcode == 1) {
                this.exitCutscene = InputSubscriber.decode(buffer);
            }
        }
    }
}