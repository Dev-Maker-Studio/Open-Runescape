package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Steven - 18/09/2023
 *
 */
public class AddExistingMember extends AbstractClanSettingUpdate {

    public final ClanSettingsDelta clanSettingsDelta;
    public long memberHash;
    public String memberName;
    
    // Reference: https://runescape.wiki/w/Runedate     
    public int joinRuneday;

    public AddExistingMember(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
        this.memberHash = -1L;
        this.memberName = null;
        this.joinRuneday = 0;
    }

    @Override
    public void decode(Packet buffer) {
        if (buffer.readUnsignedByte() != 255) {
            buffer.currentPosition--;
            this.memberHash = buffer.readLong();
        }
        this.memberName = buffer.readNullString();
        this.joinRuneday = buffer.readUnsignedShort();
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.addMember(this.memberHash, this.memberName, this.joinRuneday);
    }
}
