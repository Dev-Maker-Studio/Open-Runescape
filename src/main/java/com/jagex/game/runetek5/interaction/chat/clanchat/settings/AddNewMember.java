package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 11/01/2023
 *
 */
public class AddNewMember extends AbstractClanSettingUpdate {

    public long memberHash;
    public String memberName;
    public final ClanSettingsDelta clanSettingsDelta;

    public AddNewMember(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
        this.memberHash = -1L;
        this.memberName = null;
    }

    @Override
    public void decode(Packet buffer) {
        if (buffer.readUnsignedByte() != 255) {
            buffer.currentPosition--;
            this.memberHash = buffer.readLong();
        }
        this.memberName = buffer.readNullString();
    }

    @Override
    public void apply(ClanSettings clansettings) {
        clansettings.addMember(this.memberHash, this.memberName, 0);
    }
}
