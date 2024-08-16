package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Steven - 18/09/2023
 *
 */
public class RemoveMember extends AbstractClanSettingUpdate {

    public int memberIndex;
    public final ClanSettingsDelta clanSettingsDelta;

    public RemoveMember(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
        this.memberIndex = -1;
    }

    @Override
    public void decode(Packet buffer) {
        this.memberIndex = buffer.readUnsignedShort();
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.removeMember(this.memberIndex);
    }
}
