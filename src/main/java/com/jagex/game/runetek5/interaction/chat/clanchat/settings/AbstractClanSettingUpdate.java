package com.jagex.game.runetek5.interaction.chat.clanchat.settings;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;

public abstract class AbstractClanSettingUpdate extends Node {
	
    public abstract void decode(Packet buffer);

    public abstract void apply(ClanSettings clansettings);
}
