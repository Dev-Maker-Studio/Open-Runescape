package com.jagex.game.runetek5.client;

public enum BuildType {

    LIVE("LIVE", 0),
    BUILD_LIVE("BUILDLIVE", 3),
    RC("RC", 1),
    WIP("WIP", 2),
    INT_BETA("INTBETA", 4);

    public final String name;
    public final int id;

    BuildType(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public static BuildType parse(int id) {
        BuildType[] allTypes = getAll();
        for (int index = 0; index < allTypes.length; index++) {
            BuildType buildTypes = allTypes[index];
            if (id == buildTypes.id) {
                return buildTypes;
            }
        }
        return null;
    }

    public static BuildType[] getAll() {
        return new BuildType[]{BUILD_LIVE, WIP, INT_BETA, LIVE, RC};
    }
}
