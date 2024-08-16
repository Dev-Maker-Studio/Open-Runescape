package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Steven - 18/09/2023
 *
 */
public class UpdateIntVariable extends AbstractClanSettingUpdate {

    public int key;
    public int value;
    public final ClanSettingsDelta clanSettingsDelta;

    public UpdateIntVariable(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
    }

    @Override
    public void decode(Packet buffer) {
    	this.key = buffer.readInt();
    	this.value = buffer.readInt();
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.putIntVariable(this.key, this.value);
    }

}
