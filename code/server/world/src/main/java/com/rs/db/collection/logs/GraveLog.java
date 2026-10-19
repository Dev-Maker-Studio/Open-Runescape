package com.rs.db.collection.logs;

import org.darkan.core.model.GroundItem;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.rs.utils.KtExtensionsKt.copyItem;

public class GraveLog {
	public enum Event { CREATED, STORED, RESTORED, COLLAPSED, DEMOLISHED, REPLACED }

	private final String uuid;
	private final String player;
	private final Event event;
	private final Tile tile;
	private final List<Item> items;

	public GraveLog(String player, Event event, Tile tile, List<GroundItem> groundItems) {
		this.player = player;
		this.event = event;
		this.tile = tile;
		items = new ArrayList<>();
		for (GroundItem item : groundItems)
			if (item != null)
				items.add(copyItem(item.getItem()));
		this.uuid = UUID.randomUUID().toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GraveLog graveLog = (GraveLog) o;
		return Objects.equals(uuid, graveLog.uuid) && Objects.equals(player, graveLog.player) && event == graveLog.event && Objects.equals(tile, graveLog.tile) && Objects.equals(items, graveLog.items);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, player, event, tile, items);
	}
}
