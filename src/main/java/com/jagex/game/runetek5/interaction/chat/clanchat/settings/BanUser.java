package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Steven - 18/09/2023
 *
 */
public class BanUser extends AbstractClanSettingUpdate {

    public long bannedUserHash;
    public String bannedUserName;
    public final ClanSettingsDelta clanSettingsDelta;

    public BanUser(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
        this.bannedUserHash = -1L;
        this.bannedUserName = null;
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.banUser(this.bannedUserHash, this.bannedUserName);
    }

    @Override
    public void decode(Packet buffer) {
        if (buffer.readUByte() != 255) {
            buffer.currentPosition--;
            this.bannedUserHash = buffer.readLong();
        }

        this.bannedUserName = buffer.readNullString();
    }

}
