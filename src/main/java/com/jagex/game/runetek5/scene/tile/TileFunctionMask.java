package com.jagex.game.runetek5.scene.tile;

public class TileFunctionMask {
	/**
	 * Has similar tile heights on all 4 sides
	 */
	public static final int COMPLETELY_FLAT = 0x1;
	
	/**
	 * The tile is lacking color
	 */
	public static final int HD_LIGHT = 0x2; // HD LIGHT..?
	
	/**
	 * Has animated textures
	 */
	public static final int TEXTURE_MOVEMENT = 0x4;
	
	/**
	 * Has underwater map
	 */
	public static final int HD_WATER = 0x8;
	
	/**
	 * Has HD shadow
	 */
	public static final int HD_SHADING = 0x10;
	
	/**
	 * Has 
	 */
	public static final int DISABLE_TEXTURES = 0x20;
	
	
	
	/**
	 * ************************************ *
	 * 			Merged function masks		*
	 * ************************************ *
	 */
	
	/**
	 * Applies attributes flat, monochrome, animated textures
	 */
	public static final int HAS_WATER_LIGHTING = COMPLETELY_FLAT | HD_LIGHT | TEXTURE_MOVEMENT;
}
