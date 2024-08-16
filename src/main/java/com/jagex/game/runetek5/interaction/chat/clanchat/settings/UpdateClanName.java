package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 11/01/2022
 *
 */
public class UpdateClanName extends AbstractClanSettingUpdate {

    public String clanName;
    public final ClanSettingsDelta clanSettingsDelta;

    public UpdateClanName(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.clanName = this.clanName;
    }

    @Override
    public void decode(Packet buffer) {
        this.clanName = buffer.readString();
        buffer.readInt();
    }
}
