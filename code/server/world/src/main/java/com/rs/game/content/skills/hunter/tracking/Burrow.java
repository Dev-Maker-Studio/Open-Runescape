package com.rs.game.content.skills.hunter.tracking;

import world.gregs.voidps.type.Tile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Burrow {

    public Burrow(int burrowId, Tile burrowTile, Trail... next) {
        List<Trail> next1 = new ArrayList<>();
        next1.addAll(Arrays.asList(next));
    }

}
