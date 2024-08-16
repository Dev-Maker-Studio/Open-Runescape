package com.jagex.game.runetek5.graphics.model;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class ModelFunctionMaskGL {
	
	public static final int EMPTY = 0;
	public static final int SMOOTH_EDGES = 0x1;
	public static final int WIREFRAME_MODE = 0x2;
	public static final int TRANSPARENT = 0x4;

    public static boolean altersVerticePosition(int functionMask) {
        return (functionMask & (ALTERS_VERTICES | INVERT | FULL_DISPLACEMENT)) != 0;
    }

    public static boolean altersColor(int functionMask) {
        return (functionMask & (TINT | RECOLOR | ALTERS_COLOR)) != 0;
    }

    public static boolean altersAlpha(int functionMask) {
        return (functionMask & (ALTERS_LIGHT_ALPHA | ALTERS_ALPHA)) != 0;
    }

    public static boolean altersFacing(int functionMask) {
        return (functionMask & INVERT) != 0;
    }

    public static boolean altersTextures(int functionMask) {
        return (functionMask & RETEXTURE) != 0;
    }

    public static boolean copyBillboards(int functionMask) {
        return (functionMask & (RETEXTURE | RECOLOR | ALTERS_BILLBOARDS | ALTERS_ALPHA | ALTERS_COLOR)) != 0;
    }

    public static boolean altersShading(int functionMask) {
        return (functionMask & SHADED) != 0;
    }

    public static boolean hasVertices(int functionMask) {
        return (functionMask & HAS_TEXTURE_COORDS) != 0;
    }

    public static boolean hasNormals(int i_0, int i_1) {
        return (i_0 & HAS_TEXTURE_COORDS) != 0 && (i_1 & (ALTERS_VERTICES | INVERT | FULL_DISPLACEMENT)) != 0;
    }

    public static boolean hasColors(int functionMask) {
        return (functionMask & HAS_TEXTURE_COORDS) != 0;
    }

    public static boolean altersLight(int functionMask) {
        return (functionMask & (ALTERS_LIGHT | ALTERS_VERTICES)) == 544 | (functionMask & (INVERT | DISPLACE_NORMALS)) != 0;
    }

    public static boolean hasElementsBuffer(int functionMask) {
        return (functionMask & HAS_TEXTURE_COORDS) != 0;
    }

    public static boolean method2199(int functionMask, int i_1) {
        return hasColors(functionMask) && ((functionMask & 0xb000) != 0 | altersColor(functionMask) | altersAlpha(functionMask) || (i_1 & 0x37) == 0 & (altersLight(functionMask) | altersShading(functionMask)));
    }

    public static boolean method2200() {
        return false;
    }

    public static boolean hasTextureCoordinates(int functionMask) {
        return (functionMask & HAS_TEXTURE_COORDS) != 0;
    }

    public static boolean method2202(int functionMask) {
        return (functionMask & (SHADOWED | PROJECTION | SHADED)) != 0 | ModelFunctionMaskGL.altersVerticesX(functionMask) || altersAnyVerticeType(functionMask);
    }

    public static boolean method2204(int functionMask, int i_1) {
        return (functionMask & SHADED) != 0 | altersLight(functionMask) || method2261(functionMask, i_1) || (i_1 & 0x37) == 0 && method2199(functionMask, i_1);
    }

    public static boolean method2205(int functionMask, int i_1) {
        return altersColor(functionMask) || method2199(functionMask, i_1);
    }

    public static boolean method2206(int functionMask, int i_1) {
        return (functionMask & SHADOWED) != 0 | altersAlpha(functionMask) || method2199(functionMask, i_1);
    }

    public static boolean method2211(int functionMask, int i_1) {
        return (functionMask & (PROJECTION | SHADOWED)) != 0 | altersFacing(functionMask) || method2199(functionMask, i_1) || ModelFunctionMaskGL.copyElementsBuffer(functionMask);
    }

    public static boolean method2212(int functionMask, int i_1) {
        return (functionMask & HAS_TEXTURE_COORDS) != 0 | altersTextures(functionMask) || method2199(functionMask, i_1);
    }

    public static boolean method2229(int functionMask) {
        return (functionMask & 0x70000) != 0 | ModelFunctionMaskGL.altersVerticesY(functionMask) || altersAnyVerticeType(functionMask);
    }

    public static boolean method2241() {
        return method2200();
    }

    public static boolean method2261(int functionMask, int i_1) {
        return ((functionMask & ALTERS_CONTRAST) != 0 | altersLight(functionMask) | altersShading(functionMask)) & hasNormals(functionMask, i_1);
    }

	public static boolean altersBillboards(int functionMask) {
	    return (functionMask & ALTERS_BILLBOARDS) != 0;
	}

	public static boolean altersFaces(int functionMask) {
	    return (functionMask & (ALTERS_ALPHA | ALTERS_COLOR)) != 0;
	}

	public static boolean copyElementsBuffer(int functionMask) {
	    return altersFacing(functionMask) & hasElementsBuffer(functionMask);
	}

	public static boolean method2252(int functionMask) {
	    return (functionMask & (SHADOWED | PROJECTION | SHADED)) != 0 | altersVerticesZ(functionMask) || altersAnyVerticeType(functionMask);
	}

    public static boolean altersAnyVerticeType(int functionMask) {
        return (altersVerticesX(functionMask) |
        		altersVerticesY(functionMask) |
        		altersVerticesZ(functionMask)) &
        		hasVertices(functionMask);
    }
	
    public static boolean altersVertices(int functionMask) {
        return (functionMask & ALTERS_VERTICES) != 0;
    }
	
	public static boolean altersVerticesX(int functionMask) {
	    return (functionMask & (ALTERS_VERTICES | DISPLACE_X)) != 0;
	}

	public static boolean altersVerticesY(int functionMask) {
	    return (functionMask & (ALTERS_VERTICES | DISPLACE_Y)) != 0;
	}
	
    public static boolean altersVerticesZ(int functionMask) {
        return (functionMask & (ALTERS_VERTICES | DISPLACE_Z | INVERT)) != 0;
    }

}
