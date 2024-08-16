package com.jagex.game.runetek5.interaction.chat.clanchat.channel;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 14/01/2023
 *
 */
public class UpdatePlayer extends AbstractClanChannelUpdate {

    public int playerIndex;

    public byte rank;

    public int world;

    public String name;

    public final ClanChannelDelta clanChannelDelta;

    public UpdatePlayer(ClanChannelDelta clanChannelDelta) {
    	this.clanChannelDelta = clanChannelDelta;
        this.playerIndex = -1;
    }

    @Override
    public void applyTo(ClanChannel clanChannel) {
        ClanChannelPlayer player = clanChannel.players[this.playerIndex];
        player.rank = this.rank;
        player.world = this.world;
        player.name = this.name;
    }

    @Override
    public void decode(Packet buffer) {
        this.playerIndex = buffer.readUnsignedShort();
        this.rank = buffer.readByte();
        this.world = buffer.readUnsignedShort();
        buffer.readLong();
        this.name = buffer.readString();
    }
}
