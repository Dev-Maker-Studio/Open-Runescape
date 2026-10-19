package com.rs.game.content.skills.hunter.tracking;

import world.gregs.voidps.type.Tile;

public enum Track {
	PISC_1(new Burrow(19439, Tile.of(2353, 3595, 0),
			new Trail(19375, Tile.of(2347, 3607, 0), 2976, 4,
					new Trail(19428, Tile.of(2355, 3601, 0), 2978, 3),
					new Trail(19428, Tile.of(2354, 3609, 0), 2983, 3),
					new Trail(19376, Tile.of(2348, 3612, 0), 2988, 3,
							new Trail(19428, Tile.of(2354, 3609, 0), 2989, 3),
							new Trail(29428, Tile.of(2351, 3619, 0), 2990, 3))),
			new Trail(19375, Tile.of(2347, 3607, 0), 2976, 4)
			));

    private Track(Burrow burrow) {
    }
}
