package com.rs.plugin.handlers;

import com.rs.plugin.events.ItemEquipEvent;

import java.util.function.Consumer;

public class ItemEquipHandler extends PluginHandler<ItemEquipEvent> {
	public ItemEquipHandler(Object[] namesOrIds, Consumer<ItemEquipEvent> handler) {
		super(namesOrIds, handler);
	}
	
	public ItemEquipHandler(int id, Consumer<ItemEquipEvent> handler) {
		this(new Object[] { id }, handler);
	}
	
	public ItemEquipHandler(String name, Consumer<ItemEquipEvent> handler) {
		this(new Object[] { name }, handler);
	}
}
