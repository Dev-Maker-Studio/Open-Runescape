package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 11/01/2022
 *
 */
public class UpdatePermission extends AbstractClanSettingUpdate {

    public boolean allowGuests;
    public byte talkRank;
    public byte kickRank;
    public byte lootshareRank;
    public byte coinShare;
    public final ClanSettingsDelta clanSettingsDelta;

    public UpdatePermission(ClanSettingsDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
    }

    @Override
    public void apply(ClanSettings clanSettings) {
        clanSettings.allowGuests = this.allowGuests;
        clanSettings.talkRank = this.talkRank;
        clanSettings.kickRank = this.kickRank;
        clanSettings.lootshareRank = this.lootshareRank;
        clanSettings.coinShare = this.coinShare;
    }

    @Override
    public void decode(Packet buffer) {
        this.allowGuests = buffer.readUnsignedByte() == 1;
        this.talkRank = buffer.readByte();
        this.kickRank = buffer.readByte();
        this.lootshareRank = buffer.readByte();
        this.coinShare = buffer.readByte();
    }
}
