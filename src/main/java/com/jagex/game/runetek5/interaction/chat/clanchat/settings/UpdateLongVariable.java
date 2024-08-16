package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 11/01/2022
 *
 */
public class UpdateLongVariable extends AbstractClanSettingUpdate {

    public int key;
    public long value;
    public final ClanSettingsDelta clanSettingsDelta;

    public UpdateLongVariable(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.setLongVariable(this.key, this.value);
    }

    @Override
    public void decode(Packet buffer) {
        this.key = buffer.readInt();
        this.value = buffer.readLong();
    }
}
