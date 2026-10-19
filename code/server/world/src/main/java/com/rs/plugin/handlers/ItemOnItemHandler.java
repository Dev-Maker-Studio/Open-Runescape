package com.rs.plugin.handlers;

import com.rs.plugin.events.ItemOnItemEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ItemOnItemHandler extends PluginHandler<ItemOnItemEvent> {

	public ItemOnItemHandler(int[] itemsUsed, int[] usedWiths, Consumer<ItemOnItemEvent> handler) {
		super(null, handler);
		Set<Object> list = new HashSet<>();
		if (usedWiths.length > 0)
			for (Integer usedWith : usedWiths)
				for (int used : itemsUsed) {
					list.add((usedWith << 16) + used);
					list.add((used << 16) + usedWith);
				}
		else
			for (int used : itemsUsed)
				list.add(-used);
		keys = list.toArray();
	}

	public ItemOnItemHandler(int itemUsed, int[] usedWith, Consumer<ItemOnItemEvent> handler) {
		this(new int[] { itemUsed }, usedWith, handler);
	}

	public ItemOnItemHandler(int[] itemUsed, int usedWith, Consumer<ItemOnItemEvent> handler) {
		this(itemUsed, new int[] { usedWith }, handler);
	}

	public ItemOnItemHandler(int used, int usedWith, Consumer<ItemOnItemEvent> handler) {
		this(new int[] { used },  new int[] { usedWith }, handler);
	}

	public ItemOnItemHandler(int[] used, Consumer<ItemOnItemEvent> handler) {
		this(used,  new int[] { }, handler);
	}

	public ItemOnItemHandler(int used, Consumer<ItemOnItemEvent> handler) {
		this(new int[] { used },  new int[] { }, handler);
	}

	public ItemOnItemHandler(boolean directKeySet, Object[] keys, Consumer<ItemOnItemEvent> handler) {
		super(keys, handler);
	}

}
