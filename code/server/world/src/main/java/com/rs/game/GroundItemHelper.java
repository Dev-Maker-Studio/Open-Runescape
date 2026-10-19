package com.rs.game;

import org.darkan.core.model.GroundItem;
import org.darkan.core.model.GroundItemType;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;

/**
 * Java-friendly factory for creating GroundItem instances.
 * The Kotlin GroundItem data class has default parameter values that are not
 * accessible from Java without @JvmOverloads. This helper provides the
 * most common constructor patterns.
 */
public final class GroundItemHelper {

    private GroundItemHelper() {}

    /** Create a GroundItem with just item and tile (all other fields default). */
    public static GroundItem of(Item item, Tile tile) {
        return new GroundItem(item, tile, null, GroundItemType.NORMAL, 25, -1, -1);
    }

    /** Create a GroundItem with item, tile, and owner. */
    public static GroundItem of(Item item, Tile tile, String ownerUsername) {
        return new GroundItem(item, tile, ownerUsername, GroundItemType.NORMAL, 25, -1, -1);
    }

    /** Create a GroundItem with item, tile, owner, and type. */
    public static GroundItem of(Item item, Tile tile, String ownerUsername, GroundItemType type) {
        return new GroundItem(item, tile, ownerUsername, type, 25, -1, -1);
    }
}
