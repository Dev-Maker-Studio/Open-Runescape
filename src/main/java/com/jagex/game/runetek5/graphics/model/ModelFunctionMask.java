package com.jagex.game.runetek5.graphics.model;

/**
 * Usage: import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;
 */
public class ModelFunctionMask {
	// Predefined masks
	public static final int DISPLACE_X = 0x1;							// 1
	public static final int DISPLACE_Y = 0x2;							// 2
	public static final int DISPLACE_Z = 0x4;							// 4
	
	/**
	 * This might be incorrect
	 */
	public static final int DISPLACE_NORMALS = 0x8;						// 8
	public static final int INVERT = 0x10;								// 16
	public static final int ALTERS_VERTICES = 0x20;						// 32
	public static final int DISABLE_TEXTURES = 0x40;					// 64
	public static final int ALTERS_COLOR = 0x80;						// 128
	public static final int ALTERS_ALPHA = 0x100;						// 256
	public static final int ALTERS_LIGHT = 0x200;						// 512
	public static final int ALTERS_BILLBOARDS = 0x400;					// 1024
	public static final int HAS_TEXTURE_COORDS = 0x800;					// 2048
	
	
	public static final int ALTERS_AMBIENT = 0x1000;					// 4096
	public static final int ALTERS_CONTRAST = 0x2000;					// 8192
	public static final int RECOLOR = 0x4000;							// 16384
	public static final int RETEXTURE = 0x8000;							// 32768
	public static final int SHADED = 0x10000;							// 65536
	/*FIXME: */public static final int PROJECTION = 0x20000;			// 131072
	public static final int SHADOWED = 0x40000;							// 262144
	public static final int TINT = 0x80000;								// 524288
	public static final int ALTERS_LIGHT_ALPHA = 0x100000;				// 1048576
	
	
	// Merged masks
	public static final int TST = SHADOWED | PROJECTION | SHADED;
	public static final int FULL_DISPLACEMENT = DISPLACE_X | DISPLACE_Y | DISPLACE_Z;			// 7, 	0x7
	public static final int ROTATE_WITH_NORMALS = DISPLACE_NORMALS | DISPLACE_X | DISPLACE_Z;	// 13,	0xd
	public static final int ALTERS_FACES = ALTERS_COLOR | ALTERS_ALPHA; 						// 384, 0x180
	// 0x97098, 618648
	
	/**
	 * Merged mask consisting of
	 * {@link #DISPLACE_NORMALS}
	 * {@link #INVERT}
	 * {@link #ALTERS_COLOR}
	 * {@link #ALTERS_AMBIENT}
	 * {@link #ALTERS_CONTRAST}
	 * {@link #TINT}
	 * {@link #SHADED}
	 * {@link #RECOLOR}
	 */
	public static final int RESET_COLOR = DISPLACE_NORMALS | INVERT | ALTERS_COLOR | ALTERS_AMBIENT | ALTERS_CONTRAST | TINT | SHADED | RECOLOR;
}