package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 11/01/2022
 *
 */
public class UnbanUser extends AbstractClanSettingUpdate {

    public int bannedUserIndex;
    public final ClanSettingsDelta clanSettingsDelta;

    public UnbanUser(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
        this.bannedUserIndex = -1;
    }

    @Override
    public void decode(Packet buffer) {
        this.bannedUserIndex = buffer.readUnsignedShort();
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.unbanUser(this.bannedUserIndex);
    }
}
