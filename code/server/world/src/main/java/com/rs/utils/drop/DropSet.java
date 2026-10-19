package com.rs.utils.drop;

import java.util.Arrays;
import java.util.List;

public class DropSet {

	protected int[] ids;
	protected String[] names;
	protected DropTable[] tables;
	protected transient DropList dropList;
	private transient boolean overflowed;

	public DropSet(int[] ids, String[] names, DropTable[] tables) {
		this.ids = ids;
		this.names = names;
		this.tables = tables;
	}

	public DropSet(DropTable... tables) {
		this.tables = tables;
	}

	public DropSet(List<DropTable> tables) {
		this.tables = new DropTable[tables.size()];
		this.tables = tables.toArray(this.tables);
	}

	public int[] getIds() {
		return ids;
	}

	public String[] getNames() {
		return names;
	}

	public DropTable[] getTables() {
		return tables;
	}

	public void setNames(String[] names) {
		this.names = names;
	}

	public DropList createDropList() {
		dropList = new DropList(tables);
		overflowed = dropList.isOverflowed();
		return dropList;
	}

	public DropList getDropList() {
		if (dropList == null)
			return createDropList();
		return dropList;
	}

	public boolean isOverflowed() {
		return overflowed;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (ids != null && ids.length > 0)
			s.append(Arrays.toString(ids)).append("\n");
		if (names != null && names.length > 0)
			s.append(Arrays.toString(names)).append("\n");
		for (DropTable table : tables)
			s.append(table.toString()).append("\n");
		s.append("[ (").append(getDropList().getNothingFracString()).append(") - (-1(Nothing)\n");
		return s.toString();
	}

	public boolean isEmpty() {
		return tables == null || tables.length == 0;
	}
}
