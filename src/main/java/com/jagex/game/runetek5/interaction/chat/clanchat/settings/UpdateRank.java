package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 11/01/2022
 *
 */
public class UpdateRank extends AbstractClanSettingUpdate {

    public final ClanSettingsDelta clanSettingsDelta;
    public int memberIndex;
    public byte rank;

    public UpdateRank(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
        this.memberIndex = -1;
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.updateRank(this.memberIndex, this.rank);
    }

    @Override
    public void decode(Packet buffer) {
        this.memberIndex = buffer.readUnsignedShort();
        this.rank = buffer.readByte();
    }
}
