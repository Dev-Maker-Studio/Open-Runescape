package com.jagex.game.runetek5.interaction.chat.clanchat.channel;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 14/01/2023
 *
 */
public class UpdateRights extends AbstractClanChannelUpdate {

    public String clanName;
    public byte guestsTalk;
    public byte minRankToKick;
    public final ClanChannelDelta clanSettingsDelta;

    public UpdateRights(ClanChannelDelta clanSettingsDelta) {
        this.clanSettingsDelta = clanSettingsDelta;
    }

    @Override
    public void applyTo(ClanChannel channel) {
        channel.clanName = this.clanName;
        if (this.clanName != null) {
            channel.guestsTalk = this.guestsTalk;
            channel.minRankToKick = this.minRankToKick;
        }
    }

    @Override
    public void decode(Packet buffer) {
        this.clanName = buffer.readNullString();
        if (this.clanName != null) {
            buffer.readUnsignedByte();
            this.guestsTalk = buffer.readByte();
            this.minRankToKick = buffer.readByte();
        }
    }
}
