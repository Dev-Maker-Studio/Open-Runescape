package com.rs.utils.drop;

public class WeightedTable extends DropTable {

	private final transient int weight;

	public WeightedTable(int weight, Drop... drops) {
		this.weight = weight;
		this.drops = drops;
		dropOne = true;
	}

	public WeightedTable(int weight, String rollTable) {
		this.weight = weight;
		this.rollTable = rollTable;
	}

	public WeightedTable(Drop... drops) {
		this(1, drops);
	}

	public WeightedTable(String rollTable) {
		this(1, rollTable);
	}

	public int getWeight() {
		return weight;
	}

}
