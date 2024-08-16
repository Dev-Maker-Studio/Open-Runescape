package com.jagex.game.runetek5.interaction.chat.clanchat.channel;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.NodeCollection;

public class ClanChannelDelta {

    public long pointer;

    public long updateNumber = -1L;

    public final NodeCollection updates = new NodeCollection();

    public ClanChannelDelta(Packet buffer) {
        decode(buffer);
    }

    public void decode(Packet buffer) {
        this.pointer = buffer.readLong();
        this.updateNumber = buffer.readLong();
        for (int opcode = buffer.readUnsignedByte(); opcode != 0; opcode = buffer.readUnsignedByte()) {
        	AbstractClanChannelUpdate channelUpdate;
            if (opcode == 1) {
                channelUpdate = new AddPlayer(this);
            } else if (opcode == 2) {
            	channelUpdate = new UpdatePlayer(this);
            } else if (opcode == 3) {
                channelUpdate = new KickPlayer(this);
            } else if (opcode == 4) {
                channelUpdate = new UpdateRights(this);
            }  else {
                throw new RuntimeException("");
            }
            channelUpdate.decode(buffer);
            this.updates.addFirst(channelUpdate);
        }
    }

    public void apply(ClanChannel clanChannel) {
        if (clanChannel.pointer == this.pointer && clanChannel.nextUpdateNumber == this.updateNumber) {
            for (AbstractClanChannelUpdate update = (AbstractClanChannelUpdate) this.updates.getFirst(); update != null; update = (AbstractClanChannelUpdate) this.updates.next()) {
                update.applyTo(clanChannel);
            }
            clanChannel.nextUpdateNumber++;
        } else {
            throw new RuntimeException("");
        }
    }
}
