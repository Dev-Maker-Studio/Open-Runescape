package com.rs.utils.drop;

import org.darkan.core.model.Item;
import org.darkan.core.Utils;
import com.rs.utils.DropSets;

public class Drop {

	private int id = -1;
	private int min;
	private int max;
	private String rollTable;

	public Drop(String rollTable) {
		this.rollTable = rollTable;
	}

	public Drop(int itemId, int min, int max) {
		id = itemId;
		this.min = min;
		this.max = max;
	}

	public Drop(int itemId, int amount) {
		id = itemId;
		min = amount;
		max = amount;
	}

	public Drop(int itemId) {
		id = itemId;
		min = 1;
		max = 1;
	}

	public Item toItem() {
		return new Item(id, getAmount());
	}

	public int getId() {
		return id;
	}

	public int getAmount() {
		int amt = Utils.random(min, max+1);
		if (max < 2)
			amt = 1;
		return amt;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public DropSet getRollTable() {
		return rollTable == null ? null : DropSets.getDropSet(rollTable);
	}

	@Override
	public String toString() {
		return "[" + id + ", " + min + ", " + max + "]";
	}
}
