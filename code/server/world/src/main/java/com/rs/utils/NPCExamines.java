package com.rs.utils;

import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import com.rs.Settings;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.JsonFileManager;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.plugin.annotations.ServerStartupEvent.Priority;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@PluginEventHandler
public class NPCExamines {
	private static Map<Integer, String> EXAMINES = new HashMap<>();
	private final static String PATH = "./data/npcs/examines.json";

	@ServerStartupEvent(Priority.FILE_IO)
	public static void init() throws JsonIOException, IOException {
		if (new File(PATH).exists())
			EXAMINES = JsonFileManager.loadJsonFile(new File(PATH), new TypeToken<Map<Integer, String>>(){}.getType());
		else
			System.err.println("No NPC examines file found at " + PATH + "!");
	}

	public static String getExamine(NPC npc, Player player) {
		if(Settings.getConfig().isDebug())
			player.sendMessage(npc.getId() + ", X: " + npc.getX() + ", Y: " + npc.getY() + ", Plane: " + npc.getPlane() + ". Transforms with varbit "
					+ npc.getDefinitions().getVarbit() + " and var " + npc.getDefinitions().getVarp());
		String examine = EXAMINES.get(npc.getId());
		if (examine != null)
			return examine;
		return "It's " + Utils.addArticle(npc.getDefinitions().getName(player.getVars())).toLowerCase() + ".";
	}
}
