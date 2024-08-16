package com.jagex.game.runetek5.interaction.chat.clanchat.channel;

import com.jagex.game.runetek5.buffer.Packet;

public class KickPlayer extends AbstractClanChannelUpdate {

    public int playerIndex;
    public final ClanChannelDelta clanChannelDelta;

    public KickPlayer(ClanChannelDelta clanChannelDelta) {
        this.clanChannelDelta = clanChannelDelta;
        this.playerIndex = -1;
    }

    @Override
    public void decode(Packet buffer) {
        this.playerIndex = buffer.readUnsignedShort();
        buffer.readUnsignedByte();
        if (buffer.readUnsignedByte() != 255) {
            buffer.currentPosition--;
            buffer.readLong();
        }

    }

    @Override
    public void applyTo(ClanChannel clanChannel) {
        clanChannel.removePlayer(this.playerIndex);
    }
}
