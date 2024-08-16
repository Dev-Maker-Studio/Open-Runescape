package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Steven - 18/09/2023
 *
 */
public class UpdateBitpackedVariable extends AbstractClanSettingUpdate {

    public int key;
    public int value;
    public int shift;
    public int bitmaskSize;
    public final ClanSettingsDelta clanSettingsDelta;

    public UpdateBitpackedVariable(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
    }

    @Override
    public void decode(Packet buffer) {
        this.key = buffer.readInt();
        this.value = buffer.readInt();
        this.shift = buffer.readUnsignedByte();
        this.bitmaskSize = buffer.readUnsignedByte();
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.updateBitpackedVariable(this.key, this.value, this.shift, this.bitmaskSize);
    }
}
