package com.rs.game;

import world.gregs.voidps.type.Tile;
import world.gregs.voidps.type.Tiles;

/**
 * Java-friendly bridge for creating and manipulating {@link Tile} instances.
 * <p>
 * Kotlin inline value classes like {@code Tile} have mangled JVM method signatures
 * that make their companion factory methods inaccessible from Java. This class
 * provides clean static methods that Java code can call directly.
 * <p>
 * Usage: {@code Tile tile = Tile.of(3200, 3200, 0);}
 */
public final class TileHelper {

    private TileHelper() {}

    /** Create a Tile from x, y, and level coordinates. */
    public static Tile of(int x, int y, int level) {
        return (Tile) Tiles.of(x, y, level);
    }

    /** Create a Tile from x and y (level = 0). */
    public static Tile of(int x, int y) {
        return (Tile) Tiles.of(x, y);
    }

    /** Create a Tile from a packed tile id. */
    public static Tile ofId(int id) {
        return (Tile) Tiles.ofId(id);
    }

    /** Create a copy of the given tile. */
    public static Tile of(Tile tile) {
        return (Tile) Tiles.copy(tile);
    }

    /** Create a tile randomized within {@code randomize} distance of {@code tile}. */
    public static Tile of(Tile tile, int randomize) {
        return (Tile) Tiles.ofRandom(tile, randomize);
    }

    /** Transform (translate) a tile by the given deltas. */
    public static Tile transform(Tile tile, int dx, int dy, int dl) {
        return (Tile) Tiles.transform(tile, dx, dy, dl);
    }

    /** Transform (translate) a tile by the given deltas (no level change). */
    public static Tile transform(Tile tile, int dx, int dy) {
        return (Tile) Tiles.transform(tile, dx, dy);
    }

    /** Get the X coordinate within the scene for the given tile and base chunk id. */
    public static int getXInScene(Tile tile, int baseChunkId) {
        return Tiles.getXInScene(tile, baseChunkId);
    }

    /** Get the Y coordinate within the scene for the given tile and base chunk id. */
    public static int getYInScene(Tile tile, int baseChunkId) {
        return Tiles.getYInScene(tile, baseChunkId);
    }

    /** Get the chunk X for a tile (x / 8). */
    public static int getChunkX(Tile tile) {
        return Tiles.getChunkX(tile);
    }

    /** Get the chunk Y for a tile (y / 8). */
    public static int getChunkY(Tile tile) {
        return Tiles.getChunkY(tile);
    }

    /** Check if the given tile is at the specified x, y position. */
    public static boolean isAt(Tile tile, int x, int y) {
        return Tiles.isAt(tile, x, y);
    }
}
