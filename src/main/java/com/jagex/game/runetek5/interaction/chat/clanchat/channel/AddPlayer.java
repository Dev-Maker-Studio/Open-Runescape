package com.jagex.game.runetek5.interaction.chat.clanchat.channel;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 14/01/2023
 *
 */
public class AddPlayer extends AbstractClanChannelUpdate {

    public final ClanChannelDelta clanChannelDelta;

    public String name;

    public int world;

    public byte rank;

    public AddPlayer(ClanChannelDelta clanChannelDelta) {
        this.clanChannelDelta = clanChannelDelta;
        this.name = null;
    }

    @Override
    public void applyTo(ClanChannel channel) {
        ClanChannelPlayer player = new ClanChannelPlayer();
        player.name = this.name;
        player.world = this.world;
        player.rank = this.rank;
        channel.addPlayer(player);
    }

    @Override
    public void decode(Packet buffer) {
        if (buffer.readUnsignedByte() != 255) {
            buffer.currentPosition--;
            buffer.readLong();
        }
        this.name = buffer.readNullString();
        this.world = buffer.readUnsignedShort();
        this.rank = buffer.readByte();
        buffer.readLong();
    }
}
