package com.rs.plugin.handlers;

import com.rs.plugin.events.NPCDropEvent;

import java.util.function.Consumer;

public class NPCDropHandler extends PluginHandler<NPCDropEvent> {
	private final Object[] npcKeys;
    private final Object[] itemKeys;
	
	public NPCDropHandler(Object[] npcNamesOrIds, Object[] itemNamesOrIds, Consumer<NPCDropEvent> handler) {
		super(new Object[] { "meme" }, handler);
		this.npcKeys = npcNamesOrIds;
		this.itemKeys = itemNamesOrIds;
	}

	public Object[] getNpcKeys() {
		return npcKeys;
	}

	public Object[] getItemKeys() {
		return itemKeys;
	}
}
