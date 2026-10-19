package com.rs.utils.drop;

public class DropEntry {
	private final DropTable table;
	private final double min;
	private final double max;
	private boolean always;

	public DropEntry(DropTable table) {
		this(table, 0.0, 0.0);
		always = true;
	}

	public DropEntry(DropTable table, double min, double max) {
		this.table = table;
		this.min = min;
		this.max = max;
	}

	public DropTable getTable() {
		return table;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public boolean isAlways() {
		return always;
	}

	@Override
	public String toString() {
		return "[" + (table == null ? "Nothing" : table.toString()) + " (" + min + "-" + max + ")]";
	}
}
