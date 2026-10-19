package com.rs.game.content.skills.hunter.tracking;

import world.gregs.voidps.type.Tile;

import java.util.ArrayList;
import java.util.List;

public class Trail {

	private Trail prev;

	public Trail(int nextObj, Tile nextObjTile, int varbit, int value, Trail... nexts) {
		if (nexts.length <= 0)
			return;
		List<Trail> next = new ArrayList<>();
		for (Trail t : nexts) {
			t.prev = this;
			next.add(t);
		}
	}
}