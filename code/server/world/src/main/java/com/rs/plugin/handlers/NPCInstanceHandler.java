package com.rs.plugin.handlers;

import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.events.NPCInstanceEvent;

import java.util.function.BiFunction;

public class NPCInstanceHandler extends PluginHandler<NPCInstanceEvent> {
	
	private final BiFunction<Integer, Tile, NPC> instantiator;

	public NPCInstanceHandler(Object[] keys, BiFunction<Integer, Tile, NPC> instantiator) {
		super(keys, null);
		this.instantiator = instantiator;
	}
	
	public NPCInstanceHandler(int id, BiFunction<Integer, Tile, NPC> instantiator) {
		super(new Object[] { id }, null);
		this.instantiator = instantiator;
	}
	
	public NPCInstanceHandler(String name, BiFunction<Integer, Tile, NPC> instantiator) {
		super(new Object[] { name }, null);
		this.instantiator = instantiator;
	}

	@Override
	public final boolean handleGlobal(NPCInstanceEvent e) { return false; }

	@Override
	public final Object getObj(NPCInstanceEvent e) {
		NPC npc = instantiator.apply(e.getNpcId(), e.getTile());
		npc.setSpawned(e.isSpawned());
		return npc;
	}

}
