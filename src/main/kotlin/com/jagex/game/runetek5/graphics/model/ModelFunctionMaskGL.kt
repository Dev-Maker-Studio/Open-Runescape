package com.jagex.game.runetek5.graphics.model

object ModelFunctionMaskGL {

    const val EMPTY = 0
    const val SMOOTH_EDGES = 0x1
    const val WIREFRAME_MODE = 0x2
    const val TRANSPARENT = 0x4

    @JvmStatic
    fun altersVerticePosition(functionMask: Int): Boolean {
        return (functionMask and (ModelFunctionMask.ALTERS_VERTICES or ModelFunctionMask.INVERT or ModelFunctionMask.FULL_DISPLACEMENT)) != 0
    }

    @JvmStatic
    fun altersColor(functionMask: Int): Boolean {
        return (functionMask and (ModelFunctionMask.TINT or ModelFunctionMask.RECOLOR or ModelFunctionMask.ALTERS_COLOR)) != 0
    }

    @JvmStatic
    fun altersAlpha(functionMask: Int): Boolean {
        return (functionMask and (ModelFunctionMask.ALTERS_LIGHT_ALPHA or ModelFunctionMask.ALTERS_ALPHA)) != 0
    }

    @JvmStatic
    fun altersFacing(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.INVERT) != 0
    }

    @JvmStatic
    fun altersTextures(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.RETEXTURE) != 0
    }

    @JvmStatic
    fun copyBillboards(functionMask: Int): Boolean {
        return (functionMask and (ModelFunctionMask.RETEXTURE or ModelFunctionMask.RECOLOR or ModelFunctionMask.ALTERS_BILLBOARDS or ModelFunctionMask.ALTERS_ALPHA or ModelFunctionMask.ALTERS_COLOR)) != 0
    }

    @JvmStatic
    fun altersShading(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.SHADED) != 0
    }

    @JvmStatic
    fun hasVertices(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.HAS_TEXTURE_COORDS) != 0
    }

    @JvmStatic
    fun hasNormals(i_0: Int, i_1: Int): Boolean {
        return (i_0 and ModelFunctionMask.HAS_TEXTURE_COORDS) != 0 && (i_1 and (ModelFunctionMask.ALTERS_VERTICES or ModelFunctionMask.INVERT or ModelFunctionMask.FULL_DISPLACEMENT)) != 0
    }

    @JvmStatic
    fun hasColors(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.HAS_TEXTURE_COORDS) != 0
    }

    @JvmStatic
    fun altersLight(functionMask: Int): Boolean {
        return ((functionMask and (ModelFunctionMask.ALTERS_LIGHT or ModelFunctionMask.ALTERS_VERTICES)) == 544) or ((functionMask and (ModelFunctionMask.INVERT or ModelFunctionMask.DISPLACE_NORMALS)) != 0)
    }

    @JvmStatic
    fun hasElementsBuffer(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.HAS_TEXTURE_COORDS) != 0
    }

    @JvmStatic
    fun method2199(functionMask: Int, i_1: Int): Boolean {
        return hasColors(functionMask) && (((functionMask and 0xb000) != 0) or altersColor(functionMask) or altersAlpha(
            functionMask
        ) || ((i_1 and 0x37) == 0) and (altersLight(functionMask) or altersShading(functionMask)))
    }

    @JvmStatic
    fun method2200(): Boolean {
        return false
    }

    @JvmStatic
    fun hasTextureCoordinates(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.HAS_TEXTURE_COORDS) != 0
    }

    @JvmStatic
    fun method2202(functionMask: Int): Boolean {
        return ((functionMask and (ModelFunctionMask.SHADOWED or ModelFunctionMask.PROJECTION or ModelFunctionMask.SHADED)) != 0) or altersVerticesX(functionMask) || altersAnyVerticeType(
            functionMask
        )
    }

    @JvmStatic
    fun method2204(functionMask: Int, i_1: Int): Boolean {
        return ((functionMask and ModelFunctionMask.SHADED) != 0) or altersLight(functionMask) || method2261(
            functionMask,
            i_1
        ) || ((i_1 and 0x37) == 0) && method2199(functionMask, i_1)
    }

    @JvmStatic
    fun method2205(functionMask: Int, i_1: Int): Boolean {
        return altersColor(functionMask) || method2199(functionMask, i_1)
    }

    @JvmStatic
    fun method2206(functionMask: Int, i_1: Int): Boolean {
        return ((functionMask and ModelFunctionMask.SHADOWED) != 0) or altersAlpha(functionMask) || method2199(functionMask, i_1)
    }

    @JvmStatic
    fun method2211(functionMask: Int, i_1: Int): Boolean {
        return ((functionMask and (ModelFunctionMask.PROJECTION or ModelFunctionMask.SHADOWED)) != 0) or altersFacing(functionMask) || method2199(
            functionMask,
            i_1
        ) || copyElementsBuffer(functionMask)
    }

    @JvmStatic
    fun method2212(functionMask: Int, i_1: Int): Boolean {
        return ((functionMask and ModelFunctionMask.HAS_TEXTURE_COORDS) != 0) or altersTextures(functionMask) || method2199(
            functionMask,
            i_1
        )
    }

    @JvmStatic
    fun method2229(functionMask: Int): Boolean {
        return ((functionMask and 0x70000) != 0) or altersVerticesY(functionMask) || altersAnyVerticeType(functionMask)
    }

    @JvmStatic
    fun method2241(): Boolean {
        return method2200()
    }

    @JvmStatic
    fun method2261(functionMask: Int, i_1: Int): Boolean {
        return (((functionMask and ModelFunctionMask.ALTERS_CONTRAST) != 0) or altersLight(functionMask) or altersShading(functionMask)) and hasNormals(
            functionMask,
            i_1
        )
    }

    @JvmStatic
    fun altersBillboards(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.ALTERS_BILLBOARDS) != 0
    }

    @JvmStatic
    fun altersFaces(functionMask: Int): Boolean {
        return (functionMask and (ModelFunctionMask.ALTERS_ALPHA or ModelFunctionMask.ALTERS_COLOR)) != 0
    }

    @JvmStatic
    fun copyElementsBuffer(functionMask: Int): Boolean {
        return altersFacing(functionMask) and hasElementsBuffer(functionMask)
    }

    @JvmStatic
    fun method2252(functionMask: Int): Boolean {
        return ((functionMask and (ModelFunctionMask.SHADOWED or ModelFunctionMask.PROJECTION or ModelFunctionMask.SHADED)) != 0) or altersVerticesZ(functionMask) || altersAnyVerticeType(
            functionMask
        )
    }

    @JvmStatic
    fun altersAnyVerticeType(functionMask: Int): Boolean {
        return (altersVerticesX(functionMask) or
                altersVerticesY(functionMask) or
                altersVerticesZ(functionMask)) and
                hasVertices(functionMask)
    }

    @JvmStatic
    fun altersVertices(functionMask: Int): Boolean {
        return (functionMask and ModelFunctionMask.ALTERS_VERTICES) != 0
    }

    @JvmStatic
    fun altersVerticesX(functionMask: Int): Boolean {
        return (functionMask and (ModelFunctionMask.ALTERS_VERTICES or ModelFunctionMask.DISPLACE_X)) != 0
    }

    @JvmStatic
    fun altersVerticesY(functionMask: Int): Boolean {
        return (functionMask and (ModelFunctionMask.ALTERS_VERTICES or ModelFunctionMask.DISPLACE_Y)) != 0
    }

    @JvmStatic
    fun altersVerticesZ(functionMask: Int): Boolean {
        return (functionMask and (ModelFunctionMask.ALTERS_VERTICES or ModelFunctionMask.DISPLACE_Z or ModelFunctionMask.INVERT)) != 0
    }
}
