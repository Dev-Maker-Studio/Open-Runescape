package com.rs.utils.json;

import com.google.gson.*;
import com.rs.game.model.entity.player.Controller;
import org.darkan.core.Logger;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.plugin.annotations.ServerStartupEvent.Priority;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PluginEventHandler
public class ControllerAdapter implements JsonSerializer<Controller>, JsonDeserializer<Controller> {
	
	private static final Map<String, Class<?>> CONTROLLER_CLASSES = new HashMap<>();
	
	@ServerStartupEvent(Priority.FILE_IO)
	public static void init() {
		try {
			List<Class<Controller>> classes = Utils.getSubClasses("com.rs", Controller.class);
			for (Class<Controller> clazz : classes) {
				// Anonymous controllers have no simple name to save under and cannot be restored,
				// so they are runtime-only by nature rather than a registry collision.
				if (clazz.isAnonymousClass())
					continue;
				if (CONTROLLER_CLASSES.put(clazz.getSimpleName(), clazz) != null)
					Logger.error(ControllerAdapter.class, "init", "Duplicate controller class: " + clazz.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Logger.info(ControllerAdapter.class, "init", "Loaded " + CONTROLLER_CLASSES.size() + " controllers...");
	}

	@Override
	public JsonElement serialize(Controller controller, Type type, JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		result.add("type", new JsonPrimitive(controller.getClass().getSimpleName()));
		result.add("properties", context.serialize(controller, controller.getClass()));
		return result;
	}

	@Override
	public Controller deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
		String type = jsonObject.get("type").getAsString();
		JsonElement element = jsonObject.get("properties");
		Class<?> clazz = CONTROLLER_CLASSES.get(type);
		if (clazz == null) {
			Logger.handle(ControllerAdapter.class, "deserialize", new Error("Controller not found: " + type));
			return null;
		}
		return context.deserialize(element, clazz);
	}

}