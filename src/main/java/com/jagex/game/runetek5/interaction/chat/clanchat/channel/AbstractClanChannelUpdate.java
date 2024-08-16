package com.jagex.game.runetek5.interaction.chat.clanchat.channel;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;

public abstract class AbstractClanChannelUpdate extends Node<AbstractClanChannelUpdate> {

    public abstract void decode(Packet buffer);

    public abstract void applyTo(ClanChannel channel);
}
