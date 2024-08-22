package com.jagex.game.runetek5.config.rsinterface;

import java.util.Arrays;

/**
 * Usage: import static com.jagex.game.runetek5.config.rsinterface.ComponentContentType.*;
 * @author Steven - 15/10/2023
 *
 */
public enum ComponentContentType {

	CONTENT_TYPE_328(328),
	
	/**
	 * The inner game window on which the game graphics,
	 * player, scene etc are drawn
	 */
	GAME_WINDOW(1337),
	
	/**
	 * Fullscreen window?
	 */
	CONTENT_TYPE_1403(1403),
	
	/**
	 * The inner part of the minimap that displays
	 * the world and entites in it.
	 * Does not include border
	 */
	MINIMAP(1338),
	
	/**
	 * The compass directly next to the minimap    
	 */
	COMPASS(1339),
	
	WORLDMAP(1400),
	
	/**
	 * The mini window displaying the world
	 * if clicked on 'overview' button whilst
	 * having the world map open
	 */
	WORLD_MAP_OVERVIEW(1401),
	
	/**
	 * The debug overlay with memory, fps etc
	 */
	DEBUG_INFORMATION(1405),
	
	CONTENT_TYPE_1406(1406),
	
	/**
	 * The fading of the screen encountered during cutscenes
	 */
	CUTSCENE_FADE(1407),
	
	/**
	 * The color picker used for example for picking
	 * completionist cape colors
	 */
	COLOR_PICKER(1408),
	
	CONTENT_TYPE_1409(1409);

	public final int id;

	ComponentContentType(int id) {
		this.id = id;
	}

	public static ComponentContentType forId(int id) {
		return Arrays.stream(ComponentContentType.values()).filter(it -> it.id == id).findFirst().orElse(null);
	}
}
