package com.rs.plugin.handlers;

import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.events.NPCInteractionDistanceEvent;

import java.util.function.BiFunction;

public class NPCInteractionDistanceHandler extends PluginHandler<NPCInteractionDistanceEvent> {
	
	private final BiFunction<Player, NPC, Integer> supplier;

	public NPCInteractionDistanceHandler(Object[] keys, BiFunction<Player, NPC, Integer> supplier) {
		super(keys, null);
		this.supplier = supplier;
	}
	
	public NPCInteractionDistanceHandler(int id, BiFunction<Player, NPC, Integer> supplier) {
		super(new Object[] { id }, null);
		this.supplier = supplier;
	}
	
	public NPCInteractionDistanceHandler(String name, BiFunction<Player, NPC, Integer> supplier) {
		super(new Object[] { name }, null);
		this.supplier = supplier;
	}

	@Override
	public final boolean handleGlobal(NPCInteractionDistanceEvent e) { return false; }

	@Override
	public final Object getObj(NPCInteractionDistanceEvent e) {
		return supplier.apply(e.getPlayer(), e.getNpc());
	}

}
