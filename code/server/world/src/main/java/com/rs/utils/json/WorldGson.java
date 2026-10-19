package com.rs.utils.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rs.game.model.entity.player.Controller;
import world.gregs.voidps.type.Tile;

import java.util.Date;

/**
 * The one Gson configuration used for every world-side JSON round trip - player saves, GE offers,
 * logs and the data files. Anything that needs to read or write those documents outside the
 * running server (the migration verifier, tooling) must build its Gson here so the adapters can
 * never drift from what the server itself writes.
 */
public final class WorldGson {

	private WorldGson() {
	}

	public static Gson create() {
		return new GsonBuilder()
				.registerTypeAdapter(Controller.class, new ControllerAdapter())
				.registerTypeAdapter(Tile.class, new TileAdapter())
				.registerTypeAdapter(Date.class, new DateAdapter())
				.disableHtmlEscaping()
				.setPrettyPrinting()
				.create();
	}
}
