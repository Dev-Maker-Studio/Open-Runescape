package com.rs.utils.json;

import com.google.gson.*;
import world.gregs.voidps.type.Tile;

import java.lang.reflect.Type;

public class TileAdapter implements JsonSerializer<Tile>, JsonDeserializer<Tile> {

	@Override
	public JsonElement serialize(Tile tile, Type type, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("x", tile.getX());
		obj.addProperty("y", tile.getY());
		obj.addProperty("plane", tile.getLevel());
		return obj;
	}

	@Override
	public Tile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if (json.isJsonPrimitive()) {
			return new Tile(json.getAsInt());
		}
		JsonObject obj = json.getAsJsonObject();
		// Packed id format: {"id": 12345}
		if (obj.has("id")) {
			return new Tile(obj.get("id").getAsInt());
		}
		// Legacy named format: {"x": 3222, "y": 3218, "plane": 0}
		int x = obj.has("x") ? obj.get("x").getAsInt() : 0;
		int y = obj.has("y") ? obj.get("y").getAsInt() : 0;
		int plane = obj.has("plane") ? obj.get("plane").getAsInt()
				: obj.has("level") ? obj.get("level").getAsInt() : 0;
		return new Tile(x, y, plane);
	}
}
