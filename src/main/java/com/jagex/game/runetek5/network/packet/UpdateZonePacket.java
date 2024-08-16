package com.jagex.game.runetek5.network.packet;

public enum UpdateZonePacket {

    CUSTOMIZE_OBJECT(-1),
    REMOVE_GROUND_ITEM(3),
    CREATE_GROUND_ITEM(5),
    DESTROY_OBJECT(2),
    CREATE_OBJECT(6),
    MAP_PROJANIM(16),
    MAP_PROJANIM_HALFSQ(19),
    OBJECT_PREFETCH(5),
    GROUND_ITEM_REVEAL(7),
    GROUND_ITEM_COUNT(7),
    MIDI_SONG_LOCATION(8),
    TILE_MESSAGE(-1),
    OBJ_ANIM(6),
    SOUND_AREA(9),
    SPOT_ANIM(8);

    public final int size;
	public static int UPDATE_ZONE_PLANE;
	public static int UPDATE_ZONE_X;
	public static int UPDATE_ZONE_Y;

    UpdateZonePacket(int size) {
        this.size = size;
    }

}
