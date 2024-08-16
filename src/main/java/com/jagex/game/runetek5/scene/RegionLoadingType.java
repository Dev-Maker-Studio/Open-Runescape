package com.jagex.game.runetek5.scene;

public class RegionLoadingType {

	/**
	 * The position of the camera reached the edge of the map
	 * on the animated login screen
	 */
    public static final RegionLoadingType ANIMATED_LOGIN_SCREEN = new RegionLoadingType(false, false);
    /**
     * Cutscene loading is requested
     */
    public static final RegionLoadingType CUTSCENE = new RegionLoadingType(true, false);
    public static final RegionLoadingType REGION_PACKET = new RegionLoadingType(false, false);
    public static final RegionLoadingType DYNAMIC_REGION = new RegionLoadingType(true, false);
    public static final RegionLoadingType MAPTYPE2 = new RegionLoadingType(true, false);
    public static final RegionLoadingType MAPTYPE3 = new RegionLoadingType(true, true);
    public static final RegionLoadingType MAPTYPE4 = new RegionLoadingType(true, true);
    /**
     * Reload requested by debug opcode
     */
    public static final RegionLoadingType RELOAD = new RegionLoadingType(false, false);

    public final boolean constructedMap;
    public final boolean aBool3159;

    public RegionLoadingType(boolean constructedMap, boolean bool2) {
        this.constructedMap = constructedMap;
        this.aBool3159 = bool2;
    }

    public boolean method4401() {
        return this.aBool3159;
    }

    /**
     * Map build from dynamic map packet,
     * Example: player owned houses
     */
    public boolean constructedMap() {
        return this.constructedMap;
    }
}
