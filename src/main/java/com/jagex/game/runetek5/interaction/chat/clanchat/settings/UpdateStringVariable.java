package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Steven - 18/09/2023
 *
 */
public class UpdateStringVariable extends AbstractClanSettingUpdate {

    public int key;
    public String value;
    public final ClanSettingsDelta clanSettingsDelta;

    public UpdateStringVariable(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.putStringValue(this.key, this.value);
    }

    @Override
    public void decode(Packet buffer) {
    	this.key = buffer.readInt();
    	this.value = buffer.readString();
    }
}
