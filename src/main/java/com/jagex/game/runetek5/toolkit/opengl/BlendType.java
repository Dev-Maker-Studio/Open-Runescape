package com.jagex.game.runetek5.toolkit.opengl;

/**
 * 
 * @author Steven - 15/10/2023
 *
 */
public class BlendType {

	/**
	 * No blending
	 */
	public static final int DISABLED = 0;
	
	/**
	 * Used for standard alpha blending, 
	 * creating smooth and transparent effects
	 */
	public static final int ALPHA = 1;
	
	/**
	 * Used for additive blending, 
	 * which brightens and intensifies colors
	 */
	public static final int ADDITIVE = 2;
	
	/**
	 * Used for multiplying the source and destination colors, 
	 * often for shadowing and darkening effects
	 */
	public static final int MULTIPLY = 3;
}
