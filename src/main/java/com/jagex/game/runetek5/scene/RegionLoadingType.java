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
    public static final RegionLoadingType STANDARD = new RegionLoadingType(false, false);
    public static final RegionLoadingType INSTANCE = new RegionLoadingType(true, false);
    public static final RegionLoadingType INSTANCE_NO_ENTITY_RESET = new RegionLoadingType(true, false);
    public static final RegionLoadingType INSTANCE_LARGE = new RegionLoadingType(true, true);
    public static final RegionLoadingType INSTANCE_LARGE_NO_RELOAD_UPDATEZONES = new RegionLoadingType(true, true);
    /**
     * Reload requested by debug opcode
     */
    public static final RegionLoadingType RELOAD = new RegionLoadingType(false, false);

    public final boolean constructedMap;
    public final boolean ignoreUpdatezoneSizeLimitations;

    public RegionLoadingType(boolean constructedMap, boolean ignoreUpdatezoneSizeLimitations) {
        this.constructedMap = constructedMap;
        this.ignoreUpdatezoneSizeLimitations = ignoreUpdatezoneSizeLimitations;
    }

    public boolean ignoreUpdateZoneSizeLimitations() {
        return this.ignoreUpdatezoneSizeLimitations;
    }

    /**
     * Map build from dynamic map packet,
     * Example: player owned houses
     */
    public boolean constructedMap() {
        return this.constructedMap;
    }
}
