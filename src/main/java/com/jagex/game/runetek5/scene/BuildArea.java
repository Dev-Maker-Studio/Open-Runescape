package com.jagex.game.runetek5.scene;

public class BuildArea {

	public static final BuildArea ORIGINAL_SIZE = new BuildArea(0, 4);

    public static final BuildArea EXTENDED_SIZE = new BuildArea(1, 5);

    public static final BuildArea LARGE_SIZE = new BuildArea(2, 6);
    public final int chunkSize;
    public final int id;

    public BuildArea(int id, int size) {
        this.id = id;
        this.chunkSize = size;
    }

    public static BuildArea[] getAll() {
        return new BuildArea[]{LARGE_SIZE, ORIGINAL_SIZE, EXTENDED_SIZE};
    }

	public static BuildArea parse(int id) {
	    BuildArea[] dimensions = getAll();
	    for (int index = 0; index < dimensions.length; index++) {
	        BuildArea dimension = dimensions[index];
	        if (id == dimension.id) {
	            return dimension;
	        }
	    }
	    return null;
	}
}
